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

package com.intellij.gwt.module;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.CssFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public abstract class GwtModulesManager  {
  @NonNls public static final String GWT_SUFFIX = ".gwt";
  @NonNls public static final String GWT_XML_SUFFIX = GWT_SUFFIX + ".xml";
  @NonNls public static final String DEFAULT_PUBLIC_PATH = "public";
  @NonNls public static final String DEFAULT_SOURCE_PATH = "client";

  public static GwtModulesManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GwtModulesManager.class);
  }

  @NotNull
  public abstract GwtModule[] getAllGwtModules();

  @NotNull
  public abstract GwtModule[] getGwtModules(@NotNull Module module);

  @NotNull
  public abstract List<GwtModule> findGwtModulesByClientSourceFile(@NotNull VirtualFile file);

  @Nullable
  public abstract GwtModule findGwtModuleByClientSourceFile(@NotNull VirtualFile file);

  @Nullable
  public abstract GwtModule findGwtModuleByClientOrPublicFile(@NotNull VirtualFile file);

  @Nullable
  public abstract GwtModule findGwtModuleByName(final @NotNull String qualifiedName, final GlobalSearchScope scope);

  @NotNull
  public abstract List<GwtModule> findModulesByClass(@NotNull PsiElement context, final @Nullable String className);

  @Nullable
  public abstract GwtModule findGwtModuleByEntryPoint(@NotNull PsiClass psiClass);


  @NotNull
  public abstract List<Pair<GwtModule, String>> findGwtModulesByPublicFile(@NotNull  VirtualFile file);

  @Nullable
  public abstract String getPathFromPublicRoot(@NotNull GwtModule gwtModule, @NotNull VirtualFile file);

  @Nullable
  public abstract XmlFile findHtmlFileByModule(@NotNull GwtModule module);


  @Nullable
  public abstract PsiElement findTagById(@NotNull XmlFile htmlFile, String id);

  public abstract String[] getAllIds(@NotNull XmlFile htmlFile);


  @Nullable
  public abstract CssClass findCssDeclarationByClass(GwtModule module, String cssClass);

  public abstract String[] getAllCssClassNames(final GwtModule module);

  @Nullable
  public abstract CssFile findPreferableCssFile(final GwtModule module);

  public abstract boolean isGwtModuleFile(final VirtualFile file);

  public abstract boolean isInheritedOrSelf(GwtModule gwtModule, GwtModule inheritedModule);

  public abstract boolean isInheritedOrSelf(GwtModule gwtModule, List<GwtModule> referencedModules);

  public abstract boolean isLibraryModule(GwtModule module);

  public abstract boolean isUnderGwtModule(final VirtualFile virtualFile);

  @Nullable
  public abstract GwtModule getGwtModuleByXmlFile(@NotNull PsiFile file);

}
