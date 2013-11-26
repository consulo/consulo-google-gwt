package com.intellij.gwt.inspections;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;

/**
 * @author nik
 */
public class GwtRawAsyncCallbackInspection extends BaseGwtInspection {
  private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.inspections.GwtRawAsyncCallbackInspection");

  public ProblemDescriptor[] checkClass(@NotNull final PsiClass aClass, @NotNull final InspectionManager manager,
                                        final boolean isOnTheFly) {
    GwtFacet gwtFacet = getFacet(aClass);
    if (gwtFacet == null || !gwtFacet.getSdkVersion().isGenericsSupported()) {
      return null;
    }

    PsiClass sync = RemoteServiceUtil.findSynchronousInterface(aClass);
    if (sync != null) {
      return checkAsyncronousInterface(aClass, sync, manager);
    }

    final List<ProblemDescriptor> problems = new SmartList<ProblemDescriptor>();
    JavaRecursiveElementVisitor visitor = new JavaRecursiveElementVisitor() {
      public void visitReferenceExpression(final PsiReferenceExpression expression) {
      }

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

  private static ProblemDescriptor[] checkAsyncronousInterface(final @NotNull PsiClass async, final @NotNull PsiClass sync,
                                                               final InspectionManager manager) {
    final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

    for (PsiMethod method : async.getMethods()) {
      checkAsyncMethod(method, sync, manager, problems, null);
    }

    return problems.toArray(new ProblemDescriptor[problems.size()]);
  }

  private static void checkAsyncMethod(final PsiMethod method, final PsiClass sync, final InspectionManager manager, final List<ProblemDescriptor> problems,
                                       final @Nullable PsiMethodCallExpression expression) {
    PsiParameter[] parameters = method.getParameterList().getParameters();
    if (parameters.length == 0) {
      return;
    }


    PsiParameter lastParameter = parameters[parameters.length - 1];
    PsiType type = lastParameter.getType();
    if (!(type instanceof PsiClassType)) {
      return;
    }

    final PsiClassType classType = (PsiClassType)type;
    PsiClass psiClass = classType.resolve();
    if (psiClass == null || !RemoteServiceUtil.ASYNC_CALLBACK_INTERFACE_NAME.equals(psiClass.getQualifiedName())) {
      return;
    }

    PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
    if (syncMethod == null) return;
    PsiType returnType = syncMethod.getReturnType();
    if (returnType == PsiType.VOID || returnType == null) return;

    PsiAnonymousClass rawAnonymous = null;
    if (expression != null) {
      PsiExpression[] arguments = expression.getArgumentList().getExpressions();
      if (arguments.length == parameters.length) {
        PsiExpression lastArg = arguments[arguments.length - 1];
        if (lastArg instanceof PsiNewExpression) {
          PsiAnonymousClass anonymousClass = ((PsiNewExpression)lastArg).getAnonymousClass();
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
      final String methodDescription = PsiFormatUtil.formatMethod(method, PsiSubstitutor.EMPTY,
                                                                  PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE);
      final String message = GwtBundle.message("problem.description.raw.use.of.asynccallback.interface", methodDescription);
      PsiElement place = rawAnonymous != null ? getElementToHighlight(rawAnonymous) : expression != null ? expression : lastParameter;
      problems.add(manager.createProblemDescriptor(place, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
    }
  }

  @NotNull
  public String getDisplayName() {
    return GwtBundle.message("inspection.name.raw.use.of.asynccallback.in.asynchronous.service.interfaces");
  }

  @NotNull
  public String getShortName() {
    return "gwtRawAsyncCallback";
  }

  @NotNull
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.WARNING;
  }

  private static class GenerifyAsyncCallbackFix extends BaseGwtLocalQuickFix {
    private final PsiType myType;
    private final PsiMethod myMethodToFix;
    private final PsiAnonymousClass myAnonymousToFix;

    private GenerifyAsyncCallbackFix(final @NotNull PsiType type, final @Nullable PsiMethod methodToFix, final @Nullable PsiAnonymousClass anonymousToFix) {
      super(GwtBundle.message("quickfix.name.replace.asynccallback.by.asynccallback.0", type.getCanonicalText()));
      myType = type;
      myMethodToFix = methodToFix;
      myAnonymousToFix = anonymousToFix;
    }

    public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor) {
      List<VirtualFile> affectedFiles = new ArrayList<VirtualFile>();
      if (myMethodToFix != null) {
        affectedFiles.add(myMethodToFix.getContainingFile().getVirtualFile());
      }
      if (myAnonymousToFix != null) {
        affectedFiles.add(myAnonymousToFix.getContainingFile().getVirtualFile());
      }
      if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(affectedFiles.toArray(new VirtualFile[affectedFiles.size()])).hasReadonlyFiles()) {
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

    private static void generifyAnonymous(final @NotNull PsiAnonymousClass anonymous, final @NotNull PsiType type, final Project project,
                                          final PsiElementFactory elementFactory)
        throws IncorrectOperationException {
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

    private static void generifyMethod(final @NotNull PsiMethod method, final @NotNull PsiType type, final PsiElementFactory elementFactory) throws IncorrectOperationException {
      PsiParameter[] parameters = method.getParameterList().getParameters();
      if (parameters.length == 0) return;

      PsiParameter last = parameters[parameters.length - 1];
      last.getTypeElement().replace(elementFactory.createTypeElement(RemoteServiceUtil.createAsynchCallbackType(method, type)));
    }
  }
}
