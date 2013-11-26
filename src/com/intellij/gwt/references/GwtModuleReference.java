package com.intellij.gwt.references;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public abstract class GwtModuleReference<T extends PsiElement> extends PsiReferenceBase<T> {
  private GwtModulesManager myGwtModulesManager;

  public GwtModuleReference(T element) {
    super(element);
    myGwtModulesManager = GwtModulesManager.getInstance(element.getProject());
  }

  @Nullable
  public Module getModule() {
    return ModuleUtil.findModuleForPsiElement(myElement);
  }

  public Object[] getVariants() {
    List<String> names = new ArrayList<String>();
    for (GwtModule module : myGwtModulesManager.getAllGwtModules()) {
      names.add(module.getQualifiedName());
    }
    return ArrayUtil.toStringArray(names);
  }

  @Nullable
  public PsiElement resolve() {
    String moduleName = getStringValue();
    if (moduleName != null) {
      final Module module = getModule();
      final GlobalSearchScope scope = module != null ? GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module) : GlobalSearchScope.allScope(myElement.getProject());
      final GwtModule gwtModule = myGwtModulesManager.findGwtModuleByName(moduleName, scope);
      if (gwtModule != null) {
        return gwtModule.getXmlTag();
      }
    }
    return null;
  }

  @Nullable
  protected abstract String getStringValue();
}
