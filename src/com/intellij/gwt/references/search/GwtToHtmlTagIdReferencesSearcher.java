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

package com.intellij.gwt.references.search;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.references.GwtToHtmlTagReference;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.TextOccurenceProcessor;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;

/**
 * @author nik
 */
public class GwtToHtmlTagIdReferencesSearcher implements QueryExecutor<PsiReference, ReferencesSearch.SearchParameters>
{
	@Override
	public boolean execute(final ReferencesSearch.SearchParameters queryParameters, final Processor<PsiReference> consumer)
	{
		return new ReadAction<Boolean>()
		{
			@Override
			protected void run(final Result<Boolean> result)
			{
				result.setResult(doExecute(queryParameters, consumer));
			}
		}.execute().getResultObject();
	}

	private static boolean doExecute(final ReferencesSearch.SearchParameters queryParameters, final Processor<PsiReference> consumer)
	{
		final PsiElement element = queryParameters.getElementToSearch();
		if(!(element instanceof XmlAttributeValue))
		{
			return true;
		}

		final PsiElement parent = element.getParent();
		if(!(parent instanceof XmlAttribute) || !"id".equals(((XmlAttribute) parent).getLocalName()))
		{
			return true;
		}
		String id = ((XmlAttributeValue) element).getValue();

		final PsiElement tag = parent.getParent();
		if(!(tag instanceof XmlTag))
		{
			return true;
		}

		final PsiFile file = parent.getContainingFile();
		if(!file.getLanguage().equals(StdLanguages.HTML) && !file.getLanguage().equals(StdLanguages.XHTML))
		{
			return true;
		}

		final GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(file.getProject());
		final VirtualFile virtualFile = file.getVirtualFile();
		if(virtualFile == null)
		{
			return true;
		}

		final GwtModule gwtModule = gwtModulesManager.findGwtModuleByClientOrPublicFile(virtualFile);
		if(gwtModule == null)
		{
			return true;
		}

		final PsiSearchHelper searchHelper = PsiSearchHelper.SERVICE.getInstance(element.getProject());
		return searchHelper.processElementsWithWord(new TextOccurenceProcessor()
		{
			@Override
			public boolean execute(PsiElement element, int offsetInElement)
			{
				if(!(element instanceof PsiLiteralExpression))
				{
					return true;
				}

				final PsiReference[] references = element.getReferences();
				for(PsiReference reference : references)
				{
					if(reference instanceof GwtToHtmlTagReference && reference.isReferenceTo(tag))
					{
						if(!consumer.process(reference))
						{
							return false;
						}
					}
				}
				return true;
			}
		}, queryParameters.getScope(), id, UsageSearchContext.IN_STRINGS, true);
	}
}
