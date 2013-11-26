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

package com.intellij.gwt.i18n;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public abstract class GwtI18nManager {

  public static GwtI18nManager getInstance(Project project) {
    return ServiceManager.getService(project, GwtI18nManager.class);
  }

  @NotNull
  public abstract PropertiesFile[] getPropertiesFiles(@NotNull PsiClass anInterface);

  @Nullable
  public abstract PsiClass getPropertiesInterface(@NotNull PropertiesFile file);

  @NotNull
  public abstract Property[] getProperties(@NotNull PsiMethod method);

  @Nullable
  public abstract PsiMethod getMethod(@NotNull Property property);

  public abstract boolean isConstantsInterface(@NotNull PsiClass aClass);

  public abstract boolean isLocalizableInterface(@NotNull PsiClass aClass);
}
