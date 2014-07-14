/*
 * Copyright 2013-2014 must-be.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt;

import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JavascriptParserDefinition;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class GwtJsInjector implements ApplicationComponent
{
	@Override
	public void initComponent()
	{
		JavascriptParserDefinition.setGwtReferenceExpressionCreator(new Function<ASTNode, PsiElement>()
		{
			@Override
			public PsiElement fun(final ASTNode astNode)
			{
				return new JSGwtReferenceExpressionImpl(astNode);
			}
		});
	}

	@Override
	public void disposeComponent()
	{

	}

	@NotNull
	@Override
	public String getComponentName()
	{
		return "GwtJsInjector";
	}
}
