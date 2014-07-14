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

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.consulo.module.extension.ui.ModuleExtensionWithSdkPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.facet.GwtFacetEditor;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class JavaEEGoogleGwtMutableModuleExtension extends JavaEEGoogleGwtModuleExtension implements
		MutableModuleExtensionWithSdk<JavaEEGoogleGwtModuleExtension>
{
	public JavaEEGoogleGwtMutableModuleExtension(@NotNull String id, @NotNull ModifiableRootModel rootModel)
	{
		super(id, rootModel);
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

	@NotNull
	@Override
	public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk()
	{
		return (MutableModuleInheritableNamedPointer<Sdk>) super.getInheritableSdk();
	}

	@Nullable
	@Override
	public JComponent createConfigurablePanel(@NotNull Runnable runnable)
	{
		JPanel panel = new JPanel(new VerticalFlowLayout());
		panel.add(new ModuleExtensionWithSdkPanel(this, runnable));
		panel.add(new GwtFacetEditor(this));
		return wrapToNorth(panel);
	}

	@Override
	public void setEnabled(boolean b)
	{
		myIsEnabled = b;
	}

	@Override
	public boolean isModified(@NotNull JavaEEGoogleGwtModuleExtension originExtension)
	{
		if(isModifiedImpl(originExtension))
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
		if(!Comparing.equal(myPackagingPaths, originExtension.myPackagingPaths))
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
