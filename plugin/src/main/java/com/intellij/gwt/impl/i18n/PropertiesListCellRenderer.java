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

package com.intellij.gwt.impl.i18n;

import com.intellij.lang.properties.psi.Property;
import consulo.language.editor.ui.PsiElementListCellRenderer;

/**
 * @author nik
 */
public class PropertiesListCellRenderer extends PsiElementListCellRenderer<Property>
{
	@Override
	public String getElementText(final Property element)
	{
		return element.getContainingFile().getName();
	}

	@Override
	protected String getContainerText(final Property element, final String name)
	{
		return null;
	}

	@Override
	protected int getIconFlags()
	{
		return 0;
	}
}
