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

package com.intellij.gwt.impl.i18n;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class GwtI18nManager
{
	public static GwtI18nManager getInstance(Project project)
	{
		return ServiceManager.getService(project, GwtI18nManager.class);
	}

	@Nonnull
	@RequiredReadAction
	public abstract PropertiesFile[] getPropertiesFiles(@Nonnull PsiClass anInterface);

	@Nullable
	public abstract PsiClass getPropertiesInterface(@Nonnull PropertiesFile file);

	@Nonnull
	@RequiredReadAction
	public abstract IProperty[] getProperties(@Nonnull PsiMethod method);

	@Nullable
	public abstract PsiMethod getMethod(@Nonnull IProperty property);

	public abstract boolean isConstantsInterface(@Nonnull PsiClass aClass);

	public abstract boolean isLocalizableInterface(@Nonnull PsiClass aClass);
}
