/*
 * Copyright 2013-2026 consulo.io
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

import com.intellij.javaee.DeploymentDescriptorsConstants;
import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;

/**
 * A web.xml whose content replaces the module one; hidden means the module web.xml.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class GwtCustomWebXmlFragment implements RunConfigurationFragment<GwtRunConfiguration> {
    @Override
    public String getId() {
        return "gwtCustomWebXml";
    }

    @Override
    public LocalizeValue getName() {
        return GwtLocalize.runConfigurationFragmentCustomWebXml();
    }

    @Override
    public boolean isVisibleByDefault(GwtRunConfiguration target) {
        return target.getCustomWebXml() != null;
    }

    @Override
    public void applyHidden(GwtRunConfiguration target) {
        target.setCustomWebXml(null);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<GwtRunConfiguration> createEditor(Project project, Disposable parent) {
        FileChooserTextBoxBuilder.Controller webXmlField = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(createWebXmlChooserDescriptor(project))
            .uiDisposable(parent)
            .build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(
                    LocalizeValue.join(GwtLocalize.checkboxTextUseCustomWebXml(), LocalizeValue.colon()),
                    webXmlField.getComponent()
                );
            }

            @RequiredUIAccess
            @Override
            public void reset(GwtRunConfiguration target) {
                String customWebXml = target.getCustomWebXml();
                String path = customWebXml == null ? "" : FileUtil.toSystemDependentName(VirtualFileUtil.urlToPath(customWebXml));
                webXmlField.setValue(path, false);
            }

            @RequiredUIAccess
            @Override
            public void apply(GwtRunConfiguration target) {
                String path = StringUtil.notNullize(webXmlField.getValue()).trim();
                if (path.isEmpty()) {
                    target.setCustomWebXml(null);
                }
                else {
                    target.setCustomWebXml(VirtualFileUtil.pathToUrl(FileUtil.toSystemIndependentName(path)));
                }
            }
        };
    }

    private static FileChooserDescriptor createWebXmlChooserDescriptor(Project project) {
        String webXmlName = DeploymentDescriptorsConstants.WEB_XML_META_DATA.getFileName();
        return new FileChooserDescriptor(true, false, false, false, false, false)
            .withExtensionFilter("xml")
            .withFileFilter(file -> webXmlName.equals(file.getName()))
            .withRoots(ProjectRootManager.getInstance(project).getContentRoots());
    }
}
