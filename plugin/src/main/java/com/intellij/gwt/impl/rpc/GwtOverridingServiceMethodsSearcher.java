package com.intellij.gwt.impl.rpc;

import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.java.indexing.search.searches.OverridingMethodsSearch;
import com.intellij.java.indexing.search.searches.OverridingMethodsSearchExecutor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;

import java.util.function.Predicate;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtOverridingServiceMethodsSearcher extends GwtSearcherBase<PsiMethod, OverridingMethodsSearch.SearchParameters>
    implements OverridingMethodsSearchExecutor {
    @Override
    protected PsiFile getContainingFile(final OverridingMethodsSearch.SearchParameters parameters) {
        return parameters.getMethod().getContainingFile();
    }

    @Override
    public boolean doExecute(OverridingMethodsSearch.SearchParameters queryParameters, Predicate<? super PsiMethod> consumer) {
        PsiMethod method = queryParameters.getMethod();
        PsiClass async = method.getContainingClass();
        PsiClass sync = RemoteServiceUtil.findSynchronousInterface(async);
        if (sync != null) {
            PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
            if (syncMethod != null) {
                if (!consumer.test(syncMethod)) {
                    return false;
                }
                if (!OverridingMethodsSearch.search(syncMethod, queryParameters.getScope(), queryParameters.isCheckDeep())
                    .forEach(consumer)) {
                    return false;
                }
            }
        }
        return true;
    }
}
