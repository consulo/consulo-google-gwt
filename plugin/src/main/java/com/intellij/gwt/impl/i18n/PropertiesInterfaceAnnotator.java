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

package com.intellij.gwt.impl.i18n;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.base.i18n.GwtI18nUtil;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.application.AllIcons;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.List;

/**
 * @author nik
 */
public class PropertiesInterfaceAnnotator implements Annotator
{
	private static final Implementable<PsiMethod, IProperty> PROPERTY_IMPLEMENTABLE = new Implementable<PsiMethod, IProperty>()
	{
		@Override
		public void navigate(final IProperty target)
		{
			GwtI18nUtil.navigateToProperty(target);
		}

		@Override
		public String getPopupChooserTitle(final PsiMethod source, final IProperty[] targets)
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
			target.getContainingFile().navigate(true);
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
			final IProperty[] properties = manager.getProperties(method);
			if(properties.length != 0)
			{
				final Annotation annotation = holder.createInfoAnnotation(method.getNameIdentifier(), null);
				annotation.setGutterIconRenderer(new ImplementedGutterIconRenderer<PsiMethod, IProperty>(PROPERTY_IMPLEMENTABLE, method, properties));
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

	private static interface Implementable<S extends PsiElement, T>
	{
		void navigate(T target);

		String getPopupChooserTitle(S source, T[] targets);

		String getGutterTooltip(S source, String files);

		ListCellRenderer getListCellRenderer();
	}

	private static class ImplementedGutterIconRenderer<S extends PsiElement, T> extends GutterIconRenderer
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
		@Nonnull
		public Image getIcon()
		{
			return AllIcons.Gutter.ImplementedMethod;
		}

		@Override
		@Nullable
		public String getTooltipText()
		{
			final StringBuilder files = new StringBuilder();
			for(T target : myTargets)
			{
				if(target instanceof PropertiesFile)
				{
					files.append(MessageFormat.format(IMPLEMENTING_PROPERTY_FILE_FORMAT, ((PropertiesFile) target).getContainingFile().getName()));
				}
				else if(target instanceof IProperty)
				{
					files.append(MessageFormat.format(IMPLEMENTING_PROPERTY_FILE_FORMAT, ((IProperty) target).getPropertiesFile().getName()));
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

		@Override
		public boolean equals(Object o)
		{
			return false;
		}

		@Override
		public int hashCode()
		{
			return 0;
		}
	}

	private static class NavigateAction<S extends PsiElement, T> extends AnAction
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
				final String title = myImplementable.getPopupChooserTitle(mySource, myTargets);

				final JBPopup popup = JBPopupFactory.getInstance().createPopupChooserBuilder(List.of(myTargets)).
						setTitle(title).
						setMovable(true).
						setRenderer(myImplementable.getListCellRenderer()).
						setItemsChosenCallback(items ->
						{
							for(T i : items)
							{
								myImplementable.navigate(i);
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
