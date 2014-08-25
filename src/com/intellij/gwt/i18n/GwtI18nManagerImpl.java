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

package com.intellij.gwt.i18n;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtensionUtil;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;

/**
 * @author nik
 */
public class GwtI18nManagerImpl extends GwtI18nManager
{
	private static final Property[] EMPTY_PROPERTIES_ARRAY = new Property[0];
	private static final PropertiesFile[] EMPTY_PROPERTIES_FILE_ARRAY = new PropertiesFile[0];
	private final PsiManager myPsiManager;
	private final Project myProject;

	public GwtI18nManagerImpl(PsiManager psiManager, final Project project)
	{
		myProject = project;
		myPsiManager = psiManager;
	}

	private boolean isConstantsOrMessagesInterface(@NotNull PsiClass aClass)
	{
		if(!GoogleGwtModuleExtensionUtil.hasModuleExtension(myProject, getOriginalContainingFile(aClass).getVirtualFile()) || !aClass.isInterface())
		{
			return false;
		}

		return isConstantsInterface(aClass) || isExtendingInterface(aClass, GwtI18nUtil.MESSAGES_INTERFACE_NAME);
	}

	private boolean isExtendingInterface(final PsiClass aClass, final String superInterfaceName)
	{
		if(!aClass.isInterface())
		{
			return false;
		}

		final PsiClass constantsClass = JavaPsiFacade.getInstance(myPsiManager.getProject()).findClass(superInterfaceName, aClass.getResolveScope());
		return constantsClass != null && aClass.isInheritor(constantsClass, true);
	}

	@Override
	public boolean isConstantsInterface(@NotNull final PsiClass aClass)
	{
		return isExtendingInterface(aClass, GwtI18nUtil.CONSTANTS_INTERFACE_NAME);
	}

	@Override
	public boolean isLocalizableInterface(@NotNull PsiClass aClass)
	{
		return isExtendingInterface(aClass, GwtI18nUtil.LOCALIZABLE_INTERFACE_NAME);
	}

	@Override
	@NotNull
	public PropertiesFile[] getPropertiesFiles(@NotNull PsiClass anInterface)
	{
		PsiFile containingFile = getOriginalContainingFile(anInterface);
		final PsiDirectory psiDirectory = containingFile.getContainingDirectory();
		if(psiDirectory == null || !isConstantsOrMessagesInterface(anInterface))
		{
			return EMPTY_PROPERTIES_FILE_ARRAY;
		}

		final PsiFile[] psiFiles = psiDirectory.getFiles();
		List<PropertiesFile> files = new ArrayList<PropertiesFile>();
		for(PsiFile psiFile : psiFiles)
		{
			if(psiFile instanceof PropertiesFile)
			{
				final PropertiesFile propertiesFile = (PropertiesFile) psiFile;
				final String fileName = propertiesFile.getName();
				final String interfaceName = anInterface.getName();
				if(isFileNameForInterfaceName(fileName, interfaceName))
				{
					files.add(propertiesFile);
				}
			}
		}
		return files.toArray(new PropertiesFile[files.size()]);
	}

	private static PsiFile getOriginalContainingFile(final PsiClass anInterface)
	{
		PsiFile containingFile = anInterface.getContainingFile();
		PsiFile originalFile = containingFile.getOriginalFile();
		if(originalFile != null)
		{
			containingFile = originalFile;
		}
		return containingFile;
	}

	private static boolean isFileNameForInterfaceName(final @Nullable String fileName, final @Nullable String interfaceName)
	{
		return fileName != null && interfaceName != null &&
				(fileName.equals(interfaceName + "." + StdFileTypes.PROPERTIES.getDefaultExtension()) || fileName.startsWith(interfaceName + "_"));
	}

	@Override
	@Nullable
	public PsiClass getPropertiesInterface(@NotNull PropertiesFile file)
	{
		final String fileName = file.getName();
		final PsiDirectory directory = file.getContainingFile().getContainingDirectory();
		if(directory == null || !GoogleGwtModuleExtensionUtil.hasModuleExtension(myProject, file.getVirtualFile()))
		{
			return null;
		}

		for(PsiFile psiFile : directory.getFiles())
		{
			if(psiFile instanceof PsiJavaFile)
			{
				final PsiClass[] psiClasses = ((PsiJavaFile) psiFile).getClasses();
				for(PsiClass psiClass : psiClasses)
				{
					if(isFileNameForInterfaceName(fileName, psiClass.getName()) && isConstantsOrMessagesInterface(psiClass))
					{
						return psiClass;
					}
				}
			}
		}

		return null;
	}

	@Override
	@NotNull
	public IProperty[] getProperties(@NotNull PsiMethod method)
	{
		final PsiClass aClass = method.getContainingClass();
		if(aClass == null)
		{
			return EMPTY_PROPERTIES_ARRAY;
		}

		final PropertiesFile[] files = getPropertiesFiles(aClass);
		if(files.length == 0)
		{
			return EMPTY_PROPERTIES_ARRAY;
		}

		final String propertyName = GwtI18nUtil.getPropertyName(method);
		List<IProperty> properties = new ArrayList<IProperty>();
		for(PropertiesFile file : files)
		{
			final IProperty property = file.findPropertyByKey(propertyName);
			if(property != null)
			{
				properties.add(property);
			}
		}
		return properties.toArray(new IProperty[properties.size()]);
	}

	@Override
	@Nullable
	public PsiMethod getMethod(@NotNull IProperty property)
	{
		final PsiClass psiClass = getPropertiesInterface(property.getPropertiesFile());
		if(psiClass == null)
		{
			return null;
		}

		final PsiMethod[] psiMethods = psiClass.getMethods();
		for(PsiMethod psiMethod : psiMethods)
		{
			final String propertyName = GwtI18nUtil.getPropertyName(psiMethod);
			if(propertyName != null && propertyName.equals(property.getUnescapedKey()))
			{
				return psiMethod;
			}
		}

		return null;
	}
}
