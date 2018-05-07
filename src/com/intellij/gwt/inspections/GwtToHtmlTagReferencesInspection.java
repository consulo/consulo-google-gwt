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

package com.intellij.gwt.inspections;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.references.GwtToHtmlTagReference;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;

/**
 * @author nik
 */
public class GwtToHtmlTagReferencesInspection extends BaseGwtInspection
{
	@Override
	@Nonnull
	public PsiElementVisitor buildVisitor(@Nonnull final ProblemsHolder holder, boolean isOnTheFly)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitReferenceExpression(PsiReferenceExpression expression)
			{
			}

			@Override
			public void visitLiteralExpression(PsiLiteralExpression expression)
			{
				final PsiReference[] references = expression.getReferences();
				for(PsiReference reference : references)
				{
					if(reference instanceof GwtToHtmlTagReference && reference.resolve() == null)
					{
						holder.registerProblem(expression, GwtBundle.message("problem.description.html.tag.with.id.0.is.not.found", expression.getValue()),
								ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
					}
				}
			}
		};
	}

	@Override
	@Nonnull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.unresolved.references.to.html.tags");
	}

	@Override
	@Nonnull
	@NonNls
	public String getShortName()
	{
		return "GwtToHtmlReferences";
	}

}
