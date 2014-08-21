/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.gwt.facet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author nik
 */
@Deprecated
public class GwtFacet
{
	//private LocalFileSystem.WatchRequest myCompilerOutputWatchRequest;

	@Nullable
	public static GoogleGwtModuleExtension<?> findFacetBySourceFile(@NotNull Project project, @Nullable VirtualFile file)
	{
		if(file == null)
		{
			return null;
		}

		final Module module = ModuleUtil.findModuleForFile(file, project);
		if(module == null)
		{
			return null;
		}

		return ModuleUtilCore.getExtension(module, GoogleGwtModuleExtension.class);
	}

	public static boolean isInModuleWithGwtFacet(final @NotNull Project project, final @Nullable VirtualFile file)
	{
		return findFacetBySourceFile(project, file) != null;
	}

	/*
	@Override
	public void initFacet()
	{
		GwtFacetConfiguration configuration = getConfiguration();
		GwtSdkManager.getInstance().registerGwtSdk(configuration.getGwtSdkUrl());
		String path = configuration.getCompilerOutputPath();
		if(!StringUtil.isEmpty(path))
		{
			myCompilerOutputWatchRequest = LocalFileSystem.getInstance().addRootToWatch(path, true);
		}
	}

	@Override
	public void disposeFacet()
	{
		if(myCompilerOutputWatchRequest != null)
		{
			LocalFileSystem.getInstance().removeWatchedRoot(myCompilerOutputWatchRequest);
		}
	}

	public void updateCompilerOutputWatchRequest()
	{
		String path = getConfiguration().getCompilerOutputPath();
		if(myCompilerOutputWatchRequest != null && !myCompilerOutputWatchRequest.getRootPath().equals(path))
		{
			LocalFileSystem.getInstance().removeWatchedRoot(myCompilerOutputWatchRequest);
			myCompilerOutputWatchRequest = null;
		}
		if(myCompilerOutputWatchRequest == null && !StringUtil.isEmpty(path))
		{
			myCompilerOutputWatchRequest = LocalFileSystem.getInstance().addRootToWatch(path, true);
		}
	}
	    */
	@NotNull
	public static GwtVersion getGwtVersion(final @Nullable GoogleGwtModuleExtension gwtFacet)
	{
		return gwtFacet != null ? gwtFacet.getSdkVersion() : GwtVersionImpl.VERSION_1_5;
	}
}
