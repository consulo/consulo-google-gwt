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

import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author nik
 */
@State(
  name = "GwtSdkManager",
  storages = {
    @Storage(
      id ="other",
      file = "$APP_CONFIG$/other.xml"
    )}
)
public class GwtSdkManagerImpl extends GwtSdkManager implements PersistentStateComponent<GwtSdkManagerImpl.GwtSdkList> {
  private Map<String, GwtSdkImpl> myGwtSdkMap = new HashMap<String, GwtSdkImpl>();
  private GwtSdkList myGwtSdkList = new GwtSdkList();

  @NotNull
  public GwtSdk getGwtSdk(@NotNull final String sdkHomeUrl) {
    GwtSdkImpl gwtSdk = myGwtSdkMap.get(sdkHomeUrl);
    if (gwtSdk == null) {
      gwtSdk = new GwtSdkImpl(sdkHomeUrl);
      myGwtSdkMap.put(sdkHomeUrl, gwtSdk);
      if (gwtSdk.isValid()) {
        myGwtSdkList.getSdkInstallations().add(StringUtil.trimEnd(sdkHomeUrl, "/"));
      }
    }
    return gwtSdk;
  }

  public void registerGwtSdk(final String gwtSdkUrl) {
    getGwtSdk(gwtSdkUrl);
  }

  public void moveToTop(@NotNull final GwtSdk sdk) {
    String url = sdk.getHomeDirectoryUrl();
    Set<String> list = myGwtSdkList.getSdkInstallations();
    list.remove(url);
    list.add(url);
  }

  @Nullable
  public GwtSdk suggestGwtSdk() {
    GwtSdk last = null;
    for (String url : myGwtSdkList.getSdkInstallations()) {
      GwtSdk sdk = getGwtSdk(url);
      if (sdk.isValid()) {
        last = sdk;
      }
    }
    return last;
  }

  public List<String> getAllSdkPaths() {
    ArrayList<String> paths = new ArrayList<String>();
    for (String url : myGwtSdkList.getSdkInstallations()) {
      paths.add(0, VfsUtil.urlToPath(url));
    }
    return paths;
  }

  public GwtSdkList getState() {
    return myGwtSdkList;
  }

  public void loadState(final GwtSdkList state) {
    myGwtSdkList = state;
  }

  public static class GwtSdkList {
    private Set<String> mySdkInstallations = new LinkedHashSet<String>();

    @Tag("gwt-sdk-list")
    @AbstractCollection(elementTag = "gwt-sdk", elementValueAttribute = "url", surroundWithTag = false)
    public Set<String> getSdkInstallations() {
      return mySdkInstallations;
    }

    public void setSdkInstallations(final Set<String> sdkInstallations) {
      mySdkInstallations = sdkInstallations;
    }
  }
}
