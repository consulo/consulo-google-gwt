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

package com.intellij.gwt.make;

import java.io.File;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author nik
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class GwtCompilerPaths
{
	@NonNls
	private static final String CACHES_DIR_NAME = "caches";

	private GwtCompilerPaths()
	{
	}

	public static File getDirectoryForGenerated(final @NotNull Module module)
	{
		return new File(getCompilerOutputRoot(module), "gen");
	}

	public static File getCompilerOutputRoot(final @NotNull Module module)
	{
		return new File(getOutputRoot(module), "compile");
	}

	public static File getTestGenDirectory(@NotNull Module module)
	{
		return new File(getTestOutputRoot(module), "gen");
	}

	public static File getTestOutputDirectory(@NotNull Module module)
	{
		return new File(getTestOutputRoot(module), "www");
	}

	private static File getTestOutputRoot(@NotNull Module module)
	{
		return new File(getOutputRoot(module), "test");
	}

	public static File getOutputRoot(final @NotNull Module module)
	{
		final Project project = module.getProject();
		final String cacheDirName = project.getName() + "." + project.getLocationHash();
		return new File(new File(getOutputRoot(), cacheDirName), getOutputDirectoryName(module));
	}

	public static File getOutputRoot()
	{
		return new File(PathManager.getSystemPath() + File.separator + "gwt");
	}

	private static String getOutputDirectoryName(@NotNull Module module)
	{
		final String moduleName = module.getName();
		final String modulePath = module.getModuleDirPath();
		return moduleName.replace(' ', '_') + "." + Integer.toHexString(modulePath.hashCode());
	}

	public static File getGwtCachesDir()
	{
		return new File(getOutputRoot(), CACHES_DIR_NAME);
	}

	public static void clearOutputDirs()
	{
		final File root = getOutputRoot();
		final File[] dirs = root.listFiles();
		if(dirs != null)
		{
			for(File dir : dirs)
			{
				if(!CACHES_DIR_NAME.equals(dir.getName()))
				{
					FileUtil.delete(dir);
				}
			}
		}
	}

	public static File getOutputDirectory(final GoogleGwtModuleExtension facet)
	{
		String outputPath = facet.getCompilerOutputPath();
		final File outputDir;
		if(StringUtil.isEmpty(outputPath))
		{
			outputDir = new File(getCompilerOutputRoot(facet.getModule()), "www");
		}
		else
		{
			outputDir = new File(FileUtil.toSystemDependentName(outputPath));
		}
		return outputDir;
	}
}
