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

package com.intellij.gwt.jsinject.inspections;

import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.jsinject.GwtClassMemberReference;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.javascript.lang.GwtJavaScriptVersion;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtJavaScriptReferencesInspection extends BaseGwtInspection {
    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionNameUnresolvedReferencesInJsniMethods();
    }

    @Override
    @NonNls
    @Nonnull
    public String getShortName() {
        return "GwtJavaScriptReferences";
    }

    @Override
    @Nullable
    public ProblemDescriptor[] checkFile(@Nonnull final PsiFile file, @Nonnull final InspectionManager manager, final boolean isOnTheFly, Object state) {
        if (!(file.getLanguageVersion() instanceof GwtJavaScriptVersion)) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(final PsiElement element) {
                if (element instanceof JSGwtReferenceExpressionImpl) {
                    PsiReference[] references = element.getReferences();
                    for (PsiReference reference : references) {
                        if (reference.resolve() == null) {
                            if (reference instanceof GwtClassMemberReference) {
                                PsiClass psiClass = ((GwtClassMemberReference) reference).resolveQualifier();
                                if (psiClass != null) {
                                    String message = GwtLocalize.problemDescriptionCannotResolveSymbol0In1(reference.getCanonicalText(), psiClass.getQualifiedName()).get();
                                    problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), message,
                                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
                                }
                            }
                            else {
                                String message = GwtLocalize.problemDescriptionCannotResolve0(reference.getCanonicalText()).get();
                                problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), message,
                                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
                            }
                        }
                    }
                }
                else {
                    super.visitElement(element);
                }
            }
        });
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
