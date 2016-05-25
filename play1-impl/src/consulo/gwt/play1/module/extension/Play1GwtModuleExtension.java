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

package consulo.gwt.play1.module.extension;

import org.jetbrains.annotations.NotNull;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.module.extension.impl.GoogleGwtModuleExtensionImpl;
import com.intellij.openapi.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class Play1GwtModuleExtension extends GoogleGwtModuleExtensionImpl<Play1GwtModuleExtension> implements GoogleGwtModuleExtension<Play1GwtModuleExtension>
{
	public Play1GwtModuleExtension(@NotNull String id, @NotNull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
	}
}