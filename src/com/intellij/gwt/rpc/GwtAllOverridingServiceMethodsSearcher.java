package com.intellij.gwt.rpc;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.searches.AllOverridingMethodsSearch;
import com.intellij.util.Processor;

/**
 * @author nik
 */
public class GwtAllOverridingServiceMethodsSearcher extends GwtSearcherBase<Pair<PsiMethod, PsiMethod>, AllOverridingMethodsSearch.SearchParameters> {
  protected PsiFile getContainingFile(final AllOverridingMethodsSearch.SearchParameters parameters) {
    return parameters.getPsiClass().getContainingFile();
  }

  public boolean doExecute(final AllOverridingMethodsSearch.SearchParameters queryParameters, final Processor<Pair<PsiMethod, PsiMethod>> consumer) {
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
