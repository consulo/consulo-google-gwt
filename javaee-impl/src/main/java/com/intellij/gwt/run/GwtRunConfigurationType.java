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

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.gwt.GwtBundle;
import consulo.gwt.GwtIcons;
import consulo.ui.image.Image;

public class GwtRunConfigurationType implements ConfigurationType
{
	@Nonnull
	public static GwtRunConfigurationType getInstance()
	{
		return CONFIGURATION_TYPE_EP.findExtension(GwtRunConfigurationType.class);
	}

	private GwtRunConfigurationFactory myConfigurationFactory;

	GwtRunConfigurationType()
	{
		myConfigurationFactory = new GwtRunConfigurationFactory(this);
	}

	@Override
	public String getDisplayName()
	{
		return GwtBundle.message("run.gwt.configuration.display.name");
	}

	@Override
	public String getConfigurationTypeDescription()
	{
		return GwtBundle.message("run.gwt.configuration.description");
	}

	@Override
	public Image getIcon()
	{
		return GwtIcons.Gwt;
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories()
	{
		return new ConfigurationFactory[]{myConfigurationFactory};
	}

	@Override
	@NonNls
	@Nonnull
	public String getId()
	{
		return "GWT.ConfigurationType";
	}

}

