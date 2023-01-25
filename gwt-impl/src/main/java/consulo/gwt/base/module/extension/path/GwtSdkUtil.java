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

package consulo.gwt.base.module.extension.path;

import com.intellij.gwt.base.i18n.GwtI18nUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.base.sdk.GwtVersionImpl;
import com.intellij.java.impl.openapi.roots.libraries.LibrariesHelper;
import com.intellij.java.language.impl.JarArchiveFileType;
import com.intellij.java.language.impl.JavaClassFileType;
import com.intellij.java.language.impl.JavaFileType;
import consulo.application.util.SystemInfo;
import consulo.content.bundle.Sdk;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Serializable;

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


		VirtualFile devJar = JarArchiveFileType.INSTANCE.getFileSystem().findFileByPath(FileUtil.toSystemIndependentName(getDevJarPath(sdk)) + ArchiveFileSystem.ARCHIVE_SEPARATOR);
		if(devJar != null)
		{
			String[] files = {devJar.getUrl()};
			if(LibrariesHelper.getInstance().isClassAvailable(files, GwtVersionImpl.GWT_16_COMPILER_MAIN_CLASS))
			{
				return GwtVersionImpl.VERSION_1_6_OR_LATER;
			}
		}

		VirtualFile userJar = getUserJar(sdk);
		if(userJar != null)
		{
			String[] files = {userJar.getUrl()};
			if(!LibrariesHelper.getInstance().isClassAvailable(files, GwtI18nUtil.CONSTANTS_INTERFACE_NAME))
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

	static String getUserJarPath(Sdk sdk)
	{
		return getUserJarPath(sdk.getHomePath());
	}

	@Nullable
	static VirtualFile getUserJar(@Nullable Sdk sdk)
	{
		if(sdk == null)
		{
			return null;
		}
		String jarPath = getUserJarPath(sdk);
		return JarArchiveFileType.INSTANCE.getFileSystem().findFileByPath(FileUtil.toSystemIndependentName(jarPath) + ArchiveFileSystem.ARCHIVE_SEPARATOR);
	}

	public static String getUserJarPath(String base)
	{
		return base + File.separator + GWT_USER_JAR;
	}

	static String getDevJarPath(Sdk sdk)
	{
		return getDevJarPath(sdk.getHomePath());
	}

	public static String getDevJarPath(String base)
	{
		return base + File.separator + getDevJarName();
	}

	static String getDevJarName()
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
