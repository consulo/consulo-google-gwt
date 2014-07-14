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

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.Tag;

/**
 * @author nik
 */
@Deprecated
public class GwtFacetConfiguration// implements FacetConfiguration, PersistentStateComponent<GwtFacetConfiguration.GwtFacetState>
{
	@NonNls
	private static final String GWT_URL_ATTRIBUTE = "gwtSdkUrl";
	@NonNls
	private static final String COMPILER_OUTPUT_PATH_ATTRIBUTE = "compilerOutputPath";
	@NonNls
	private static final String WEB_FACET_ATTRIBUTE = "webFacet";
	@NonNls
	private static final String GWT_SCRIPT_OUTPUT_STYLE_ATTRIBUTE = "gwtScriptOutputStyle";
	@NonNls
	private static final String RUN_GWT_COMPILER_ON_MAKE_ATTRIBUTE = "runGwtCompilerOnMake";
	@NonNls
	private static final String ADDITIONAL_COMPILER_PARAMETERS_ATTRIBUTE = "additionalCompilerParameters";
	@NonNls
	private static final String COMPILER_MAX_HEAP_SIZE_ATTRIBUTE = "compilerMaxHeapSize";
	@NonNls
	private static final String PACKAGING_PATHS_ELEMENT = "packaging";
	@NonNls
	private static final String GWT_MODULE_ELEMENT = "module";
	@NonNls
	private static final String MODULE_NAME_ATTRIBUTE = "name";
	@NonNls
	private static final String PACKAGING_PATH_ATTRIBUTE = "path";
	private String myGwtSdkUrl = "";
	private GwtJavaScriptOutputStyle myOutputStyle = GwtJavaScriptOutputStyle.DETAILED;
	private boolean myRunGwtCompilerOnMake = true;
	private int myCompilerMaxHeapSize = 128;
	private String myAdditionalCompilerParameters = "";
	private String myWebFacetName;
	private final Map<String, String> myPackagingPaths = new HashMap<String, String>();
	private String myCompilerOutputPath = "";

	public void loadState(final GwtFacetState state)
	{
		myPackagingPaths.clear();
		myPackagingPaths.putAll(state.getPackagingPaths());

		Map<String, String> settings = state.getFacetSettings();
		String sdkUrl = settings.get(GWT_URL_ATTRIBUTE);
		setGwtSdkUrl(sdkUrl == null ? "" : sdkUrl);
		final String styleId = settings.get(GWT_SCRIPT_OUTPUT_STYLE_ATTRIBUTE);
		final GwtJavaScriptOutputStyle style = GwtJavaScriptOutputStyle.byId(styleId);
		if(style != null)
		{
			myOutputStyle = style;
		}
		myRunGwtCompilerOnMake = !Boolean.toString(false).equals(settings.get(RUN_GWT_COMPILER_ON_MAKE_ATTRIBUTE));
		myWebFacetName = settings.get(WEB_FACET_ATTRIBUTE);
		myCompilerOutputPath = settings.get(COMPILER_OUTPUT_PATH_ATTRIBUTE);
		if(myCompilerOutputPath == null)
		{
			myCompilerOutputPath = "";
		}
		myAdditionalCompilerParameters = settings.get(ADDITIONAL_COMPILER_PARAMETERS_ATTRIBUTE);
		if(myAdditionalCompilerParameters == null)
		{
			myAdditionalCompilerParameters = "";
		}
		try
		{
			myCompilerMaxHeapSize = Integer.parseInt(settings.get(COMPILER_MAX_HEAP_SIZE_ATTRIBUTE));
		}
		catch(NumberFormatException e)
		{
			myCompilerMaxHeapSize = 128;
		}
	}

	public GwtFacetState getState()
	{
		GwtFacetState state = new GwtFacetState();
		Map<String, String> settings = state.getFacetSettings();
		settings.put(GWT_URL_ATTRIBUTE, myGwtSdkUrl);

		if(myWebFacetName != null)
		{
			settings.put(WEB_FACET_ATTRIBUTE, myWebFacetName);
		}
		settings.put(COMPILER_OUTPUT_PATH_ATTRIBUTE, myCompilerOutputPath);
		settings.put(GWT_SCRIPT_OUTPUT_STYLE_ATTRIBUTE, myOutputStyle.getId());
		settings.put(RUN_GWT_COMPILER_ON_MAKE_ATTRIBUTE, String.valueOf(myRunGwtCompilerOnMake));
		settings.put(ADDITIONAL_COMPILER_PARAMETERS_ATTRIBUTE, myAdditionalCompilerParameters);
		settings.put(COMPILER_MAX_HEAP_SIZE_ATTRIBUTE, String.valueOf(myCompilerMaxHeapSize));

		state.getPackagingPaths().putAll(myPackagingPaths);
		return state;
	}

	public String getGwtSdkPath()
	{
		return FileUtil.toSystemDependentName(VfsUtil.urlToPath(myGwtSdkUrl));
	}

	public void setGwtSdkUrl(final String gwtUrl)
	{
		myGwtSdkUrl = gwtUrl;
	}

	public String getGwtSdkUrl()
	{
		return myGwtSdkUrl;
	}

	public boolean isRunGwtCompilerOnMake()
	{
		return myRunGwtCompilerOnMake;
	}

	public void setRunGwtCompilerOnMake(final boolean runGwtCompiler)
	{
		myRunGwtCompilerOnMake = runGwtCompiler;
	}

	public String getAdditionalCompilerParameters()
	{
		return myAdditionalCompilerParameters;
	}

	public void setAdditionalCompilerParameters(final String additionalCompilerParameters)
	{
		myAdditionalCompilerParameters = additionalCompilerParameters;
	}

	public int getCompilerMaxHeapSize()
	{
		return myCompilerMaxHeapSize;
	}

	public void setCompilerMaxHeapSize(final int compilerMaxHeapSize)
	{
		myCompilerMaxHeapSize = compilerMaxHeapSize;
	}

	public GwtJavaScriptOutputStyle getOutputStyle()
	{
		return myOutputStyle;
	}

	public void setOutputStyle(final GwtJavaScriptOutputStyle outputStyle)
	{
		myOutputStyle = outputStyle;
	}

	public void setWebFacetName(final String webFacetName)
	{
		myWebFacetName = webFacetName;
	}

	@Nullable
	public String getWebFacetName()
	{
		return myWebFacetName;
	}

	public String getCompilerOutputPath()
	{
		return myCompilerOutputPath;
	}

	public void setCompilerOutputPath(final String compilerOutputPath)
	{
		myCompilerOutputPath = compilerOutputPath;
	}

	@NotNull
	public String getPackagingRelativePath(@NotNull GwtModule module)
	{
		String moduleName = module.getQualifiedName();
		String path = myPackagingPaths.get(moduleName);
		if(path != null)
		{
			return path;
		}
		return "/" + moduleName;
	}

	public void setPackagingRelativePath(@NotNull String moduleName, @NotNull String path)
	{
		myPackagingPaths.put(moduleName, path);
	}

	public static class GwtFacetState
	{
		//todo[nik] patch OptionTagBinding and use it
		private Map<String, String> myPackagingPaths = new HashMap<String, String>();
		private Map<String, String> myFacetSettings = new HashMap<String, String>();

		@Tag(PACKAGING_PATHS_ELEMENT)
		@MapAnnotation(surroundKeyWithTag = false, surroundValueWithTag = false, surroundWithTag = false, entryTagName = GWT_MODULE_ELEMENT,
				keyAttributeName = MODULE_NAME_ATTRIBUTE, valueAttributeName = PACKAGING_PATH_ATTRIBUTE)
		public Map<String, String> getPackagingPaths()
		{
			return myPackagingPaths;
		}

		public void setPackagingPaths(final Map<String, String> packagingPaths)
		{
			myPackagingPaths = packagingPaths;
		}

		@Property(surroundWithTag = false)
		@MapAnnotation(surroundKeyWithTag = false, surroundValueWithTag = false, surroundWithTag = false,
				entryTagName = "setting", keyAttributeName = "name", valueAttributeName = "value")
		public Map<String, String> getFacetSettings()
		{
			return myFacetSettings;
		}

		public void setFacetSettings(final Map<String, String> facetSettings)
		{
			myFacetSettings = facetSettings;
		}
	}
}
