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

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.extensions.CompositeExtensionPointName;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.roots.ModuleRootLayer;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * @author VISTALL
 * @since 24-May-16
 */
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

		@NotNull
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

		@NotNull
		public List<String> getAdditionalClasspath()
		{
			return Collections.emptyList();
		}
	}

	CompositeExtensionPointName<GwtLibraryPathProvider> EP_NAME = CompositeExtensionPointName.applicationPoint("com.intellij.gwt.libraryPathProvider", GwtLibraryPathProvider.class);

	@Nullable
	Info resolveInfo(@NotNull GoogleGwtModuleExtension<?> extension);

	@CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
	boolean canChooseBundle(@NotNull ModuleRootLayer layer);
}
