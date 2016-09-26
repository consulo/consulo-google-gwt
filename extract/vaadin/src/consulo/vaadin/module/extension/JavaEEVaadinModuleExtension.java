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

package consulo.vaadin.module.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.gwt.module.extension.impl.GoogleGwtModuleExtensionImpl;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ModuleRootLayer;
import consulo.vaadin.bundle.VaadinBundleType;

/**
 * @author VISTALL
 * @since 25-May-16
 */
public class JavaEEVaadinModuleExtension extends GoogleGwtModuleExtensionImpl<JavaEEVaadinModuleExtension>
{
	protected final Map<String, String> myPackagingPaths = new HashMap<String, String>();

	public JavaEEVaadinModuleExtension(@NotNull String id, @NotNull ModuleRootLayer rootModel)
	{
		super(id, rootModel);
		myCompilerMaxHeapSize = 1024;
	}

	@Override
	public void addFilesForCompilation(GwtModule gwtModule, List<FileProcessingCompiler.ProcessingItem> result)
	{
		ContentFolder[] contentFolders = getModuleRootLayer().getContentFolders(ContentFolderScopes.productionAndTest());
		for(ContentFolder contentFolder : contentFolders)
		{
			VirtualFile file = contentFolder.getFile();
			if(file == null)
			{
				continue;
			}

			addFilesRecursively(gwtModule, this, file, result);
		}
	}

	@NotNull
	@Override
	public Class<? extends SdkType> getSdkTypeClass()
	{
		return VaadinBundleType.class;
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
