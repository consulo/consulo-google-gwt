package com.intellij.gwt.module;

import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiFile;
import com.intellij.psi.css.inspections.UnusedCssSymbolInspectionExclusion;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;

/**
 * @author nik
 */
public class GwtUnusedCssSymbolInspectionExclusion extends UnusedCssSymbolInspectionExclusion {
  public boolean ignoreFile(@NotNull PsiFile file) {
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile != null) {
      final Project project = file.getProject();
      final Module module = ModuleUtil.findModuleForFile(virtualFile, project);
      if (module != null && GwtFacet.getInstance(module) != null) {
        final GwtModulesManager modulesManager = GwtModulesManager.getInstance(project);
        final GwtModule gwtModule = modulesManager.findGwtModuleByClientOrPublicFile(virtualFile);
        if (gwtModule != null) {
          return true;
        }
      }
    }
    return false;
  }
}
