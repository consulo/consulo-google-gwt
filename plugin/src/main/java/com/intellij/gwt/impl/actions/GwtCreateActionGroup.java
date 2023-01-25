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

package com.intellij.gwt.impl.actions;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.base.actions.GwtCreateActionBase;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.google.gwt.base.icon.GwtIconGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author nik
 */
@ActionImpl(id = "GWT", children = {
	@ActionRef(type = CreateGwtTestCaseAction.class),
	@ActionRef(type = CreateGwtModuleAction.class),
	@ActionRef(type = CreateGwtEntryPointAction.class),
	@ActionRef(type = CreateGwtSerializableClassAction.class),
	@ActionRef(type = AnSeparator.class),
	@ActionRef(type = CreateGwtSampleAppAction.class),
}, parents = @ActionParentRef(@ActionRef(id = "NewGroup")))
public class GwtCreateActionGroup extends DefaultActionGroup
{
	public GwtCreateActionGroup()
	{
		super(GwtBundle.message("action.group.gwt.title"), true);
		getTemplatePresentation().setDescription(GwtBundle.message("action.group.gwt.description"));
		getTemplatePresentation().setIcon(GwtIconGroup.gwt());
	}


	@Override
	public void update(AnActionEvent e)
	{
		e.getPresentation().setVisible(GwtCreateActionBase.isUnderSourceRootsOfModuleWithGwtFacet(e));
	}

}
