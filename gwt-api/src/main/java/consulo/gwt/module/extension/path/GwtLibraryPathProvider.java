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

import com.intellij.gwt.sdk.GwtVersion;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 24-May-16
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface GwtLibraryPathProvider
{
	public static class Info
	{
		private final GwtVersion myVersion;
		private final String myUserJarPath;
		private final String myDevJarPath;

		public Info(GwtVersion version, String userJarPath, String devJarPath)
		{
			myVersion = version;
			myUserJarPath = userJarPath;
			myDevJarPath = devJarPath;
		}

		@Nonnull
		public GwtVersion getVersion()
		{
			return myVersion;
		}

		@Nullable
		public String getUserJarPath()
		{
			return myUserJarPath;
		}

		@Nullable
		public VirtualFile getUserJar()
		{
			if(myUserJarPath == null)
			{
				return null;
			}
			VirtualFile file = LocalFileSystem.getInstance().findFileByPath(myUserJarPath);
			if(file == null)
			{
				return null;
			}
			return ArchiveVfsUtil.getArchiveRootForLocalFile(file);
		}

		@Nullable
		public String getDevJarPath()
		{
			return myDevJarPath;
		}

		@Nonnull
		public List<String> getAdditionalClasspath()
		{
			return Collections.emptyList();
		}
	}

	ExtensionPointName<GwtLibraryPathProvider> EP_NAME = ExtensionPointName.create(GwtLibraryPathProvider.class);

	@Nullable
	Info resolveInfo(@Nonnull GoogleGwtModuleExtension<?> extension);

	boolean canChooseBundle(@Nonnull ModuleRootLayer layer);
}
