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

package com.intellij.gwt.actions;

import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;

public class CreateGwtEntryPointAction extends GwtCreateActionBase
{
	public CreateGwtEntryPointAction()
	{
		super(GwtBundle.message("newentrypoint.menu.action.text"), GwtBundle.message("newentrypoint.menu.action.description"));
	}

	@Override
	protected boolean requireGwtModule()
	{
		return true;
	}

	@Override
	protected String getDialogPrompt()
	{
		return GwtBundle.message("newentrypoint.dlg.prompt");
	}

	@Override
	protected String getDialogTitle()
	{
		return GwtBundle.message("newentrypoint.dlg.title");
	}

	@Override
	@NotNull
	protected PsiElement[] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception
	{
		final PsiClass entryPointClass = createClassFromTemplate(directory, name, GwtTemplates.GWT_ENTRY_POINT_JAVA);

		XmlFile xml = gwtModule.getModuleXmlFile();
		if(xml == null)
		{
			return PsiElement.EMPTY_ARRAY;
		}

		gwtModule.addEntryPoint().getEntryClass().setValue(entryPointClass.getQualifiedName());

		return new PsiElement[]{entryPointClass.getContainingFile()};
	}


	@Override
	protected PsiFile[] getAffectedFiles(final GwtModule gwtModule)
	{
		return new PsiFile[]{gwtModule.getModuleXmlFile()};
	}

	@Override
	protected String getCommandName()
	{
		return GwtBundle.message("newentrypoint.command.name");
	}

	@Override
	protected String getActionName(PsiDirectory directory, String newName)
	{
		return GwtBundle.message("newentrypoint.progress.text", newName);
	}
}