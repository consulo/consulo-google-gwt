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

package com.intellij.gwt.impl.inspections;

import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.impl.references.GwtToHtmlTagReference;
import com.intellij.java.language.psi.JavaElementVisitor;
import com.intellij.java.language.psi.PsiLiteralExpression;
import com.intellij.java.language.psi.PsiReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtToHtmlTagReferencesInspection extends BaseGwtInspection {
    @Nonnull
    @Override
    public PsiElementVisitor buildVisitorImpl(@Nonnull ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session, Object o) {
        return new JavaElementVisitor() {
            @Override
            public void visitReferenceExpression(PsiReferenceExpression expression) {
            }

            @RequiredReadAction
            @Override
            public void visitLiteralExpression(PsiLiteralExpression expression) {
                final PsiReference[] references = expression.getReferences();
                for (PsiReference reference : references) {
                    if (reference instanceof GwtToHtmlTagReference && reference.resolve() == null) {
                        holder.registerProblem(expression, GwtLocalize.problemDescriptionHtmlTagWithId0IsNotFound(expression.getValue()).get(),
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                    }
                }
            }
        };
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionNameUnresolvedReferencesToHtmlTags();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "GwtToHtmlReferences";
    }

}
