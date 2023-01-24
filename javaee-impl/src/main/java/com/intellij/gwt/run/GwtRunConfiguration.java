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

package com.intellij.gwt.run;

import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.gwt.GwtBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import consulo.gwt.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.java.module.extension.JavaModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class GwtRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule>
{
	@NonNls
	private static final String MODULE = "module";
	@NonNls
	private static final String PAGE = "page";
	public String VM_PARAMETERS = "";
	public String SHELL_PARAMETERS = "";
	public String RUN_PAGE = "";
	public String CUSTOM_WEB_XML;

	public GwtRunConfiguration(String name, Project project, GwtRunConfigurationFactory configurationFactory)
	{
		super(name, new JavaRunConfigurationModule(project, true), configurationFactory);
	}

	public GwtRunConfiguration(Project project, GwtRunConfigurationFactory configurationFactory)
	{
		this(GwtBundle.message("default.gwt.run.configuration.name"), project, configurationFactory);
	}

	@Nonnull
	@Override
	public SettingsEditor<? extends RunConfiguration> getConfigurationEditor()
	{
		return new GwtRunConfigurationEditor(getProject());
	}

	@Override
	public RunProfileState getState(@Nonnull final Executor executor, @Nonnull final ExecutionEnvironment env) throws ExecutionException
	{
		final Module module = getModule();
		if(module == null)
		{
			throw CantRunException.noModuleConfigured(getConfigurationModule().getModuleName());
		}

		final JavaEEGoogleGwtModuleExtension extension = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
		if(extension == null)
		{
			throw new ExecutionException(GwtBundle.message("error.text.gwt.facet.not.configured.in.module.0", module.getName()));
		}


		final Sdk jdk = ModuleUtilCore.getSdk(module, JavaModuleExtension.class);
		if(jdk == null)
		{
			throw CantRunException.noJdkForModule(getModule());
		}

		final JavaCommandLineState state = new GwtCommandLineState(extension, env, RUN_PAGE, VM_PARAMETERS, SHELL_PARAMETERS, CUSTOM_WEB_XML);

		state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
		return state;
	}

	@Override
	public void checkConfiguration() throws RuntimeConfigurationException
	{
		getConfigurationModule().checkForWarning();
	}


	@Override
	public Collection<Module> getValidModules()
	{
		return getAllModules();
	}

	public
	@Nullable
	Module getModule()
	{
		return getConfigurationModule().getModule();
	}

	@Override
	public void readExternal(Element element) throws InvalidDataException
	{
		DefaultJDOMExternalizer.readExternal(this, element);
		Element module = element.getChild(MODULE);
		if(module != null)
		{
			final String page = module.getAttributeValue(PAGE);
			if(!StringUtil.isEmpty(page))
			{
				RUN_PAGE = page;
			}
		}
		super.readExternal(element);
	}

	@Override
	public ModuleBasedConfiguration clone()
	{
		return super.clone();
	}

	@Override
	public void writeExternal(Element element) throws WriteExternalException
	{
		DefaultJDOMExternalizer.writeExternal(this, element);
		super.writeExternal(element);
	}

	public void setPage(String runPage)
	{
		if(runPage == null)
		{
			runPage = "";
		}
		RUN_PAGE = runPage;
	}

	public String getPage()
	{
		return RUN_PAGE;
	}

}
