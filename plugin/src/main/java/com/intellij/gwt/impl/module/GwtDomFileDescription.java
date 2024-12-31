/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.gwt.impl.module;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.util.Iconable;
import consulo.google.gwt.base.icon.GwtIconGroup;
import consulo.gwt.base.module.extension.impl.GoogleGwtModuleExtensionImpl;
import consulo.language.util.ModuleUtilCore;
import consulo.ui.image.Image;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomFileDescription;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
@ExtensionImpl
public class GwtDomFileDescription extends DomFileDescription<GwtModule>
{
	public GwtDomFileDescription()
	{
		super(GwtModule.class, "module");
	}

	@Nullable
	@Override
	public Image getFileIcon(@Iconable.IconFlags int flags)
	{
		return GwtIconGroup.gwt();
	}

	@Override
	public boolean isMyFile(@Nonnull XmlFile file)
	{
		if(!(file.getName().endsWith(GwtModulesManager.GWT_XML_SUFFIX) && super.isMyFile(file)))
		{
			return false;
		}
		return ModuleUtilCore.getExtension(file, GoogleGwtModuleExtensionImpl.class) != null;
	}

	@Override
	public boolean isAutomaticHighlightingEnabled()
	{
		return false;
	}
}
