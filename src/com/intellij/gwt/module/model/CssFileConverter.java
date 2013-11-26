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

package com.intellij.gwt.module.model;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.css.CssFile;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;

/**
 * @author nik
 */
public class CssFileConverter extends Converter<CssFile>
{
	@Override
	@Nullable
	public CssFile fromString(final @Nullable String path, final ConvertContext context)
	{
		GwtModule module = context.getInvocationElement().<GwtModule>getRoot().getRootElement();
		final List<VirtualFile> publicRoots = module.getPublicRoots();

		if(path == null)
		{
			return null;
		}

		for(VirtualFile root : publicRoots)
		{
			final VirtualFile cssFile = root.findFileByRelativePath(path);
			if(cssFile != null)
			{
				final PsiManager psiManager = context.getPsiManager();
				final PsiFile psiFile = psiManager.findFile(cssFile);
				if(psiFile instanceof CssFile)
				{
					return (CssFile) psiFile;
				}
			}
		}

		return null;
	}

	@Override
	public String toString(final CssFile cssFile, final ConvertContext context)
	{
		throw new UnsupportedOperationException();
	}
}
