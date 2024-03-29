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

package com.intellij.gwt.impl.module.model.impl;

import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtInheritsEntry;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtRelativePath;
import com.intellij.gwt.module.model.GwtStylesheetRef;
import com.intellij.java.language.psi.JavaDirectoryService;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class GwtModuleImpl implements GwtModule
{
	private static final Logger LOG = Logger.getInstance(GwtModuleImpl.class);
	private String myCachedFileUrl;
	private VirtualFile myModuleFile;
	private String myName;
	private String myShortName;
	private VirtualFile myModuleDirectory;
	private GwtModulesManager myGwtModulesManager;

	@Override
	public String getQualifiedName()
	{
		ensureInitialized();
		return myName;
	}

	private void ensureInitialized()
	{
		PsiFile psiFile = getModuleXmlFile();
		myModuleFile = psiFile.getVirtualFile();
		if(myModuleFile == null)
		{
			psiFile = psiFile.getOriginalFile();
			LOG.assertTrue(psiFile != null);
			myModuleFile = psiFile.getVirtualFile();
		}
		LOG.assertTrue(myModuleFile != null);
		if(myModuleFile.getUrl().equals(myCachedFileUrl))
		{
			return;
		}

		myModuleDirectory = myModuleFile.getParent();
		LOG.assertTrue(myModuleDirectory != null);
		final Project project = psiFile.getProject();
		myGwtModulesManager = GwtModulesManager.getInstance(project);
		final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
		VirtualFile sourceRoot = index.getSourceRootForFile(myModuleFile);
		if(sourceRoot == null)
		{
			sourceRoot = index.getClassRootForFile(myModuleFile);
		}
		LOG.assertTrue(sourceRoot != null);
		String relativePath = VirtualFileUtil.getRelativePath(myModuleFile, sourceRoot, '.');
		myName = relativePath.substring(0, relativePath.length() - GwtModulesManager.GWT_XML_SUFFIX.length());
		myShortName = StringUtil.getShortName(myName);
		myCachedFileUrl = myModuleFile.getUrl();
	}

	@Override
	public VirtualFile getModuleFile()
	{
		ensureInitialized();
		return myModuleFile;
	}

	@Override
	public XmlFile getModuleXmlFile()
	{
		return DomService.getInstance().getContainingFile(this);
	}

	@Override
	public VirtualFile getModuleDirectory()
	{
		ensureInitialized();
		return myModuleDirectory;
	}

	@Override
	public List<GwtModule> getInherited(final GlobalSearchScope scope)
	{
		ensureInitialized();
		final ArrayList<GwtModule> list = new ArrayList<GwtModule>();
		for(GwtInheritsEntry inheritsEntry : getInheritss())
		{
			final String value = inheritsEntry.getName().getValue();
			if(value != null)
			{
				GwtModule gwtModule = myGwtModulesManager.findGwtModuleByName(value, scope);
				if(gwtModule != null)
				{
					list.add(gwtModule);
				}
			}
		}
		return list;
	}

	@Override
	public List<String> getStylesheetFiles()
	{
		ensureInitialized();
		List<String> list = new ArrayList<String>();
		for(GwtStylesheetRef stylesheetRef : getStylesheets())
		{
			final String cssFile = stylesheetRef.getSrc().getValue();
			if(cssFile != null)
			{
				list.add(cssFile);
			}
		}
		return list;
	}

	@Override
	public String getShortName()
	{
		ensureInitialized();
		return myShortName;
	}

	@Override
	public List<VirtualFile> getSourceRoots()
	{
		return getRootsByRelativePaths(getSources(), GwtModulesManager.DEFAULT_SOURCE_PATH);
	}

	@Override
	public List<VirtualFile> getPublicRoots()
	{
		return getRootsByRelativePaths(getPublics(), GwtModulesManager.DEFAULT_PUBLIC_PATH);
	}

	private List<VirtualFile> getGwtModuleRoots()
	{
		PsiManager psiManager = PsiManager.getInstance(getManager().getProject());
		PsiDirectory psiDirectory = psiManager.findDirectory(myModuleDirectory);
		Module module = getModule();
		if(module != null && psiDirectory != null)
		{
			PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
			if(aPackage != null)
			{
				List<VirtualFile> roots = new ArrayList<VirtualFile>();
				PsiDirectory[] directories = aPackage.getDirectories(GlobalSearchScope.moduleWithDependenciesScope(module));
				for(PsiDirectory directory : directories)
				{
					roots.add(directory.getVirtualFile());
				}
				if(!roots.isEmpty())
				{
					return roots;
				}
			}
		}
		return Collections.singletonList(myModuleDirectory);
	}

	private List<VirtualFile> getRootsByRelativePaths(final List<GwtRelativePath> relativePaths, final String defaultPath)
	{
		ensureInitialized();
		final ArrayList<VirtualFile> roots = new ArrayList<VirtualFile>();

		for(VirtualFile moduleRoot : getGwtModuleRoots())
		{
			if(relativePaths.size() == 0)
			{
				final VirtualFile file = moduleRoot.findFileByRelativePath(defaultPath);
				if(file != null)
				{
					roots.add(file);
				}
			}

			for(GwtRelativePath relativePath : relativePaths)
			{
				final String pathValue = relativePath.getPath().getValue();
				if(pathValue != null)
				{
					final VirtualFile file = moduleRoot.findFileByRelativePath(FileUtil.toSystemIndependentName(pathValue));
					if(file != null)
					{
						roots.add(file);
					}
				}
			}
		}
		return roots;
	}
}
