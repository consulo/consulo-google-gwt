package com.intellij.gwt.refactorings;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.util.ArrayUtil;

/**
 * @author nik
 */
abstract class GwtJavadocTagInfo implements JavadocTagInfo
{
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
	public Object[] getPossibleValues(final PsiElement context, final PsiElement place, final String prefix)
	{
		return ArrayUtil.EMPTY_OBJECT_ARRAY;
	}

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
