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

import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.util.PathsList;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public interface GoogleGwtModuleExtension<T extends GoogleGwtModuleExtension<T>> extends ModuleExtensionWithSdk<T>
{
	@NotNull
	String getPackagingRelativePath(@NotNull GwtModule module);

	@NotNull
	GwtJavaScriptOutputStyle getOutputStyle();

	boolean isRunGwtCompilerOnMake();

	String getAdditionalCompilerParameters();

	int getCompilerMaxHeapSize();

	String getCompilerOutputPath();

	@NotNull
	GwtVersion getSdkVersion();

	void setupCompilerClasspath(PathsList pathsList);
}
