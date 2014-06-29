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

package com.intellij.gwt.refactorings;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.i18n.GwtI18nManager;
import com.intellij.gwt.rpc.RemoteServiceUtil;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.javadoc.JavadocManagerImpl;
import com.intellij.psi.javadoc.JavadocManager;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.refactoring.RenameRefactoring;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import com.intellij.refactoring.listeners.RefactoringListenerManager;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;

public class GwtRefactoringListenerProvider implements ProjectComponent, RefactoringElementListenerProvider
{
	private Project myProject;
	private static final Function<String, String> STRING2STRING_ID = new Function<String, String>()
	{
		@Override
		public String fun(final String s)
		{
			return s;
		}
	};
	private ThreadLocal<Boolean> myInsideGwtListener = new ThreadLocal<Boolean>()
	{
		@Override
		protected Boolean initialValue()
		{
			return Boolean.FALSE;
		}
	};

	public GwtRefactoringListenerProvider(final Project project)
	{
		myProject = project;
		JavadocManagerImpl javadocManager = (JavadocManagerImpl) JavadocManager.SERVICE.getInstance(project);
		javadocManager.registerTagInfo(new GwtJavadocTagInfo("gwt.typeArgs")
		{
			@Override
			protected boolean isValidFor(final @NotNull PsiMethod psiMethod)
			{
				return RemoteServiceUtil.isRemoteServiceInterface(psiMethod.getContainingClass());
			}
		});
		javadocManager.registerTagInfo(new GwtJavadocTagInfo("gwt.key")
		{
			@Override
			protected boolean isValidFor(final @NotNull PsiMethod psiMethod)
			{
				PsiClass aClass = psiMethod.getContainingClass();
				return aClass != null && GwtI18nManager.getInstance(project).isLocalizableInterface(aClass);
			}
		});
	}

	@Override
	public void initComponent()
	{
	}

	@Override
	public void disposeComponent()
	{
	}

	@Override
	@NotNull
	public String getComponentName()
	{
		return "GwtRefactoringListenerProvider";
	}

	@Override
	public void projectOpened()
	{
		RefactoringListenerManager.getInstance(myProject).addListenerProvider(this);
	}

	@Override
	public void projectClosed()
	{
		RefactoringListenerManager.getInstance(myProject).removeListenerProvider(this);
	}

	@Override
	@Nullable
	public RefactoringElementListener getListener(PsiElement element)
	{
		final PsiFile containingFile = element.getContainingFile();
		if(containingFile == null || !GwtFacet.isInModuleWithGwtFacet(element.getProject(), containingFile.getVirtualFile()))
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
			public void elementRenamed(@NotNull final PsiElement newElement)
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
			public void elementRenamed(@NotNull PsiElement newElement)
			{
				rename(async, ((PsiClass) newElement).getName() + RemoteServiceUtil.ASYNC_SUFFIX);
			}
		};
	}

	@Nullable
	private RefactoringElementListener getPropertiesClassListener(PsiElement element)
	{
		final GwtI18nManager i18nManager = GwtI18nManager.getInstance(myProject);
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
					elementsToRename.put(file.getContainingFile(), new Function<String, String>()
					{
						@Override
						public String fun(final String s)
						{
							return s + suffix;
						}
					});
				}
			}
		}

		if(!elementsToRename.isEmpty())
		{
			return new RefactoringElementListenerBase()
			{
				@Override
				public void elementRenamed(@NotNull PsiElement newElement)
				{
					for(Map.Entry<PsiNamedElement, Function<String, String>> entry : elementsToRename.entrySet())
					{
						final PsiNamedElement psiElement = entry.getKey();
						final String newName = ((PsiNamedElement) newElement).getName();
						rename(psiElement, entry.getValue().fun(newName));
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
			RenameRefactoring rename = RefactoringFactory.getInstance(myProject).createRename(element, newName);
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
		public void elementMoved(@NotNull PsiElement newElement)
		{
		}

		@Override
		public void elementRenamed(@NotNull PsiElement newElement)
		{
		}
	}

}
