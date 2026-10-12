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

package consulo.gwt.base.module.extension;

import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.module.extension.GoogleGwtMutableModuleExtension;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.ui.extension.ModuleExtensionBundleBoxBuilder;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public final class GwtModuleExtensionPanel {
    private GwtModuleExtensionPanel() {
    }

    @RequiredUIAccess
    public static Component create(
        GoogleGwtMutableModuleExtension<?> extension,
        ModuleRootLayer moduleRootLayer,
        Disposable uiDisposable,
        Runnable updater
    ) {
        ComboBox<GwtJavaScriptOutputStyle> outputStyleBox = ComboBox.create(GwtJavaScriptOutputStyle.values());
        outputStyleBox.setTextRenderer(style -> style == null ? LocalizeValue.empty() : style.getPresentableName());
        outputStyleBox.setValue(extension.getOutputStyle());
        outputStyleBox.addValueListener(event -> {
            GwtJavaScriptOutputStyle style = event.getValue();
            if (style != null) {
                extension.setOutputStyle(style);
            }
        });

        FileChooserTextBoxBuilder.Controller outputDirBox = FileChooserTextBoxBuilder.create(extension.getProject())
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .dialogDescription(GwtLocalize.fileChooserDescriptionSelectOutputDirectoryForFilesGeneratedByGwtCompiler())
            .uiDisposable(uiDisposable)
            .build();
        String compilerOutputUrl = extension.getCompilerOutputUrl();
        outputDirBox.setValue(StringUtil.isEmpty(compilerOutputUrl) ? "" : VirtualFileManager.extractPath(compilerOutputUrl), false);
        outputDirBox.getComponent().addValueListener(event -> {
            String path = StringUtil.nullize(outputDirBox.getValue(), true);
            extension.setCompilerOutputUrl(
                path == null ? null : VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, FileUtil.toSystemIndependentName(path))
            );
        });

        IntBox heapSizeBox = IntBox.create(extension.getCompilerMaxHeapSize());
        heapSizeBox.addValueListener(event -> {
            Integer value = event.getValue();
            if (value != null) {
                extension.setCompilerMaxHeapSize(value);
            }
        });

        TextBox additionalParametersBox = TextBox.create(extension.getAdditionalCompilerParameters());
        additionalParametersBox.addValueListener(event ->
            extension.setAdditionalCompilerParameters(StringUtil.notNullize(event.getValue())));

        TextBox additionalVmParametersBox = TextBox.create(extension.getAdditionalVmCompilerParameters());
        additionalVmParametersBox.addValueListener(event ->
            extension.setAdditionalCompilerVmParameters(StringUtil.notNullize(event.getValue())));

        Label outputDirLabel = Label.create(GwtLocalize.labelTextCompilerOutputDirectory());
        Label heapSizeLabel = Label.create(GwtLocalize.labelTextGwtCompilerHeapSize());
        Label additionalParametersLabel = Label.create(GwtLocalize.labelTextAdditionalCompilerParameters());
        Label additionalVmParametersLabel = Label.create(GwtLocalize.labelTextAdditionalVmCompilerParameters());

        FormBuilder formBuilder = FormBuilder.create();
        formBuilder.addLabeled(GwtLocalize.labelSelectScriptOutputStyleText(), outputStyleBox);
        formBuilder.addLabeled(outputDirLabel, outputDirBox.getComponent());
        formBuilder.addLabeled(heapSizeLabel, heapSizeBox);
        formBuilder.addLabeled(additionalParametersLabel, additionalParametersBox);
        formBuilder.addLabeled(additionalVmParametersLabel, additionalVmParametersBox);

        MutableFlatDataModel<GwtModulePackagingItem> packagingModel = FlatDataModel.of(List.of());
        Table<GwtModulePackagingItem> packagingTable = Table.create(packagingModel);
        packagingTable.addColumn(GwtLocalize.tableColumnNameGwtModule(), GwtModulePackagingItem::moduleName);
        packagingTable.addColumn(GwtLocalize.tableColumnNameOutputRelativePath(), GwtModulePackagingItem::outputPath);
        loadPackagingItems(extension, packagingModel, uiDisposable);

        List<Component> compilerComponents = List.of(
            outputDirLabel,
            outputDirBox.getComponent(),
            heapSizeLabel,
            heapSizeBox,
            additionalParametersLabel,
            additionalParametersBox,
            additionalVmParametersLabel,
            additionalVmParametersBox,
            packagingTable
        );

        CheckBox runCompilerBox = CheckBox.create(GwtLocalize.checkboxRunGwtCompilerOnMake(), extension.isRunGwtCompilerOnMake());
        runCompilerBox.addValueListener(event -> {
            boolean enabled = Boolean.TRUE.equals(event.getValue());
            extension.setRunGwtCompilerOnMake(enabled);
            for (Component component : compilerComponents) {
                component.setEnabled(enabled);
            }
        });
        for (Component component : compilerComponents) {
            component.setEnabled(extension.isRunGwtCompilerOnMake());
        }

        VerticalLayout topLayout = VerticalLayout.create();
        if (GwtLibraryPathProvider.EP_NAME.findFirstSafe(it -> it.canChooseBundle(moduleRootLayer)) != null) {
            topLayout.add(ModuleExtensionBundleBoxBuilder.createAndDefine(extension, uiDisposable, updater).build());
        }
        topLayout.add(runCompilerBox);
        topLayout.add(formBuilder.build());

        DockLayout layout = DockLayout.create();
        layout.top(topLayout);
        layout.center(ScrollableLayout.create(packagingTable));
        return layout;
    }

    @RequiredUIAccess
    private static void loadPackagingItems(
        GoogleGwtMutableModuleExtension<?> extension,
        MutableFlatDataModel<GwtModulePackagingItem> packagingModel,
        Disposable uiDisposable
    ) {
        Project project = extension.getProject();
        Module module = extension.getModule();

        UIAccess uiAccess = UIAccess.current();
        CoroutineScope scope = CoroutineScope.of(project.coroutineContext());
        Disposer.register(uiDisposable, scope::cancel);

        Coroutine.first(ReadLock.<Object, List<GwtModulePackagingItem>>apply(ignored -> collectPackagingItems(extension, project, module)))
            .runAsync(scope, null)
            .toFuture()
            .whenCompleteAsync((items, error) -> {
                if (error == null && items != null && !project.isDisposed() && !Disposer.isDisposed(uiDisposable)) {
                    packagingModel.replaceAll(items);
                }
            }, uiAccess);
    }

    @RequiredReadAction
    private static List<GwtModulePackagingItem> collectPackagingItems(
        GoogleGwtMutableModuleExtension<?> extension,
        Project project,
        Module module
    ) {
        List<GwtModulePackagingItem> items = new ArrayList<>();
        for (GwtModule gwtModule : GwtModulesManager.getInstance(project).getGwtModules(module)) {
            items.add(new GwtModulePackagingItem(gwtModule.getQualifiedName(), extension.getPackagingRelativePath(gwtModule)));
        }
        return items;
    }
}
