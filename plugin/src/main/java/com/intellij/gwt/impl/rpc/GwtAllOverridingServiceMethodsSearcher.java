package com.intellij.gwt.impl.rpc;

import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.java.indexing.search.searches.AllOverridingMethodsSearch;
import com.intellij.java.indexing.search.searches.AllOverridingMethodsSearchExecutor;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtAllOverridingServiceMethodsSearcher extends GwtSearcherBase<Pair<PsiMethod, PsiMethod>, AllOverridingMethodsSearch.SearchParameters> implements AllOverridingMethodsSearchExecutor {
    @Override
    protected PsiFile getContainingFile(final AllOverridingMethodsSearch.SearchParameters parameters) {
        return parameters.getPsiClass().getContainingFile();
    }

    @Override
    public boolean doExecute(
        final AllOverridingMethodsSearch.SearchParameters queryParameters,
        final Processor<? super Pair<PsiMethod, PsiMethod>> consumer
    ) {
        PsiClass async = queryParameters.getPsiClass();
        PsiClass sync = RemoteServiceUtil.findSynchronousInterface(async);
        if (sync != null) {
            for (PsiMethod method : async.getMethods()) {
                PsiMethod syncMethod = RemoteServiceUtil.findMethodInSync(method, sync);
                if (syncMethod != null) {
                    if (!consumer.process(Pair.create(method, syncMethod))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
