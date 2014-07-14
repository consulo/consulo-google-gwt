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

package com.intellij.gwt;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.vfs.LocalFileSystem;

/**
 * @author nik
 */
public class GwtApplicationComponent implements ApplicationComponent
{
	private LocalFileSystem.WatchRequest myWatchRequest;

	@Override
	@NonNls
	@NotNull
	public String getComponentName()
	{
		return "GwtApplicationComponent";
	}

	@Override
	public void initComponent()
	{
		myWatchRequest = LocalFileSystem.getInstance().addRootToWatch(GwtCompilerPaths.getOutputRoot().getAbsolutePath(), true);
	}

	@Override
	public void disposeComponent()
	{
		if(myWatchRequest != null)
		{
			LocalFileSystem.getInstance().removeWatchedRoot(myWatchRequest);
		}
	}
}
