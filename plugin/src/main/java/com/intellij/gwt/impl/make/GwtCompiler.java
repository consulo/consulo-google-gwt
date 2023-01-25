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
import com.intellij.gwt.base.make.GwtCompilerPaths;
import com.intellij.gwt.base.make.GwtItemValidityState;
import com.intellij.gwt.base.make.GwtModuleFileProcessingItem;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.util.function.Processor;
import consulo.compiler.*;
import consulo.compiler.scope.CompileScope;
import consulo.compiler.util.CompilerUtil;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.OrderEnumerator;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersList;
import consulo.project.Project;
import consulo.util.collection.MultiValuesMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PathsList;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class GwtCompiler implements ClassInstrumentingCompiler
{
	private static final Logger LOG = Logger.getInstance(GwtCompiler.class);
	private Project myProject;
	private GwtModulesManager myGwtModulesManager;
	@NonNls
	public static final String LOG_LEVEL_ARGUMENT = "-logLevel";
	@NonNls
	public static final String GEN_AGRUMENT = "-gen";
	@NonNls
	public static final String STYLE_ARGUMENT = "-style";

	@Inject
	public GwtCompiler(Project project, GwtModulesManager modulesManager)
	{
		myProject = project;
		myGwtModulesManager = modulesManager;
	}

	@Override
	@Nonnull
	public String getDescription()
	{
		return GwtBundle.message("compiler.description.google.compiler");
	}

	@Override
	public boolean validateConfiguration(CompileScope scope)
	{
		return true;
	}

	@Override
	public ValidityState createValidityState(DataInput in) throws IOException
	{
		return new GwtItemValidityState(in);
	}

	@Override
	@Nonnull
	public ProcessingItem[] getProcessingItems(final CompileContext context)
	{
		final ArrayList<ProcessingItem> result = new ArrayList<ProcessingItem>();
		ApplicationManager.getApplication().runReadAction(new Runnable()
		{
			@Override
			public void run()
			{
				CompilerManager compilerConfiguration = CompilerManager.getInstance(myProject);
				final Module[] modules = context.getCompileScope().getAffectedModules();
				for(Module module : modules)
				{
					GoogleGwtModuleExtension extension = ModuleUtilCore.getExtension(module, GoogleGwtModuleExtension.class);
					if(extension == null || !extension.isRunGwtCompilerOnMake())
					{
						continue;
					}
					final GwtModule[] gwtModules = myGwtModulesManager.getGwtModules(module);
					for(GwtModule gwtModule : gwtModules)
					{
						/*if(myGwtModulesManager.isLibraryModule(gwtModule))
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("GWT module " + gwtModule.getQualifiedName() + " has not entry points and html files so it won't be " +
										"compiled.");
							}
							continue;
						}*/

						VirtualFile moduleFile = gwtModule.getModuleFile();
						if(compilerConfiguration.isExcludedFromCompilation(moduleFile))
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("GWT module '" + gwtModule.getQualifiedName() + "' is excluded from compilation.");
							}
							continue;
						}

						extension.addFilesForCompilation(gwtModule, result);
					}
				}
			}
		});
		return result.toArray(new ProcessingItem[result.size()]);
	}

	@Override
	public ProcessingItem[] process(final CompileContext context, ProcessingItem[] items)
	{
		MultiValuesMap<Pair<GoogleGwtModuleExtension, GwtModule>, GwtModuleFileProcessingItem> module2Items = new MultiValuesMap<Pair<GoogleGwtModuleExtension, GwtModule>,
				GwtModuleFileProcessingItem>();
		for(ProcessingItem item : items)
		{
			final GwtModuleFileProcessingItem processingItem = (GwtModuleFileProcessingItem) item;
			module2Items.put(Pair.create(processingItem.getFacet(), processingItem.getModule()), processingItem);
		}

		final ArrayList<ProcessingItem> compiled = new ArrayList<ProcessingItem>();

		for(Pair<GoogleGwtModuleExtension, GwtModule> pair : module2Items.keySet())
		{
			if(compile(context, pair.getFirst(), pair.getSecond()))
			{
				compiled.addAll(module2Items.get(pair));
			}
		}

		return compiled.toArray(new ProcessingItem[compiled.size()]);
	}

	private static boolean compile(final CompileContext context, final GoogleGwtModuleExtension extension, final GwtModule gwtModule)
	{
		final Ref<VirtualFile> gwtModuleFile = Ref.create(null);
		final Ref<File> outputDirRef = Ref.create(null);
		final Ref<String> gwtModuleName = Ref.create(null);
		final Module module = ReadAction.compute(() ->
		{
			gwtModuleName.set(gwtModule.getQualifiedName());
			gwtModuleFile.set(gwtModule.getModuleFile());
			outputDirRef.set(GwtCompilerPaths.getOutputDirectory(extension));
			return gwtModule.getModule();
		});

		final File generatedDir = GwtCompilerPaths.getDirectoryForGenerated(module);
		generatedDir.mkdirs();
		File outputDir = outputDirRef.get();
		outputDir.mkdirs();

		try
		{
			GwtLibraryPathProvider.Info pathInfo = GwtLibraryPathProvider.EP_NAME.computeSafeIfAny(it -> it.resolveInfo(extension));
			assert pathInfo != null;
			if(pathInfo.getDevJarPath() == null)
			{
				context.addMessage(CompilerMessageCategory.ERROR, "gwt-dev.jar is not resolved", null, -1, -1);
				return false;
			}

			OwnJavaParameters command = createCommand(extension, pathInfo, gwtModule, outputDir, generatedDir, gwtModuleName.get());
			GeneralCommandLine commandLine = command.toCommandLine();
			if(LOG.isDebugEnabled())
			{
				LOG.debug("GWT Compiler command line: " + commandLine.getCommandLineString());
			}
			commandLine.setWorkDirectory(outputDir);
			context.getProgressIndicator().setText(GwtBundle.message("progress.text.compiling.gwt.module.0", gwtModuleName.get()));

			GwtCompilerProcessHandler handler = new GwtCompilerProcessHandler(commandLine, context, gwtModuleFile.get().getUrl(), extension.getModule());
			handler.startNotify();
			handler.waitFor();
			Integer exitCode = handler.getExitCode();
			if(exitCode == null || exitCode != 0)
			{
				context.addMessage(CompilerMessageCategory.ERROR, "Compiler process exited with code: " + exitCode, null, -1, 1);
			}
		}
		catch(Exception e)
		{
			LOG.warn(e);
			context.addMessage(CompilerMessageCategory.ERROR, ExceptionUtil.getThrowableText(e), null, -1, -1);
			return false;
		}

		// we need this, due gwt compiler set nocache file timestamp equal to gwt module file, ignored other sources
		FileUtil.visitFiles(outputDir, new Processor<File>()
		{
			@Override
			public boolean process(File file)
			{
				if(StringUtil.endsWith(file.getName(), "nocache.js"))
				{
					file.setLastModified(System.currentTimeMillis());
					LOG.info("Updating timestamp for " + file.getPath());
					return true;
				}
				return true;
			}
		});

		CompilerUtil.refreshIODirectories(Collections.singletonList(outputDir));

		return context.getMessageCount(CompilerMessageCategory.ERROR) == 0;
	}

	@Nonnull
	private static OwnJavaParameters createCommand(GoogleGwtModuleExtension extension,
												   GwtLibraryPathProvider.Info pathInfo,
												   final GwtModule module,
												   final File outputDir,
												   final File generatedDir,
												   final String gwtModuleName)
	{
		final OwnJavaParameters javaParameters = new OwnJavaParameters();
		javaParameters.setJdk(ModuleUtilCore.getSdk(extension.getModule(), JavaModuleExtension.class));
		ParametersList vmParameters = javaParameters.getVMParametersList();
		vmParameters.addParametersString(extension.getAdditionalVmCompilerParameters());
		vmParameters.replaceOrAppend("-Xmx", "-Xmx" + extension.getCompilerMaxHeapSize() + "m");

		createClasspath(extension, pathInfo, module.getModule(), javaParameters.getClassPath());
		final GwtVersion sdkVersion = pathInfo.getVersion();
		javaParameters.setMainClass(sdkVersion.getCompilerClassName());
		ParametersList parameters = javaParameters.getProgramParametersList();
		String additionalCompilerParameters = extension.getAdditionalCompilerParameters();
		if(!StringUtil.isEmpty(additionalCompilerParameters))
		{
			parameters.add(additionalCompilerParameters);
		}
		parameters.add(LOG_LEVEL_ARGUMENT);
		parameters.add("TRACE");
		parameters.add(sdkVersion.getCompilerOutputDirParameterName());
		parameters.add(outputDir.getAbsolutePath());
		parameters.add(GEN_AGRUMENT);
		parameters.add(generatedDir.getAbsolutePath());
		parameters.add(STYLE_ARGUMENT);
		parameters.add(extension.getOutputStyle().getId());
		parameters.add(gwtModuleName);
		return javaParameters;
	}

	private static void createClasspath(@Nonnull GoogleGwtModuleExtension extension, GwtLibraryPathProvider.Info pathInfo, Module module, final PathsList classPath)
	{
		OrderEnumerator orderEnumerator = ModuleRootManager.getInstance(module).orderEntries();

		classPath.addVirtualFiles(orderEnumerator.recursively().classes().getRoots());
		classPath.addVirtualFiles(orderEnumerator.sources().getRoots());

		extension.setupCompilerClasspath(classPath);

		classPath.addFirst(pathInfo.getDevJarPath());
		List<String> additionalClasspath = pathInfo.getAdditionalClasspath();
		for(String path : additionalClasspath)
		{
			classPath.add(path);
		}
	}
}
