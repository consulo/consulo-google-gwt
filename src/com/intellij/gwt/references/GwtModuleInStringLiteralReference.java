package com.intellij.gwt.references;

import javax.annotation.Nullable;

import com.intellij.psi.PsiLiteralExpression;

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
