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

import javax.annotation.Nonnull;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;

public class CreateGwtSerializableClassAction extends GwtCreateActionBase
{
	public CreateGwtSerializableClassAction()
	{
		super(GwtBundle.message("newserial.menu.action.text"), GwtBundle.message("newserial.menu.action.description"));
	}

	@Override
	protected boolean requireGwtModule()
	{
		return true;
	}

	@Override
	protected String getDialogPrompt()
	{
		return GwtBundle.message("newserial.dlg.prompt");
	}

	@Override
	protected String getDialogTitle()
	{
		return GwtBundle.message("newserial.dlg.title");
	}

	@Override
	protected String getCommandName()
	{
		return GwtBundle.message("newserial.command.name");
	}

	@Override
	protected String getActionName(PsiDirectory directory, String newName)
	{
		return GwtBundle.message("newserial.progress.text", newName);
	}

	@Override
	@Nonnull
	protected PsiElement[] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception
	{
		return new PsiElement[]{
				createClassFromTemplate(directory, name, GwtTemplates.GWT_SERIAL_CLASS_JAVA).getContainingFile()
		};
	}
}
