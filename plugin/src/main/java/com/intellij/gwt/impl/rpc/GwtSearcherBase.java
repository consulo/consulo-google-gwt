package com.intellij.gwt.impl.rpc;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.application.util.function.Processor;
import consulo.application.util.query.QueryExecutor;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.language.psi.PsiFile;

/**
 * @author nik
 */
public abstract class GwtSearcherBase<Result, Param> implements QueryExecutor<Result, Param>
{
	@Override
	public boolean execute(final Param queryParameters, final Processor<? super Result> consumer)
	{
		return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>()
		{
			@Override
			public Boolean compute()
			{
				PsiFile file = getContainingFile(queryParameters);
				if(file != null)
				{
					GoogleGwtModuleExtension gwtFacet = GwtModuleExtensionUtil.findModuleExtension(file.getProject(), file.getVirtualFile());
					if(gwtFacet != null)
					{
						return doExecute(queryParameters, consumer);
					}
				}
				return true;
			}
		});
	}

	protected abstract PsiFile getContainingFile(Param parameters);

	protected abstract boolean doExecute(final Param queryParameters, final Processor<? super Result> consumer);
}
