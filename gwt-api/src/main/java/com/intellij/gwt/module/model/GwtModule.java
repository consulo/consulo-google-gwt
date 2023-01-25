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

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.psi.xml.XmlFile;
import consulo.xml.util.xml.DomElement;
import consulo.xml.util.xml.HyphenNameStrategy;
import consulo.xml.util.xml.NameStrategyForAttributes;

import java.util.List;

/**
 * @author nik
 */
@NameStrategyForAttributes(HyphenNameStrategy.class)
public interface GwtModule extends DomElement
{
	GwtModule[] EMPTY_ARRAY = new GwtModule[0];

	List<GwtEntryPoint> getEntryPoints();

	GwtEntryPoint addEntryPoint();

	List<GwtRelativePath> getSources();

	GwtRelativePath addSource();

	List<GwtRelativePath> getPublics();

	GwtRelativePath addPublic();

	List<GwtServlet> getServlets();

	GwtServlet addServlet();

	List<GwtInheritsEntry> getInheritss();

	GwtInheritsEntry addInherits();

	List<GwtStylesheetRef> getStylesheets();

	String getQualifiedName();

	VirtualFile getModuleFile();

	XmlFile getModuleXmlFile();

	String getShortName();

	List<VirtualFile> getSourceRoots();

	List<VirtualFile> getPublicRoots();

	VirtualFile getModuleDirectory();

	List<GwtModule> getInherited(final GlobalSearchScope scope);

	List<String> getStylesheetFiles();
}
