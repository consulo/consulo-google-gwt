/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.gwt.jsinject;

import com.intellij.java.impl.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.util.collection.ArrayUtil;

import jakarta.annotation.Nonnull;

public class JSGwtReferenceExpressionImpl extends JSReferenceExpressionImpl
{
	public JSGwtReferenceExpressionImpl(final ASTNode node)
	{
		super(node);
	}

	@Override
	@Nonnull
	public PsiReference[] getReferences()
	{
		PsiElement at = findChildByType(JSTokenTypes.AT);
		if(at == null)
		{
			return PsiReference.EMPTY_ARRAY;
		}
		PsiElement classNameStart = at.getNextSibling();
		if(classNameStart == null)
		{
			return PsiReference.EMPTY_ARRAY;
		}

		PsiElement colon2 = findChildByType(JSTokenTypes.COLON_COLON);
		PsiElement classNameFinish;
		if(colon2 == null)
		{
			classNameFinish = getLastChild();
		}
		else
		{
			classNameFinish = colon2.getPrevSibling();
		}
		if(classNameFinish == null)
		{
			return PsiReference.EMPTY_ARRAY;
		}

		TextRange classNameRange = new TextRange(classNameStart.getStartOffsetInParent(), classNameFinish.getStartOffsetInParent() + classNameFinish
				.getTextLength());

		JavaClassReferenceProvider referenceProvider = new JavaClassReferenceProvider();

		PsiReference[] classReferences = referenceProvider.getReferencesByString(classNameRange.substring(getText()), this,
				classNameRange.getStartOffset());

		PsiElement member = findChildByType(JSTokenTypes.GWT_FIELD_OR_METHOD);
		if(member == null)
		{
			return classReferences;
		}
		TextRange range = TextRange.from(member.getStartOffsetInParent(), member.getTextLength());
		PsiReference classReference = classReferences.length > 0 ? classReferences[classReferences.length - 1] : null;
		GwtClassMemberReference classMemberReference = new GwtClassMemberReference(this, classReference, range);
		return ArrayUtil.append(classReferences, classMemberReference, PsiReference.class);
	}

	@Override
	public boolean shouldCheckReferences()
	{
		return false;
	}
}