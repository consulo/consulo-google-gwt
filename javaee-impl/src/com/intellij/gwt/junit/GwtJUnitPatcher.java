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

package com.intellij.gwt.junit;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import com.intellij.execution.JavaTestPatcher;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import consulo.gwt.module.extension.impl.GoogleGwtModuleExtensionImpl;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.roots.ContentFolderScopes;

/**
 * @author nik
 */
public class GwtJUnitPatcher implements JavaTestPatcher
{
	@NonNls
	private static final String GWT_ARGS_PROPERTY = "gwt.args";

	public void patchJavaParameters(@Nullable Module module, @Nonnull OwnJavaParameters javaParameters)
	{
		if(module == null)
		{
			return;
		}

		final GoogleGwtModuleExtensionImpl extension = ModuleUtilCore.getExtension(module, GoogleGwtModuleExtensionImpl.class);
		if(extension == null)
		{
			return;
		}

		GwtLibraryPathProvider.Info info = GwtLibraryPathProvider.EP_NAME.composite().resolveInfo(extension);
		assert info != null;
		String devJarPath = info.getDevJarPath();
		if(devJarPath == null)
		{
			return;
		}

		if(GwtModulesManager.getInstance(module.getProject()).getGwtModules(module).length > 0)
		{
			final PathsList classPath = javaParameters.getClassPath();
			for(VirtualFile file : ModuleRootManager.getInstance(module).getContentFolderFiles(ContentFolderScopes.productionAndTest()))
			{
				classPath.addFirst(FileUtil.toSystemDependentName(file.getPath()));
			}
			classPath.addFirst(devJarPath);
		}

		String testGenPath = GwtCompilerPaths.getTestGenDirectory(module).getAbsolutePath();
		String testOutputPath = GwtCompilerPaths.getTestOutputDirectory(module).getAbsolutePath();
		if(!SystemInfo.isWindows || !testGenPath.contains(" ") && !testOutputPath.contains(" "))
		{
			//todo[nik] fix problem with paths containing spaces
			ParametersList vmParameters = javaParameters.getVMParametersList();
			@NonNls StringBuilder builder = new StringBuilder();
			String gwtArgs = vmParameters.getPropertyValue(GWT_ARGS_PROPERTY);
			if(gwtArgs != null)
			{
				builder.append(StringUtil.unquoteString(gwtArgs)).append(' ');
			}
			builder.append("-gen ").append(GeneralCommandLine.inescapableQuote(testGenPath)).append(' ');
			builder.append("-out ").append(GeneralCommandLine.inescapableQuote(testOutputPath));
			@NonNls String prefix = "-D" + GWT_ARGS_PROPERTY + "=";
			vmParameters.replaceOrAppend(prefix, prefix + builder);
		}
	}
}
