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

import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;

/**
 * @author nik
 */
public abstract class GwtModulesManager
{
	@NonNls
	public static final String GWT_SUFFIX = ".gwt";
	@NonNls
	public static final String GWT_XML_SUFFIX = GWT_SUFFIX + ".xml";
	@NonNls
	public static final String DEFAULT_PUBLIC_PATH = "public";
	@NonNls
	public static final String DEFAULT_SOURCE_PATH = "client";

	public static GwtModulesManager getInstance(@Nonnull Project project)
	{
		return ServiceManager.getService(project, GwtModulesManager.class);
	}

	@Nonnull
	public abstract GwtModule[] getAllGwtModules();

	@Nonnull
	public abstract GwtModule[] getGwtModules(@Nonnull Module module);

	@Nonnull
	public abstract List<GwtModule> findGwtModulesByClientSourceFile(@Nonnull VirtualFile file);

	@Nullable
	public abstract GwtModule findGwtModuleByClientSourceFile(@Nonnull VirtualFile file);

	@Nullable
	public abstract GwtModule findGwtModuleByClientOrPublicFile(@Nonnull VirtualFile file);

	@Nullable
	public abstract GwtModule findGwtModuleByName(final @Nonnull String qualifiedName, final GlobalSearchScope scope);

	@Nonnull
	public abstract List<GwtModule> findModulesByClass(@Nonnull PsiElement context, final @Nullable String className);

	@Nullable
	public abstract GwtModule findGwtModuleByEntryPoint(@Nonnull PsiClass psiClass);


	@Nonnull
	public abstract List<Pair<GwtModule, String>> findGwtModulesByPublicFile(@Nonnull VirtualFile file);

	@Nullable
	public abstract String getPathFromPublicRoot(@Nonnull GwtModule gwtModule, @Nonnull VirtualFile file);

	@Nullable
	public abstract XmlFile findHtmlFileByModule(@Nonnull GwtModule module);


	@Nullable
	public abstract PsiElement findTagById(@Nonnull XmlFile htmlFile, String id);

	public abstract String[] getAllIds(@Nonnull XmlFile htmlFile);

	public abstract boolean isGwtModuleFile(final VirtualFile file);

	public abstract boolean isInheritedOrSelf(GwtModule gwtModule, GwtModule inheritedModule);

	public abstract boolean isInheritedOrSelf(GwtModule gwtModule, List<GwtModule> referencedModules);

	public abstract boolean isLibraryModule(GwtModule module);

	public abstract boolean isUnderGwtModule(final VirtualFile virtualFile);

	@Nullable
	public abstract GwtModule getGwtModuleByXmlFile(@Nonnull PsiFile file);

}
