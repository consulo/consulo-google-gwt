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

package com.intellij.gwt.base.inspections;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiIdentifier;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.access.RequiredReadAction;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class BaseGwtInspection<State> extends BaseJavaLocalInspectionTool<State> {
    @Override
    @RequiredReadAction
    public final ProblemDescriptor[] checkClass(@Nonnull final PsiClass aClass, @Nonnull final InspectionManager manager, final boolean isOnTheFly, State state) {
        GoogleGwtModuleExtension extension = getExtension(aClass);
        if (extension == null) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }

        GwtVersion version = GwtModuleExtensionUtil.getVersion(extension);
        return checkClassImpl(extension, version, aClass, manager, isOnTheFly, state);
    }

    @Nullable
    public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension,
                                              @Nonnull GwtVersion version,
                                              @Nonnull final PsiClass aClass,
                                              @Nonnull final InspectionManager manager,
                                              final boolean isOnTheFly,
                                              State state) {
        return ProblemDescriptor.EMPTY_ARRAY;
    }

    @RequiredReadAction
    protected static boolean shouldCheck(@Nonnull PsiElement psiElement) {
        return getExtension(psiElement) != null;
    }

    @Nullable
    @RequiredReadAction
    protected static GoogleGwtModuleExtension getExtension(@Nonnull PsiElement psiElement) {
        return ModuleUtilCore.getExtension(psiElement, GoogleGwtModuleExtension.class);
    }

    @Nonnull
    protected static PsiElement getElementToHighlight(@Nonnull PsiClass psiClass) {
        PsiIdentifier identifier = psiClass.getNameIdentifier();
        return identifier != null ? identifier : psiClass;
    }

    @Nonnull
    protected static PsiElement getElementToHighlight(@Nonnull PsiMethod psiMethod) {
        PsiIdentifier identifier = psiMethod.getNameIdentifier();
        return identifier != null ? identifier : psiMethod;
    }

    @Override
    @Nonnull
    public LocalizeValue getGroupDisplayName() {
        return GwtLocalize.groupGwtInspectionsName();
    }

    @Override
    @Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
}
