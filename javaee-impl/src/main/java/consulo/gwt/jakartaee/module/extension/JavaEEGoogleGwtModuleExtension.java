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

package consulo.gwt.jakartaee.module.extension;

import com.intellij.gwt.module.model.GwtModule;
import consulo.gwt.base.module.extension.impl.GoogleGwtModuleExtensionImpl;
import consulo.module.content.layer.ModuleRootLayer;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class JavaEEGoogleGwtModuleExtension extends GoogleGwtModuleExtensionImpl<JavaEEGoogleGwtModuleExtension>
{
	protected final Map<String, String> myPackagingPaths = new HashMap<String, String>();

	public JavaEEGoogleGwtModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
	}

	@Override
	@Nonnull
	public String getPackagingRelativePath(@Nonnull GwtModule module)
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
