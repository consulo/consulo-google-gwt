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

package com.intellij.gwt.make;

import com.intellij.openapi.compiler.FileProcessingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.facet.GwtFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class GwtModuleFileProcessingItem implements FileProcessingCompiler.ProcessingItem {
  private GwtModule myModule;
  private VirtualFile myFile;
  private ValidityState myValidityState;
  private GwtFacet myFacet;

  public GwtModuleFileProcessingItem(final GwtFacet facet, final GwtModule module, VirtualFile file) {
    myModule = module;
    myFile = file;
    myFacet = facet;
    myValidityState = new GwtItemValidityState(myFacet.getConfiguration().getOutputStyle(), GwtCompilerPaths.getOutputDirectory(facet));
  }

  @NotNull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nullable
  public ValidityState getValidityState() {
    return myValidityState;
  }

  public GwtModule getModule() {
    return myModule;
  }


  public GwtFacet getFacet() {
    return myFacet;
  }
}
