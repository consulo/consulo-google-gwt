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

import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.gwt.module.extension.impl.GoogleGwtModuleExtensionImpl;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;

/**
 * @author VISTALL
 * @since 21.08.14
 */
public interface GoogleGwtMutableModuleExtension<T extends GoogleGwtModuleExtensionImpl<T>> extends GoogleGwtModuleExtension<T>,
		MutableModuleExtensionWithSdk<T>
{
	void setOutputStyle(final GwtJavaScriptOutputStyle outputStyle);

	void setRunGwtCompilerOnMake(final boolean runGwtCompiler);

	void setAdditionalCompilerParameters(final String additionalCompilerParameters);

	void setCompilerMaxHeapSize(final int compilerMaxHeapSize);

	void setCompilerOutputPath(final String compilerOutputPath);
}
