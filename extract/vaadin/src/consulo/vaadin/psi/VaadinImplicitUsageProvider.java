/*
 * Copyright 2013-2017 must-be.org
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

package consulo.vaadin.psi;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

/**
 * @author VISTALL
 * @since 08-Oct-17
 */
public class VaadinImplicitUsageProvider implements ImplicitUsageProvider
{
	public static final String COM_VAADIN_SHARED_UI_CONNECT = "com.vaadin.shared.ui.Connect";

	@Override
	public boolean isImplicitUsage(PsiElement psiElement)
	{
		return psiElement instanceof PsiClass && COM_VAADIN_SHARED_UI_CONNECT.equals(((PsiClass) psiElement).getQualifiedName());
	}

	@Override
	public boolean isImplicitRead(PsiElement psiElement)
	{
		return false;
	}

	@Override
	public boolean isImplicitWrite(PsiElement psiElement)
	{
		return false;
	}
}
