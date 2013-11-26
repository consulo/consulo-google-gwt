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

package com.intellij.gwt.inspections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;

/**
 * @author nik
 */
public abstract class BaseGwtInspection extends BaseJavaLocalInspectionTool
{
	protected static boolean shouldCheck(@NotNull PsiElement psiElement)
	{
		return getFacet(psiElement) != null;
	}

	protected static boolean hasGwtFacets(@NotNull Project project)
	{
		for(Module module : ModuleManager.getInstance(project).getModules())
		{
			if(GwtFacet.getInstance(module) != null)
			{
				return true;
			}
		}
		return false;
	}

	@Nullable
	protected static GwtFacet getFacet(@NotNull PsiElement psiElement)
	{
		return GwtFacet.findFacetBySourceFile(psiElement.getProject(), psiElement.getContainingFile().getVirtualFile());
	}

	@NotNull
	protected static PsiElement getElementToHighlight(@NotNull PsiClass psiClass)
	{
		PsiIdentifier identifier = psiClass.getNameIdentifier();
		return identifier != null ? identifier : psiClass;
	}

	@NotNull
	protected static PsiElement getElementToHighlight(@NotNull PsiMethod psiMethod)
	{
		PsiIdentifier identifier = psiMethod.getNameIdentifier();
		return identifier != null ? identifier : psiMethod;
	}

	@Override
	@NotNull
	public String getGroupDisplayName()
	{
		return GwtBundle.message("group.gwt.inspections.name");
	}

	@Override
	@NotNull
	public HighlightDisplayLevel getDefaultLevel()
	{
		return HighlightDisplayLevel.ERROR;
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
	}
}
