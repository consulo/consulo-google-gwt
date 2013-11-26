package com.intellij.gwt.ant;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.ModuleChunk;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.taskdefs.AntCall;
import com.intellij.compiler.ant.taskdefs.Param;
import com.intellij.compiler.ant.taskdefs.Property;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public class CompileGwtTarget extends Target {
  private CompileGwtTarget(@NotNull GwtFacet facet, final String depends, final String description, final ModuleChunk chunk) {
    super(GwtBuildProperties.getCompileGwtTargetName(facet), depends, description, null);
    Project project = facet.getModule().getProject();

    MultiValuesMap<String,GwtFacet> paths = GwtBuildExtension.getGwtSdkPaths(project);
    if (paths.keySet().size() == 1) {
      final String sdkHome = BuildProperties.propertyRef(GwtBuildProperties.getGwtSdkHomeProperty());
      add(new Property(GwtBuildProperties.getGwtSdkHomeProperty(facet), sdkHome));
    }

    @NonNls String outputDir = facet.getConfiguration().getCompilerOutputPath();
    if (StringUtil.isEmpty(outputDir)) {
      outputDir = BuildProperties.propertyRef(BuildProperties.getModuleChunkBasedirProperty(chunk)) + "/GWTCompilerOutput" + BuildProperties.convertName(facet.getModule().getName());
    }
    add(new Property(GwtBuildProperties.getGwtCompilerOutputPropertyName(facet), outputDir));
    
    GwtModulesManager modulesManager = GwtModulesManager.getInstance(project);
    for (GwtModule gwtModule : modulesManager.getGwtModules(facet.getModule())) {
      AntCall call = new AntCall(GwtBuildProperties.getRunGwtCompilerTargetName(facet));
      call.add(new Param(GwtBuildProperties.getGwtModuleParameter(), gwtModule.getQualifiedName()));
      add(call);
    }
  }

  public static CompileGwtTarget create(@NotNull GwtFacet facet, final GenerationOptions genOptions, final ModuleChunk chunk) {
    String depends = BuildProperties.getCompileTargetName(genOptions.getChunkByModule(facet.getModule()).getName());
    String description = GwtBundle.message("ant.target.description.compile.gwt.modules.in.module.0", facet.getModule().getName());
    return new CompileGwtTarget(facet, depends, description, chunk);
  }
}
