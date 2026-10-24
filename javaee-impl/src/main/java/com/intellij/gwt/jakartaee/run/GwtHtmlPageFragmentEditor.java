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
import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.html.language.HtmlFileType;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
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
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Editor of the HTML page with suggestions of the pages of the GWT modules in the configuration module.
 */
class GwtHtmlPageFragmentEditor implements RunConfigurationFragmentEditor<GwtRunConfiguration> {
    private final Project myProject;
    private final GwtModulesManager myGwtModulesManager;
    private final TextBoxWithHistory myHtmlPageBox;
    private final ActionToolbar myHtmlPageToolbar;

    @Nullable
    private Module myModule;

    private volatile int myPagesGeneration;
    private volatile boolean myDisposed;

    @RequiredUIAccess
    GwtHtmlPageFragmentEditor(Project project, Disposable parent) {
        myProject = project;
        myGwtModulesManager = GwtModulesManager.getInstance(myProject);

        myHtmlPageBox = TextBoxWithHistory.create();
        myHtmlPageToolbar = myProject.getApplication()
            .getInstance(ActionToolbarFactory.class)
            .createActionToolbar(
                "GwtRunConfigurationEditorHtmlPage",
                ActionGroup.newImmutableBuilder().add(new ChooseHtmlPageAction()).build(),
                ActionToolbar.Style.INPLACE
            );
        myHtmlPageToolbar.setTargetUIComponent(myHtmlPageBox);
        myHtmlPageToolbar.updateActionsAsync();

        Disposer.register(parent, () -> {
            myDisposed = true;
            myPagesGeneration++;
        });
    }

    @RequiredUIAccess
    @Override
    public void addRows(RunConfigurationFragmentLayout layout) {
        layout.addLabeled(
            GwtLocalize.labelHtmlToOpenText(),
            DockLayout.create().center(myHtmlPageBox).right(myHtmlPageToolbar.getUIComponent())
        );
    }

    @RequiredUIAccess
    @Override
    public void reset(GwtRunConfiguration target) {
        myModule = target.getModule();
        fillPages(myModule);
        myHtmlPageBox.setValue(StringUtil.notNullize(target.getPage()), false);
    }

    @RequiredUIAccess
    @Override
    public void apply(GwtRunConfiguration target) {
        String page = myHtmlPageBox.getValue();
        if (page != null) {
            target.setPage(page);
        }
    }

    private boolean isActive() {
        return !myDisposed && !myProject.isDisposed();
    }

    @RequiredUIAccess
    private void fillPages(@Nullable Module module) {
        int generation = ++myPagesGeneration;
        setPageSuggestions(myHtmlPageBox, List.of());
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
                    if (!isCurrentPagesRequest(generation)) {
                        return null;
                    }

                    if (pages == null) {
                        loadPages(module, generation);
                    }
                    else {
                        setPageSuggestions(myHtmlPageBox, pages);
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
    private String getPath(GwtModule gwtModule, VirtualFile file) {
        String path = myGwtModulesManager.getPathFromPublicRoot(gwtModule, file);
        return path != null ? GwtRunConfiguration.getPagePath(gwtModule, path) : null;
    }

    @RequiredUIAccess
    private void chooseHtmlPage(AnActionEvent e) {
        Module module = myModule;
        String pagePath = StringUtil.notNullize(myHtmlPageBox.getValue());
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
            candidates.add(new GwtPageCandidate(gwtModule.getQualifiedName(), GwtRunConfiguration.getPagePath(gwtModule, pair.getSecond())));
        }
        return candidates;
    }

    @RequiredUIAccess
    private void selectPage(List<GwtPageCandidate> candidates, AnActionEvent e) {
        if (candidates.isEmpty()) {
            return;
        }

        if (candidates.size() == 1) {
            myHtmlPageBox.setValue(candidates.get(0).path());
            return;
        }

        BaseListPopupStep<GwtPageCandidate> step = new BaseListPopupStep<>(GwtLocalize.dialogTitleChooseGwtModule().get(), candidates) {
            @Override
            public String getTextFor(GwtPageCandidate value) {
                return value.gwtModuleName();
            }

            @Override
            public PopupStep onChosen(GwtPageCandidate selectedValue, boolean finalChoice) {
                return doFinalStep(() -> myHtmlPageBox.setValue(selectedValue.path()));
            }
        };

        JBPopupFactory.getInstance().createListPopup(myProject, step).showUnderneathOf(e);
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
