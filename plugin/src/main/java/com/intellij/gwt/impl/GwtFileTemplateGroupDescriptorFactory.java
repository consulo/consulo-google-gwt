/*
 * Copyright 2013-2014 must-be.org
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

package com.intellij.gwt.impl;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.base.templates.GwtTemplates;
import consulo.annotation.component.ExtensionImpl;
import consulo.fileTemplate.FileTemplateDescriptor;
import consulo.fileTemplate.FileTemplateGroupDescriptor;
import consulo.fileTemplate.FileTemplateGroupDescriptorFactory;
import consulo.google.gwt.base.icon.GwtIconGroup;
import consulo.language.file.FileTypeManager;

/**
 * @author VISTALL
 * @since 14.07.14
 */
@ExtensionImpl
public class GwtFileTemplateGroupDescriptorFactory implements FileTemplateGroupDescriptorFactory
{
	@Override
	public FileTemplateGroupDescriptor getFileTemplatesDescriptor()
	{
		final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(GwtBundle.message("file.template.group.titile.gwt"), GwtIconGroup.gwt());
		final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
		for(String template : GwtTemplates.TEMPLATES)
		{
			group.addTemplate(new FileTemplateDescriptor(template, fileTypeManager.getFileTypeByFileName(template).getIcon()));
		}
		return group;
	}
}
