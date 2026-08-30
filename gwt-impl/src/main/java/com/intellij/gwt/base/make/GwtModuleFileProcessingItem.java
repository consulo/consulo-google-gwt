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
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * @author nik
 */
public class GwtModuleFileProcessingItem implements FileProcessingCompiler.ProcessingItem {
    private final GwtModule myModule;
    private final Path myFile;
    private final ValidityState myValidityState;
    private final GoogleGwtModuleExtension myFacet;

    public GwtModuleFileProcessingItem(GoogleGwtModuleExtension facet, GwtModule module, Path file) {
        myModule = module;
        myFile = file;
        myFacet = facet;
        myValidityState = new GwtItemValidityState(myFacet.getOutputStyle(), GwtCompilerPaths.getOutputDirectory(facet));
    }

    @Override
    public Path getFile() {
        return myFile;
    }

    @Override
    public @Nullable ValidityState getValidityState() {
        return myValidityState;
    }

    public GwtModule getModule() {
        return myModule;
    }

    public GoogleGwtModuleExtension getFacet() {
        return myFacet;
    }
}
