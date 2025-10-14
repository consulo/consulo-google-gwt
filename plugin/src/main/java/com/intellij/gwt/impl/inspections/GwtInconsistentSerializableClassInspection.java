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
import com.intellij.gwt.impl.rpc.GwtSerializableUtil;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
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
public class GwtInconsistentSerializableClassInspection extends BaseGwtInspection {
    private static final Logger LOG = Logger.getInstance(GwtInconsistentSerializableClassInspection.class);

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionNameIncorrectSerializableClass();
    }

    @Override
    @NonNls
    @Nonnull
    public String getShortName() {
        return "GwtInconsistentSerializableClass";
    }


    @RequiredReadAction
    @Override
    @Nullable
    public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension,
                                              @Nonnull GwtVersion version,
                                              @Nonnull PsiClass aClass,
                                              @Nonnull InspectionManager manager,
                                              boolean isOnTheFly, Object state) {
        PsiFile containingFile = aClass.getContainingFile();
        if (containingFile == null) {
            return null;
        }
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        List<GwtModule> gwtModules = GwtModulesManager.getInstance(manager.getProject()).findGwtModulesByClientSourceFile(virtualFile);
        if (gwtModules.isEmpty()) {
            return null;
        }

        GwtSerializableUtil.SerializableChecker serializableChecker = GwtSerializableUtil.createSerializableChecker(extension, true);
        if (!serializableChecker.isMarkedSerializable(aClass)) {
            return null;
        }

        List<ProblemDescriptor> descriptors = new ArrayList<ProblemDescriptor>();
        final PsiField[] psiFields = aClass.getFields();
        for (PsiField psiField : psiFields) {
            if (!psiField.hasModifierProperty(PsiModifier.TRANSIENT)) {
                final PsiType type = psiField.getType();
                if (!serializableChecker.isSerializable(type)) {
                    final String description = GwtLocalize.problemDescriptionField0IsNotSerializable(type.getPresentableText()).get();
                    PsiElement element = psiField.getTypeElement();
                    if (element == null) {
                        element = psiField;
                    }
                    descriptors.add(manager.createProblemDescriptor(element, description, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
                }
            }
        }

        if (serializableChecker.isGwtSerializable(aClass) && !GwtSerializableUtil.hasPublicNoArgConstructor(aClass)) {
            PsiMethod constructor = GwtSerializableUtil.findNoArgConstructor(aClass);
            final String description = GwtLocalize.problemDescriptionSerializableClassShouldProvidePublicNoArgsConstructor().get();
            if (constructor == null) {
                final LocalQuickFix quickfix = new CreateDefaultConstructorQuickFix(aClass);
                descriptors.add(manager.createProblemDescriptor(getElementToHighlight(aClass), description, quickfix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            }
            else if (!version.isPrivateNoArgConstructorInSerializableClassAllowed()) {
                LocalQuickFix quickfix = new MakeConstructorPublicQuickFix(constructor);
                descriptors.add(manager.createProblemDescriptor(getElementToHighlight(constructor), description, quickfix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            }
        }

        return descriptors.toArray(new ProblemDescriptor[descriptors.size()]);
    }

    private static class CreateDefaultConstructorQuickFix extends BaseGwtLocalQuickFix {
        private final PsiClass myClass;

        public CreateDefaultConstructorQuickFix(final PsiClass aClass) {
            super(GwtLocalize.quickfixNameCreatePublicNoArgsConstructorIn0(aClass.getName()));
            myClass = aClass;
        }

        @Override
        public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
            try {
                myClass.add(JavaPsiFacade.getInstance(myClass.getProject()).getElementFactory().createConstructor());
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }

    private static class MakeConstructorPublicQuickFix extends BaseGwtLocalQuickFix {
        private final PsiMethod myConstructor;

        private MakeConstructorPublicQuickFix(final PsiMethod constructor) {
            super(GwtLocalize.quickfixNameMake0Public(constructor.getContainingClass().getName()));
            myConstructor = constructor;
        }

        @Override
        public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
            try {
                myConstructor.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }
}
