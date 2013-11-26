package com.intellij.gwt.rpc;

import com.intellij.psi.PsiFile;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;

/**
 * @author nik
 */
public abstract class GwtSearcherBase<Result, Param> implements QueryExecutor<Result, Param> {
  public boolean execute(final Param queryParameters, final Processor<Result> consumer) {
    return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
      public Boolean compute() {
        PsiFile file = getContainingFile(queryParameters);
        if (file != null) {
          GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(file.getProject(), file.getVirtualFile());
          if (gwtFacet != null) {
            return doExecute(queryParameters, consumer);
          }
        }
        return true;
      }
    });
  }

  protected abstract PsiFile getContainingFile(Param parameters);

  protected abstract boolean doExecute(final Param queryParameters, final Processor<Result> consumer);
}
