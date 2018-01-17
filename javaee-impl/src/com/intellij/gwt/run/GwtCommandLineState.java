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

package com.intellij.gwt.run;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SystemProperties;
import consulo.gwt.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.java.execution.configurations.OwnJavaParameters;

/**
 * @author nik
 */
public class GwtCommandLineState extends JavaCommandLineState
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.run.GwtCommandLineState");
	@NonNls
	private static final String WEB_XML_PATH = "com/google/gwt/dev/etc/tomcat/webapps/ROOT/WEB-INF/web.xml";
	private final Module myModule;
	private String myRunPage;
	private String myVMParameters;
	private JavaEEGoogleGwtModuleExtension myModuleExtension;
	private final String myShellParameters;
	private final String myCustomWebXmlUrl;
	private final GwtLibraryPathProvider.Info myLibraryPathInfo;

	public GwtCommandLineState(final JavaEEGoogleGwtModuleExtension moduleExtension,
			final ExecutionEnvironment environment,
			final String runPage,
			final String vmParameters,
			final String shellParameters,
			final String customWebXmlUrl)
	{
		super(environment);
		myModuleExtension = moduleExtension;
		myShellParameters = shellParameters;
		myCustomWebXmlUrl = customWebXmlUrl;
		myModule = myModuleExtension.getModule();
		myRunPage = runPage;
		myVMParameters = vmParameters;
		myLibraryPathInfo = GwtLibraryPathProvider.EP_NAME.composite().resolveInfo(myModuleExtension);
		assert myLibraryPathInfo != null;
	}

	@Override
	protected OwnJavaParameters createJavaParameters() throws ExecutionException
	{
		final OwnJavaParameters params = new OwnJavaParameters();

		params.setWorkingDirectory(getTempOutputDir());

		params.configureByModule(myModule, OwnJavaParameters.JDK_AND_CLASSES);

		if(SystemInfo.isMac)
		{
			params.getVMParametersList().add("-XstartOnFirstThread");
		}
		params.getVMParametersList().addParametersString(myVMParameters);

		if(myLibraryPathInfo.getDevJarPath() == null)
		{
			throw new ExecutionException("gwt-dev.jar is not found");
		}

		final GwtVersion sdkVersion = myLibraryPathInfo.getVersion();
		final ParametersList programParameters = params.getProgramParametersList();
		programParameters.add("-style");
		programParameters.add(myModuleExtension.getOutputStyle().getId());
		programParameters.add("-out");
		programParameters.add(getOutputPath().getAbsolutePath());
		programParameters.add("-gen");
		programParameters.add(getGenPath().getAbsolutePath());
		programParameters.addParametersString(myShellParameters);
		programParameters.add(myRunPage);

		OrderEnumerator orderEnumerator = ModuleRootManager.getInstance(myModule).orderEntries();

		VirtualFile[] roots = orderEnumerator.recursively().sources().getRoots();

		for(VirtualFile path : roots)
		{
			params.getClassPath().add(path);
		}

		params.getClassPath().addFirst(myLibraryPathInfo.getDevJarPath());
		params.setMainClass(sdkVersion.getShellClassName());

		return params;
	}

	private File getGenPath()
	{
		return new File(getTempOutputDir(), "gen");
	}

	private File getOutputPath()
	{
		return new File(getTempOutputDir(), "www");
	}

	private File getTempOutputDir()
	{
		return new File(GwtCompilerPaths.getOutputRoot(myModule), "run");
	}

	@NotNull
	@Override
	public ExecutionResult execute(@NotNull final Executor executor, @NotNull final ProgramRunner runner) throws ExecutionException
	{
		getOutputPath().mkdirs();
		getGenPath().mkdirs();

		final File outputDir = getTempOutputDir();
		if(myCustomWebXmlUrl != null)
		{
			File targetWebXml = new File(outputDir.getAbsolutePath() + "/tomcat/webapps/ROOT/WEB-INF/web.xml".replace('/', File.separatorChar));
			try
			{
				targetWebXml.getParentFile().mkdirs();
				FileUtil.copy(new File(FileUtil.toSystemDependentName(VfsUtil.urlToPath(myCustomWebXmlUrl))), targetWebXml);
				patchWebXml(targetWebXml);
			}
			catch(IOException e)
			{
				LOG.info(e);
			}
			catch(JDOMException e)
			{
				LOG.info(e);
			}
		}

		final ExecutionResult result = super.execute(executor, runner);
		result.getProcessHandler().addProcessListener(new ProcessAdapter()
		{
			@Override
			public void processTerminated(final ProcessEvent event)
			{
				FileUtil.delete(outputDir);
			}
		});
		return result;
	}

	private void patchWebXml(final File webXml) throws IOException, JDOMException
	{
		String devJarPath = myLibraryPathInfo.getDevJarPath();
		assert devJarPath != null;
		File devJar = new File(devJarPath);
		if(!devJar.exists())
		{
			return;
		}
		ZipFile zipFile = new ZipFile(devJar);
		try
		{
			ZipEntry zipEntry = zipFile.getEntry(WEB_XML_PATH);
			InputStream input = zipFile.getInputStream(zipEntry);
			Document document = JDOMUtil.loadDocument(input);

			Document target = JDOMUtil.loadDocument(webXml);
			final Element targetRoot = target.getRootElement();
			new ReadAction()
			{
				@Override
				protected void run(final Result result)
				{
					deleteGwtServlets(targetRoot);
				}
			}.execute();

			//noinspection unchecked
			for(Element child : (List<Element>) document.getRootElement().getChildren())
			{
				Element copy = (Element) child.clone();
				copy.setNamespace(targetRoot.getNamespace());
				//noinspection unchecked
				for(Element grandChild : (List<Element>) copy.getChildren())
				{
					grandChild.setNamespace(targetRoot.getNamespace());
				}
				targetRoot.addContent(copy);
			}
			JDOMUtil.writeDocument(target, webXml, SystemProperties.getLineSeparator());
		}
		finally
		{
			zipFile.close();
		}
	}

	private void deleteGwtServlets(final Element root)
	{
		JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myModule.getProject());
		GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);

		List<Element> toRemove = new ArrayList<Element>();
		Set<String> servletNamesToRemove = new HashSet<String>();

		//noinspection unchecked
		for(Element servlet : (List<Element>) root.getChildren("servlet", root.getNamespace()))
		{
			String className = servlet.getChildTextTrim("servlet-class", root.getNamespace());
			if(className != null)
			{
				PsiClass psiClass = psiFacade.findClass(className, scope);
				if(RemoteServiceUtil.isRemoteServiceImplementation(psiClass))
				{
					toRemove.add(servlet);
					servletNamesToRemove.add(servlet.getChildTextTrim("servlet-name", root.getNamespace()));
				}
			}
		}

		//noinspection unchecked
		for(Element mapping : ((List<Element>) root.getChildren("servlet-mapping", root.getNamespace())))
		{
			String servletName = mapping.getChildTextTrim("servlet-name", root.getNamespace());
			if(servletNamesToRemove.contains(servletName))
			{
				toRemove.add(mapping);
			}
		}

		for(Element element : toRemove)
		{
			root.removeContent(element);
		}
	}
}
