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

package com.intellij.gwt.references;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.util.ArrayUtil;

/**
 * @author nik
*/
public class GwtToCssClassReference extends BaseGwtReference {
  public GwtToCssClassReference(final PsiLiteralExpression element) {
    super(element);
  }

  public PsiElement resolve() {
    final Object value = myElement.getValue();
    if (!(value instanceof String)) return null;

    final GwtModule module = findGwtModule();
    if (module == null) {
      return null;
    }
    final String cssClass = (String)value;
    return myGwtModulesManager.findCssDeclarationByClass(module, cssClass);
  }

  public Object[] getVariants() {
    final GwtModule module = findGwtModule();
    if (module == null) {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
    return myGwtModulesManager.getAllCssClassNames(module);
  }
}
