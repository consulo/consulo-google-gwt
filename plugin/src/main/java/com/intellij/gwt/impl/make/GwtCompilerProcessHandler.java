/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.impl.make;

import com.intellij.gwt.GwtBundle;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import consulo.application.ReadAction;
import consulo.compiler.CompileContext;
import consulo.compiler.CompilerMessageCategory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.collection.FactoryMap;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author nik
 */
public class GwtCompilerProcessHandler
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.make.GwtCompilerProcessHandler");
	@NonNls
	private static final String ANALYSING_SOURCES_PREFIX = "Analyzing source";
	@NonNls
	private static final String COPYING_PUBLIC_FILES_PREFIX = "Copying all files found on public path";
	@NonNls
	private static final String COMPILATION_START_PREFIX = "Output will be written into";
	@NonNls
	private static final String LOADING_INHERITED_PREFIX = "Loading inherited module";
	@NonNls
	private static final String FINDING_ENTRY_POINTS_PREFIX = "Finding entry point classes";
	@NonNls
	private static final String ERROR_PREFIX = "[ERROR] ";
	@NonNls
	private static final String WARNING_PREFIX = "[WARN] ";
	@NonNls
	private static final String WARNING_IN_STDERR_PREFIX = "WARNING:";
	@NonNls
	private static final String ERROR_FILE_PREFIX = "Errors in ";
	@NonNls
	private static final String ERROR_LINE_PREFIX = "Line ";
	@NonNls
	private static final String ERROR_LINE_SUFFIX = ": ";
	@NonNls
	private static final String BUILD_FAILED_MESSAGE = "Build failed";
	@NonNls
	private static final String STACKTRACE_PREFIX = "at ";
	@NonNls
	private static final Set<String> MODULE_FILE_ERRORS = new HashSet<>(Arrays.asList("Module has no entry points defined"));
	@NonNls
	private static final String[] CLASS_NAME_PREFIXES = {
			"Type ",
			"Return type: ",
			"Parameter: "
	};

	private final Map<Key, GwtCompilerOutputParser> myParsers = FactoryMap.create(key -> new GwtCompilerOutputParser(ProcessOutputTypes.STDERR.equals(key)));

	private final CompileContext myContext;
	private final String myModuleFileUrl;
	private final Module myModule;

	private final ProcessHandler myProcessHandler;

	public GwtCompilerProcessHandler(final GeneralCommandLine commandLine, final CompileContext context, final String moduleFileUrl, final Module module) throws ExecutionException
	{
		myProcessHandler = ProcessHandlerBuilder.create(commandLine).build();
		myProcessHandler.addProcessListener(new ProcessListener()
		{
			@Override
			public void onTextAvailable(ProcessEvent event, Key outputType)
			{
				String text = event.getText();

				if(outputType.equals(ProcessOutputTypes.STDERR) && text.contains(OutOfMemoryError.class.getName()))
				{
					myContext.addMessage(CompilerMessageCategory.ERROR, text.trim(), null, -1, -1);
					event.getProcessHandler().destroyProcess();
					return;
				}

				if(text.startsWith("Error: "))
				{
					myContext.addMessage(CompilerMessageCategory.ERROR, text, null, -1, -1);
				}

				if(outputType == ProcessOutputTypes.STDERR)
				{
					return;
				}
				myParsers.get(outputType).parseOutput(text);
			}
		});

		myContext = context;
		myModuleFileUrl = moduleFileUrl;
		myModule = module;
	}

	public void startNotify()
	{
		myProcessHandler.startNotify();
	}

	public void waitFor()
	{
		myProcessHandler.waitFor();
	}

	public Integer getExitCode()
	{
		return myProcessHandler.getExitCode();
	}

	private static String fixFileUrl(String url)
	{
		url = StringUtil.replace(url, "%20", " ");//todo[nik]
		if(url.contains(":/"))
		{
			return VirtualFileUtil.fixURLforIDEA(url);
		}
		return VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, FileUtil.toSystemIndependentName(url));
	}

	private class GwtCompilerOutputParser
	{
		private String myCurrentFileUrl;
		private boolean myFindingEntryPoints = false;
		private boolean myStackTraceExpected = false;
		private StringBuilder myBuffer = new StringBuilder();
		private final boolean myErrorStream;
		private boolean myCurrentMessageIsWarning;

		public GwtCompilerOutputParser(final boolean isErrorStream)
		{
			myErrorStream = isErrorStream;
		}

		public void parseOutput(final String text)
		{
			myBuffer.append(text);
			int start = 0;
			while(true)
			{
				int lineEnd1 = myBuffer.indexOf("\n", start);
				int lineEnd2 = myBuffer.indexOf("\r", start);
				if(lineEnd1 == -1 && lineEnd2 == -1)
				{
					break;
				}

				int lineEnd = lineEnd1 == -1 ? lineEnd2 : lineEnd2 == -1 ? lineEnd1 : Math.min(lineEnd1, lineEnd2);
				parseLine(myBuffer.substring(start, lineEnd).trim());
				start = lineEnd + 1;
			}

			myBuffer.delete(0, start);
		}

		private void parseLine(String line)
		{
			if(line.length() == 0)
			{
				return;
			}

			if(LOG.isDebugEnabled())
			{
				LOG.debug((myErrorStream ? "[stderr]" : "") + line);
			}

			if(line.startsWith(ERROR_FILE_PREFIX))
			{
				myStackTraceExpected = false;
				setCurrentFileUrl(line.substring(ERROR_FILE_PREFIX.length()));
			}
			else if(line.startsWith(WARNING_PREFIX))
			{
				String message = line.substring(WARNING_PREFIX.length());
				myContext.addMessage(CompilerMessageCategory.WARNING, message, myCurrentFileUrl, -1, -1);
			}
			else if(line.startsWith(ERROR_PREFIX))
			{
				myStackTraceExpected = false;
				boolean errorLineParsed = false;
				int start = ERROR_PREFIX.length();
				if(line.startsWith(ERROR_FILE_PREFIX, start))
				{
					start += ERROR_FILE_PREFIX.length();
					int first = line.indexOf('\'', start);
					int last = line.lastIndexOf('\'');
					if(first != -1 && last != -1)
					{
						setCurrentFileUrl(line.substring(first + 1, last));
						errorLineParsed = true;
					}
				}
				else if(line.startsWith(ERROR_LINE_PREFIX, start))
				{
					start += ERROR_LINE_PREFIX.length();
					final int end = line.indexOf(ERROR_LINE_SUFFIX, start);
					if(end != -1)
					{
						try
						{
							int lineNumber = Integer.parseInt(line.substring(start, end));
							String message = line.substring(end + ERROR_LINE_SUFFIX.length());
							myContext.addMessage(CompilerMessageCategory.ERROR, message, myCurrentFileUrl, lineNumber, 0);
							errorLineParsed = true;
						}
						catch(NumberFormatException e)
						{
						}
					}
				}
				else
				{
					line = line.substring(start);
				}

				if(MODULE_FILE_ERRORS.contains(line) || myFindingEntryPoints)
				{
					myContext.addMessage(CompilerMessageCategory.ERROR, line, myModuleFileUrl, -1, -1);
					errorLineParsed = true;
				}

				if(!errorLineParsed && !BUILD_FAILED_MESSAGE.equals(line))
				{
					myContext.addMessage(CompilerMessageCategory.ERROR, line, null, -1, -1);
					myStackTraceExpected = true;
				}
			}
			else if(line.startsWith(ANALYSING_SOURCES_PREFIX))
			{
				myContext.getProgressIndicator().setText(GwtBundle.message("progress.text.analyzing.sources"));
			}
			else if(line.startsWith(COPYING_PUBLIC_FILES_PREFIX))
			{
				myContext.getProgressIndicator().setText(GwtBundle.message("progress.text.copying.files.from.public.paths"));
			}
			else if(line.startsWith(COMPILATION_START_PREFIX))
			{
				myContext.getProgressIndicator().setText(GwtBundle.message("progress.text.compiling.sources"));
			}
			else if(line.startsWith(LOADING_INHERITED_PREFIX))
			{
				myContext.getProgressIndicator().setText(GwtBundle.message("progress.text.loading.inherited.modules"));
			}
			else if(line.startsWith(FINDING_ENTRY_POINTS_PREFIX))
			{
				myFindingEntryPoints = true;
			}
			else if(line.startsWith(STACKTRACE_PREFIX) && myStackTraceExpected)
			{
				myContext.addMessage(CompilerMessageCategory.ERROR, line, null, -1, -1);
			}
			else if(myErrorStream)
			{
				processStderrLine(line);
			}
			else
			{
				for(String prefix : CLASS_NAME_PREFIXES)
				{
					if(line.startsWith(prefix))
					{
						int start = prefix.length();
						int end = line.indexOf(' ', start);
						if(end == -1)
						{
							end = line.length();
						}
						setClassName(line.substring(start, end));
						break;
					}
				}
				myFindingEntryPoints = false;
			}
		}

		private void processStderrLine(String line)
		{
			CompilerMessageCategory category = CompilerMessageCategory.ERROR;
			if(line.startsWith(WARNING_IN_STDERR_PREFIX))
			{
				myCurrentMessageIsWarning = true;
				line = line.substring(WARNING_IN_STDERR_PREFIX.length());
				category = CompilerMessageCategory.WARNING;
			}
			else if(myCurrentMessageIsWarning)
			{
				category = CompilerMessageCategory.WARNING;
				myCurrentMessageIsWarning = false;
			}
			myContext.addMessage(category, line, null, -1, -1);
		}

		private void setClassName(final String className)
		{
			ReadAction.run(() ->
			{
				GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);
				PsiClass psiClass = JavaPsiFacade.getInstance(myModule.getProject()).findClass(className, scope);
				if(psiClass != null)
				{
					PsiFile psiFile = psiClass.getContainingFile();
					if(psiFile != null)
					{
						VirtualFile file = psiFile.getVirtualFile();
						if(file != null)
						{
							myCurrentFileUrl = file.getUrl();
						}
					}
				}
			});
		}

		private void setCurrentFileUrl(final String url)
		{
			myCurrentFileUrl = fixFileUrl(url);
		}

	}
}
