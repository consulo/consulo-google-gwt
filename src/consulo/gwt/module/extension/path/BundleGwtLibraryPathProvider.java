/*
 * Copyright 2013-2016 must-be.org
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

package consulo.gwt.module.extension.path;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.extensions.CompositeExtensionPointName;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootLayer;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;

/**
 * @author VISTALL
 * @since 24-May-16
 */
public class BundleGwtLibraryPathProvider implements GwtLibraryPathProvider
{
	@Nullable
	@Override
	public Info resolveInfo(@NotNull GoogleGwtModuleExtension<?> extension)
	{
		Sdk sdk = extension.getSdk();
		if(sdk == null)
		{
			return new Info(GwtSdkUtil.detectVersion(null), null, null);
		}

		return new Info(GwtSdkUtil.detectVersion(sdk), GwtSdkUtil.getUserJarPath(sdk), GwtSdkUtil.getDevJarPath(sdk));
	}

	@CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
	public boolean canChooseBundle(@NotNull ModuleRootLayer layer)
	{
		return true;
	}
}
