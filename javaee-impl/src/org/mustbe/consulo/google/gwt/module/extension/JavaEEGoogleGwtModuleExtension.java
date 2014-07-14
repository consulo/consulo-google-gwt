/*
 * Copyright 2013-2014 must-be.org
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

package org.mustbe.consulo.google.gwt.module.extension;

import java.util.HashMap;
import java.util.Map;

import org.consulo.module.extension.impl.ModuleExtensionWithSdkImpl;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.google.gwt.sdk.GoogleGwtSdkType;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ModifiableRootModel;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class JavaEEGoogleGwtModuleExtension extends ModuleExtensionWithSdkImpl<JavaEEGoogleGwtModuleExtension> implements
		GoogleGwtModuleExtension<JavaEEGoogleGwtModuleExtension>
{
	protected GwtJavaScriptOutputStyle myOutputStyle = GwtJavaScriptOutputStyle.DETAILED;
	protected boolean myRunGwtCompilerOnMake = true;
	protected int myCompilerMaxHeapSize = 128;
	protected String myAdditionalCompilerParameters = "";
	protected final Map<String, String> myPackagingPaths = new HashMap<String, String>();
	protected String myCompilerOutputPath = "";

	public JavaEEGoogleGwtModuleExtension(@NotNull String id, @NotNull ModifiableRootModel rootModel)
	{
		super(id, rootModel);
	}

	public GwtJavaScriptOutputStyle getOutputStyle()
	{
		return myOutputStyle;
	}

	public boolean isRunGwtCompilerOnMake()
	{
		return myRunGwtCompilerOnMake;
	}

	public String getAdditionalCompilerParameters()
	{
		return myAdditionalCompilerParameters;
	}

	public int getCompilerMaxHeapSize()
	{
		return myCompilerMaxHeapSize;
	}

	public String getCompilerOutputPath()
	{
		return myCompilerOutputPath;
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

	@NotNull
	@Override
	public Class<? extends SdkType> getSdkTypeClass()
	{
		return GoogleGwtSdkType.class;
	}

	@NotNull
	@Override
	public GwtVersion getSdkVersion()
	{
		return GwtSdkUtil.detectVersion(getSdk());
	}
}
