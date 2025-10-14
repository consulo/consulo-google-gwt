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

package com.intellij.gwt.impl.inspections;

import com.intellij.gwt.base.i18n.GwtI18nUtil;
import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.impl.i18n.GwtI18nManager;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.psi.*;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
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
public class GwtMethodWithParametersInConstantsInterfaceInspection extends BaseGwtInspection {
    private static final Logger LOG = Logger.getInstance(GwtMethodWithParametersInConstantsInterfaceInspection.class);

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionNameMethodWithParametersInInterfaceExtendingConstants();
    }

    @Override
    @NonNls
    @Nonnull
    public String getShortName() {
        return "GwtMethodWithParametersInConstantsInterface";
    }

    @RequiredReadAction
    @Override
    @Nullable
    public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension,
                                              @Nonnull GwtVersion version,
                                              @Nonnull final PsiClass aClass,
                                              @Nonnull final InspectionManager manager,
                                              final boolean isOnTheFly,
                                              Object state) {
        if (!shouldCheck(aClass)) {
            return null;
        }

        GwtI18nManager i18nManager = GwtI18nManager.getInstance(manager.getProject());
        PropertiesFile[] files = i18nManager.getPropertiesFiles(aClass);
        if (files.length == 0 || !i18nManager.isConstantsInterface(aClass)) {
            return null;
        }

        PsiJavaCodeReferenceElement extendsConstantsElement = findExtendsContsantsElement(aClass);

        List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (PsiMethod psiMethod : aClass.getMethods()) {
            if (psiMethod.getParameterList().getParametersCount() > 0) {
                ReplaceConstantsByMessagesInExtendsListQuickFix quickFix = null;
                if (extendsConstantsElement != null) {
                    quickFix = new ReplaceConstantsByMessagesInExtendsListQuickFix(aClass, extendsConstantsElement);
                }

                problems.add(manager.createProblemDescriptor(getElementToHighlight(psiMethod), GwtLocalize.problemDescriptionMethodsWithParametersAreNotAllowedInAnInterfaceExtendingConstants().get(), quickFix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            }
        }

        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @Override
    public ProblemDescriptor[] checkFile(@Nonnull final PsiFile file, @Nonnull final InspectionManager manager, final boolean isOnTheFly, Object state) {
        if (!shouldCheck(file) || !(file instanceof PropertiesFile)) {
            return null;
        }
        PropertiesFile propertiesFile = (PropertiesFile) file;

        GwtI18nManager i18nManager = GwtI18nManager.getInstance(manager.getProject());
        PsiClass propertiesInterface = i18nManager.getPropertiesInterface(propertiesFile);
        if (propertiesInterface == null || !i18nManager.isConstantsInterface(propertiesInterface)) {
            return null;
        }

        List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        PsiJavaCodeReferenceElement extendsConstantsElement = findExtendsContsantsElement(propertiesInterface);
        for (IProperty property : propertiesFile.getProperties()) {
            if (GwtI18nUtil.getParametersCount(property.getValue()) > 0) {
                ReplaceConstantsByMessagesInExtendsListQuickFix quickFix = null;
                if (extendsConstantsElement != null) {
                    quickFix = new ReplaceConstantsByMessagesInExtendsListQuickFix(propertiesInterface, extendsConstantsElement);
                }
                String message = GwtLocalize.problemDescriptionPropertiesWithParametersAreNotAllowedIfTheAssociatedInterfaceExtendsConstants().get();
                problems.add(manager.createProblemDescriptor(property.getPsiElement(), message, quickFix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            }
        }

        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @Nullable
    private static PsiJavaCodeReferenceElement findExtendsContsantsElement(final PsiClass aClass) {
        PsiJavaCodeReferenceElement extendsConstantsElement = null;
        PsiReferenceList list = aClass.getExtendsList();
        if (list != null) {
            for (PsiJavaCodeReferenceElement element : list.getReferenceElements()) {
                PsiElement anInterface = element.resolve();
                if (anInterface instanceof PsiClass && GwtI18nUtil.CONSTANTS_INTERFACE_NAME.equals(((PsiClass) anInterface).getQualifiedName())) {
                    extendsConstantsElement = element;
                    break;
                }
            }
        }
        return extendsConstantsElement;
    }

    private static class ReplaceConstantsByMessagesInExtendsListQuickFix extends BaseGwtLocalQuickFix {
        private PsiClass myInterface;
        private final PsiJavaCodeReferenceElement myExtendsConstantsElement;

        private ReplaceConstantsByMessagesInExtendsListQuickFix(final PsiClass anInterface, final PsiJavaCodeReferenceElement extendsConstantsElement) {
            super(GwtLocalize.quickfixNameInherit0FromMessagesInsteadOfConstants(anInterface.getName()));
            myInterface = anInterface;
            myExtendsConstantsElement = extendsConstantsElement;
        }

        @Override
        public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
            if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(myInterface.getContainingFile().getVirtualFile()).hasReadonlyFiles()) {
                return;
            }

            try {
                myExtendsConstantsElement.delete();
                PsiElementFactory factory = JavaPsiFacade.getInstance(myInterface.getProject()).getElementFactory();
                PsiClassType messagesType = factory.createTypeByFQClassName(GwtI18nUtil.MESSAGES_INTERFACE_NAME, myInterface.getResolveScope());
                PsiReferenceList extendsList = myInterface.getExtendsList();
                LOG.assertTrue(extendsList != null);
                extendsList.add(factory.createReferenceElementByType(messagesType));
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }
    }
}
