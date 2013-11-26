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

package com.intellij.gwt.sdk;

import java.io.File;

import javax.swing.JComponent;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.facet.ui.FacetConfigurationQuickFix;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.gwt.GwtBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author nik
 */
public class GwtSdkUtil
{
	@NonNls
	private static final String GWT_USER_JAR = "gwt-user.jar";
	@NonNls
	private static final String GWT_DEV_WINDOWS_JAR = "gwt-dev-windows.jar";
	@NonNls
	private static final String GWT_DEV_LINUX_JAR = "gwt-dev-linux.jar";
	@NonNls
	private static final String GWT_DEV_MAC_JAR = "gwt-dev-mac.jar";
	@NonNls
	public static final String GWT_CLASS_NAME = "com.google.gwt.core.client.GWT";
	private static final FacetConfigurationQuickFix DOWNLOAD_GWT_FIX = new FacetConfigurationQuickFix(GwtBundle.message("fix.download.gwt"))
	{
		public void run(final JComponent place)
		{
			BrowserUtil.launchBrowser("http://code.google.com/webtoolkit/download.html");
		}
	};

	private GwtSdkUtil()
	{
	}

	public static String getUserJarPath(String base)
	{
		return base + File.separator + GWT_USER_JAR;
	}

	public static String getDevJarPath(String base)
	{
		return base + File.separator + getDevJarName();
	}

	public static String getDevJarName()
	{
		final String jarName;
		if(SystemInfo.isWindows)
		{
			jarName = GWT_DEV_WINDOWS_JAR;
		}
		else if(SystemInfo.isMac)
		{
			jarName = GWT_DEV_MAC_JAR;
		}
		else
		{
			jarName = GWT_DEV_LINUX_JAR;
		}
		return jarName;
	}

	private static ValidationResult checkClass(final @NonNls String className, String gwtPath, final String jarPath)
	{
		final VirtualFile jarFile = JarFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(jarPath) + JarFileSystem
				.JAR_SEPARATOR);
		if(jarFile == null)
		{
			return invalidGwtInstallation(gwtPath, GwtBundle.message("error.file.not.found.message", jarPath));
		}
		if(!LibraryUtil.isClassAvailableInLibrary(new VirtualFile[]{jarFile}, className))
		{
			return invalidGwtInstallation(gwtPath, GwtBundle.message("error.class.not.found.in.jar", className, jarFile));
		}
		return ValidationResult.OK;
	}

	private static ValidationResult invalidGwtInstallation(final String gwtPath, final String errorMessage)
	{
		return new ValidationResult(GwtBundle.message("error.invalid.gwt.installation.message", gwtPath, errorMessage), DOWNLOAD_GWT_FIX);
	}

	public static ValidationResult checkGwtSdkPath(final String gwtPath)
	{
		if(gwtPath.contains("!"))
		{
			return new ValidationResult(GwtBundle.message("error.message.path.to.gwt.sdk.must.not.contain.character"));
		}

		ValidationResult result = checkClass("com.google.gwt.dev.GWTCompiler", gwtPath, getDevJarPath(gwtPath));
		if(result.isOk())
		{
			result = checkClass("com.google.gwt.dev.GWTShell", gwtPath, getDevJarPath(gwtPath));
		}
		if(result.isOk())
		{
			result = checkClass(GWT_CLASS_NAME, gwtPath, getUserJarPath(gwtPath));
		}
		return result;
	}

	public static Library findOrCreateGwtUserLibrary(final @NotNull LibrariesContainer container, final @NotNull VirtualFile userJar)
	{
		final Library library = findLibrary(container, userJar);
		if(library != null)
		{
			return library;
		}

		return container.createLibrary("gwt-user", LibrariesContainer.LibraryLevel.PROJECT, new VirtualFile[]{userJar}, new VirtualFile[]{userJar});
	}

	@Nullable
	private static Library findLibrary(final LibrariesContainer container, final VirtualFile userJar)
	{
		for(Library library : container.getAllLibraries())
		{
			final VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
			for(VirtualFile file : files)
			{
				if(userJar.equals(file))
				{
					return library;
				}
			}
		}
		return null;
	}
}
