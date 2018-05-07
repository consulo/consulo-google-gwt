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
package com.intellij.gwt.module;

import javax.annotation.Nonnull;
import javax.swing.Icon;

import javax.annotation.Nullable;
import consulo.awt.TargetAWT;
import consulo.gwt.GwtIcons;
import consulo.gwt.module.extension.impl.GoogleGwtModuleExtensionImpl;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.impl.GwtModuleImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomFileDescription;

/**
 * @author peter
 */
public class GwtDomFileDescription extends DomFileDescription<GwtModule>
{
	public GwtDomFileDescription()
	{
		super(GwtModule.class, "module");
	}

	@Nullable
	@Override
	public Icon getFileIcon(@Iconable.IconFlags int flags)
	{
		return TargetAWT.to(GwtIcons.Gwt);
	}

	@Override
	protected void initializeFileDescription()
	{
		registerImplementation(GwtModule.class, GwtModuleImpl.class);
	}

	@Override
	public boolean isMyFile(@Nonnull XmlFile file, final Module module)
	{
		if(!(file.getName().endsWith(GwtModulesManager.GWT_XML_SUFFIX) && super.isMyFile(file, module)))
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
