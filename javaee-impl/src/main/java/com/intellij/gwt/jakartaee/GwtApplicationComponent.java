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

package com.intellij.gwt.jakartaee;

import com.intellij.gwt.base.make.GwtCompilerPaths;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author nik
 */
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
@Singleton
public class GwtApplicationComponent implements Disposable
{
	private final LocalFileSystem myLocalFileSystem;
	private final LocalFileSystem.WatchRequest myWatchRequest;

	@Inject
	public GwtApplicationComponent(VirtualFileManager virtualFileManager)
	{
		myLocalFileSystem = LocalFileSystem.get(virtualFileManager);
		myWatchRequest = myLocalFileSystem.addRootToWatch(GwtCompilerPaths.getOutputRoot().getAbsolutePath(), true);
	}

	@Override
	public void dispose()
	{
		if(myWatchRequest != null)
		{
			myLocalFileSystem.removeWatchedRoot(myWatchRequest);
		}
	}
}
