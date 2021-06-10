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

package com.intellij.gwt.module.index;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author nik
 */
class GwtHtmlFileInfo
{
	private long myTimestamp;
	private Set<String> myGwtModules = null;

	public GwtHtmlFileInfo(final long timestamp)
	{
		myTimestamp = timestamp;
	}

	public GwtHtmlFileInfo(DataInputStream input) throws IOException
	{
		myTimestamp = input.readLong();
		int size = input.readInt();
		while(size-- > 0)
		{
			addGwtModule(input.readUTF());
		}
	}

	public void addGwtModule(final String moduleName)
	{
		if(myGwtModules == null)
		{
			myGwtModules = new HashSet<String>();
		}
		myGwtModules.add(moduleName);
	}

	public void write(DataOutputStream output) throws IOException
	{
		output.writeLong(myTimestamp);
		if(myGwtModules == null)
		{
			output.writeInt(0);
		}
		else
		{
			output.writeInt(myGwtModules.size());
			for(String gwtModule : myGwtModules)
			{
				output.writeUTF(gwtModule);
			}
		}
	}


	public
	@Nullable
	Set<String> getGwtModules()
	{
		return myGwtModules;
	}

	public long getTimestamp()
	{
		return myTimestamp;
	}
}
