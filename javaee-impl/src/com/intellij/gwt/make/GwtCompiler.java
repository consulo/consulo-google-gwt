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

package com.intellij.gwt.make;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.consulo.java.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.google.gwt.module.extension.JavaEEGoogleGwtModuleExtension;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.execution.configurations.CommandLineBuilder;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.ClassInstrumentingCompiler;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootsEnumerator;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;

public class GwtCompiler implements ClassInstrumentingCompiler
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.make.GwtCompiler");
	private Project myProject;
	private GwtModulesManager myGwtModulesManager;
	@NonNls
	public static final String LOG_LEVEL_ARGUMENT = "-logLevel";
	@NonNls
	public static final String GEN_AGRUMENT = "-gen";
	@NonNls
	public static final String STYLE_ARGUMENT = "-style";

	public GwtCompiler(Project project, GwtModulesManager modulesManager)
	{
		myProject = project;
		myGwtModulesManager = modulesManager;
	}

	@Override
	@NotNull
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
	public void init(@NotNull CompilerManager compilerManager)
	{

	}

	@Override
	public ValidityState createValidityState(DataInput in) throws IOException
	{
		return new GwtItemValidityState(in);
	}

	@Override
	@NotNull
	public ProcessingItem[] getProcessingItems(final CompileContext context)
	{
		final ArrayList<ProcessingItem> result = new ArrayList<ProcessingItem>();
		ApplicationManager.getApplication().runReadAction(new Runnable()
		{
			@Override
			public void run()
			{
				CompilerManager compilerConfiguration = CompilerManager.getInstance(myProject);
				RunConfiguration runConfiguration = CompileStepBeforeRun.getRunConfiguration(context);
				final Module[] modules = context.getCompileScope().getAffectedModules();
				for(Module module : modules)
				{
					JavaEEGoogleGwtModuleExtension facet = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
					if(facet == null || !facet.isRunGwtCompilerOnMake())
					{
						continue;
					}

				/*	if(runConfiguration != null)
					{
						WebFacet webFacet = facet.getWebFacet();
						if(!(runConfiguration instanceof CommonModel) || webFacet == null)
						{
							continue;
						}

						final CommonModel commonModel = (CommonModel) runConfiguration;
						DeploymentModel model = commonModel.getDeploymentModel(webFacet);
						if(model == null || !DeploymentManager.getInstance(myProject).isModuleDeployedOrIncludedInDeployed(model))
						{
							continue;
						}
					}    */

					final GwtModule[] gwtModules = myGwtModulesManager.getGwtModules(module);
					for(GwtModule gwtModule : gwtModules)
					{
						if(myGwtModulesManager.isLibraryModule(gwtModule))
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("GWT module " + gwtModule.getQualifiedName() + " has not entry points and html files so it won't be " +
										"compiled.");
							}
							continue;
						}

						VirtualFile moduleFile = gwtModule.getModuleFile();
						if(compilerConfiguration.isExcludedFromCompilation(moduleFile))
						{
							if(LOG.isDebugEnabled())
							{
								LOG.debug("GWT module '" + gwtModule.getQualifiedName() + "' is excluded from compilation.");
							}
							continue;
						}

						addFilesRecursively(gwtModule, facet, moduleFile, result);

						for(VirtualFile file : gwtModule.getPublicRoots())
						{
							addFilesRecursively(gwtModule, facet, file, result);
						}
						for(VirtualFile file : gwtModule.getSourceRoots())
						{
							addFilesRecursively(gwtModule, facet, file, result);
						}
					}
				}
			}
		});
		return result.toArray(new ProcessingItem[result.size()]);
	}

	private static void addFilesRecursively(final GwtModule module, JavaEEGoogleGwtModuleExtension facet, final VirtualFile file,
			final List<ProcessingItem> result)
	{
		if(!file.isValid() || FileTypeManager.getInstance().isFileIgnored(file.getName()))
		{
			return;
		}

		if(file.isDirectory())
		{
			final VirtualFile[] children = file.getChildren();
			for(VirtualFile child : children)
			{
				addFilesRecursively(module, facet, child, result);
			}
		}
		else
		{
			result.add(new GwtModuleFileProcessingItem(facet, module, file));
		}
	}

	@Override
	public ProcessingItem[] process(final CompileContext context, ProcessingItem[] items)
	{
		MultiValuesMap<Pair<JavaEEGoogleGwtModuleExtension, GwtModule>, GwtModuleFileProcessingItem> module2Items = new
				MultiValuesMap<Pair<JavaEEGoogleGwtModuleExtension, GwtModule>, GwtModuleFileProcessingItem>();
		for(ProcessingItem item : items)
		{
			final GwtModuleFileProcessingItem processingItem = (GwtModuleFileProcessingItem) item;
			module2Items.put(Pair.create(processingItem.getFacet(), processingItem.getModule()), processingItem);
		}

		final ArrayList<ProcessingItem> compiled = new ArrayList<ProcessingItem>();

		for(Pair<JavaEEGoogleGwtModuleExtension, GwtModule> pair : module2Items.keySet())
		{
			if(compile(context, pair.getFirst(), pair.getSecond()))
			{
				compiled.addAll(module2Items.get(pair));
			}
		}

		return compiled.toArray(new ProcessingItem[compiled.size()]);
	}

	private static boolean compile(final CompileContext context, final JavaEEGoogleGwtModuleExtension facet, final GwtModule gwtModule)
	{
		final Ref<VirtualFile> gwtModuleFile = Ref.create(null);
		final Ref<File> outputDirRef = Ref.create(null);
		final Ref<String> gwtModuleName = Ref.create(null);
		final Module module = new ReadAction<Module>()
		{
			@Override
			protected void run(final Result<Module> result)
			{
				gwtModuleName.set(gwtModule.getQualifiedName());
				gwtModuleFile.set(gwtModule.getModuleFile());
				outputDirRef.set(GwtCompilerPaths.getOutputDirectory(facet));
				result.setResult(gwtModule.getModule());
			}
		}.execute().getResultObject();

		final File generatedDir = GwtCompilerPaths.getDirectoryForGenerated(module);
		generatedDir.mkdirs();
		File outputDir = outputDirRef.get();
		outputDir.mkdirs();

		try
		{
			GeneralCommandLine commandLine = CommandLineBuilder.createFromJavaParameters(createCommand(facet, gwtModule, outputDir, generatedDir,
					gwtModuleName.get()));
			if(LOG.isDebugEnabled())
			{
				LOG.debug("GWT Compiler command line: " + commandLine.getCommandLineString());
			}
			commandLine.setWorkDirectory(outputDir);
			context.getProgressIndicator().setText2(GwtBundle.message("progress.text.compiling.gwt.module.0", gwtModuleName.get()));

			GwtCompilerProcessHandler handler = new GwtCompilerProcessHandler(commandLine.createProcess(), context, gwtModuleFile.get().getUrl(),
					facet.getModule());
			handler.startNotify();
			handler.waitFor();
		}
		catch(Exception e)
		{
			LOG.info(e);
			context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
			return false;
		}

		CompilerUtil.refreshIODirectories(Collections.singletonList(outputDir));

		return context.getMessageCount(CompilerMessageCategory.ERROR) == 0;
	}

	private static JavaParameters createCommand(JavaEEGoogleGwtModuleExtension facet, final GwtModule module, final File outputDir,
			final File generatedDir, final String gwtModuleName)
	{
		final JavaParameters javaParameters = new JavaParameters();
		javaParameters.setJdk(ModuleUtilCore.getSdk(facet.getModule(), JavaModuleExtension.class));
		ParametersList vmParameters = javaParameters.getVMParametersList();
		vmParameters.addParametersString(facet.getAdditionalCompilerParameters());
		vmParameters.replaceOrAppend("-Xmx", "-Xmx" + facet.getCompilerMaxHeapSize() + "m");

		createClasspath(facet, module.getModule(), javaParameters.getClassPath());
		final GwtVersion sdkVersion = facet.getSdkVersion();
		javaParameters.setMainClass(sdkVersion.getCompilerClassName());
		ParametersList parameters = javaParameters.getProgramParametersList();
		parameters.add(LOG_LEVEL_ARGUMENT);
		parameters.add("TRACE");
		parameters.add(sdkVersion.getCompilerOutputDirParameterName());
		parameters.add(outputDir.getAbsolutePath());
		parameters.add(GEN_AGRUMENT);
		parameters.add(generatedDir.getAbsolutePath());
		parameters.add(STYLE_ARGUMENT);
		parameters.add(facet.getOutputStyle().getId());
		parameters.add(gwtModuleName);
		return javaParameters;
	}

	private static void createClasspath(final JavaEEGoogleGwtModuleExtension facet, Module module, final PathsList classPath)
	{
		OrderEnumerator orderEnumerator = ModuleRootManager.getInstance(module).orderEntries();
		OrderRootsEnumerator classes = orderEnumerator.recursively().classes();

		/*ProjectRootsTraversing.collectRoots(module, new ProjectRootsTraversing.RootTraversePolicy(ProjectRootsTraversing.RootTraversePolicy
				.PRODUCTION_SOURCES, ProjectRootsTraversing.RootTraversePolicy.ADD_CLASSES, ProjectRootsTraversing.RootTraversePolicy.ADD_CLASSES,
				ProjectRootsTraversing.RootTraversePolicy.RECURSIVE), classPath);

		ProjectRootsTraversing.collectRoots(module, new ProjectRootsTraversing.RootTraversePolicy(ProjectClasspathTraversing.GENERAL_OUTPUT, null,
				null, ProjectRootsTraversing.RootTraversePolicy.RECURSIVE), classPath); */

		classPath.addVirtualFiles(classes.getRoots());
		classPath.addFirst(GwtSdkUtil.getDevJarPath(facet.getSdk()));
	}
}