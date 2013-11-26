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

package com.intellij.gwt.junit;

import com.intellij.execution.JUnitPatcher;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.facet.FacetManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class GwtJUnitPatcher extends JUnitPatcher {
  @NonNls private static final String GWT_ARGS_PROPERTY = "gwt.args";

  public void patchJavaParameters(@Nullable Module module, JavaParameters javaParameters) {
    if (module == null) {
      return;
    }

    final GwtFacet facet = FacetManager.getInstance(module).getFacetByType(GwtFacetType.ID);
    if (facet == null) {
      return;
    }

    if (GwtModulesManager.getInstance(module.getProject()).getGwtModules(module).length > 0) {
      final PathsList classPath = javaParameters.getClassPath();
      for (VirtualFile file : ModuleRootManager.getInstance(module).getSourceRoots()) {
        classPath.addFirst(FileUtil.toSystemDependentName(file.getPath()));
      }
      classPath.addFirst(facet.getConfiguration().getSdk().getDevJarPath());
    }

    String testGenPath = GwtCompilerPaths.getTestGenDirectory(module).getAbsolutePath();
    String testOutputPath = GwtCompilerPaths.getTestOutputDirectory(module).getAbsolutePath();
    if (!SystemInfo.isWindows || !testGenPath.contains(" ") && !testOutputPath.contains(" ")) {
      //todo[nik] fix problem with paths containing spaces
      ParametersList vmParameters = javaParameters.getVMParametersList();
      @NonNls StringBuilder builder = new StringBuilder();
      String gwtArgs = vmParameters.getPropertyValue(GWT_ARGS_PROPERTY);
      if (gwtArgs != null) {
        builder.append(StringUtil.unquoteString(gwtArgs)).append(' ');
      }
      builder.append("-gen ").append(GeneralCommandLine.quote(testGenPath)).append(' ');
      builder.append("-out ").append(GeneralCommandLine.quote(testOutputPath));
      @NonNls String prefix = "-D" + GWT_ARGS_PROPERTY + "=";
      vmParameters.replaceOrAppend(prefix, prefix + builder);
    }
  }
}
