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

package com.intellij.gwt.facet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.facet.*;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author nik
 */
public class GwtFacet extends Facet<GwtFacetConfiguration>
{
	private LocalFileSystem.WatchRequest myCompilerOutputWatchRequest;

	public GwtFacet(@NotNull final FacetType facetType, @NotNull final Module module, final String name,
			@NotNull final GwtFacetConfiguration configuration)
	{
		super(facetType, module, name, configuration, null);
	}

	@Nullable
	public static GwtFacet getInstance(@NotNull Module module)
	{
		return FacetManager.getInstance(module).getFacetByType(GwtFacetType.ID);
	}

	@Nullable
	public static GwtFacet findFacetBySourceFile(@NotNull Project project, @Nullable VirtualFile file)
	{
		if(file == null)
		{
			return null;
		}

		final Module module = ModuleUtil.findModuleForFile(file, project);
		if(module == null)
		{
			return null;
		}

		return getInstance(module);
	}

	public static boolean isInModuleWithGwtFacet(final @NotNull Project project, final @Nullable VirtualFile file)
	{
		return findFacetBySourceFile(project, file) != null;
	}

	@Nullable
	public WebFacet getWebFacet()
	{
		final String webFacetName = getConfiguration().getWebFacetName();
		return webFacetName != null ? FacetManager.getInstance(getModule()).findFacet(WebFacet.ID, webFacetName) : null;
	}

	@NotNull
	public GwtVersion getSdkVersion()
	{
		return getConfiguration().getSdk().getVersion();
	}

	@Override
	public void initFacet()
	{
		GwtFacetConfiguration configuration = getConfiguration();
		GwtSdkManager.getInstance().registerGwtSdk(configuration.getGwtSdkUrl());
		String path = configuration.getCompilerOutputPath();
		if(!StringUtil.isEmpty(path))
		{
			myCompilerOutputWatchRequest = LocalFileSystem.getInstance().addRootToWatch(path, true);
		}
	}

	@Override
	public void disposeFacet()
	{
		if(myCompilerOutputWatchRequest != null)
		{
			LocalFileSystem.getInstance().removeWatchedRoot(myCompilerOutputWatchRequest);
		}
	}

	public void updateCompilerOutputWatchRequest()
	{
		String path = getConfiguration().getCompilerOutputPath();
		if(myCompilerOutputWatchRequest != null && !myCompilerOutputWatchRequest.getRootPath().equals(path))
		{
			LocalFileSystem.getInstance().removeWatchedRoot(myCompilerOutputWatchRequest);
			myCompilerOutputWatchRequest = null;
		}
		if(myCompilerOutputWatchRequest == null && !StringUtil.isEmpty(path))
		{
			myCompilerOutputWatchRequest = LocalFileSystem.getInstance().addRootToWatch(path, true);
		}
	}

	public static GwtFacet createNewFacet(final @NotNull Module module)
	{
		FacetManager facetManager = FacetManager.getInstance(module);
		final ModifiableFacetModel model = facetManager.createModifiableModel();
		GwtFacet facet = model.getFacetByType(GwtFacetType.ID);
		if(facet != null)
		{
			return facet;
		}

		GwtFacetType type = GwtFacetType.INSTANCE;
		GwtFacetConfiguration configuration = ProjectFacetManager.getInstance(module.getProject()).createDefaultConfiguration(type);
		facet = facetManager.createFacet(type, type.getDefaultFacetName(), configuration, null);
		model.addFacet(facet);

		final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
		setupGwtSdkAndLibraries(configuration, rootModel, null);

		new WriteAction()
		{
			@Override
			protected void run(final Result result)
			{
				model.commit();
				rootModel.commit();
			}
		}.execute();
		return facet;
	}

	public static void setupGwtSdkAndLibraries(final GwtFacetConfiguration configuration, ModifiableRootModel rootModel, @Nullable GwtSdk gwtSdk)
	{
		if(gwtSdk == null || !gwtSdk.isValid())
		{
			gwtSdk = GwtSdkManager.getInstance().suggestGwtSdk();
		}

		if(gwtSdk != null)
		{
			configuration.setGwtSdkUrl(gwtSdk.getHomeDirectoryUrl());
			GwtSdkManager.getInstance().moveToTop(gwtSdk);
			VirtualFile userJar = gwtSdk.getUserJar();
			if(userJar != null)
			{
				final Project project = rootModel.getModule().getProject();
				Library library = GwtSdkUtil.findOrCreateGwtUserLibrary(LibrariesContainerFactory.createContainer(project), userJar);
				rootModel.addLibraryEntry(library);
			}

		}
	}

	@NotNull
	public static GwtVersion getGwtVersion(final @Nullable GwtFacet gwtFacet)
	{
		return gwtFacet != null ? gwtFacet.getSdkVersion() : GwtVersionImpl.VERSION_1_5;
	}
}
