package com.intellij.gwt.ant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.google.gwt.module.extension.JavaEEGoogleGwtModuleExtension;
import com.intellij.compiler.ant.ChunkBuildExtension;
import com.intellij.compiler.ant.CompositeGenerator;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.ModuleChunk;
import com.intellij.compiler.ant.PropertyFileGenerator;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.util.ArrayUtil;

/**
 * @author nik
 */
public class GwtBuildExtension extends ChunkBuildExtension
{
	@Override
	@NotNull
	public String[] getTargets(final ModuleChunk chunk)
	{
		List<String> targets = new ArrayList<String>();
		for(Module module : chunk.getModules())
		{
			JavaEEGoogleGwtModuleExtension gwtFacet = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
			if(gwtFacet != null && gwtFacet.isRunGwtCompilerOnMake())
			{
				targets.add(GwtBuildProperties.getCompileGwtTargetName(gwtFacet));
			}
		}
		return ArrayUtil.toStringArray(targets);
	}

	@Override
	public void generateProperties(final PropertyFileGenerator generator, final Project project, final GenerationOptions options)
	{
		MultiValuesMap<String, JavaEEGoogleGwtModuleExtension> gwtSdkPaths = getGwtSdkPaths(project);
		Set<String> paths = gwtSdkPaths.keySet();
		if(paths.size() == 1)
		{
			generator.addProperty(GwtBuildProperties.getGwtSdkHomeProperty(), paths.iterator().next());
		}
		else
		{
			for(String path : gwtSdkPaths.keySet())
			{
				Collection<JavaEEGoogleGwtModuleExtension> facets = gwtSdkPaths.get(path);
				if(facets != null)
				{
					for(JavaEEGoogleGwtModuleExtension facet : facets)
					{
						generator.addProperty(GwtBuildProperties.getGwtSdkHomeProperty(facet), path);
					}
				}
			}
		}

		if(!paths.isEmpty())
		{
			generator.addProperty(GwtBuildProperties.getGwtSdkDevJarNameProperty(), GwtSdkUtil.getDevJarName());
		}
	}

	public static MultiValuesMap<String, JavaEEGoogleGwtModuleExtension> getGwtSdkPaths(final Project project)
	{
		MultiValuesMap<String, JavaEEGoogleGwtModuleExtension> gwtSdkPaths = new MultiValuesMap<String, JavaEEGoogleGwtModuleExtension>(true);
		Module[] modules = ModuleManager.getInstance(project).getModules();
		for(Module module : modules)
		{
			JavaEEGoogleGwtModuleExtension gwtFacet = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
			if(gwtFacet != null)
			{
				if(gwtFacet.isRunGwtCompilerOnMake())
				{
					Sdk sdk = gwtFacet.getSdk();
					if(sdk == null)
					{
						continue;
					}
					gwtSdkPaths.put(sdk.getHomePath(), gwtFacet);
				}
			}
		}
		return gwtSdkPaths;
	}

	@Override
	public void process(final Project project, final ModuleChunk chunk, final GenerationOptions genOptions, final CompositeGenerator generator)
	{
		Module[] modules = chunk.getModules();
		for(Module module : modules)
		{
			JavaEEGoogleGwtModuleExtension gwtFacet = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
			if(gwtFacet != null)
			{
				//Generator comment = new Generator(GwtBundle.message("ant.target.comment.run.gwt.compiler.for.gwt.module.0",
				//		BuildProperties.propertyRef(GwtBuildProperties.getGwtModuleParameter())));
				//generator.add(comment, 1);
				generator.add(new RunGwtCompilerTarget(gwtFacet, genOptions));
				generator.add(CompileGwtTarget.create(gwtFacet, genOptions, chunk), 1);
			}
		}
	}
}
