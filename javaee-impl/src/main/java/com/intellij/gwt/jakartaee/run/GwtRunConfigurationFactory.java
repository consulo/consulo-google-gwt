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

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.gwt.jakartaee.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;

import javax.annotation.Nonnull;

public class GwtRunConfigurationFactory extends ConfigurationFactory
{
	public GwtRunConfigurationFactory(GwtRunConfigurationType gwtConfigurationType)
	{
		super(gwtConfigurationType);
	}

	@Nonnull
	@Override
	public String getId()
	{
		return "GWT Configuration";
	}

	@Override
	public boolean isApplicable(@Nonnull Project project)
	{
		return ModuleExtensionHelper.getInstance(project).hasModuleExtension(JavaEEGoogleGwtModuleExtension.class);
	}

	@Override
	public RunConfiguration createTemplateConfiguration(Project project)
	{
		return new GwtRunConfiguration(project, this);
	}
}
