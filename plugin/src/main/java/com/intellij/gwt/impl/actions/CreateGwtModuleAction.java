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

package com.intellij.gwt.impl.actions;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.base.actions.GwtCreateActionBase;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.base.templates.GwtTemplates;
import com.intellij.java.language.psi.JavaDirectoryService;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.component.ActionImpl;
import consulo.fileTemplate.FileTemplate;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.util.lang.StringUtil;
import consulo.xml.ide.highlighter.HtmlFileType;

import javax.annotation.Nonnull;
import java.util.ArrayList;

@ActionImpl(id = "GWT.NewModule")
public class CreateGwtModuleAction extends GwtCreateActionBase
{
	public CreateGwtModuleAction()
	{
		super(GwtBundle.message("newmodule.menu.action.text"), GwtBundle.message("newmodule.menu.action.description"));
	}

	@Override
	protected boolean requireGwtModule()
	{
		return false;
	}

	@Override
	protected String getDialogPrompt()
	{
		return GwtBundle.message("newmodule.dlg.prompt");
	}

	@Override
	protected String getDialogTitle()
	{
		return GwtBundle.message("newmodule.dlg.title");
	}

	@Override
	protected void doCheckBeforeCreate(final String name, final PsiDirectory directory) throws IncorrectOperationException
	{
		String[] names = name.split("\\.");
		for(String id : names)
		{
			PsiUtil.checkIsIdentifier(directory.getManager(), id);
		}
	}

	/**
	 * @return created elements. Never null.
	 */
	@Override
	@Nonnull
	protected PsiElement[] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception
	{
		JavaDirectoryService javaDirectoryService = JavaDirectoryService.getInstance();
		PsiJavaPackage psiPackage = javaDirectoryService.getPackage(directory);
		if(psiPackage == null)
		{
			return PsiElement.EMPTY_ARRAY;
		}

		int dot = name.indexOf('.');
		if(dot != -1)
		{
			PsiJavaPackage aPackage;
			while((aPackage = javaDirectoryService.getPackage(directory)) != null && aPackage.getParentPackage() != null)
			{
				directory = directory.getParentDirectory();
			}

			while(dot != -1)
			{
				String directoryName = name.substring(0, dot);
				PsiDirectory subDirectory = directory.findSubdirectory(directoryName);
				if(subDirectory == null)
				{
					subDirectory = directory.createSubdirectory(directoryName);
				}
				directory = subDirectory;
				name = name.substring(dot + 1);
				dot = name.indexOf('.');
			}
			psiPackage = javaDirectoryService.getPackage(directory);
			if(psiPackage == null)
			{
				return PsiElement.EMPTY_ARRAY;
			}
		}

		String moduleName = StringUtil.capitalize(name);
		final ArrayList<PsiElement> res = new ArrayList<PsiElement>();

		GwtVersion version = GwtModuleExtensionUtil.getVersion(directory);

		PsiDirectory client = directory.createSubdirectory(GwtModulesManager.DEFAULT_SOURCE_PATH);
		res.add(client);
		final PsiClass entryPointClass = createClassFromTemplate(client, moduleName, GwtTemplates.GWT_ENTRY_POINT_JAVA);

		String appPackageName = psiPackage.getQualifiedName();
		res.add(createFromTemplateInternal(directory, moduleName, moduleName + GwtModulesManager.GWT_XML_SUFFIX, GwtTemplates.GWT_MODULE_GWT_XML,
				"ENTRY_POINT_CLASS", entryPointClass.getQualifiedName()));

		PsiDirectory server = directory.createSubdirectory("server");
		res.add(server);

		PsiDirectory publicDir = directory.createSubdirectory(GwtModulesManager.DEFAULT_PUBLIC_PATH);
		res.add(publicDir);
		final String gwtModuleName = appPackageName.length() > 0 ? appPackageName + "." + moduleName : moduleName;
		String gwtModuleHtml = version.getGwtModuleHtmlTemplate();
		res.add(createFromTemplate(publicDir, moduleName + "." + HtmlFileType.INSTANCE.getDefaultExtension(), gwtModuleHtml,
				FileTemplate.ATTRIBUTE_PACKAGE_NAME, appPackageName, "GWT_MODULE_NAME", gwtModuleName));
		/*res.add(createFromTemplate(publicDir, moduleName + "." + CSS_EXTENSION, GwtTemplates.GWT_MODULE_CSS, FileTemplate.ATTRIBUTE_PACKAGE_NAME,
				appPackageName));*/
		res.add(entryPointClass);

		return res.toArray(new PsiElement[res.size()]);
	}

	@Override
	protected String getCommandName()
	{
		return GwtBundle.message("newmodule.command.name");
	}

	@Override
	protected String getActionName(PsiDirectory directory, String newName)
	{
		return GwtBundle.message("newmodule.progress.text", newName);
	}
}
