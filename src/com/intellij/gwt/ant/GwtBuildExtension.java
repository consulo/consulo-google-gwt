package com.intellij.gwt.ant;

import com.intellij.compiler.ant.*;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class GwtBuildExtension extends ChunkBuildExtension {
  public boolean haveSelfOutputs(final Module[] modules) {
    for (Module module : modules) {
      if (GwtFacet.getInstance(module) != null) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public String[] getTargets(final ModuleChunk chunk) {
    List<String> targets = new ArrayList<String>();
    for (Module module : chunk.getModules()) {
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet != null && gwtFacet.getConfiguration().isRunGwtCompilerOnMake()) {
        targets.add(GwtBuildProperties.getCompileGwtTargetName(gwtFacet));
      }
    }
    return ArrayUtil.toStringArray(targets);
  }

  @Override
  public void generateProperties(final PropertyFileGenerator generator, final Project project, final GenerationOptions options) {
    MultiValuesMap<String, GwtFacet> gwtSdkPaths = getGwtSdkPaths(project);
    Set<String> paths = gwtSdkPaths.keySet();
    if (paths.size() == 1) {
      generator.addProperty(GwtBuildProperties.getGwtSdkHomeProperty(), paths.iterator().next());
    }
    else {
      for (String path : gwtSdkPaths.keySet()) {
        Collection<GwtFacet> facets = gwtSdkPaths.get(path);
        if (facets != null) {
          for (GwtFacet facet : facets) {
            generator.addProperty(GwtBuildProperties.getGwtSdkHomeProperty(facet), path);
          }
        }
      }
    }

    if (!paths.isEmpty()) {
      generator.addProperty(GwtBuildProperties.getGwtSdkDevJarNameProperty(), GwtSdkUtil.getDevJarName());
    }
  }

  public static MultiValuesMap<String, GwtFacet> getGwtSdkPaths(final Project project) {
    MultiValuesMap<String, GwtFacet> gwtSdkPaths = new MultiValuesMap<String, GwtFacet>(true);
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet != null) {
        GwtFacetConfiguration configuration = gwtFacet.getConfiguration();
        if (configuration.isRunGwtCompilerOnMake()) {
          gwtSdkPaths.put(VfsUtil.urlToPath(configuration.getSdk().getHomeDirectoryUrl()), gwtFacet);
        }
      }
    }
    return gwtSdkPaths;
  }

  public void process(final Project project, final ModuleChunk chunk, final GenerationOptions genOptions, final CompositeGenerator generator) {
    Module[] modules = chunk.getModules();
    for (Module module : modules) {
      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet != null) {
        Comment comment = new Comment(
          GwtBundle.message("ant.target.comment.run.gwt.compiler.for.gwt.module.0",
                            BuildProperties.propertyRef(GwtBuildProperties.getGwtModuleParameter())));
        generator.add(comment, 1);
        generator.add(new RunGwtCompilerTarget(gwtFacet, genOptions));
        generator.add(CompileGwtTarget.create(gwtFacet, genOptions, chunk), 1);
      }
    }
  }
}
