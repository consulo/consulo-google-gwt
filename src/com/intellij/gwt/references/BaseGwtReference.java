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

import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlFile;

/**
 * @author nik
 */
public abstract class BaseGwtReference extends PsiReferenceBase<PsiLiteralExpression>
{
	protected final GwtModulesManager myGwtModulesManager;

	public BaseGwtReference(PsiLiteralExpression element)
	{
		super(element);
		myGwtModulesManager = GwtModulesManager.getInstance(myElement.getProject());
	}

	@Nullable
	public XmlFile getHtmlFileForModule()
	{
		final GwtModule module = findGwtModule();
		if(module == null)
		{
			return null;
		}

		return myGwtModulesManager.findHtmlFileByModule(module);
	}

	@Nullable
	public GwtModule findGwtModule()
	{
		final PsiFile psiFile = myElement.getContainingFile();
		if(psiFile == null)
		{
			return null;
		}

		VirtualFile virtualFile = psiFile.getVirtualFile();
		if(virtualFile == null)
		{
			final PsiFile originalFile = psiFile.getOriginalFile();
			if(originalFile != null)
			{
				virtualFile = originalFile.getVirtualFile();
			}
			if(virtualFile == null)
			{
				return null;
			}
		}

		return myGwtModulesManager.findGwtModuleByClientSourceFile(virtualFile);

	}
}
