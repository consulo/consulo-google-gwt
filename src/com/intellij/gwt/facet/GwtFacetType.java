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

package com.intellij.gwt.facet;

import static com.intellij.patterns.StandardPatterns.string;

import java.util.Collection;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetModel;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.autodetecting.FacetDetector;
import com.intellij.facet.autodetecting.FacetDetectorRegistry;
import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.gwt.GwtBundle;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;

/**
 * @author nik
 */
public class GwtFacetType extends FacetType<GwtFacet, GwtFacetConfiguration>
{
	public static final FacetTypeId<GwtFacet> ID = new FacetTypeId<GwtFacet>("gwt");
	public static final GwtFacetType INSTANCE = new GwtFacetType();
	public static final Icon SMALL_ICON = IconLoader.getIcon("/icons/google-small.png");

	private GwtFacetType()
	{
		super(ID, "gwt", GwtBundle.message("facet.type.name.gwt"));
	}

	public GwtFacetConfiguration createDefaultConfiguration()
	{
		return new GwtFacetConfiguration();
	}

	public GwtFacet createFacet(@NotNull Module module, final String name, @NotNull GwtFacetConfiguration configuration,
			@Nullable Facet underlyingFacet)
	{
		return new GwtFacet(this, module, name, configuration);
	}


	public boolean isOnlyOneFacetAllowed()
	{
		return true;
	}

	public boolean isSuitableModuleType(ModuleType moduleType)
	{
		return moduleType instanceof JavaModuleType;
	}

	public void registerDetectors(final FacetDetectorRegistry<GwtFacetConfiguration> detectorRegistry)
	{
		detectorRegistry.registerUniversalDetector(StdFileTypes.XML, PlatformPatterns.virtualFile().withName(string().endsWith(".gwt.xml")),
				new GwtFacetDetector());
	}

	@Override
	public String getHelpTopic()
	{
		return "IntelliJ.IDEA.Procedures.Java.EE.Development.Managing.Facets.Facet.Specific.Settings.GWT";
	}

	public Icon getIcon()
	{
		return SMALL_ICON;
	}

	public DefaultFacetSettingsEditor createDefaultConfigurationEditor(@NotNull final Project project,
			@NotNull final GwtFacetConfiguration configuration)
	{
		return new DefaultGwtFacetSettingsEditor(project, configuration);
	}

	public MultipleFacetSettingsEditor createMultipleConfigurationsEditor(@NotNull final Project project, @NotNull final FacetEditor[] editors)
	{
		return new MultipleGwtFacetSettingsEditor(project, editors);
	}

	private static class GwtFacetDetector extends FacetDetector<VirtualFile, GwtFacetConfiguration>
	{
		public GwtFacetDetector()
		{
			super("gwt-detector");
		}

		public GwtFacetConfiguration detectFacet(final VirtualFile source, final Collection<GwtFacetConfiguration> existentFacetConfigurations)
		{
			if(!existentFacetConfigurations.isEmpty())
			{
				return existentFacetConfigurations.iterator().next();
			}
			return new GwtFacetConfiguration();
		}

		@Override
		public void beforeFacetAdded(@NotNull final Facet facet, final FacetModel facetModel, @NotNull final ModifiableRootModel modifiableRootModel)
		{
			final GwtFacetConfiguration configuration = ((GwtFacet) facet).getConfiguration();
			GwtFacet.setupGwtSdkAndLibraries(configuration, modifiableRootModel, configuration.getSdk());
		}
	}
}
