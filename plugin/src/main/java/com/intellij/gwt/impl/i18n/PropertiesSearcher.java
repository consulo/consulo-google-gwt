/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.psi.search.DefinitionsScopedSearchExecutor;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author peter
 */
@ExtensionImpl
public class PropertiesSearcher implements DefinitionsScopedSearchExecutor {
    @Override
    public boolean execute(
        @Nonnull DefinitionsScopedSearch.SearchParameters queryParameters,
        @Nonnull Predicate<? super PsiElement> consumer
    ) {
        final PsiElement sourceElement = queryParameters.getElement();
        if (sourceElement instanceof PsiMethod method) {
            IProperty[] properties = Application.get().runReadAction(
                (Supplier<IProperty[]>)() -> GwtI18nManager.getInstance(method.getProject()).getProperties(method)
            );
            for (IProperty property : properties) {
                if (!consumer.test(property.getPsiElement())) {
                    return false;
                }
            }
        }
        else if (sourceElement instanceof PsiClass psiClass) {
            final PropertiesFile[] files = Application.get().runReadAction(
                (Supplier<PropertiesFile[]>)() -> GwtI18nManager.getInstance(psiClass.getProject()).getPropertiesFiles(psiClass)
            );
            for (PropertiesFile file : files) {
                if (!consumer.test(file.getContainingFile())) {
                    return false;
                }
            }
        }
        return true;
    }
}
