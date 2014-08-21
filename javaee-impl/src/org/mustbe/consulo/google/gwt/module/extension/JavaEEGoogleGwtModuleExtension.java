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

import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.google.gwt.module.extension.impl.GoogleGwtModuleExtensionImpl;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class JavaEEGoogleGwtModuleExtension extends GoogleGwtModuleExtensionImpl<JavaEEGoogleGwtModuleExtension>
{
	protected final Map<String, String> myPackagingPaths = new HashMap<String, String>();

	public JavaEEGoogleGwtModuleExtension(@NotNull String id, @NotNull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
	}

	@Override
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
}
