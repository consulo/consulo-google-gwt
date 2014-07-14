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

package com.intellij.gwt.actions;

import org.mustbe.consulo.google.gwt.GoogleGwtIcons;
import com.intellij.gwt.GwtBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * @author nik
 */
public class GwtCreateActionGroup extends DefaultActionGroup
{
	public GwtCreateActionGroup()
	{
		super(GwtBundle.message("action.group.gwt.title"), true);
		getTemplatePresentation().setDescription(GwtBundle.message("action.group.gwt.description"));
		getTemplatePresentation().setIcon(GoogleGwtIcons.Gwt);
	}


	@Override
	public void update(AnActionEvent e)
	{
		e.getPresentation().setVisible(GwtCreateActionBase.isUnderSourceRootsOfModuleWithGwtFacet(e));
	}

}
