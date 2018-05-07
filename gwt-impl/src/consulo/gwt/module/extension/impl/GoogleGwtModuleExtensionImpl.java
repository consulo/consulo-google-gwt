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

package consulo.gwt.module.extension.impl;

import java.util.List;

import javax.annotation.Nonnull;

import org.jdom.Element;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.make.GwtModuleFileProcessingItem;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import consulo.annotations.RequiredReadAction;
import consulo.extension.impl.ModuleExtensionWithSdkImpl;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.sdk.GoogleGwtSdkType;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 03.12.13.
 */
public abstract class GoogleGwtModuleExtensionImpl<T extends GoogleGwtModuleExtensionImpl<T>> extends ModuleExtensionWithSdkImpl<T> implements GoogleGwtModuleExtension<T>
{
	protected GwtJavaScriptOutputStyle myOutputStyle = GwtJavaScriptOutputStyle.DETAILED;
	protected boolean myRunGwtCompilerOnMake = true;
	protected int myCompilerMaxHeapSize = 256;
	protected String myAdditionalCompilerParameters = "";
	protected String myAdditionalCompilerVmParameters = "";
	protected String myCompilerOutputUrl = "";

	public GoogleGwtModuleExtensionImpl(@Nonnull String id, @Nonnull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
	}

	@Override
	public void addFilesForCompilation(GwtModule gwtModule, List<FileProcessingCompiler.ProcessingItem> result)
	{
		addFilesRecursively(gwtModule, this, gwtModule.getModuleFile(), result);

		for(VirtualFile file : gwtModule.getPublicRoots())
		{
			addFilesRecursively(gwtModule, this, file, result);
		}
		for(VirtualFile file : gwtModule.getSourceRoots())
		{
			addFilesRecursively(gwtModule, this, file, result);
		}
	}

	protected static void addFilesRecursively(final GwtModule module, GoogleGwtModuleExtension extension, final VirtualFile file, final List<FileProcessingCompiler.ProcessingItem> result)
	{
		if(!file.isValid() || FileTypeManager.getInstance().isFileIgnored(file.getName()))
		{
			return;
		}

		if(file.isDirectory())
		{
			final VirtualFile[] children = file.getChildren();
			for(VirtualFile child : children)
			{
				addFilesRecursively(module, extension, child, result);
			}
		}
		else
		{
			result.add(new GwtModuleFileProcessingItem(extension, module, VfsUtilCore.virtualToIoFile(file)));
		}
	}

	@Override
	public void setupCompilerClasspath(PathsList pathsList)
	{
	}

	@Override
	@Nonnull
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
	@Nonnull
	public String getAdditionalCompilerParameters()
	{
		return myAdditionalCompilerParameters;
	}

	@Nonnull
	@Override
	public String getAdditionalVmCompilerParameters()
	{
		return myAdditionalCompilerVmParameters;
	}

	@Override
	public int getCompilerMaxHeapSize()
	{
		return myCompilerMaxHeapSize;
	}

	@Override
	public String getCompilerOutputUrl()
	{
		return myCompilerOutputUrl;
	}

	@Nonnull
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

	public void setAdditionalCompilerParameters(String additionalCompilerParameters)
	{
		myAdditionalCompilerParameters = additionalCompilerParameters;
	}

	public void setAdditionalCompilerVmParameters(final String vmAdditionalCompilerParameters)
	{
		myAdditionalCompilerVmParameters = vmAdditionalCompilerParameters;
	}

	public void setCompilerMaxHeapSize(final int compilerMaxHeapSize)
	{
		myCompilerMaxHeapSize = compilerMaxHeapSize;
	}

	public void setCompilerOutputUrl(final String compilerOutputUrl)
	{
		myCompilerOutputUrl = compilerOutputUrl;
	}

	@Override
	@Nonnull
	public String getPackagingRelativePath(@Nonnull GwtModule module)
	{
		return "";
	}

	@RequiredReadAction
	@Override
	protected void loadStateImpl(@Nonnull Element element)
	{
		super.loadStateImpl(element);

		myOutputStyle = GwtJavaScriptOutputStyle.valueOf(element.getAttributeValue("output-style", "PRETTY"));
		myCompilerOutputUrl = element.getAttributeValue("compiler-output-url");
		myCompilerMaxHeapSize = Integer.parseInt(element.getAttributeValue("compiler-max-heap-size", "256"));
		myAdditionalCompilerParameters = element.getAttributeValue("compiler-parameters", "");
		myAdditionalCompilerVmParameters = element.getAttributeValue("compiler-vm-parameters", "");
	}

	@Override
	protected void getStateImpl(@Nonnull Element element)
	{
		super.getStateImpl(element);

		element.setAttribute("output-style", myOutputStyle.name());
		if(myCompilerOutputUrl != null)
		{
			element.setAttribute("compiler-output-url", myCompilerOutputUrl);
		}
		element.setAttribute("compiler-max-heap-size", String.valueOf(myCompilerMaxHeapSize));
		element.setAttribute("compiler-parameters", myAdditionalCompilerParameters);
		element.setAttribute("compiler-vm-parameters", myAdditionalCompilerVmParameters);
	}

	@Override
	public void commit(@Nonnull T mutableModuleExtension)
	{
		super.commit(mutableModuleExtension);

		myAdditionalCompilerParameters = mutableModuleExtension.myAdditionalCompilerParameters;
		myAdditionalCompilerVmParameters = mutableModuleExtension.myAdditionalCompilerVmParameters;
		myOutputStyle = mutableModuleExtension.myOutputStyle;
		myRunGwtCompilerOnMake = mutableModuleExtension.myRunGwtCompilerOnMake;
		myCompilerMaxHeapSize = mutableModuleExtension.myCompilerMaxHeapSize;
		myCompilerOutputUrl = mutableModuleExtension.myCompilerOutputUrl;
	}

	public boolean isModifiedImpl(@Nonnull T originExtension)
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
		if(!Comparing.equal(myAdditionalCompilerVmParameters, originExtension.myAdditionalCompilerVmParameters))
		{
			return true;
		}
		if(!Comparing.equal(myAdditionalCompilerParameters, originExtension.myAdditionalCompilerParameters))
		{
			return true;
		}
		if(!Comparing.equal(myCompilerOutputUrl, originExtension.myCompilerOutputUrl))
		{
			return true;
		}
		return false;
	}
}
