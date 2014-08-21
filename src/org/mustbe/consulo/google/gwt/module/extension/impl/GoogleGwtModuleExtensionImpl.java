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

package org.mustbe.consulo.google.gwt.module.extension.impl;

import org.consulo.module.extension.impl.ModuleExtensionWithSdkImpl;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtension;
import org.mustbe.consulo.google.gwt.sdk.GoogleGwtSdkType;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.PathsList;

/**
 * @author VISTALL
 * @since 03.12.13.
 */
public abstract class GoogleGwtModuleExtensionImpl<T extends GoogleGwtModuleExtensionImpl<T>> extends ModuleExtensionWithSdkImpl<T> implements
		GoogleGwtModuleExtension<T>
{
	protected GwtJavaScriptOutputStyle myOutputStyle = GwtJavaScriptOutputStyle.DETAILED;
	protected boolean myRunGwtCompilerOnMake = true;
	protected int myCompilerMaxHeapSize = 256;
	protected String myAdditionalCompilerParameters = "";
	protected String myCompilerOutputPath = "";

	public GoogleGwtModuleExtensionImpl(@NotNull String id, @NotNull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
	}

	@Override
	public void setupCompilerClasspath(PathsList pathsList)
	{

	}

	@Override
	@NotNull
	public GwtJavaScriptOutputStyle getOutputStyle()
	{
		return myOutputStyle;
	}

	@Override
	public boolean isRunGwtCompilerOnMake()
	{
		return myRunGwtCompilerOnMake;
	}

	@Override
	public String getAdditionalCompilerParameters()
	{
		return myAdditionalCompilerParameters;
	}

	@Override
	public int getCompilerMaxHeapSize()
	{
		return myCompilerMaxHeapSize;
	}

	@Override
	public String getCompilerOutputPath()
	{
		return myCompilerOutputPath;
	}

	@NotNull
	@Override
	public Class<? extends SdkType> getSdkTypeClass()
	{
		return GoogleGwtSdkType.class;
	}

	public void setOutputStyle(final GwtJavaScriptOutputStyle outputStyle)
	{
		myOutputStyle = outputStyle;
	}

	public void setRunGwtCompilerOnMake(final boolean runGwtCompiler)
	{
		myRunGwtCompilerOnMake = runGwtCompiler;
	}

	public void setAdditionalCompilerParameters(final String additionalCompilerParameters)
	{
		myAdditionalCompilerParameters = additionalCompilerParameters;
	}

	public void setCompilerMaxHeapSize(final int compilerMaxHeapSize)
	{
		myCompilerMaxHeapSize = compilerMaxHeapSize;
	}

	public void setCompilerOutputPath(final String compilerOutputPath)
	{
		myCompilerOutputPath = compilerOutputPath;
	}

	@Override
	@NotNull
	public String getPackagingRelativePath(@NotNull GwtModule module)
	{
		return "";
	}

	@Override
	protected void loadStateImpl(@NotNull Element element)
	{
		super.loadStateImpl(element);

		myOutputStyle = GwtJavaScriptOutputStyle.valueOf(element.getAttributeValue("output-style", "PRETTY"));
		myCompilerOutputPath = element.getAttributeValue("compiler-path");
		myCompilerMaxHeapSize = Integer.parseInt(element.getAttributeValue("compiler-max-heap-size", "256"));
	}

	@Override
	protected void getStateImpl(@NotNull Element element)
	{
		super.getStateImpl(element);

		element.setAttribute("output-style", myOutputStyle.name());
		element.setAttribute("compiler-path", myCompilerOutputPath);
		element.setAttribute("compiler-max-heap-size", String.valueOf(myCompilerMaxHeapSize));
	}

	@Override
	public void commit(@NotNull T mutableModuleExtension)
	{
		super.commit(mutableModuleExtension);

		myAdditionalCompilerParameters = mutableModuleExtension.myAdditionalCompilerParameters;
		myOutputStyle = mutableModuleExtension.myOutputStyle;
		myRunGwtCompilerOnMake = mutableModuleExtension.myRunGwtCompilerOnMake;
		myCompilerMaxHeapSize = mutableModuleExtension.myCompilerMaxHeapSize;
		myCompilerOutputPath = mutableModuleExtension.myCompilerOutputPath;
	}

	@Override
	@NotNull
	public GwtVersion getSdkVersion()
	{
		return GwtSdkUtil.detectVersion(getSdk());
	}

	public boolean isModifiedImpl(@NotNull T originExtension)
	{
		if(super.isModifiedImpl(originExtension))
		{
			return true;
		}
		if(!Comparing.equal(myOutputStyle, originExtension.myOutputStyle))
		{
			return true;
		}
		if(myRunGwtCompilerOnMake != originExtension.myRunGwtCompilerOnMake)
		{
			return true;
		}
		if(myCompilerMaxHeapSize != originExtension.myCompilerMaxHeapSize)
		{
			return true;
		}
		if(!Comparing.equal(myAdditionalCompilerParameters, originExtension.myAdditionalCompilerParameters))
		{
			return true;
		}
		if(!Comparing.equal(myCompilerOutputPath, originExtension.myCompilerOutputPath))
		{
			return true;
		}
		return false;
	}
}
