package com.intellij.gwt.impl.actions;

import com.intellij.gwt.base.actions.GwtCreateActionBase;
import com.intellij.gwt.base.templates.GwtTemplates;
import com.intellij.gwt.module.model.GwtModule;
import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.ide.IdeView;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ActionImpl(id = "GWT.NewTestCase")
public class CreateGwtTestCaseAction extends GwtCreateActionBase {
    private static final String GWT_MODULE_PARAMETER = "GWT_MODULE_NAME";

    public CreateGwtTestCaseAction() {
        super(GwtLocalize.actionNameCreateGwtTestCase(), GwtLocalize.actionDescriptionCreatesNewGwtTestCase());
    }

    @Override
    @Nonnull
    protected PsiElement[] doCreate(final String newName, final PsiDirectory directory, final GwtModule gwtModule) throws Exception {
        return new PsiElement[]{
            createClassFromTemplate(directory, newName, GwtTemplates.GWT_TEST_CASE_JAVA, GWT_MODULE_PARAMETER, gwtModule.getQualifiedName())
        };
    }

    @Override
    protected boolean isAvailable(final DataContext dataContext) {
        if (!super.isAvailable(dataContext)) {
            return false;
        }

        final Project project = dataContext.getData(PlatformDataKeys.PROJECT);
        final IdeView view = dataContext.getData(IdeView.KEY);
        if (view == null || project == null) {
            return false;
        }

        PsiDirectory[] directories = view.getDirectories();
        for (PsiDirectory psiDirectory : directories) {
            VirtualFile directory = psiDirectory.getVirtualFile();
            if (ProjectRootsUtil.isInTestSource(directory, project)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean requireGwtModule() {
        return true;
    }

    @Override
    protected LocalizeValue getDialogPrompt() {
        return GwtLocalize.dialogPromtEnterNameForGwtTestCase();
    }

    @Override
    protected LocalizeValue getDialogTitle() {
        return GwtLocalize.dialogTitleNewGwtTestCase();
    }

    @Override
    protected LocalizeValue getCommandName() {
        return GwtLocalize.commandNameCreateGwtTestCase();
    }

    @Override
    protected LocalizeValue getActionName(final PsiDirectory directory, final String newName) {
        return GwtLocalize.actionProgressCreatingGwtTestCase0(newName);
    }
}
