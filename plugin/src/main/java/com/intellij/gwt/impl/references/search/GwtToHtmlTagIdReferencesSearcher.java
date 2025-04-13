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

package com.intellij.gwt.impl.references.search;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.impl.references.GwtToHtmlTagReference;
import com.intellij.java.language.psi.PsiLiteralExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AccessRule;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.lang.html.HTMLLanguage;
import consulo.xml.lang.xhtml.XHTMLLanguage;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlAttributeValue;
import consulo.xml.psi.xml.XmlTag;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtToHtmlTagIdReferencesSearcher implements ReferencesSearchQueryExecutor {
    @Override
    public boolean execute(
        @Nonnull ReferencesSearch.SearchParameters queryParameters,
        @Nonnull Predicate<? super PsiReference> consumer
    ) {
        return AccessRule.read(() -> doExecute(queryParameters, consumer));
    }

    @RequiredReadAction
    private static boolean doExecute(
        ReferencesSearch.SearchParameters queryParameters,
        Predicate<? super PsiReference> consumer
    ) {
        PsiElement element = queryParameters.getElementToSearch();
        if (!(element instanceof XmlAttributeValue attrValue)) {
            return true;
        }

        PsiElement parent = element.getParent();
        if (!(parent instanceof XmlAttribute attr && "id".equals(attr.getLocalName()))) {
            return true;
        }
        String id = attrValue.getValue();

        PsiElement tag = parent.getParent();
        if (!(tag instanceof XmlTag)) {
            return true;
        }

        PsiFile file = parent.getContainingFile();
        Language language = file.getLanguage();
        if (!language.equals(HTMLLanguage.INSTANCE) && !language.equals(XHTMLLanguage.INSTANCE)) {
            return true;
        }

        GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(file.getProject());
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return true;
        }

        GwtModule gwtModule = gwtModulesManager.findGwtModuleByClientOrPublicFile(virtualFile);
        if (gwtModule == null) {
            return true;
        }

        PsiSearchHelper searchHelper = PsiSearchHelper.SERVICE.getInstance(element.getProject());
        return searchHelper.processElementsWithWord(
            (element1, offsetInElement) -> {
                if (!(element1 instanceof PsiLiteralExpression)) {
                    return true;
                }

                for (PsiReference reference : element1.getReferences()) {
                    if (reference instanceof GwtToHtmlTagReference && reference.isReferenceTo(tag) && !consumer.test(reference)) {
                        return false;
                    }
                }
                return true;
            },
            queryParameters.getScope(),
            id,
            UsageSearchContext.IN_STRINGS,
            true
        );
    }
}
