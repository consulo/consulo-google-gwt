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

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.base.rpc.GwtGenericsUtil;
import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.gwt.impl.rpc.GwtSerializableUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.analysis.codeInsight.intention.QuickFixFactory;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtNonSerializableRemoteServiceMethodParametersInspection extends BaseGwtInspection<GwtSerializableInspectionState> {
    @Nonnull
    @Override
    public InspectionToolState<? extends GwtSerializableInspectionState> createStateProvider() {
        return new GwtSerializableInspectionStateProvider();
    }

    @RequiredReadAction
    @Override
    @Nullable
    public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension,
                                              @Nonnull GwtVersion version,
                                              @Nonnull PsiClass aClass,
                                              @Nonnull InspectionManager manager,
                                              boolean isOnTheFly,
                                              GwtSerializableInspectionState state) {
        GoogleGwtModuleExtension gwtFacet = getExtension(aClass);
        if (gwtFacet == null) {
            return null;
        }

        if (RemoteServiceUtil.isRemoteServiceInterface(aClass)) {
            return checkRemoteService(gwtFacet, aClass, manager, state);
        }
        return null;
    }

    @RequiredReadAction
    private ProblemDescriptor[] checkRemoteService(final GoogleGwtModuleExtension gwtFacet, final PsiClass aClass, final InspectionManager manager, GwtSerializableInspectionState state) {
        ArrayList<ProblemDescriptor> result = new ArrayList<>(0);

        GwtSerializableUtil.SerializableChecker serializableChecker = GwtSerializableUtil.createSerializableChecker(gwtFacet, state.isReportInterfaces());

        GlobalSearchScope scope = aClass.getResolveScope();
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(manager.getProject());
        PsiClass exceptionClass = psiFacade.findClass(Exception.class.getName(), scope);

        for (final PsiMethod method : aClass.getMethods()) {
            for (final PsiParameter param : method.getParameterList().getParameters()) {
                String typeParametersString = GwtGenericsUtil.getTypeParametersString(method, param.getName());
                checkTypeSerial(param.getTypeElement(), typeParametersString, serializableChecker, manager, result);
            }
            final PsiTypeElement returnTypeElement = method.getReturnTypeElement();
            if (returnTypeElement != null) {
                String typeParameters = GwtGenericsUtil.getReturnTypeParametersString(method);
                checkTypeSerial(returnTypeElement, typeParameters, serializableChecker, manager, result);
            }

            PsiJavaCodeReferenceElement[] thrown = method.getThrowsList().getReferenceElements();
            for (PsiJavaCodeReferenceElement referenceElement : thrown) {
                PsiClassType classType = psiFacade.getElementFactory().createType(referenceElement);
                PsiClass psiClass = classType.resolve();

                if (exceptionClass != null && psiClass != null && !InheritanceUtil.isInheritorOrSelf(psiClass, exceptionClass, true)) {
                    String message = GwtBundle.message("problem.description.0.is.not.a.checked.exception", psiClass.getQualifiedName());
                    result.add(manager.createProblemDescriptor(referenceElement, message, LocalQuickFix.EMPTY_ARRAY,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
                }
                else {
                    checkTypeSerial(classType, referenceElement, null, serializableChecker, manager, result);
                }
            }
        }

        return result.toArray(new ProblemDescriptor[result.size()]);
    }

    @RequiredReadAction
    private static void checkTypeSerial(PsiTypeElement typeElement, final @Nullable String typeParameterStrings,
                                        final GwtSerializableUtil.SerializableChecker serializableChecker, InspectionManager manager, List<ProblemDescriptor> result) {
        PsiType type = typeElement.getType();
        checkTypeSerial(type, typeElement, typeParameterStrings, serializableChecker, manager, result);
    }

    @RequiredReadAction
    private static void checkTypeSerial(PsiType type, final PsiElement typeElement, final @Nullable String typeParameterStrings,
                                        final GwtSerializableUtil.SerializableChecker serializableChecker, final InspectionManager manager, final List<ProblemDescriptor> result) {
        if (!type.isValid()) {
            return;
        }
        List<PsiType> typeParameters = GwtGenericsUtil.getTypeParameters(typeElement, typeParameterStrings);

        if (serializableChecker.isSerializable(type, typeParameters)) {
            return;
        }
        while (type instanceof PsiArrayType) {
            type = ((PsiArrayType) type).getComponentType();
        }

        if (!(type instanceof PsiClassType)) {
            return;
        }

        PsiClassType classType = (PsiClassType) type;

        if (!serializableChecker.getVersion().isGenericsSupported() && classType.getParameters().length > 0) {
            String description = GwtBundle.message("problem.description.generics.isnt.supported.in.gwt.before.1.5.version");
            result.add(manager.createProblemDescriptor(typeElement, description, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
            return;
        }

        PsiClass aClass = classType.resolve();
        if (aClass != null) {
            boolean haveGenericParameters = serializableChecker.getVersion().isGenericsSupported() && classType.getParameters().length > 0;
            final String description;
            final LocalQuickFix[] quickFixes;
            String typeString = type.getCanonicalText();
            if (typeParameterStrings == null && !haveGenericParameters && GwtSerializableUtil.isCollection(type)) {
                description = GwtBundle.message("problem.description.type.of.collection.elements.is.not.specified", typeString);
                quickFixes = new LocalQuickFix[]{};
            }
            else {
                if (typeParameterStrings != null && !haveGenericParameters) {
                    typeString += typeParameterStrings;
                }

                if (!isInSources(aClass)) {
                    quickFixes = LocalQuickFix.EMPTY_ARRAY;
                    description = GwtBundle.message("problem.description.type.is.not.serializable", typeString);
                }
                else {
                    final List<PsiClass> list = serializableChecker.getSerializableMarkerInterfaces();
                    final PsiElementFactory psiFactory = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory();
                    quickFixes = new LocalQuickFix[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        quickFixes[i] = IntentionManager.getInstance().convertToFix(QuickFixFactory.getInstance().createExtendsListFix(aClass,
                            psiFactory.createType(list.get(i)), true));
                    }
                    description = GwtBundle.message("problem.description.gwt.serializable.type.0.should.implements.marker.interface.1", typeString,
                        serializableChecker.getPresentableSerializableClassesString());
                }
            }
            result.add(manager.createProblemDescriptor(typeElement, description, quickFixes, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
    }

    private static boolean isInSources(final PsiClass aClass) {
        VirtualFile file = aClass.getContainingFile().getVirtualFile();
        if (file == null) {
            return false;
        }
        Module module = ModuleUtilCore.findModuleForFile(file, aClass.getProject());
        return module != null && ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(file);
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return GwtBundle.message("inspection.name.non.serializable.service.method.parameters");
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "NonSerializableServiceParameters";
    }
}
