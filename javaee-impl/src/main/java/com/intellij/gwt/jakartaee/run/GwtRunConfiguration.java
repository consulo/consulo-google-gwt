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

import com.intellij.java.execution.configurations.JavaCommandLineState;
import com.intellij.java.execution.configurations.JavaRunConfigurationModule;
import consulo.content.bundle.Sdk;
import consulo.execution.CantRunException;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.jakartaee.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;

public class GwtRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {
    @NonNls
    private static final String MODULE = "module";
    @NonNls
    private static final String PAGE = "page";
    public String VM_PARAMETERS = "";
    public String SHELL_PARAMETERS = "";
    public String RUN_PAGE = "";
    public String CUSTOM_WEB_XML;

    public GwtRunConfiguration(String name, Project project, GwtRunConfigurationFactory configurationFactory) {
        super(name, new JavaRunConfigurationModule(project, true), configurationFactory);
    }

    public GwtRunConfiguration(Project project, GwtRunConfigurationFactory configurationFactory) {
        this(GwtLocalize.defaultGwtRunConfigurationName().get(), project, configurationFactory);
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new GwtRunConfigurationEditor(getProject());
    }

    @Override
    public RunProfileState getState(@Nonnull final Executor executor, @Nonnull final ExecutionEnvironment env) throws ExecutionException {
        final Module module = getModule();
        if (module == null) {
            throw CantRunException.noModuleConfigured(getConfigurationModule().getModuleName());
        }

        final JavaEEGoogleGwtModuleExtension extension = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
        if (extension == null) {
            throw new ExecutionException(GwtLocalize.errorTextGwtFacetNotConfiguredInModule0(module.getName()).get());
        }


        final Sdk jdk = ModuleUtilCore.getSdk(module, JavaModuleExtension.class);
        if (jdk == null) {
            throw CantRunException.noJdkForModule(getModule());
        }

        final JavaCommandLineState state = new GwtCommandLineState(extension, env, RUN_PAGE, VM_PARAMETERS, SHELL_PARAMETERS, CUSTOM_WEB_XML);

        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        return state;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        getConfigurationModule().checkForWarning();
    }


    @Override
    public Collection<Module> getValidModules() {
        return getAllModules();
    }

    @Nullable
    public
    Module getModule() {
        return getConfigurationModule().getModule();
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
        Element module = element.getChild(MODULE);
        if (module != null) {
            final String page = module.getAttributeValue(PAGE);
            if (!StringUtil.isEmpty(page)) {
                RUN_PAGE = page;
            }
        }
        super.readExternal(element);
    }

    @Override
    public ModuleBasedConfiguration clone() {
        return super.clone();
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
        super.writeExternal(element);
    }

    public void setPage(String runPage) {
        if (runPage == null) {
            runPage = "";
        }
        RUN_PAGE = runPage;
    }

    public String getPage() {
        return RUN_PAGE;
    }

}
