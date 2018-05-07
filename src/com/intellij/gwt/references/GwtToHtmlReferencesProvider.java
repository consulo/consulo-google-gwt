/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.gwt.references;

import javax.annotation.Nonnull;
import consulo.annotations.RequiredReadAction;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;

/**
 * @author nik
 */
public class GwtToHtmlReferencesProvider extends PsiReferenceProvider
{

	@Override
	@Nonnull
	@RequiredReadAction
	public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull final ProcessingContext context)
	{
		if(element instanceof PsiLiteralExpression)
		{
			GoogleGwtModuleExtension extension = ModuleUtilCore.getExtension(element, GoogleGwtModuleExtension.class);
			if(extension == null)
			{
				return PsiReference.EMPTY_ARRAY;
			}
			final PsiLiteralExpression literalExpression = (PsiLiteralExpression) element;
			if(literalExpression.getValue() instanceof String)
			{
				return new PsiReference[]{new GwtToHtmlTagReference(literalExpression)};
			}
		}
		return PsiReference.EMPTY_ARRAY;
	}

}
