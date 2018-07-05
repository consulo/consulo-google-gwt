package com.intellij.gwt.rpc;

import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.module.extension.GwtModuleExtensionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;

/**
 * @author nik
 */
public abstract class GwtSearcherBase<Result, Param> implements QueryExecutor<Result, Param>
{
	@Override
	public boolean execute(final Param queryParameters, final Processor<Result> consumer)
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

	protected abstract boolean doExecute(final Param queryParameters, final Processor<Result> consumer);
}
