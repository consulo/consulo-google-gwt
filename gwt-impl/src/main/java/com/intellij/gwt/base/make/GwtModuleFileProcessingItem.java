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

package com.intellij.gwt.base.make;

import com.intellij.gwt.module.model.GwtModule;
import consulo.compiler.FileProcessingCompiler;
import consulo.compiler.ValidityState;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;

/**
 * @author nik
 */
public class GwtModuleFileProcessingItem implements FileProcessingCompiler.ProcessingItem
{
	private GwtModule myModule;
	private File myFile;
	private ValidityState myValidityState;
	private GoogleGwtModuleExtension myFacet;

	public GwtModuleFileProcessingItem(final GoogleGwtModuleExtension facet, final GwtModule module, File file)
	{
		myModule = module;
		myFile = file;
		myFacet = facet;
		myValidityState = new GwtItemValidityState(myFacet.getOutputStyle(), GwtCompilerPaths.getOutputDirectory(facet));
	}

	@Override
	@Nonnull
	public File getFile()
	{
		return myFile;
	}

	@Override
	@Nullable
	public ValidityState getValidityState()
	{
		return myValidityState;
	}

	public GwtModule getModule()
	{
		return myModule;
	}

	public GoogleGwtModuleExtension getFacet()
	{
		return myFacet;
	}
}
