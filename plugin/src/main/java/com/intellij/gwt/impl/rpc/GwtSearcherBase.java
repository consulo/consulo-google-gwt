package com.intellij.gwt.impl.rpc;

import consulo.application.Application;
import consulo.application.util.query.QueryExecutor;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author nik
 */
public abstract class GwtSearcherBase<Result, Param> implements QueryExecutor<Result, Param> {
    @Override
    public boolean execute(@Nonnull Param queryParameters, @Nonnull Predicate<? super Result> consumer) {
        return Application.get().runReadAction((Supplier<Boolean>)() -> {
            PsiFile file = getContainingFile(queryParameters);
            if (file != null) {
                GoogleGwtModuleExtension gwtFacet =
                    GwtModuleExtensionUtil.findModuleExtension(file.getProject(), file.getVirtualFile());
                if (gwtFacet != null) {
                    return doExecute(queryParameters, consumer);
                }
            }
            return true;
        });
    }

    protected abstract PsiFile getContainingFile(Param parameters);

    protected abstract boolean doExecute(Param queryParameters, Predicate<? super Result> consumer);
}
