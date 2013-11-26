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

package com.intellij.gwt.facet;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.facet.impl.ui.FacetTypeFrameworkSupportProvider;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportConfigurable;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportProvider;
import com.intellij.ide.util.newProjectWizard.FrameworkSupportModel;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * @author nik
 */
public class GwtFacetFrameworkSupportProvider extends FrameworkSupportProvider {
  public GwtFacetFrameworkSupportProvider() {
    super(FacetTypeFrameworkSupportProvider.getProviderId(GwtFacetType.ID), GwtBundle.message("framework.title.google.web.toolkit"));
  }

  public String[] getPrecedingFrameworkProviderIds() {
    return new String[]{FacetTypeFrameworkSupportProvider.getProviderId(WebFacet.ID)};
  }

  @NotNull
  public FrameworkSupportConfigurable createConfigurable(final @NotNull FrameworkSupportModel model) {
    return new GwtFrameworkSupportConfigurable();
  }

  private static class GwtFrameworkSupportConfigurable extends FrameworkSupportConfigurable {
    private GwtSdkPathEditor mySdkPathEditor;

    private GwtFrameworkSupportConfigurable() {
      mySdkPathEditor = new GwtSdkPathEditor(null);
      Project defaultProject = ProjectManager.getInstance().getDefaultProject();
      String path = ProjectFacetManager.getInstance(defaultProject).createDefaultConfiguration(GwtFacetType.INSTANCE).getGwtSdkPath();
      if (StringUtil.isEmpty(path)) {
        GwtSdk sdk = GwtSdkManager.getInstance().suggestGwtSdk();
        if (sdk != null) {
          path = VfsUtil.urlToPath(sdk.getHomeDirectoryUrl());
        }
      }
      mySdkPathEditor.setPath(path);
    }

    public JComponent getComponent() {
      return mySdkPathEditor.getMainComponent();
    }

    public void addSupport(final Module module, final ModifiableRootModel rootModel, final @Nullable Library library) {
      FacetManager facetManager = FacetManager.getInstance(module);
      ModifiableFacetModel facetModel = facetManager.createModifiableModel();
      GwtFacet facet = facetManager.createFacet(GwtFacetType.INSTANCE, GwtFacetType.INSTANCE.getDefaultFacetName(), null);
      Collection<WebFacet> facets = WebFacet.getInstances(facet.getModule());
      if (!facets.isEmpty()) {
        facet.getConfiguration().setWebFacetName(facets.iterator().next().getName());
      }
      facetModel.addFacet(facet);
      facetModel.commit();
      String sdkUrl = VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(mySdkPathEditor.getPath()));
      GwtSdk gwtSdk = GwtSdkManager.getInstance().getGwtSdk(sdkUrl);
      GwtFacet.setupGwtSdkAndLibraries(facet.getConfiguration(), rootModel, gwtSdk);
    }
  }
}
