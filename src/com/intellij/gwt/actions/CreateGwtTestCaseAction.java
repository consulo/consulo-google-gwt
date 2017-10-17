package com.intellij.gwt.actions;

import java.io.File;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.IdeView;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * @author nik
 */
public class CreateGwtTestCaseAction extends GwtCreateActionBase
{
	@NonNls
	private static final String GWT_MODULE_PARAMETER = "GWT_MODULE_NAME";

	public CreateGwtTestCaseAction()
	{
		super(GwtBundle.message("action.name.create.gwt.test.case"), GwtBundle.message("action.description.creates.new.gwt.test.case"));
	}

	@Override
	@NotNull
	protected PsiElement[] doCreate(final String newName, final PsiDirectory directory, final GwtModule gwtModule) throws Exception
	{
		Module module = gwtModule.getModule();
		if(module != null)
		{
			GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
			PsiClass testCaseClass = JavaPsiFacade.getInstance(module.getProject()).findClass("junit.framework.TestCase", scope);
			if(testCaseClass == null)
			{
				ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
				Library library = model.getModuleLibraryTable().createLibrary();
				String url = VfsUtil.getUrlForLibraryRoot(new File(JavaSdkUtil.getJunit3JarPath()));
				Library.ModifiableModel libraryModel = library.getModifiableModel();
				libraryModel.addRoot(url, OrderRootType.CLASSES);
				libraryModel.commit();
				model.commit();
			}
		}
		return new PsiElement[]{
				createClassFromTemplate(directory, newName, GwtTemplates.GWT_TEST_CASE_JAVA, GWT_MODULE_PARAMETER, gwtModule.getQualifiedName())
		};
	}

	@Override
	protected boolean isAvailable(final DataContext dataContext)
	{
		if(!super.isAvailable(dataContext))
		{
			return false;
		}

		final Project project = dataContext.getData(PlatformDataKeys.PROJECT);
		final IdeView view = dataContext.getData(LangDataKeys.IDE_VIEW);
		if(view == null || project == null)
		{
			return false;
		}

		PsiDirectory[] directories = view.getDirectories();
		for(PsiDirectory psiDirectory : directories)
		{
			VirtualFile directory = psiDirectory.getVirtualFile();
			if(ProjectRootsUtil.isInTestSource(directory, project))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean requireGwtModule()
	{
		return true;
	}

	@Override
	protected String getDialogPrompt()
	{
		return GwtBundle.message("dialog.promt.enter.name.for.gwt.test.case");
	}

	@Override
	protected String getDialogTitle()
	{
		return GwtBundle.message("dialog.title.new.gwt.test.case");
	}

	@Override
	protected String getCommandName()
	{
		return GwtBundle.message("command.name.create.gwt.test.case");
	}

	@Override
	protected String getActionName(final PsiDirectory directory, final String newName)
	{
		return GwtBundle.message("action.progress.creating.gwt.test.case.0", newName);
	}
}
