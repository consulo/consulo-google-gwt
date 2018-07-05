/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.gwt.javascript.lang.GwtJavaScriptVersion;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.jsinject.GwtClassMemberReference;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiReference;

/**
 * @author nik
 */
public class GwtJavaScriptReferencesInspection extends BaseGwtInspection
{
	@Override
	@Nls
	@Nonnull
	public String getDisplayName()
	{
		return GwtBundle.message("inspection.name.unresolved.references.in.jsni.methods");
	}

	@Override
	@NonNls
	@Nonnull
	public String getShortName()
	{
		return "GwtJavaScriptReferences";
	}

	@Override
	@Nullable
	public ProblemDescriptor[] checkFile(@Nonnull final PsiFile file, @Nonnull final InspectionManager manager, final boolean isOnTheFly)
	{
		if(!(file.getLanguageVersion() instanceof GwtJavaScriptVersion))
		{
			return ProblemDescriptor.EMPTY_ARRAY;
		}

		final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
		file.accept(new PsiRecursiveElementVisitor()
		{
			@Override
			public void visitElement(final PsiElement element)
			{
				if(element instanceof JSGwtReferenceExpressionImpl)
				{
					PsiReference[] references = element.getReferences();
					for(PsiReference reference : references)
					{
						if(reference.resolve() == null)
						{
							if(reference instanceof GwtClassMemberReference)
							{
								PsiClass psiClass = ((GwtClassMemberReference) reference).resolveQualifier();
								if(psiClass != null)
								{
									String message = GwtBundle.message("problem.description.cannot.resolve.symbol.0.in.1", reference.getCanonicalText(),
											psiClass.getQualifiedName());
									problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), message,
											ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
								}
							}
							else
							{
								String message = GwtBundle.message("problem.description.cannot.resolve.0", reference.getCanonicalText());
								problems.add(manager.createProblemDescriptor(element, reference.getRangeInElement(), message,
										ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
							}
						}
					}
				}
				else
				{
					super.visitElement(element);
				}
			}
		});
		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}
}
