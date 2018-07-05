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

package consulo.gwt.module.extension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;
import consulo.annotations.RequiredDispatchThread;
import consulo.extension.ui.ModuleExtensionSdkBoxBuilder;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class JavaEEGoogleGwtMutableModuleExtension extends JavaEEGoogleGwtModuleExtension implements GoogleGwtMutableModuleExtension<JavaEEGoogleGwtModuleExtension>
{
	public JavaEEGoogleGwtMutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
	}

	@Nonnull
	@Override
	public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk()
	{
		return (MutableModuleInheritableNamedPointer<Sdk>) super.getInheritableSdk();
	}

	@Nullable
	@Override
	@RequiredDispatchThread
	public JComponent createConfigurablePanel(@Nonnull Runnable runnable)
	{
		JPanel panel = new JPanel(new VerticalFlowLayout(true, false));
		if(GwtLibraryPathProvider.EP_NAME.composite().canChooseBundle(getModuleRootLayer()))
		{
			panel.add(ModuleExtensionSdkBoxBuilder.createAndDefine(this, runnable).build());
		}
		panel.add(new GwtModuleExtensionPanel(this));
		return panel;
	}

	@Override
	public void setEnabled(boolean b)
	{
		myIsEnabled = b;
	}

	@Override
	public boolean isModified(@Nonnull JavaEEGoogleGwtModuleExtension originExtension)
	{
		if(isModifiedImpl(originExtension))
		{
			return true;
		}
		if(!Comparing.equal(myPackagingPaths, originExtension.myPackagingPaths))
		{
			return true;
		}
		return false;
	}
}
