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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.CommonBundle;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.JavaTemplateUtil;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;

public abstract class GwtCreateActionBase extends CreateElementActionBase
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.actions.GwtCreateActionBase");
	@NonNls
	private static final String NAME_TEMPLATE_PROPERTY = "NAME";

	public GwtCreateActionBase(String text, String description)
	{
		super(text, description, null);
	}

	@Override
	@Nonnull
	protected final PsiElement[] invokeDialog(final Project project, final PsiDirectory directory)
	{
		Module module = ModuleUtil.findModuleForFile(directory.getVirtualFile(), project);
		if(module == null)
		{
			return PsiElement.EMPTY_ARRAY;
		}

		GoogleGwtModuleExtension facet = ModuleUtilCore.getExtension(module, GoogleGwtModuleExtension.class);
		if(facet == null)
		{
			return PsiElement.EMPTY_ARRAY;
		}

		if(requireGwtModule())
		{
			final GwtModule gwtModule = findGwtModule(project, directory);
			if(gwtModule == null)
			{
				final String message = GwtBundle.message("error.message.this.action.is.allowed.only.for.client.side.packages.of.a.gwt.module");
				Messages.showErrorDialog(project, message, CommonBundle.getErrorTitle());
				return PsiElement.EMPTY_ARRAY;
			}
		}

		MyInputValidator validator = new MyInputValidator(project, directory);
		Messages.showInputDialog(project, getDialogPrompt(), getDialogTitle(), Messages.getQuestionIcon(), "", validator);

		return validator.getCreatedElements();
	}

	protected PsiFile[] getAffectedFiles(final GwtModule gwtModule)
	{
		return PsiFile.EMPTY_ARRAY;
	}

	protected abstract boolean requireGwtModule();

	protected abstract String getDialogPrompt();

	protected abstract String getDialogTitle();

	private static
	@Nullable
	GwtModule findGwtModule(Project project, PsiDirectory directory)
	{
		return GwtModulesManager.getInstance(project).findGwtModuleByClientSourceFile(directory.getVirtualFile());
	}

	@Override
	public final void update(final AnActionEvent e)
	{
		final Presentation presentation = e.getPresentation();
		super.update(e);

		if(presentation.isEnabled() && !isUnderSourceRootsOfModuleWithGwtFacet(e))
		{
			presentation.setEnabled(false);
			presentation.setVisible(false);
		}
	}

	public static boolean isUnderSourceRootsOfModuleWithGwtFacet(final AnActionEvent e)
	{
		Module module = e.getData(LangDataKeys.MODULE);
		if(module == null)
		{
			return false;
		}

		if(ModuleUtilCore.getExtension(module, GoogleGwtModuleExtension.class) == null)
		{
			return false;
		}

		final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
		final Project project = e.getData(PlatformDataKeys.PROJECT);
		if(view != null && project != null)
		{
			ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
			PsiDirectory[] dirs = view.getDirectories();
			for(PsiDirectory dir : dirs)
			{
				if(projectFileIndex.isInSourceContent(dir.getVirtualFile()) && JavaDirectoryService.getInstance().getPackage(dir) != null)
				{
					return true;
				}
			}
		}

		return false;
	}

	@Override
	@Nonnull
	protected PsiElement[] create(String newName, PsiDirectory directory) throws Exception
	{
		final GwtModule gwtModule;
		if(requireGwtModule())
		{
			gwtModule = findGwtModule(directory.getProject(), directory);
		}
		else
		{
			gwtModule = null;
		}
		return doCreate(newName, directory, gwtModule);
	}

	@Nonnull
	protected abstract PsiElement[] doCreate(String newName, PsiDirectory directory, final GwtModule gwtModule) throws Exception;

	protected static PsiClass createClassFromTemplate(final PsiDirectory directory, String className, String templateName,
			@NonNls String... parameters) throws IncorrectOperationException
	{
		final PsiFile file = createFromTemplateInternal(directory, className, className + JavaFileType.DOT_DEFAULT_EXTENSION, templateName,
				parameters);
		return ((PsiJavaFile) file).getClasses()[0];
	}

	protected static PsiFile createFromTemplate(final PsiDirectory directory, String fileName, String templateName,
			@NonNls String... parameters) throws IncorrectOperationException
	{
		return createFromTemplateInternal(directory, FileUtil.getNameWithoutExtension(fileName), fileName, templateName, parameters);
	}

	protected static PsiFile createFromTemplateInternal(final PsiDirectory directory, final String name, String fileName, String templateName,
			@NonNls String... parameters) throws IncorrectOperationException
	{
		final FileTemplate template = FileTemplateManager.getInstance().getJ2eeTemplate(templateName);

		Properties properties = new Properties(FileTemplateManager.getInstance().getDefaultProperties());
		JavaTemplateUtil.setPackageNameAttribute(properties, directory);
		properties.setProperty(NAME_TEMPLATE_PROPERTY, name);
		LOG.assertTrue(parameters.length % 2 == 0);
		for(int i = 0; i < parameters.length; i += 2)
		{
			properties.setProperty(parameters[i], parameters[i + 1]);
		}
		String text;
		try
		{
			text = template.getText(properties);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Unable to load template for " + FileTemplateManager.getInstance().internalTemplateToSubject(templateName), e);
		}

		final PsiManager psiManager = PsiManager.getInstance(directory.getProject());
		final PsiFile file = PsiFileFactory.getInstance(directory.getProject()).createFileFromText(fileName, text);

		CodeStyleManager.getInstance(psiManager).reformat(file);

		return (PsiFile) directory.add(file);
	}


	@Override
	protected String getErrorTitle()
	{
		return CommonBundle.getErrorTitle();
	}

	protected final void checkBeforeCreate(String newName, PsiDirectory directory) throws IncorrectOperationException
	{
		doCheckBeforeCreate(newName, directory);
		List<VirtualFile> files = new ArrayList<VirtualFile>();
		for(PsiFile psiFile : getAffectedFiles(findGwtModule(directory.getProject(), directory)))
		{
			final VirtualFile virtualFile = psiFile.getVirtualFile();
			if(virtualFile != null)
			{
				files.add(virtualFile);
			}
		}
		ReadonlyStatusHandler.getInstance(directory.getProject()).ensureFilesWritable(files.toArray(new VirtualFile[files.size()]));
	}

	protected void doCheckBeforeCreate(String newName, PsiDirectory directory) throws IncorrectOperationException
	{
		JavaDirectoryService.getInstance().checkCreateClass(directory, newName);
	}

}
