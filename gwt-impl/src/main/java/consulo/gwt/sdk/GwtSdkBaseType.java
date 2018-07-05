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

package consulo.gwt.sdk;

import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.types.SourcesOrderRootType;
import consulo.vfs.util.ArchiveVfsUtil;

/**
 * @author VISTALL
 * @since 25-May-16
 */
public abstract class GwtSdkBaseType extends SdkType
{
	protected GwtSdkBaseType(@NonNls String name)
	{
		super(name);
	}

	@Override
	public void setupSdkPaths(Sdk sdk)
	{
		SdkModificator sdkModificator = sdk.getSdkModificator();

		VirtualFile homeDirectory = sdk.getHomeDirectory();
		if(homeDirectory == null)
		{
			sdkModificator.commitChanges();
			return;
		}

		for(VirtualFile virtualFile : homeDirectory.getChildren())
		{
			String name = virtualFile.getName();

			// skip compiler library
			if(StringUtil.startsWith(name, "vaadin-client-compiler"))
			{
				continue;
			}

			if(virtualFile.getFileType() == JarArchiveFileType.INSTANCE)
			{
				VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
				if(archiveRootForLocalFile == null)
				{
					continue;
				}

				sdkModificator.addRoot(archiveRootForLocalFile, BinariesOrderRootType.getInstance());

				if(StringUtil.startsWith(name, "vaadin-client"))
				{
					sdkModificator.addRoot(archiveRootForLocalFile, SourcesOrderRootType.getInstance());
				}
			}
		}

		VirtualFile libFile = homeDirectory.findChild("lib");
		if(libFile != null)
		{
			for(VirtualFile virtualFile : libFile.getChildren())
			{
				if(virtualFile.getFileType() == JarArchiveFileType.INSTANCE)
				{
					VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
					if(archiveRootForLocalFile == null)
					{
						continue;
					}

					sdkModificator.addRoot(archiveRootForLocalFile, BinariesOrderRootType.getInstance());
				}
			}
		}

		sdkModificator.commitChanges();
	}

	@Override
	public boolean isRootTypeApplicable(OrderRootType type)
	{
		return JavaSdk.getInstance().isRootTypeApplicable(type);
	}

	@Nonnull
	public abstract GwtVersion getVersion(Sdk sdk);

	@Nullable
	public abstract String getDevJarPath(Sdk sdk);

	@Nullable
	public abstract String getUserJarPath(Sdk sdk);
}
