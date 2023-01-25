package com.intellij.gwt.impl.refactorings;

import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.javadoc.JavadocTagInfo;
import com.intellij.java.language.psi.javadoc.PsiDocTagValue;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
abstract class GwtJavadocTagInfo implements JavadocTagInfo
{
	private String myName;

	GwtJavadocTagInfo(final String name)
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

	protected abstract boolean isValidFor(final @Nonnull PsiMethod psiMethod);

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
