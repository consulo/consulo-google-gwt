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

package com.intellij.gwt.impl.references;

import com.intellij.java.language.psi.PsiLiteralExpression;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.util.ProcessingContext;
import consulo.xml.psi.xml.XmlAttributeValue;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class GwtModuleReferencesProvider extends PsiReferenceProvider
{

	@Override
	@Nonnull
	public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull final ProcessingContext context)
	{
		if(element instanceof XmlAttributeValue)
		{
			return new PsiReference[]{
					new GwtModuleInXmlAttributeReference((XmlAttributeValue) element)
			};
		}
		if(element instanceof PsiLiteralExpression)
		{
			return new PsiReference[]{
					new GwtModuleInStringLiteralReference((PsiLiteralExpression) element)
			};
		}
		return PsiReference.EMPTY_ARRAY;
	}
}
