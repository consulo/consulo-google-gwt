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

package com.intellij.gwt;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.gwt.inspections.*;

/**
 * @author VISTALL
 * @since 14.07.14
 */
public class GwtInspectionToolProvider implements InspectionToolProvider
{
	@Override
	public Class[] getInspectionClasses()
	{
		return new Class[]{
				GwtInconsistentAsyncInterfaceInspection.class,
				GwtToCssClassReferencesInspection.class,
				GwtNonSerializableRemoteServiceMethodParametersInspection.class,
				GwtToHtmlTagReferencesInspection.class,
				NonJREEmulationClassesInClientCodeInspection.class,
				GwtServiceNotRegisteredInspection.class,
				GwtInconsistentLocalizableInterfaceInspection.class,
				GwtInconsistentSerializableClassInspection.class,
				GwtMethodWithParametersInConstantsInterfaceInspection.class,
				GwtJavaScriptReferencesInspection.class,
				GwtObsoleteTypeArgsJavadocTagInspection.class,
				GwtRawAsyncCallbackInspection.class,
				GwtDeprecatedPropertyKeyJavadocTagInspection.class
		};
	}
}
