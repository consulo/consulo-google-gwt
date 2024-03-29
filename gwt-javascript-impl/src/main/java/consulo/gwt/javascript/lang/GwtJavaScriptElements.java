/*
 * Copyright 2013-2015 must-be.org
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

package consulo.gwt.javascript.lang;

import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import consulo.javascript.language.JavaScriptLanguage;
import consulo.language.ast.ElementTypeAsPsiFactory;
import consulo.language.ast.IElementType;

/**
 * @author VISTALL
 * @since 19.03.2015
 */
public interface GwtJavaScriptElements
{
	IElementType GWT_REFERENCE_EXPRESSION = new ElementTypeAsPsiFactory("GWT_REFERENCE_EXPRESSION", JavaScriptLanguage.INSTANCE, JSGwtReferenceExpressionImpl::new);
}
