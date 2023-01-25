/*
 * Copyright 2013-2014 must-be.org
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

package consulo.gwt.module.extension;

import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.module.model.GwtModule;
import consulo.compiler.FileProcessingCompiler;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.virtualFileSystem.util.PathsList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public interface GoogleGwtModuleExtension<T extends GoogleGwtModuleExtension<T>> extends ModuleExtensionWithSdk<T>
{
	@Nonnull
	String getPackagingRelativePath(@Nonnull GwtModule module);

	@Nonnull
	GwtJavaScriptOutputStyle getOutputStyle();

	boolean isRunGwtCompilerOnMake();

	@Nonnull
	String getAdditionalCompilerParameters();

	@Nonnull
	String getAdditionalVmCompilerParameters();

	int getCompilerMaxHeapSize();

	@Nullable
	String getCompilerOutputUrl();

	void setupCompilerClasspath(PathsList pathsList);

	void addFilesForCompilation(GwtModule gwtModule, List<FileProcessingCompiler.ProcessingItem> result);
}
