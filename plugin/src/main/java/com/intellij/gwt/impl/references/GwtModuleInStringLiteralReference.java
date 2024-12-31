package com.intellij.gwt.impl.references;

import com.intellij.java.language.psi.PsiLiteralExpression;

import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class GwtModuleInStringLiteralReference extends GwtModuleReference<PsiLiteralExpression>
{
	public GwtModuleInStringLiteralReference(final PsiLiteralExpression element)
	{
		super(element);
	}

	@Override
	@Nullable
	protected String getStringValue()
	{
		Object value = myElement.getValue();
		return value instanceof String ? (String) value : null;
	}
}
