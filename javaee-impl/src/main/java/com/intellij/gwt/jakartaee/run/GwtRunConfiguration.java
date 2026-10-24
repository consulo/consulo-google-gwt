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

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.java.execution.configurations.JavaCommandLineState;
import com.intellij.java.execution.configurations.JavaRunConfigurationModule;
import consulo.content.bundle.Sdk;
import consulo.execution.CantRunException;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
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
import jakarta.annotation.Nonnull;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GwtRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {
    private static final String MODULE = "module";
    private static final String PAGE = "page";

    public GwtRunConfiguration(String name, Project project, GwtRunConfigurationFactory configurationFactory) {
        super(name, new JavaRunConfigurationModule(project, true), configurationFactory);
    }

    public GwtRunConfiguration(Project project, GwtRunConfigurationFactory configurationFactory) {
        this(GwtLocalize.defaultGwtRunConfigurationName().get(), project, configurationFactory);
    }

    @Override
    protected Class<GwtRunConfigurationOptions> getOptionsClass() {
        return GwtRunConfigurationOptions.class;
    }

    @Override
    public GwtRunConfigurationOptions getOptions() {
        return (GwtRunConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        GwtRunConfigurationLegacyState old = converter.readXml(GwtRunConfigurationLegacyState.class);

        String page = old.RUN_PAGE;
        Element module = converter.getElement().getChild(MODULE);
        if (module != null) {
            String modulePage = module.getAttributeValue(PAGE);
            if (!StringUtil.isEmpty(modulePage)) {
                page = modulePage;
            }
        }

        GwtRunConfigurationOptions options = new GwtRunConfigurationOptions();
        options.setPage(page);
        options.setVmParameters(old.VM_PARAMETERS);
        options.setShellParameters(old.SHELL_PARAMETERS);
        options.setCustomWebXml(old.CUSTOM_WEB_XML);
        converter.setOptions(options);
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(
            new GwtHtmlPageFragment(),
            new GwtVmParametersFragment(),
            new GwtShellParametersFragment(),
            new GwtCustomWebXmlFragment()
        );
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

        final JavaCommandLineState state = new GwtCommandLineState(
            extension,
            env,
            StringUtil.notNullize(getPage()),
            StringUtil.notNullize(getVmParameters()),
            StringUtil.notNullize(getShellParameters()),
            getCustomWebXml()
        );

        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        return state;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        getConfigurationModule().checkForWarning();
    }

    @Override
    public Collection<Module> getValidModules() {
        List<Module> modules = new ArrayList<>();
        for (Module module : getAllModules()) {
            if (ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class) != null) {
                modules.add(module);
            }
        }
        return modules;
    }

    @Nullable
    public Module getModule() {
        return getConfigurationModule().getModule();
    }

    @Override
    public ModuleBasedConfiguration clone() {
        return super.clone();
    }

    public void setPage(@Nullable String page) {
        getOptions().setPage(page);
    }

    @Nullable
    public String getPage() {
        return getOptions().getPage();
    }

    @Nullable
    public String getVmParameters() {
        return getOptions().getVmParameters();
    }

    public void setVmParameters(@Nullable String vmParameters) {
        getOptions().setVmParameters(vmParameters);
    }

    @Nullable
    public String getShellParameters() {
        return getOptions().getShellParameters();
    }

    public void setShellParameters(@Nullable String shellParameters) {
        getOptions().setShellParameters(shellParameters);
    }

    /**
     * @return the URL of the web.xml whose content replaces the module one, {@code null} for the module web.xml
     */
    @Nullable
    public String getCustomWebXml() {
        return getOptions().getCustomWebXml();
    }

    public void setCustomWebXml(@Nullable String customWebXml) {
        getOptions().setCustomWebXml(customWebXml);
    }

    public static String getPagePath(GwtModule gwtModule, String relativePath) {
        return gwtModule.getQualifiedName() + "/" + relativePath;
    }
}
