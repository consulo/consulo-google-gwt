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
import com.intellij.java.language.impl.JavaFileType;
import com.intellij.java.language.psi.JavaDirectoryService;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.intellij.java.language.psi.util.PsiUtil;
import consulo.annotation.component.ActionImpl;
import consulo.fileTemplate.FileTemplate;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import consulo.xml.ide.highlighter.HtmlFileType;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;

@ActionImpl(id = "GWT.NewAppSample")
public class CreateGwtSampleAppAction extends GwtCreateActionBase
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.actions.CreateGwtSampleAppAction");
	@NonNls
	private static final String SERVICE_SUFFIX = "Service";
	@NonNls
	private static final String SERVICE_ASYNC_SUFFIX = "ServiceAsync";
	@NonNls
	private static final String SERVICE_IMPL_SUFFIX = "ServiceImpl";
	@NonNls
	private static final String SERVICE_NAME_PROPERTY = "SERVICE_NAME";
	@NonNls
	private static final String CLIENT_PACKAGE_PROPERTY = "CLIENT_PACKAGE";
	@NonNls
	private static final String RELATIVE_PATH_PROPERTY = "RELATIVE_SERVLET_PATH";

	public CreateGwtSampleAppAction()
	{
		super(GwtBundle.message("newapp.menu.action.text"), GwtBundle.message("newapp.menu.action.description"));
	}

	@Override
	protected boolean requireGwtModule()
	{
		return false;
	}

	@Override
	protected String getDialogPrompt()
	{
		return GwtBundle.message("newapp.dlg.prompt");
	}

	@Override
	protected String getDialogTitle()
	{
		return GwtBundle.message("newapp.dlg.title");
	}

	@Override
	protected void doCheckBeforeCreate(String name, PsiDirectory directory) throws IncorrectOperationException
	{
		GwtVersion version = GwtModuleExtensionUtil.getVersion(directory);
		LOG.assertTrue(version != null);
		if(!version.isGenericsSupported())
		{
			final String message = GwtBundle.message("error.message.sample.application.requires.gwt.1.5.or.later");
			throw new IncorrectOperationException(message);
		}

		PsiUtil.checkIsIdentifier(directory.getManager(), name);
		directory.checkCreateSubdirectory(name);
	}

	@Override
	@Nonnull
	protected PsiElement[] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception
	{
		name = StringUtil.capitalize(name);
		final ArrayList<PsiElement> result = new ArrayList<PsiElement>(0);

		PsiDirectory moduleDir = directory.createSubdirectory(name.toLowerCase());
		result.add(moduleDir);
		final PsiJavaPackage psiPackage = JavaDirectoryService.getInstance().getPackage(moduleDir);
		if(psiPackage == null)
		{
			return PsiElement.EMPTY_ARRAY;
		}
		String appPackageName = psiPackage.getQualifiedName();
		result.add(createFromTemplateInternal(moduleDir, name, name + GwtModulesManager.GWT_XML_SUFFIX, GwtTemplates.GWT_SAMPLE_APP_GWT_XML));

		PsiDirectory clientDir = moduleDir.createSubdirectory(GwtModulesManager.DEFAULT_SOURCE_PATH);
		result.add(clientDir);
		String serviceName = name + SERVICE_SUFFIX;
		result.add(createClassFromTemplate(clientDir, serviceName, GwtTemplates.GWT_SAMPLE_APP_SERVICE_JAVA, RELATIVE_PATH_PROPERTY,
				appPackageName + "." + name + "/" + serviceName));
		result.add(createClassFromTemplate(clientDir, name + SERVICE_ASYNC_SUFFIX, GwtTemplates.GWT_SAMPLE_APP_SERVICE_ASYNC_JAVA));
		result.add(createClassFromTemplate(clientDir, name, GwtTemplates.GWT_SAMPLE_ENTRY_POINT_JAVA));

		PsiDirectory serverDir = moduleDir.createSubdirectory("server");
		result.add(serverDir);
		final PsiJavaPackage aPackage = JavaDirectoryService.getInstance().getPackage(clientDir);
		LOG.assertTrue(aPackage != null);
		result.add(createFromTemplate(serverDir, name + SERVICE_IMPL_SUFFIX + JavaFileType.DOT_DEFAULT_EXTENSION,
				GwtTemplates.GWT_SAMPLE_APP_SERVICE_IMPL_JAVA, SERVICE_NAME_PROPERTY, serviceName, CLIENT_PACKAGE_PROPERTY, aPackage.getQualifiedName()));

		PsiDirectory publicDir = moduleDir.createSubdirectory(GwtModulesManager.DEFAULT_PUBLIC_PATH);
		result.add(publicDir);
		result.add(createFromTemplate(publicDir, name + "." + HtmlFileType.INSTANCE.getDefaultExtension(), GwtTemplates.GWT_SAMPLE_APP_HTML,
				FileTemplate.ATTRIBUTE_PACKAGE_NAME, appPackageName));

		return result.toArray(new PsiElement[result.size()]);
	}

	@Override
	protected String getCommandName()
	{
		return GwtBundle.message("newapp.command.name");
	}

	@Override
	protected String getActionName(PsiDirectory directory, String newName)
	{
		return GwtBundle.message("newapp.progress.text", newName);
	}
}
