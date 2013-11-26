/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package com.intellij.gwt.sdk.impl;

import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class GwtSdkImpl implements GwtSdk {
  @NonNls private static final String EMUL_ROOT = "com/google/gwt/emul/";
  private final String myHomeDirectoryUrl;
  private GwtVersion myVersion;
  private Map<String, Boolean> myCachedJreEmulationClasses = new HashMap<String, Boolean>();

  public GwtSdkImpl(final String homeDirectoryUrl) {
    myHomeDirectoryUrl = homeDirectoryUrl;
  }

  public String getHomeDirectoryUrl() {
    return myHomeDirectoryUrl;
  }

  @NotNull
  public GwtVersion getVersion() {
    if (myVersion == null) {
      myVersion = detectGwtVersion();
    }
    return myVersion;
  }

  public String getDevJarPath() {
    return GwtSdkUtil.getDevJarPath(getHomeDirectoryPath());
  }

  public boolean containsJreEmulationClass(final String className) {
    Boolean contains = myCachedJreEmulationClasses.get(className);
    if (contains == null) {
      VirtualFile userJar = getUserJar();
      contains = userJar != null && userJar.findFileByRelativePath(getJreEmulationClassPath(className)) != null;
      myCachedJreEmulationClasses.put(className, contains);
    }
    return contains.booleanValue();
  }

  public boolean isValid() {
    return getUserJar() != null;
  }

  private String getHomeDirectoryPath() {
    return FileUtil.toSystemDependentName(VfsUtil.urlToPath(myHomeDirectoryUrl));
  }

  @Nullable
  public VirtualFile getUserJar() {
    String jarPath = GwtSdkUtil.getUserJarPath(getHomeDirectoryPath());
    return JarFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(jarPath) + JarFileSystem.JAR_SEPARATOR);
  }

  @NotNull
  private GwtVersion detectGwtVersion() {
    VirtualFile devJar = JarFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(getDevJarPath()) + JarFileSystem.JAR_SEPARATOR);
    if (devJar != null) {
      VirtualFile[] files = {devJar};
      if (LibraryUtil.isClassAvailableInLibrary(files, GwtVersionImpl.GWT_16_COMPILER_MAIN_CLASS)) {
        return GwtVersionImpl.VERSION_1_6_OR_LATER;
      }
    }

    VirtualFile userJar = getUserJar();
    if (userJar != null) {
      VirtualFile[] files = {userJar};
      if (!LibraryUtil.isClassAvailableInLibrary(files, GwtI18nUtil.CONSTANTS_INTERFACE_NAME)) {
        return GwtVersionImpl.VERSION_1_0;
      }
      if (userJar.findFileByRelativePath(getJreEmulationClassPath(Iterable.class.getName())) != null) {
        return GwtVersionImpl.VERSION_1_5;
      }
      if (userJar.findFileByRelativePath(getJreEmulationClassPath(Serializable.class.getName())) != null) {
        return GwtVersionImpl.VERSION_1_4;
      }
    }

    return GwtVersionImpl.VERSION_FROM_1_1_TO_1_3;
  }

  private static String getJreEmulationClassPath(String className) {
    return EMUL_ROOT + className.replace('.', '/') + "." + StdFileTypes.JAVA.getDefaultExtension();
  }
}
