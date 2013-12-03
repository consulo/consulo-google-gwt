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

package com.intellij.gwt;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.gwt.inspections.*;
import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JavascriptParserDefinition;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;

/**
 * @author nik
 */
public class GwtApplicationComponent implements ApplicationComponent, InspectionToolProvider, FileTemplateGroupDescriptorFactory
{
	private LocalFileSystem.WatchRequest myWatchRequest;

	@Override
	@NonNls
	@NotNull
	public String getComponentName()
	{
		return "GwtApplicationComponent";
	}

	@Override
	public void initComponent()
	{
		JavascriptParserDefinition.setGwtReferenceExpressionCreator(new Function<ASTNode, PsiElement>()
		{
			@Override
			public PsiElement fun(final ASTNode astNode)
			{
				return new JSGwtReferenceExpressionImpl(astNode);
			}
		});
		myWatchRequest = LocalFileSystem.getInstance().addRootToWatch(GwtCompilerPaths.getOutputRoot().getAbsolutePath(), true);
	}

	@Override
	public void disposeComponent()
	{
		if(myWatchRequest != null)
		{
			LocalFileSystem.getInstance().removeWatchedRoot(myWatchRequest);
		}
	}

	@Override
	public Class[] getInspectionClasses()
	{
		return new Class[]{
				GwtInconsistentAsyncInterfaceInspection.class,
				GwtToCssClassReferencesInspection.class,
				GwtNonSerializableRemoteServiceMethodParametersInspection.class,
				GwtToHtmlTagReferencesInspection.class,
				NonJREEmulationClassesInClientCodeInspection.class,
				GwtServiceNotRegisteredInspection.class,
				GwtInconsistentLocalizableInterfaceInspection.class,
				GwtInconsistentSerializableClassInspection.class,
				GwtMethodWithParametersInConstantsInterfaceInspection.class,
				GwtJavaScriptReferencesInspection.class,
				GwtObsoleteTypeArgsJavadocTagInspection.class,
				GwtRawAsyncCallbackInspection.class,
				GwtDeprecatedPropertyKeyJavadocTagInspection.class
		};
	}

	@Override
	public FileTemplateGroupDescriptor getFileTemplatesDescriptor()
	{
		final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(GwtBundle.message("file.template.group.titile.gwt"),
				GwtFacetType.SMALL_ICON);
		final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
		for(String template : GwtTemplates.TEMPLATES)
		{
			group.addTemplate(new FileTemplateDescriptor(template, fileTypeManager.getFileTypeByFileName(template).getIcon()));
		}
		return group;
	}


}
