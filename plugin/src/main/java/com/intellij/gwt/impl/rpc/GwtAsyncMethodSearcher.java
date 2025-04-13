package com.intellij.gwt.impl.rpc;

import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.java.language.psi.search.searches.SuperMethodsSearch;
import com.intellij.java.language.psi.search.searches.SuperMethodsSearchExecutor;
import com.intellij.java.language.psi.util.MethodSignatureBackedByPsiMethod;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;

import java.util.function.Predicate;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtAsyncMethodSearcher extends GwtSearcherBase<MethodSignatureBackedByPsiMethod, SuperMethodsSearch.SearchParameters>
    implements SuperMethodsSearchExecutor {
    @Override
    protected PsiFile getContainingFile(final SuperMethodsSearch.SearchParameters parameters) {
        return parameters.getMethod().getContainingFile();
    }

    @Override
    public boolean doExecute(
        final SuperMethodsSearch.SearchParameters queryParameters,
        final Predicate<? super MethodSignatureBackedByPsiMethod> consumer
    ) {
        PsiMethod method = queryParameters.getMethod();
        PsiClass sync = method.getContainingClass();
        PsiClass async = RemoteServiceUtil.findAsynchronousInterface(sync);
        if (async != null) {
            PsiMethod asyncMethod = RemoteServiceUtil.findMethodInAsync(method, async);
            if (asyncMethod != null && !consumer.test(MethodSignatureBackedByPsiMethod.create(asyncMethod, PsiSubstitutor.EMPTY))) {
                return false;
            }
        }
        return true;
    }
}
