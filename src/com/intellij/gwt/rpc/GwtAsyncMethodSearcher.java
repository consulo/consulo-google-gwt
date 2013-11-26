package com.intellij.gwt.rpc;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.search.searches.SuperMethodsSearch;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.util.Processor;

/**
 * @author nik
 */
public class GwtAsyncMethodSearcher extends GwtSearcherBase<MethodSignatureBackedByPsiMethod, SuperMethodsSearch.SearchParameters>
{
	@Override
	protected PsiFile getContainingFile(final SuperMethodsSearch.SearchParameters parameters)
	{
		return parameters.getMethod().getContainingFile();
	}

	@Override
	public boolean doExecute(final SuperMethodsSearch.SearchParameters queryParameters, final Processor<MethodSignatureBackedByPsiMethod> consumer)
	{
		PsiMethod method = queryParameters.getMethod();
		PsiClass sync = method.getContainingClass();
		PsiClass async = RemoteServiceUtil.findAsynchronousInterface(sync);
		if(async != null)
		{
			PsiMethod asyncMethod = RemoteServiceUtil.findMethodInAsync(method, async);
			if(asyncMethod != null)
			{
				if(!consumer.process(MethodSignatureBackedByPsiMethod.create(asyncMethod, PsiSubstitutor.EMPTY)))
				{
					return false;
				}
			}
		}
		return true;
	}
}
