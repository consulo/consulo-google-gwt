/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.gwt.sdk;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.components.ServiceManager;

/**
 * @author nik
 */
public abstract class GwtSdkManager
{

	public static GwtSdkManager getInstance()
	{
		return ServiceManager.getService(GwtSdkManager.class);
	}

	@NotNull
	public abstract GwtSdk getGwtSdk(@NotNull String sdkHomeUrl);

	public abstract void registerGwtSdk(final String gwtSdkUrl);

	public abstract void moveToTop(@NotNull GwtSdk sdk);

	@Nullable
	public abstract GwtSdk suggestGwtSdk();

	public abstract List<String> getAllSdkPaths();
}
