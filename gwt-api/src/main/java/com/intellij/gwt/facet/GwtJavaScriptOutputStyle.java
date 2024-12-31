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

import jakarta.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.gwt.GwtBundle;

/**
 * @author nik
 */
public enum GwtJavaScriptOutputStyle
{
	OBFUSCATED("OBF", GwtBundle.message("script.output.style.obfuscated"), 1),
	PRETTY("PRETTY", GwtBundle.message("script.output.style.pretty"), 2),
	DETAILED("DETAILED", GwtBundle.message("script.output.style.detailed"), 3);
	private
	@NonNls
	String myId;
	private int myNumericId;
	private String myPresentableName;

	GwtJavaScriptOutputStyle(final @NonNls String id, final String presentableName, int numericId)
	{
		myPresentableName = presentableName;
		myId = id;
		myNumericId = numericId;
	}

	public String getId()
	{
		return myId;
	}

	public int getNumericId()
	{
		return myNumericId;
	}

	public static
	@Nullable
	GwtJavaScriptOutputStyle byId(@Nullable String id)
	{
		for(GwtJavaScriptOutputStyle style : values())
		{
			if(style.getId().equals(id))
			{
				return style;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return myPresentableName;
	}

	public static
	@Nullable
	GwtJavaScriptOutputStyle byId(final int id)
	{
		for(GwtJavaScriptOutputStyle style : values())
		{
			if(style.getNumericId() == id)
			{
				return style;
			}
		}
		return null;
	}
}
