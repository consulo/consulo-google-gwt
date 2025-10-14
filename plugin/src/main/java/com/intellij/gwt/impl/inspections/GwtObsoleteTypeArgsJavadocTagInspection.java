package com.intellij.gwt.impl.inspections;

import com.intellij.gwt.base.inspections.BaseGwtInspection;
import com.intellij.gwt.base.rpc.GwtGenericsUtil;
import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.javadoc.PsiDocComment;
import com.intellij.java.language.psi.javadoc.PsiDocTag;
import com.intellij.java.language.psi.util.PsiFormatUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressManager;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.search.DefinitionsScopedSearch;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtObsoleteTypeArgsJavadocTagInspection extends BaseGwtInspection {
    private static final Logger LOG = Logger.getInstance(GwtObsoleteTypeArgsJavadocTagInspection.class);

    @Override
    @RequiredReadAction
    public ProblemDescriptor[] checkClassImpl(@Nonnull GoogleGwtModuleExtension extension,
                                              @Nonnull GwtVersion version,
                                              @Nonnull final PsiClass aClass,
                                              @Nonnull final InspectionManager manager,
                                              final boolean isOnTheFly,
                                              Object state) {
        if (!version.isGenericsSupported()) {
            return null;
        }

        if (RemoteServiceUtil.isRemoteServiceInterface(aClass)) {
            return checkRemoteServiceInterface(aClass, manager, version);
        }
        return null;
    }

    private static ProblemDescriptor[] checkRemoteServiceInterface(final PsiClass aClass, final InspectionManager manager, final GwtVersion gwtVersion) {
        List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (PsiMethod method : aClass.getMethods()) {
            PsiDocComment comment = method.getDocComment();
            if (comment != null) {
                PsiDocTag[] tags = comment.findTagsByName(GwtGenericsUtil.TYPE_ARGS_TAG);
                if (tags.length > 0) {
                    GenerifyServiceMethodFix fix = new GenerifyServiceMethodFix(method, gwtVersion);
                    String message = GwtLocalize.problemDescriptionGwtTypeargsTagIsObsoleteInGwt15().get();
                    problems.add(manager.createProblemDescriptor(comment, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
                }
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return GwtLocalize.inspectionNameObsoleteGwtTypeargsTagInJavadocComments();
    }

    @Override
    @Nonnull
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "GwtObsoleteTypeArgsJavadocTag";
    }

    private static class GenerifyServiceMethodFix extends BaseGwtLocalQuickFix {
        private final PsiMethod myMethod;
        private final GwtVersion myGwtVersion;

        protected GenerifyServiceMethodFix(final PsiMethod method, final GwtVersion gwtVersion) {
            super(GwtLocalize.quickfixNameGenerifyTypesInMethod0InsteadOfUsingGwtTypeargsTags(PsiFormatUtil.formatMethod(method,
                PsiSubstitutor.EMPTY, PsiFormatUtil.SHOW_NAME | PsiFormatUtil.SHOW_PARAMETERS, PsiFormatUtil.SHOW_TYPE)));
            myMethod = method;
            myGwtVersion = gwtVersion;
        }

        @Override
        public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
            try {
                PsiType newReturnType;
                String returnTypeParameters = GwtGenericsUtil.getReturnTypeParametersString(myMethod);
                if (returnTypeParameters != null) {
                    newReturnType = appendTypeParameters(myMethod.getReturnType(), returnTypeParameters, myMethod);
                }
                else {
                    newReturnType = null;
                }

                IntObjectMap<PsiType> newParameterTypes = IntMaps.newIntObjectHashMap();
                PsiParameter[] parameters = myMethod.getParameterList().getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    PsiParameter parameter = parameters[i];
                    String typeParametersString = GwtGenericsUtil.getTypeParametersString(myMethod, parameter.getName());
                    if (typeParametersString != null) {
                        PsiType type = appendTypeParameters(parameter.getType(), typeParametersString, myMethod);
                        newParameterTypes.put(i, type);
                    }
                }

                if (newReturnType == null && newParameterTypes.isEmpty()) {
                    return;
                }

                List<PsiMethod> methods = findImplementations(project);
                if (methods == null) {
                    return;
                }

                methods.add(0, myMethod);

                Set<VirtualFile> affectedFiles = new HashSet<VirtualFile>();
                for (PsiMethod method : methods) {
                    affectedFiles.add(method.getContainingFile().getVirtualFile());
                }

                PsiClass async = RemoteServiceUtil.findAsynchronousInterface(myMethod.getContainingClass());
                PsiMethod asyncMethod = null;
                if (async != null) {
                    asyncMethod = RemoteServiceUtil.findAsynchronousMethod(myMethod);
                    affectedFiles.add(async.getContainingFile().getVirtualFile());
                }

                if (ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(affectedFiles.toArray(new VirtualFile[affectedFiles.size()]))
                    .hasReadonlyFiles()) {
                    return;
                }

                SmartPsiElementPointer<PsiMethod> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(myMethod);
                for (PsiMethod method : methods) {
                    updateSignature(method, newReturnType, newParameterTypes);
                }
                PsiMethod newMethod = pointer.getElement();
                if (newMethod != null) {
                    if (asyncMethod != null) {
                        asyncMethod.delete();
                        RemoteServiceUtil.copyMethodToAsync(newMethod, async, myGwtVersion);
                    }
                    GwtGenericsUtil.removeTypeArgsJavadocTags(newMethod);
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }
        }

        private static void updateSignature(final PsiMethod method, final PsiType newReturnType, final IntObjectMap<PsiType> newParameterTypes)
            throws IncorrectOperationException {
            PsiElementFactory elementFactory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();

            if (newReturnType != null) {
                PsiTypeElement returnTypeElement = method.getReturnTypeElement();
                if (returnTypeElement != null) {
                    returnTypeElement.replace(elementFactory.createTypeElement(newReturnType));
                }
            }

            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (int i : newParameterTypes.keys()) {
                parameters[i].getTypeElement().replace(elementFactory.createTypeElement(newParameterTypes.get(i)));
            }
        }

        @Nullable
        private List<PsiMethod> findImplementations(final Project project) {
            final List<PsiMethod> methods = new ArrayList<PsiMethod>();
            if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                @Override
                public void run() {
                    Collection<PsiElement> elements = DefinitionsScopedSearch.search(myMethod).findAll();
                    for (PsiElement element : elements) {
                        if (element instanceof PsiMethod) {
                            methods.add((PsiMethod) element);
                        }
                    }
                }
            }, GwtLocalize.gwtSearchingForImplementations().get(), true, project)) {
                return null;
            }

            return methods;
        }

        private static PsiType appendTypeParameters(final @Nonnull PsiType type, final @Nonnull String typeParametersString,
                                                    final @Nonnull PsiElement context) throws IncorrectOperationException {
            if (type instanceof PsiClassType) {
                final PsiClassType classType = (PsiClassType) type;

                if (classType.isRaw()) {
                    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
                    return elementFactory.createTypeFromText(type.getCanonicalText() + typeParametersString, context);
                }
            }
            return type;
        }
    }
}
