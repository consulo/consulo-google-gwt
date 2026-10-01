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

package com.intellij.gwt.jakartaee.run;

import com.intellij.gwt.base.module.index.GwtHtmlFileIndex;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.javaee.DeploymentDescriptorsConstants;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.jakartaee.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.html.language.HtmlFileType;
import consulo.jakartaee.web.module.extension.JavaWebModuleExtension;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GwtRunConfigurationEditor extends SettingsEditor<GwtRunConfiguration> {
    private final Project myProject;
    private final GwtModulesManager myGwtModulesManager;
    private final MutableFlatDataModel<Module> myModulesModel = FlatDataModel.of(new ArrayList<>());

    @Nullable
    private ComboBox<Module> myModulesBox;
    @Nullable
    private TextBoxWithHistory myHtmlPageBox;
    @Nullable
    private TextBoxWithExpandAction myVMParameters;
    @Nullable
    private TextBoxWithExpandAction myGwtShellParameters;
    @Nullable
    private CheckBox myPatchWebXmlCheckBox;
    @Nullable
    private FileChooserTextBoxBuilder.Controller myWebXmlField;

    private volatile int myPagesGeneration;
    private volatile boolean myDisposed;
    private boolean myResetting;

    public GwtRunConfigurationEditor(Project project) {
        myProject = project;
        myGwtModulesManager = GwtModulesManager.getInstance(myProject);
    }

    @Override
    @RequiredUIAccess
    protected Component createUIComponent() {
        ComboBox<Module> modulesBox = ComboBox.create(myModulesModel);
        modulesBox.setRender((presentation, item) -> {
            Module module = item.getValue();
            if (module != null) {
                presentation.withIcon(PlatformIconGroup.nodesModule());
                presentation.append(module.getName());
            }
        });
        modulesBox.setSpeedSearchConverter(module -> module == null ? "" : module.getName());
        modulesBox.addValueListener(event -> {
            if (myResetting) {
                return;
            }
            Module module = event.getValue();
            fillPages(module);
            updateWebXmlPanel(module);
        });
        myModulesBox = modulesBox;

        TextBoxWithHistory htmlPageBox = TextBoxWithHistory.create();
        ActionToolbar htmlPageToolbar = myProject.getApplication()
            .getInstance(ActionToolbarFactory.class)
            .createActionToolbar(
                "GwtRunConfigurationEditorHtmlPage",
                ActionGroup.newImmutableBuilder().add(new ChooseHtmlPageAction()).build(),
                ActionToolbar.Style.INPLACE
            );
        htmlPageToolbar.setTargetUIComponent(htmlPageBox);
        htmlPageToolbar.updateActionsAsync();
        myHtmlPageBox = htmlPageBox;

        TextBoxWithExpandAction vmParameters = createParametersField(GwtLocalize.dialogCaptionVmParameters());
        myVMParameters = vmParameters;

        TextBoxWithExpandAction gwtShellParameters = createParametersField(GwtLocalize.dialogCaptionGwtShellParameters());
        myGwtShellParameters = gwtShellParameters;

        FileChooserTextBoxBuilder.Controller webXmlField = FileChooserTextBoxBuilder.create(myProject)
            .fileChooserDescriptor(createWebXmlChooserDescriptor())
            .uiDisposable(this)
            .build();
        webXmlField.getComponent().setEnabled(false);
        myWebXmlField = webXmlField;

        CheckBox patchWebXmlCheckBox =
            CheckBox.create(LocalizeValue.join(GwtLocalize.checkboxTextUseCustomWebXml(), LocalizeValue.colon()));
        patchWebXmlCheckBox.addValueListener(
            event -> webXmlField.getComponent().setEnabled(Boolean.TRUE.equals(event.getValue()))
        );
        myPatchWebXmlCheckBox = patchWebXmlCheckBox;

        Label moduleLabel = Label.create(GwtLocalize.labelChooseModuleText());
        moduleLabel.setTarget(modulesBox);

        Label htmlPageLabel = Label.create(GwtLocalize.labelHtmlToOpenText());
        htmlPageLabel.setTarget(htmlPageBox);

        return FormBuilder.create()
            .addLabeled(moduleLabel, modulesBox)
            .addLabeled(htmlPageLabel, DockLayout.create().center(htmlPageBox).right(htmlPageToolbar.getUIComponent()))
            .addLabeled(GwtLocalize.labelTextVmParameters(), vmParameters)
            .addLabeled(GwtLocalize.labelTextGwtShellParameters(), gwtShellParameters)
            .addLabeled(patchWebXmlCheckBox, webXmlField.getComponent())
            .build();
    }

    @RequiredUIAccess
    private static TextBoxWithExpandAction createParametersField(LocalizeValue dialogCaption) {
        return TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            dialogCaption.get(),
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );
    }

    @Override
    @RequiredUIAccess
    protected void resetEditorFrom(GwtRunConfiguration configuration) {
        ComboBox<Module> modulesBox = myModulesBox;
        TextBoxWithHistory htmlPageBox = myHtmlPageBox;
        TextBoxWithExpandAction vmParameters = myVMParameters;
        TextBoxWithExpandAction gwtShellParameters = myGwtShellParameters;
        CheckBox patchWebXmlCheckBox = myPatchWebXmlCheckBox;
        FileChooserTextBoxBuilder.Controller webXmlField = myWebXmlField;
        if (modulesBox == null || htmlPageBox == null || vmParameters == null || gwtShellParameters == null
            || patchWebXmlCheckBox == null || webXmlField == null) {
            return;
        }

        vmParameters.setValue(StringUtil.notNullize(configuration.VM_PARAMETERS));
        gwtShellParameters.setValue(StringUtil.notNullize(configuration.SHELL_PARAMETERS));

        Module module = configuration.getModule();
        List<Module> modules = new ArrayList<>(configuration.getValidModules());
        if (module != null && !modules.contains(module)) {
            modules.add(module);
        }
        myResetting = true;
        try {
            myModulesModel.replaceAll(modules);
            modulesBox.setValue(module, false);
        }
        finally {
            myResetting = false;
        }

        String customWebXml = configuration.CUSTOM_WEB_XML;
        patchWebXmlCheckBox.setValue(customWebXml != null, false);
        webXmlField.getComponent().setEnabled(customWebXml != null);
        if (customWebXml != null) {
            webXmlField.setValue(FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(customWebXml)), false);
        }
        updateWebXmlPanel(module);

        fillPages(module);
        htmlPageBox.setValue(StringUtil.notNullize(configuration.getPage()), false);
    }

    @Override
    @RequiredUIAccess
    protected void applyEditorTo(GwtRunConfiguration configuration) throws ConfigurationException {
        ComboBox<Module> modulesBox = myModulesBox;
        TextBoxWithHistory htmlPageBox = myHtmlPageBox;
        TextBoxWithExpandAction vmParameters = myVMParameters;
        TextBoxWithExpandAction gwtShellParameters = myGwtShellParameters;
        CheckBox patchWebXmlCheckBox = myPatchWebXmlCheckBox;
        FileChooserTextBoxBuilder.Controller webXmlField = myWebXmlField;
        if (modulesBox == null || htmlPageBox == null || vmParameters == null || gwtShellParameters == null
            || patchWebXmlCheckBox == null || webXmlField == null) {
            return;
        }

        configuration.setModule(modulesBox.getValue());
        String page = htmlPageBox.getValue();
        if (page != null) {
            configuration.setPage(page);
        }
        configuration.VM_PARAMETERS = StringUtil.notNullize(vmParameters.getValue());
        configuration.SHELL_PARAMETERS = StringUtil.notNullize(gwtShellParameters.getValue());
        if (Boolean.TRUE.equals(patchWebXmlCheckBox.getValue())) {
            configuration.CUSTOM_WEB_XML = VirtualFileUtil.pathToUrl(FileUtil.toSystemIndependentName(webXmlField.getValue()));
        }
        else {
            configuration.CUSTOM_WEB_XML = null;
        }
    }

    @Override
    protected void disposeEditor() {
        myDisposed = true;
        myPagesGeneration++;
    }

    private boolean isActive() {
        return !myDisposed && !myProject.isDisposed();
    }

    @Nullable
    @RequiredUIAccess
    private Module getSelectedModule() {
        ComboBox<Module> modulesBox = myModulesBox;
        return modulesBox == null ? null : modulesBox.getValue();
    }

    @RequiredUIAccess
    private void fillPages(@Nullable Module module) {
        TextBoxWithHistory htmlPageBox = myHtmlPageBox;
        if (htmlPageBox == null) {
            return;
        }

        int generation = ++myPagesGeneration;
        setPageSuggestions(htmlPageBox, List.of());
        if (module != null) {
            loadPages(module, generation);
        }
    }

    private boolean isCurrentPagesRequest(int generation) {
        return generation == myPagesGeneration && isActive();
    }

    private void loadPages(Module module, int generation) {
        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(CompletableFutureStep.<Void, Boolean>await(ignored -> {
                    CompletableFuture<Boolean> smart = new CompletableFuture<>();
                    DumbService.getInstance(myProject).runWhenSmart(() -> smart.complete(Boolean.TRUE));
                    return smart;
                }))
                .then(ReadLock.<Boolean, List<String>>apply(
                    ignored -> isCurrentPagesRequest(generation) ? collectPages(module) : null
                ))
                .then(UIAction.<List<String>, Void>apply(pages -> {
                    TextBoxWithHistory box = myHtmlPageBox;
                    if (box == null || !isCurrentPagesRequest(generation)) {
                        return null;
                    }

                    if (pages == null) {
                        loadPages(module, generation);
                    }
                    else {
                        setPageSuggestions(box, pages);
                    }
                    return null;
                }))
        );
    }

    @RequiredUIAccess
    private static void setPageSuggestions(TextBoxWithHistory htmlPageBox, List<String> pages) {
        String text = htmlPageBox.getValue();
        htmlPageBox.setHistory(pages);
        if (text != null && !text.equals(htmlPageBox.getValue())) {
            htmlPageBox.setValue(text, false);
        }
    }

    @Nullable
    @RequiredReadAction
    private List<String> collectPages(Module module) {
        if (DumbService.isDumb(myProject)) {
            return null;
        }
        if (module.isDisposed() || myProject.isDisposed()) {
            return List.of();
        }

        List<String> pages = new ArrayList<>();
        for (GwtModule gwtModule : myGwtModulesManager.getGwtModules(module)) {
            for (VirtualFile htmlFile : GwtHtmlFileIndex.getHtmlFilesByModule(myProject, gwtModule.getQualifiedName())) {
                String path = getPath(gwtModule, htmlFile);
                if (path != null) {
                    pages.add(path);
                }
            }
        }
        return pages;
    }

    @Nullable
    @RequiredReadAction
    private VirtualFile getFileByPagePath(Module module, String pagePath) {
        int index = pagePath.indexOf('/');
        if (index == -1) {
            return null;
        }

        GwtModule gwtModule = myGwtModulesManager.findGwtModuleByName(
            pagePath.substring(0, index),
            GlobalSearchScope.moduleWithDependenciesScope(module)
        );
        if (gwtModule == null) {
            return null;
        }

        String name = pagePath.substring(index + 1);
        for (VirtualFile root : gwtModule.getPublicRoots()) {
            VirtualFile file = root.findFileByRelativePath(name);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    private String getPath(@Nonnull GwtModule gwtModule, @Nonnull VirtualFile file) {
        String path = myGwtModulesManager.getPathFromPublicRoot(gwtModule, file);
        return path != null ? getPath(gwtModule, path) : null;
    }

    @Nonnull
    public static String getPath(@Nonnull GwtModule gwtModule, @Nonnull String relativePath) {
        return gwtModule.getQualifiedName() + "/" + relativePath;
    }

    @RequiredUIAccess
    private void chooseHtmlPage(AnActionEvent e) {
        TextBoxWithHistory htmlPageBox = myHtmlPageBox;
        if (htmlPageBox == null) {
            return;
        }

        Module module = getSelectedModule();
        String pagePath = StringUtil.notNullize(htmlPageBox.getValue());
        FileChooserDescriptor descriptor = createHtmlFileChooserDescriptor();

        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Void, VirtualFile>apply(ignored -> findFileToSelect(module, pagePath)))
                .then(UIAction.<VirtualFile, Void>apply(toSelect -> {
                    if (isActive()) {
                        FileChooser.chooseFile(descriptor, myProject, toSelect).whenComplete((file, error) -> {
                            if (error == null && file != null) {
                                resolveChosenPage(file, e);
                            }
                        });
                    }
                    return null;
                }))
        );
    }

    @Nullable
    @RequiredReadAction
    private VirtualFile findFileToSelect(@Nullable Module module, String pagePath) {
        if (module == null || module.isDisposed() || myProject.isDisposed() || DumbService.isDumb(myProject)) {
            return null;
        }
        return getFileByPagePath(module, pagePath);
    }

    private void resolveChosenPage(VirtualFile file, AnActionEvent e) {
        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Void, List<GwtPageCandidate>>apply(ignored -> findPageCandidates(file)))
                .then(UIAction.<List<GwtPageCandidate>, Void>apply(candidates -> {
                    if (isActive()) {
                        selectPage(candidates, e);
                    }
                    return null;
                }))
        );
    }

    @RequiredReadAction
    private List<GwtPageCandidate> findPageCandidates(VirtualFile file) {
        if (!file.isValid() || myProject.isDisposed()) {
            return List.of();
        }

        List<GwtPageCandidate> candidates = new ArrayList<>();
        for (Pair<GwtModule, String> pair : myGwtModulesManager.findGwtModulesByPublicFile(file)) {
            GwtModule gwtModule = pair.getFirst();
            candidates.add(new GwtPageCandidate(gwtModule.getQualifiedName(), getPath(gwtModule, pair.getSecond())));
        }
        return candidates;
    }

    @RequiredUIAccess
    private void selectPage(List<GwtPageCandidate> candidates, AnActionEvent e) {
        if (candidates.isEmpty()) {
            return;
        }

        if (candidates.size() == 1) {
            setPage(candidates.get(0).path());
            return;
        }

        BaseListPopupStep<GwtPageCandidate> step = new BaseListPopupStep<>(GwtLocalize.dialogTitleChooseGwtModule().get(), candidates) {
            @Override
            public String getTextFor(GwtPageCandidate value) {
                return value.gwtModuleName();
            }

            @Override
            public PopupStep onChosen(GwtPageCandidate selectedValue, boolean finalChoice) {
                return doFinalStep(() -> setPage(selectedValue.path()));
            }
        };

        JBPopupFactory.getInstance().createListPopup(myProject, step).showUnderneathOf(e);
    }

    @RequiredUIAccess
    private void setPage(String path) {
        TextBoxWithHistory htmlPageBox = myHtmlPageBox;
        if (htmlPageBox != null) {
            htmlPageBox.setValue(path);
        }
    }

    @RequiredUIAccess
    private void updateWebXmlPanel(@Nullable Module module) {
        CheckBox patchWebXmlCheckBox = myPatchWebXmlCheckBox;
        FileChooserTextBoxBuilder.Controller webXmlField = myWebXmlField;
        if (patchWebXmlCheckBox == null || webXmlField == null) {
            return;
        }

        boolean visible = isWebXmlCustomizable(module);
        patchWebXmlCheckBox.setVisible(visible);
        webXmlField.getComponent().setVisible(visible);
    }

    private static boolean isWebXmlCustomizable(@Nullable Module module) {
        return module != null
            && ModuleUtilCore.getExtension(module, JavaWebModuleExtension.class) != null
            && ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class) != null;
    }

    private FileChooserDescriptor createWebXmlChooserDescriptor() {
        String webXmlName = DeploymentDescriptorsConstants.WEB_XML_META_DATA.getFileName();
        return new FileChooserDescriptor(true, false, false, false, false, false)
            .withExtensionFilter("xml")
            .withFileFilter(file -> webXmlName.equals(file.getName()))
            .withRoots(ProjectRootManager.getInstance(myProject).getContentRoots());
    }

    private FileChooserDescriptor createHtmlFileChooserDescriptor() {
        return new FileChooserDescriptor(true, false, false, false, false, false)
            .withExtensionFilter("html")
            .withFileFilter(file -> file.getFileType() == HtmlFileType.INSTANCE)
            .withRoots(ProjectRootManager.getInstance(myProject).getContentSourceRoots())
            .withTitle(GwtLocalize.actionTextChooseHtmlPage());
    }

    private record GwtPageCandidate(String gwtModuleName, String path) {
    }

    private final class ChooseHtmlPageAction extends DumbAwareAction {
        private ChooseHtmlPageAction() {
            super(GwtLocalize.actionTextChooseHtmlPage(), LocalizeValue.empty(), PlatformIconGroup.nodesFolderopened());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            chooseHtmlPage(e);
        }
    }
}
