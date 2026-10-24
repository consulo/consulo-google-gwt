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

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

/**
 * Parameters of the GWT shell, shown once set.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class GwtShellParametersFragment implements RunConfigurationFragment<GwtRunConfiguration> {
    @Override
    public String getId() {
        return "gwtShellParameters";
    }

    @Override
    public LocalizeValue getName() {
        return GwtLocalize.runConfigurationFragmentShellParameters();
    }

    @Override
    public boolean isVisibleByDefault(GwtRunConfiguration target) {
        return target.getShellParameters() != null;
    }

    @Override
    public void applyHidden(GwtRunConfiguration target) {
        target.setShellParameters(null);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<GwtRunConfiguration> createEditor(Project project, Disposable parent) {
        TextBoxWithExpandAction shellParameters = TextBoxWithExpandAction.create(
            PlatformIconGroup.actionsShow(),
            GwtLocalize.dialogCaptionGwtShellParameters().get(),
            ParametersListUtil.DEFAULT_LINE_PARSER,
            ParametersListUtil.DEFAULT_LINE_JOINER
        );

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(GwtLocalize.labelTextGwtShellParameters(), shellParameters);
            }

            @RequiredUIAccess
            @Override
            public void reset(GwtRunConfiguration target) {
                shellParameters.setValue(StringUtil.notNullize(target.getShellParameters()));
            }

            @RequiredUIAccess
            @Override
            public void apply(GwtRunConfiguration target) {
                target.setShellParameters(shellParameters.getValue());
            }
        };
    }
}
