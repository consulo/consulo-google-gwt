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

import java.util.ResourceBundle;

import org.jetbrains.annotations.PropertyKey;
import com.intellij.CommonBundle;

public class GwtBundle
{
	private static final ResourceBundle ourBundle = ResourceBundle.getBundle("com.intellij.gwt.GwtBundle");

	private GwtBundle()
	{
	}

	public static String message(@PropertyKey(resourceBundle = "com.intellij.gwt.GwtBundle") String key, Object... params)
	{
		return CommonBundle.message(ourBundle, key, params);
	}
}
