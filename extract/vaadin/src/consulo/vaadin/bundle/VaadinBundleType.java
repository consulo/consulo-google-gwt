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

package consulo.vaadin.bundle;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.projectRoots.Sdk;
import consulo.gwt.sdk.GwtSdkBaseType;
import consulo.ui.image.Image;
import consulo.vaadin.VaadinIcons;

/**
 * @author VISTALL
 * @since 25-May-16
 */
public class VaadinBundleType extends GwtSdkBaseType
{
	private static Pattern ourClientCompilerPattern = Pattern.compile("vaadin-client-compiler-(\\d.\\d.\\d).jar");
	private static Pattern ourClientPattern = Pattern.compile("vaadin-client-(\\d.\\d.\\d).jar");

	public VaadinBundleType()
	{
		super("VAADIN_BUNDLE");
	}

	@NotNull
	@Override
	public GwtVersion getVersion(Sdk sdk)
	{
		return GwtVersionImpl.VERSION_1_6_OR_LATER;
	}

	@Override
	public boolean isValidSdkHome(String sdkHome)
	{
		return findFileByPattern(sdkHome, ourClientCompilerPattern) != null;
	}

	@Nullable
	@Override
	public String getVersionString(String sdkHome)
	{
		File file = new File(sdkHome);
		for(File child : file.listFiles())
		{
			Matcher matcher = ourClientCompilerPattern.matcher(child.getName());
			if(matcher.matches())
			{
				return matcher.group(1);
			}
		}
		return null;
	}

	@Override
	public String suggestSdkName(String currentSdkName, String sdkHome)
	{
		return getPresentableName() + " " + getVersionString(sdkHome);
	}

	@NotNull
	@Override
	public String getPresentableName()
	{
		return "Vaadin";
	}

	@Nullable
	@Override
	public Image getIcon()
	{
		return VaadinIcons.Vaadin;
	}

	@Nullable
	@Override
	public String getDevJarPath(Sdk sdk)
	{
		return findFileByPattern(sdk.getHomePath(), ourClientCompilerPattern);
	}

	@Nullable
	@Override
	public String getUserJarPath(Sdk sdk)
	{
		return findFileByPattern(sdk.getHomePath(), ourClientPattern);
	}

	@Nullable
	private static String findFileByPattern(String sdkHome, Pattern pattern)
	{
		File file = new File(sdkHome);
		for(File child : file.listFiles())
		{
			if(pattern.matcher(child.getName()).matches())
			{
				return child.getPath();
			}
		}
		return null;
	}
}
