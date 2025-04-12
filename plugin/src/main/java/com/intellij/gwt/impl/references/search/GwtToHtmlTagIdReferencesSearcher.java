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
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.util.function.Processor;
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

/**
 * @author nik
 */
@ExtensionImpl
public class GwtToHtmlTagIdReferencesSearcher implements ReferencesSearchQueryExecutor {
    @Override
    public boolean execute(final ReferencesSearch.SearchParameters queryParameters, final Processor<? super PsiReference> consumer) {
        return ReadAction.compute(() -> doExecute(queryParameters, consumer));
    }

    private static boolean doExecute(
        final ReferencesSearch.SearchParameters queryParameters,
        final Processor<? super PsiReference> consumer
    ) {
        final PsiElement element = queryParameters.getElementToSearch();
        if (!(element instanceof XmlAttributeValue)) {
            return true;
        }

        final PsiElement parent = element.getParent();
        if (!(parent instanceof XmlAttribute) || !"id".equals(((XmlAttribute)parent).getLocalName())) {
            return true;
        }
        String id = ((XmlAttributeValue)element).getValue();

        final PsiElement tag = parent.getParent();
        if (!(tag instanceof XmlTag)) {
            return true;
        }

        final PsiFile file = parent.getContainingFile();
        if (!file.getLanguage().equals(HTMLLanguage.INSTANCE) && !file.getLanguage().equals(XHTMLLanguage.INSTANCE)) {
            return true;
        }

        final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(file.getProject());
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return true;
        }

        final GwtModule gwtModule = gwtModulesManager.findGwtModuleByClientOrPublicFile(virtualFile);
        if (gwtModule == null) {
            return true;
        }

        final PsiSearchHelper searchHelper = PsiSearchHelper.SERVICE.getInstance(element.getProject());
        return searchHelper.processElementsWithWord(new TextOccurenceProcessor() {
            @Override
            public boolean execute(PsiElement element, int offsetInElement) {
                if (!(element instanceof PsiLiteralExpression)) {
                    return true;
                }

                final PsiReference[] references = element.getReferences();
                for (PsiReference reference : references) {
                    if (reference instanceof GwtToHtmlTagReference && reference.isReferenceTo(tag)) {
                        if (!consumer.process(reference)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }, queryParameters.getScope(), id, UsageSearchContext.IN_STRINGS, true);
    }
}
