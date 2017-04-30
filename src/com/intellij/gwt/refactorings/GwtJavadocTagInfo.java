package com.intellij.gwt.refactorings;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;

/**
 * @author nik
 */
abstract class GwtJavadocTagInfo implements JavadocTagInfo
{
	public static class GwtTypeArgs extends GwtJavadocTagInfo
	{
		public GwtTypeArgs()
		{
			super("gwt.typeArgs");
		}

		@Override
		protected boolean isValidFor(final @NotNull PsiMethod psiMethod)
		{
			return RemoteServiceUtil.isRemoteServiceInterface(psiMethod.getContainingClass());
		}
	}

	public static class GwtKey extends GwtJavadocTagInfo
	{
		public GwtKey()
		{
			super("gwt.key");
		}

		@Override
		protected boolean isValidFor(final @NotNull PsiMethod psiMethod)
		{
			PsiClass aClass = psiMethod.getContainingClass();
			return aClass != null && GwtI18nManager.getInstance(psiMethod.getProject()).isLocalizableInterface(aClass);
		}
	}

	private String myName;

	GwtJavadocTagInfo(final @NonNls String name)
	{
		myName = name;
	}

	@Override
	public String getName()
	{
		return myName;
	}

	@Override
	public boolean isInline()
	{
		return false;
	}

	@Override
	public boolean isValidInContext(final PsiElement element)
	{
		return element instanceof PsiMethod && isValidFor((PsiMethod) element);
	}

	protected abstract boolean isValidFor(final @NotNull PsiMethod psiMethod);

	@Override
	public String checkTagValue(final PsiDocTagValue value)
	{
		return null;
	}

	@Override
	public PsiReference getReference(final PsiDocTagValue value)
	{
		return null;
	}
}
