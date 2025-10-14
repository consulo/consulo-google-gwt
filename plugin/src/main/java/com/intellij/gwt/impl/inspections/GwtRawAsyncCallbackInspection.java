package com.intellij.gwt.impl.inspections;

import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.SmartList;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtRawAsyncCallbackInspection extends BaseGwtInspection {
    private static final Logger LOG = Logger.getInstance(GwtRawAsyncCallbackInspection.class);

    @RequiredReadAction
    @Override
    public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension,
                                              @Nonnull GwtVersion version,
                                              @Nonnull final PsiClass aClass,
                                              @Nonnull final InspectionManager manager,
                                              final boolean isOnTheFly,
                                              Object state) {
        if (!version.isGenericsSupported()) {
            return null;
        }

        PsiClass sync = RemoteServiceUtil.findSynchronousInterface(aClass);
        if (sync != null) {
            return checkAsyncronousInterface(aClass, sync, manager);
        }

        final List<ProblemDescriptor> problems = new SmartList<ProblemDescriptor>();
        JavaRecursiveElementVisitor visitor = new JavaRecursiveElementVisitor() {
            @Override
            public void visitReferenceExpression(final PsiReferenceExpression expression) {
            }

            @Override
            public void visitMethodCallExpression(final PsiMethodCallExpression expression) {
                PsiMethod method = expression.resolveMethod();
                if (method != null) {
                    PsiClass async = method.getContainingClass();
                    PsiClass sync = RemoteServiceUtil.findSynchronousInterface(async);
                    if (sync != null) {
                        checkAsyncMethod(method, sync, manager, problems, expression);
                    }
                }
            }
        };
        aClass.accept(visitor);

        return problems.isEmpty() ? null : problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    private static ProblemDescriptor[] checkAsyncronousInterface(final @Nonnull PsiClass async, final @Nonnull PsiClass sync,
                                                                 final InspectionManager manager) {
        final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

        for (PsiMethod method : async.getMethods()) {
            checkAsyncMethod(method, sync, manager, problems, null);
        }

        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    private static void checkAsyncMethod(final PsiMethod method, final PsiClass sync, final InspectionManager manager,
                                         final List<ProblemDescriptor> problems, final @Nullable PsiMethodCallExpression expression) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length == 0) {
            return;
        }


        PsiParameter lastParameter = parameters[parameters.length - 1];
        PsiType type = lastParameter.getType();
        if (!(type instanceof PsiClassType)) {
            return;
        }

        final PsiClassType classType = (PsiClassType) type;
        PsiClass psiClass = classType.resolve();
        if (psiClass == null || !RemoteServiceUtil.ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName())) {
            return;
        }

        PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
        if (syncMethod == null) {
            return;
        }
        PsiType returnType = syncMethod.getReturnType();
        if (returnType == PsiType.VOID || returnType == null) {
            return;
        }

        PsiAnonymousClass rawAnonymous = null;
        if (expression != null) {
            PsiExpression[] arguments = expression.getArgumentList().getExpressions();
            if (arguments.length == parameters.length) {
                PsiExpression lastArg = arguments[arguments.length - 1];
                if (lastArg instanceof PsiNewExpression) {
                    PsiAnonymousClass anonymousClass = ((PsiNewExpression) lastArg).getAnonymousClass();
                    if (anonymousClass != null) {
                        final PsiReferenceParameterList parameterList = anonymousClass.getBaseClassReference().getParameterList();
                        if (parameterList != null && parameterList.getTypeParameterElements().length == 0) {
                            rawAnonymous = anonymousClass;
                        }
                    }
                }
            }
        }


        if (classType.isRaw() || rawAnonymous != null) {
            final PsiMethod methodToFix = classType.isRaw() ? method : null;
            LocalQuickFix fix;
            if (methodToFix != null || rawAnonymous != null) {
                fix = new GenerifyAsyncCallbackFix(returnType, methodToFix, rawAnonymous);
            }
            else {
                fix = null;
            }
            final String methodDescription = PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil
                .SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE);
            final String message = GwtLocalize.problemDescriptionRawUseOfAsynccallbackInterface(methodDescription).get();
            PsiElement place = rawAnonymous != null ? getElementToHighlight(rawAnonymous) : expression != null ? expression : lastParameter;
            problems.add(manager.createProblemDescriptor(place, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionNameRawUseOfAsynccallbackInAsynchronousServiceInterfaces();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "gwtRawAsyncCallback";
    }

    @Override
    @Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class GenerifyAsyncCallbackFix extends BaseGwtLocalQuickFix {
        private final PsiType myType;
        private final PsiMethod myMethodToFix;
        private final PsiAnonymousClass myAnonymousToFix;

        private GenerifyAsyncCallbackFix(final @Nonnull PsiType type, final @Nullable PsiMethod methodToFix,
                                         final @Nullable PsiAnonymousClass anonymousToFix) {
            super(GwtLocalize.quickfixNameReplaceAsynccallbackByAsynccallback0(type.getCanonicalText()));
            myType = type;
            myMethodToFix = methodToFix;
            myAnonymousToFix = anonymousToFix;
        }

        @Override
        public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
            List<VirtualFile> affectedFiles = new ArrayList<VirtualFile>();
            if (myMethodToFix != null) {
                affectedFiles.add(myMethodToFix.getContainingFile().getVirtualFile());
            }
            if (myAnonymousToFix != null) {
                affectedFiles.add(myAnonymousToFix.getContainingFile().getVirtualFile());
            }
            if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(affectedFiles.toArray(new VirtualFile[affectedFiles.size()]))
                .hasReadonlyFiles()) {
                return;
            }

            try {
                PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
                if (myMethodToFix != null) {
                    generifyMethod(myMethodToFix, myType, elementFactory);
                }
                if (myAnonymousToFix != null) {
                    generifyAnonymous(myAnonymousToFix, myType, project, elementFactory);
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }

        private static void generifyAnonymous(final @Nonnull PsiAnonymousClass anonymous, final @Nonnull PsiType type, final Project project,
                                              final PsiElementFactory elementFactory) throws IncorrectOperationException {
            PsiReferenceParameterList list = anonymous.getBaseClassReference().getParameterList();
            if (list != null) {
                list.add(elementFactory.createTypeElement(type));
            }

            PsiMethod[] methods = anonymous.findMethodsByName("onSuccess", false);
            for (PsiMethod method : methods) {
                PsiParameter[] parameters = method.getParameterList().getParameters();
                if (parameters.length == 1) {
                    PsiParameter parameter = parameters[0];
                    if (PsiType.getJavaLangObject(PsiManager.getInstance(project), anonymous.getResolveScope()).equals(parameter.getType())) {
                        parameter.getTypeElement().replace(elementFactory.createTypeElement(type));
                        break;
                    }
                }
            }
        }

        private static void generifyMethod(final @Nonnull PsiMethod method, final @Nonnull PsiType type, final PsiElementFactory elementFactory) throws
            IncorrectOperationException {
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length == 0) {
                return;
            }

            PsiParameter last = parameters[parameters.length - 1];
            last.getTypeElement().replace(elementFactory.createTypeElement(RemoteServiceUtil.createAsynchCallbackType(method, type)));
        }
    }
}
