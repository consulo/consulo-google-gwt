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
import java.io.Serializable;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.ide.highlighter.JarArchiveFileType;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.ArchiveFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;

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
	@NonNls
	private static final String EMUL_ROOT = "com/google/gwt/emul/";

	private GwtSdkUtil()
	{
	}

	public static GwtVersion detectVersion(Sdk sdk)
	{
		if(sdk == null)
		{
			return GwtVersionImpl.VERSION_1_6_OR_LATER;
		}

		VirtualFile devFile = LocalFileSystem.getInstance().findFileByPath(sdk.getHomePath() + "/gwt-dev.jar");
		if(devFile != null)
		{
			String classPath = GwtVersionImpl.GWT_16_COMPILER_MAIN_CLASS.replace(".", "/") + "." + JavaClassFileType.INSTANCE.getDefaultExtension();
			VirtualFile archiveRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(devFile);
			if(archiveRoot != null)
			{
				VirtualFile compilerClass = archiveRoot.findFileByRelativePath(classPath);
				if(compilerClass != null)
				{
					return GwtVersionImpl.VERSION_1_6_OR_LATER;
				}
			}
		}


		VirtualFile devJar = JarArchiveFileType.INSTANCE.getFileSystem().findFileByPath(FileUtil.toSystemIndependentName(getDevJarPath(sdk)) +
				ArchiveFileSystem.ARCHIVE_SEPARATOR);
		if(devJar != null)
		{
			VirtualFile[] files = {devJar};
			if(LibraryUtil.isClassAvailableInLibrary(files, GwtVersionImpl.GWT_16_COMPILER_MAIN_CLASS))
			{
				return GwtVersionImpl.VERSION_1_6_OR_LATER;
			}
		}

		VirtualFile userJar = getUserJar(sdk);
		if(userJar != null)
		{
			VirtualFile[] files = {userJar};
			if(!LibraryUtil.isClassAvailableInLibrary(files, GwtI18nUtil.CONSTANTS_INTERFACE_NAME))
			{
				return GwtVersionImpl.VERSION_1_0;
			}
			if(userJar.findFileByRelativePath(getJreEmulationClassPath(Iterable.class.getName())) != null)
			{
				return GwtVersionImpl.VERSION_1_5;
			}
			if(userJar.findFileByRelativePath(getJreEmulationClassPath(Serializable.class.getName())) != null)
			{
				return GwtVersionImpl.VERSION_1_4;
			}
		}

		return GwtVersionImpl.VERSION_FROM_1_1_TO_1_3;
	}

	public static String getJreEmulationClassPath(String className)
	{
		return EMUL_ROOT + className.replace('.', '/') + JavaFileType.DOT_DEFAULT_EXTENSION;
	}

	public static String getUserJarPath(Sdk sdk)
	{
		return getUserJarPath(sdk.getHomePath());
	}

	@Nullable
	public static VirtualFile getUserJar(@Nullable Sdk sdk)
	{
		if(sdk == null)
		{
			return null;
		}
		String jarPath = getUserJarPath(sdk);
		return JarArchiveFileType.INSTANCE.getFileSystem().findFileByPath(FileUtil.toSystemIndependentName(jarPath) + ArchiveFileSystem
				.ARCHIVE_SEPARATOR);
	}

	@Nullable
	public static VirtualFile getDevJar(@Nullable Sdk sdk)
	{
		if(sdk == null)
		{
			return null;
		}
		String jarPath = getDevJarPath(sdk);
		return JarArchiveFileType.INSTANCE.getFileSystem().findFileByPath(FileUtil.toSystemIndependentName(jarPath) + ArchiveFileSystem
				.ARCHIVE_SEPARATOR);
	}

	public static String getUserJarPath(String base)
	{
		return base + File.separator + GWT_USER_JAR;
	}

	public static String getDevJarPath(Sdk sdk)
	{
		return getDevJarPath(sdk.getHomePath());
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
}
