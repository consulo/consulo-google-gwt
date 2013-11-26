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

package com.intellij.gwt.i18n;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.GwtBundle;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.awt.RelativePoint;

/**
 * @author nik
 */
public class PropertiesInterfaceAnnotator implements Annotator
{
	private static final Implementable<PsiMethod, Property> PROPERTY_IMPLEMENTABLE = new Implementable<PsiMethod, Property>()
	{
		@Override
		public void navigate(final Property target)
		{
			GwtI18nUtil.navigateToProperty(target);
		}

		@Override
		public String getPopupChooserTitle(final PsiMethod source, final Property[] targets)
		{
			return GwtBundle.message("i18n.goto.property.popup.title", targets[0].getUnescapedKey(), targets.length);
		}

		@Override
		public String getGutterTooltip(final PsiMethod source, final String files)
		{
			return GwtBundle.message("i18n.interface.method.gutter.tooltip", files);
		}

		@Override
		public ListCellRenderer getListCellRenderer()
		{
			return new PropertiesListCellRenderer();
		}
	};
	private static final Implementable<PsiClass, PropertiesFile> IMPLEMENTABLE_PROPERTIES_CLASS = new Implementable<PsiClass, PropertiesFile>()
	{
		@Override
		public void navigate(final PropertiesFile target)
		{
			target.navigate(true);
		}

		@Override
		public String getPopupChooserTitle(final PsiClass source, final PropertiesFile[] targets)
		{
			return GwtBundle.message("i18n.goto.property.popup.title", source.getName(), targets.length);
		}

		@Override
		public String getGutterTooltip(final PsiClass source, final String files)
		{
			return GwtBundle.message("i18n.class.gutter.tooltip.text", files);
		}

		@Override
		public ListCellRenderer getListCellRenderer()
		{
			return new PropertiesFilesListCellRenderer();
		}
	};

	@Override
	public void annotate(PsiElement psiElement, AnnotationHolder holder)
	{
		if(psiElement instanceof PsiMethod)
		{
			final PsiMethod method = (PsiMethod) psiElement;
			GwtI18nManager manager = GwtI18nManager.getInstance(method.getProject());
			final Property[] properties = manager.getProperties(method);
			if(properties.length != 0)
			{
				final Annotation annotation = holder.createInfoAnnotation(method.getNameIdentifier(), null);
				annotation.setGutterIconRenderer(new ImplementedGutterIconRenderer<PsiMethod, Property>(PROPERTY_IMPLEMENTABLE, method, properties));
			}
		}
		else if(psiElement instanceof PsiClass)
		{
			PsiClass aClass = (PsiClass) psiElement;
			final PropertiesFile[] files = GwtI18nManager.getInstance(aClass.getProject()).getPropertiesFiles(aClass);
			if(files.length != 0)
			{
				final Annotation annotation = holder.createInfoAnnotation(aClass.getNameIdentifier(), null);
				annotation.setGutterIconRenderer(new ImplementedGutterIconRenderer<PsiClass, PropertiesFile>(IMPLEMENTABLE_PROPERTIES_CLASS, aClass, files));
			}
		}
	}

	private static interface Implementable<S extends PsiElement, T extends PsiElement>
	{
		void navigate(T target);

		String getPopupChooserTitle(S source, T[] targets);

		String getGutterTooltip(S source, String files);

		ListCellRenderer getListCellRenderer();
	}

	private static class ImplementedGutterIconRenderer<S extends PsiElement, T extends PsiElement> extends GutterIconRenderer
	{
		private Implementable<S, T> myImplementable;
		private S mySource;
		private T[] myTargets;
		@NonNls
		private static final String IMPLEMENTING_PROPERTY_FILE_FORMAT = "&nbsp;&nbsp;&nbsp;&nbsp;{0}<br>";

		public ImplementedGutterIconRenderer(final Implementable<S, T> implementable, final S source, final T[] targets)
		{
			myImplementable = implementable;
			mySource = source;
			myTargets = targets;
		}

		@Override
		@NotNull
		public Icon getIcon()
		{
			return GwtI18nUtil.IMPLEMENTED_PROPERTY_METHOD_ICON;
		}

		@Override
		@Nullable
		public String getTooltipText()
		{
			final StringBuilder files = new StringBuilder();
			for(T target : myTargets)
			{
				if(target.isValid())
				{
					files.append(MessageFormat.format(IMPLEMENTING_PROPERTY_FILE_FORMAT, target.getContainingFile().getName()));
				}
			}
			return myImplementable.getGutterTooltip(mySource, files.toString());
		}

		@Override
		@Nullable
		public AnAction getClickAction()
		{
			return new NavigateAction<S, T>(myImplementable, mySource, myTargets);
		}

		@Override
		public boolean isNavigateAction()
		{
			return true;
		}
	}

	private static class NavigateAction<S extends PsiElement, T extends PsiElement> extends AnAction
	{
		private Implementable<S, T> myImplementable;
		private S mySource;
		private T[] myTargets;

		public NavigateAction(final Implementable<S, T> implementable, final S source, final T[] targets)
		{
			myImplementable = implementable;
			mySource = source;
			myTargets = targets;
		}

		@Override
		public void actionPerformed(AnActionEvent e)
		{
			if(myTargets.length == 1)
			{
				myImplementable.navigate(myTargets[0]);
			}
			else
			{
				final JList list = new JList(myTargets);
				list.setCellRenderer(myImplementable.getListCellRenderer());
				final String title = myImplementable.getPopupChooserTitle(mySource, myTargets);

				final JBPopup popup = new PopupChooserBuilder(list).
						setTitle(title).
						setMovable(true).
						setItemChoosenCallback(new Runnable()
						{
							@Override
							public void run()
							{
								int[] indices = list.getSelectedIndices();
								if(indices == null || indices.length == 0)
								{
									return;
								}
								for(int i : indices)
								{
									myImplementable.navigate(myTargets[i]);
								}
							}
						}).createPopup();

				final InputEvent event = e.getInputEvent();
				if(event instanceof MouseEvent)
				{
					popup.show(new RelativePoint((MouseEvent) event));
				}
				else
				{
					popup.showInBestPositionFor(e.getDataContext());
				}
			}
		}
	}
}
