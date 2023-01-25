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

package com.intellij.gwt.base.make;

import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import consulo.compiler.ValidityState;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

/**
 * @author nik
 */
public class GwtItemValidityState implements ValidityState
{
	private static final int OUTPUT_STYILE_ID_SHIFT = 3;
	private GwtJavaScriptOutputStyle myOutputStyle;
	private String myOutputDirectoryPath;

	public GwtItemValidityState(final GwtJavaScriptOutputStyle outputStyle, final File outputDirectory)
	{
		myOutputStyle = outputStyle;
		myOutputDirectoryPath = outputDirectory.getAbsolutePath();
	}

	public GwtItemValidityState(DataInput is) throws IOException
	{
		byte first = is.readByte();
		if(first <= OUTPUT_STYILE_ID_SHIFT)
		{
			//todo[nik] remove later. This code is needed to handle old cache format (before build 8827)
			myOutputStyle = GwtJavaScriptOutputStyle.byId(first);
			myOutputDirectoryPath = "";
		}
		else
		{
			myOutputStyle = GwtJavaScriptOutputStyle.byId(first - OUTPUT_STYILE_ID_SHIFT);
			myOutputDirectoryPath = is.readUTF();
		}
	}

	@Override
	public boolean equalsTo(ValidityState otherState)
	{
		if(!(otherState instanceof GwtItemValidityState))
		{
			return false;
		}
		GwtItemValidityState state = (GwtItemValidityState) otherState;
		return state.myOutputStyle == myOutputStyle && state.myOutputDirectoryPath.equals(myOutputDirectoryPath);
	}

	@Override
	public void save(DataOutput out) throws IOException
	{
		out.writeByte(myOutputStyle.getNumericId() + OUTPUT_STYILE_ID_SHIFT);
		out.writeUTF(myOutputDirectoryPath);
	}
}
