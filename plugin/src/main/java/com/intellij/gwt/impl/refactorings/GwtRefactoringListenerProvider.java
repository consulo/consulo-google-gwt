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

package com.intellij.gwt.impl.refactorings;

import com.intellij.gwt.impl.i18n.GwtI18nManager;
import com.intellij.gwt.base.rpc.RemoteServiceUtil;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import consulo.annotation.component.ExtensionImpl;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.language.editor.refactoring.RefactoringFactory;
import consulo.language.editor.refactoring.RenameRefactoring;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.RefactoringElementListenerProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.usage.UsageInfo;
import consulo.util.lang.Comparing;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@ExtensionImpl
public class GwtRefactoringListenerProvider implements RefactoringElementListenerProvider
{
	private static final Function<String, String> STRING2STRING_ID = s -> s;
	private ThreadLocal<Boolean> myInsideGwtListener = new ThreadLocal<Boolean>()
	{
		@Override
		protected Boolean initialValue()
		{
			return Boolean.FALSE;
		}
	};

	@Override
	@Nullable
	public RefactoringElementListener getListener(PsiElement element)
	{
		final PsiFile containingFile = element.getContainingFile();
		if(containingFile == null || !GwtModuleExtensionUtil.hasModuleExtension(element.getProject(), containingFile.getVirtualFile()))
		{
			return null;
		}

		RefactoringElementListener listener = null;
		if(element instanceof PsiClass)
		{
			listener = getServiceClassListener((PsiClass) element);
		}
		else if(element instanceof PsiMethod)
		{
			listener = getServiceMethodListener((PsiMethod) element);
		}

		if(listener != null)
		{
			return listener;
		}

		return getPropertiesClassListener(element);
	}

	@Nullable
	private RefactoringElementListener getServiceMethodListener(final PsiMethod method)
	{
		final PsiMethod asyncMethod = RemoteServiceUtil.findAsynchronousMethod(method);
		if(asyncMethod == null)
		{
			return null;
		}
		return new RefactoringElementListenerBase()
		{
			@Override
			public void elementRenamed(@Nonnull final PsiElement newElement)
			{
				rename(asyncMethod, ((PsiMethod) newElement).getName());
			}
		};
	}

	@Nullable
	private RefactoringElementListener getServiceClassListener(final PsiClass psiClass)
	{
		if(!RemoteServiceUtil.isRemoteServiceInterface(psiClass))
		{
			return null;
		}

		final PsiClass async = RemoteServiceUtil.findAsynchronousInterface(psiClass);

		if(async == null)
		{
			return null;
		}

		return new RefactoringElementListenerBase()
		{
			@Override
			public void elementRenamed(@Nonnull PsiElement newElement)
			{
				rename(async, ((PsiClass) newElement).getName() + RemoteServiceUtil.ASYNC_SUFFIX);
			}
		};
	}

	@Nullable
	private RefactoringElementListener getPropertiesClassListener(PsiElement element)
	{
		final GwtI18nManager i18nManager = GwtI18nManager.getInstance(element.getProject());
		final Map<PsiNamedElement, Function<String, String>> elementsToRename = new HashMap<PsiNamedElement, Function<String, String>>(1);

		if(element instanceof Property)
		{
			final Property property = (Property) element;
			final PsiMethod method = i18nManager.getMethod(property);
			if(method != null && Comparing.equal(property.getUnescapedKey(), method.getName()))
			{
				elementsToRename.put(method, STRING2STRING_ID);
			}
		}

		if(element instanceof PsiMethod)
		{
			final PsiMethod method = (PsiMethod) element;
			final IProperty[] properties = i18nManager.getProperties(method);
			for(IProperty property : properties)
			{
				if(Comparing.equal(property.getUnescapedKey(), method.getName()))
				{
					elementsToRename.put((PsiNamedElement) property.getPsiElement(), STRING2STRING_ID);
				}
			}
		}

		if(element instanceof PsiClass)
		{
			final PsiClass psiClass = (PsiClass) element;
			final String className = psiClass.getName();
			final PropertiesFile[] files = i18nManager.getPropertiesFiles(psiClass);
			for(PropertiesFile file : files)
			{
				final String fileName = file.getName();
				if(className != null && fileName.startsWith(className))
				{
					final String suffix = fileName.substring(className.length());
					elementsToRename.put(file.getContainingFile(), s -> s + suffix);
				}
			}
		}

		if(!elementsToRename.isEmpty())
		{
			return new RefactoringElementListenerBase()
			{
				@Override
				public void elementRenamed(@Nonnull PsiElement newElement)
				{
					for(Map.Entry<PsiNamedElement, Function<String, String>> entry : elementsToRename.entrySet())
					{
						final PsiNamedElement psiElement = entry.getKey();
						final String newName = ((PsiNamedElement) newElement).getName();
						rename(psiElement, entry.getValue().apply(newName));
					}
				}
			};
		}
		return null;
	}

	private void rename(final PsiElement element, final String newName)
	{
		if(Boolean.TRUE.equals(myInsideGwtListener.get()))
		{
			return;
		}
		try
		{
			myInsideGwtListener.set(true);
			RenameRefactoring rename = RefactoringFactory.getInstance(element.getProject()).createRename(element, newName);
			rename.setSearchInComments(false);
			rename.setSearchInNonJavaFiles(false);
			rename.setPreviewUsages(false);

			UsageInfo[] usage = rename.findUsages();
			if(rename.preprocessUsages(new Ref<UsageInfo[]>(usage)))
			{
				rename.doRefactoring(usage);
			}
		}
		finally
		{
			myInsideGwtListener.set(false);
		}
	}

	private static abstract class RefactoringElementListenerBase implements RefactoringElementListener
	{
		@Override
		public void elementMoved(@Nonnull PsiElement newElement)
		{
		}

		@Override
		public void elementRenamed(@Nonnull PsiElement newElement)
		{
		}
	}

}
