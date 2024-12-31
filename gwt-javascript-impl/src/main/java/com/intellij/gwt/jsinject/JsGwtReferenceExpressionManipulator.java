/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.gwt.jsinject;

import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
@ExtensionImpl
public class JsGwtReferenceExpressionManipulator extends AbstractElementManipulator<JSGwtReferenceExpressionImpl>
{
	@Override
	public JSGwtReferenceExpressionImpl handleContentChange(final JSGwtReferenceExpressionImpl element, final TextRange range,
			final String newContent) throws IncorrectOperationException
	{
		String newText = range.replace(element.getText(), newContent);
		JSExpression newExpression = JSChangeUtil.createExpressionFromText(element.getProject(), newText);
		return (JSGwtReferenceExpressionImpl) element.replace(newExpression);
	}

	@Override
	public TextRange getRangeInElement(final JSGwtReferenceExpressionImpl element)
	{
		String text = element.getText();
		int start = text.indexOf('@');
		if(start == -1)
		{
			start = 0;
		}
		int end = text.indexOf("::");
		if(end == -1)
		{
			end = text.length();
		}
		return new TextRange(start, end);
	}

	@Nonnull
	@Override
	public Class<JSGwtReferenceExpressionImpl> getElementClass()
	{
		return JSGwtReferenceExpressionImpl.class;
	}
}
