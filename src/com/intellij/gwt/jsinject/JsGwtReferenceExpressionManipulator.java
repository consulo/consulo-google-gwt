/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.gwt.jsinject;

import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JavaScriptSupportLoader;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.IncorrectOperationException;

/**
 * @author peter
 */
public class JsGwtReferenceExpressionManipulator extends AbstractElementManipulator<JSGwtReferenceExpressionImpl>
{
	@Override
	public JSGwtReferenceExpressionImpl handleContentChange(final JSGwtReferenceExpressionImpl element, final TextRange range,
			final String newContent) throws IncorrectOperationException
	{
		String newText = range.replace(element.getText(), newContent);
		ASTNode newExpression = JSChangeUtil.createExpressionFromText(element.getProject(), newText, JavaScriptSupportLoader.GWT_DIALECT);
		return (JSGwtReferenceExpressionImpl) element.replace(newExpression.getPsi());
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
}
