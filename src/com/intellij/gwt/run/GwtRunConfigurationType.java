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

import javax.swing.Icon;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.execution.LocatableConfigurationType;
import com.intellij.execution.Location;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.containers.ContainerUtil;

public class GwtRunConfigurationType implements ConfigurationType, LocatableConfigurationType
{
	private GwtRunConfigurationFactory myConfigurationFactory;

	GwtRunConfigurationType()
	{
		myConfigurationFactory = new GwtRunConfigurationFactory(this);
	}

	@Override
	public RunnerAndConfigurationSettings createConfigurationByLocation(final Location location)
	{
		Pair<GwtModule, String> pair = findGwtModule(location);
		if(pair != null)
		{
			RunManager runManager = RunManager.getInstance(location.getProject());
			GwtModule gwtModule = pair.getFirst();
			RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(gwtModule.getShortName(), myConfigurationFactory);
			String path = GwtRunConfigurationEditor.getPath(gwtModule, pair.getSecond());
			GwtRunConfiguration gwtConfiguration = (GwtRunConfiguration) settings.getConfiguration();
			gwtConfiguration.setModule(gwtModule.getModule());
			gwtConfiguration.setPage(path);
			return settings;
		}
		return null;
	}

	@Override
	public boolean isConfigurationByLocation(final RunConfiguration configuration, final Location location)
	{
		if(configuration instanceof GwtRunConfiguration)
		{
			Pair<GwtModule, String> pair = findGwtModule(location);
			if(pair != null)
			{
				GwtRunConfiguration gwtRunConfiguration = (GwtRunConfiguration) configuration;
				String pagePath1 = gwtRunConfiguration.getPage();
				Module module1 = gwtRunConfiguration.getModule();

				GwtModule gwtModule = pair.getFirst();
				String pagePath2 = GwtRunConfigurationEditor.getPath(gwtModule, pair.getSecond());
				Module module2 = gwtModule.getModule();
				return pagePath2.equals(pagePath1) && Comparing.equal(module1, module2);
			}
		}
		return false;
	}

	@Nullable
	private static Pair<GwtModule, String> findGwtModule(Location<?> location)
	{
		PsiFile psiFile = location.getPsiElement().getContainingFile();
		if(psiFile == null)
		{
			return null;
		}

		VirtualFile file = psiFile.getVirtualFile();
		if(file == null || !GwtFacet.isInModuleWithGwtFacet(location.getProject(), file))
		{
			return null;
		}

		GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(location.getProject());
		GwtModule gwtModule = gwtModulesManager.getGwtModuleByXmlFile(psiFile);
		if(gwtModule != null)
		{
			return getModuleWithFile(gwtModulesManager, gwtModule);
		}

		if(psiFile instanceof PsiJavaFile)
		{
			PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
			if(classes.length == 1)
			{
				PsiClass psiClass = classes[0];
				GwtModule module = gwtModulesManager.findGwtModuleByEntryPoint(psiClass);
				if(module != null)
				{
					return getModuleWithFile(gwtModulesManager, module);
				}
			}
		}
		return null;
	}

	@Nullable
	private static Pair<GwtModule, String> getModuleWithFile(@NotNull GwtModulesManager gwtModulesManager, @NotNull GwtModule gwtModule)
	{
		XmlFile psiHtmlFile = gwtModulesManager.findHtmlFileByModule(gwtModule);
		if(psiHtmlFile != null)
		{
			VirtualFile htmlFile = psiHtmlFile.getVirtualFile();
			if(htmlFile != null)
			{
				String path = gwtModulesManager.getPathFromPublicRoot(gwtModule, htmlFile);
				if(path != null)
				{
					return Pair.create(gwtModule, path);
				}
			}
		}
		return null;
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
	public Icon getIcon()
	{
		return GwtFacetType.SMALL_ICON;
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories()
	{
		return new ConfigurationFactory[]{myConfigurationFactory};
	}

	public static GwtRunConfigurationFactory getFactory()
	{
		return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), GwtRunConfigurationType.class).myConfigurationFactory;
	}

	@Override
	@NonNls
	@NotNull
	public String getId()
	{
		return "GWT.ConfigurationType";
	}

}

