package com.intellij.gwt.impl.refactorings;

import com.intellij.gwt.impl.i18n.GwtI18nManager;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;

import jakarta.annotation.Nonnull;

@ExtensionImpl
public class GwtKeyJavadocTagInfo extends GwtJavadocTagInfo
{
	public GwtKeyJavadocTagInfo()
	{
		super("gwt.key");
	}

	@Override
	protected boolean isValidFor(final @Nonnull PsiMethod psiMethod)
	{
		PsiClass aClass = psiMethod.getContainingClass();
		return aClass != null && GwtI18nManager.getInstance(psiMethod.getProject()).isLocalizableInterface(aClass);
	}
}
