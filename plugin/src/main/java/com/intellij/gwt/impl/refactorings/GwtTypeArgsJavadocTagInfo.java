package com.intellij.gwt.impl.refactorings;

import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class GwtTypeArgsJavadocTagInfo extends GwtJavadocTagInfo
{
	public GwtTypeArgsJavadocTagInfo()
	{
		super("gwt.typeArgs");
	}

	@Override
	protected boolean isValidFor(final @Nonnull PsiMethod psiMethod)
	{
		return RemoteServiceUtil.isRemoteServiceInterface(psiMethod.getContainingClass());
	}
}
