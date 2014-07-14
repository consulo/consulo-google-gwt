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

package com.intellij.gwt.i18n;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtension;
import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateBuilder;
import com.intellij.codeInsight.template.TemplateBuilderFactory;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInspection.i18n.JavaI18nUtil;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.lang.properties.PropertiesFileProcessor;
import com.intellij.lang.properties.PropertiesFilesManager;
import com.intellij.lang.properties.psi.I18nizedTextGenerator;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.PropertyCreationHandler;
import com.intellij.lang.properties.psi.ResourceBundleManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author nik
 */
public class GwtResourceBundleManager extends ResourceBundleManager
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.i18n.GwtResourceBundleManager");
	private final PsiManager myPsiManager;
	private final GwtI18nManager myI18nManager;
	private final GwtModulesManager myGwtModulesManager;

	public GwtResourceBundleManager(final Project project, PsiManager psiManager, GwtI18nManager gwtI18nManager, GwtModulesManager gwtModulesManager)
	{
		super(project);
		myPsiManager = psiManager;
		myI18nManager = gwtI18nManager;
		myGwtModulesManager = gwtModulesManager;
	}

	@Nullable
	public PsiClass getResourceBundle()
	{
		return null;
	}

	@NonNls
	public String getTemplateName()
	{
		return null;
	}

	@NonNls
	public String getConcatenationTemplateName()
	{
		return null;
	}

	public boolean isActive(final PsiFile context) throws ResourceBundleNotFoundException
	{
		return myGwtModulesManager.isUnderGwtModule(context.getVirtualFile());
	}

	public boolean canShowJavaCodeInfo()
	{
		return false;
	}

	@Override
	public String suggestPropertyKey(final @NotNull String value)
	{
		return GwtI18nUtil.suggetsPropertyKey(value, JavaPsiFacade.getInstance(myProject).getNameHelper(), LanguageLevel.HIGHEST);
	}

	@Override
	public List<String> suggestPropertiesFiles()
	{
		final List<String> paths = new ArrayList<String>();

		PropertiesFilesManager.getInstance(myProject).processAllPropertiesFiles(new PropertiesFileProcessor()
		{
			@Override
			public boolean process(String s, PropertiesFile propertiesFile)
			{
				if(myI18nManager.getPropertiesInterface(propertiesFile) != null)
				{
					paths.add(FileUtil.toSystemDependentName(propertiesFile.getVirtualFile().getPath()));
				}
				return true;
			}
		});

		return paths;
	}

	private void addMethod(final PsiClass anInterface, final String key, final PsiExpression[] parameters) throws IncorrectOperationException
	{
		PsiFile psiFile = anInterface.getContainingFile();
		CodeInsightUtilBase.getInstance().prepareFileForWrite(psiFile);
		final VirtualFile virtualFile = psiFile.getVirtualFile();
		LOG.assertTrue(virtualFile != null);

		GoogleGwtModuleExtension gwtFacet = GwtFacet.findFacetBySourceFile(myProject, psiFile.getVirtualFile());
		GwtVersion gwtVersion = GwtFacet.getGwtVersion(gwtFacet);
		PsiMethod method = GwtI18nUtil.addMethod(anInterface, key, gwtVersion);
		if(parameters.length > 0)
		{
			TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(method);
			CreateFromUsageUtils.setupMethodParameters(method, builder, parameters[0], PsiSubstitutor.EMPTY, parameters);
			method = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(method);

			final OpenFileDescriptor descriptor = new OpenFileDescriptor(myProject, virtualFile, method.getTextRange().getStartOffset());

			Document document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
			LOG.assertTrue(document != null);
			RangeMarker methodRange = document.createRangeMarker(method.getTextRange());
			final Editor editor = FileEditorManager.getInstance(myProject).openTextEditor(descriptor, true);
			final Template template = null;//TODO [VISTALL] TemplateManager.getInstance(myProject).createTemplate()

			editor.getCaretModel().moveToOffset(methodRange.getStartOffset());
			editor.getDocument().deleteString(methodRange.getStartOffset(), methodRange.getEndOffset());

			ApplicationManager.getApplication().invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					TemplateManager.getInstance(myProject).startTemplate(editor, template);
				}
			});
		}
	}

	@Nullable
	@Override
	public PropertyCreationHandler getPropertyCreationHandler()
	{
		return new GwtPropertyCreationHandler();
	}

	@Nullable
	@Override
	public I18nizedTextGenerator getI18nizedTextGenerator()
	{
		return new GwtI18nizedTextGenerator();
	}

	private class GwtI18nizedTextGenerator extends I18nizedTextGenerator
	{
		@NonNls
		private static final String GET_LOCALIZABLE_INSTANCE_TEMPLATE = "(({0}) " + GwtSdkUtil.GWT_CLASS_NAME + ".create({0}.class))";

		public String getI18nizedText(final String propertyKey, final @Nullable PropertiesFile propertiesFile, final PsiLiteralExpression context)
		{
			return getI18nizedConcatenationText(propertyKey, "", propertiesFile, context);
		}

		private String getI18nizedText(@Nullable @NonNls String qualifier, final String propertyKey, final PsiLiteralExpression context, String parameters)
		{
			if(qualifier == null)
			{
				qualifier = "constants";
			}
			String methodName = GwtI18nUtil.convertPropertyName2MethodName(propertyKey, JavaPsiFacade.getInstance(myPsiManager.getProject()).getNameHelper(),
					PsiUtil.getLanguageLevel(context));
			return qualifier + "." + methodName + "(" + parameters + ")";
		}

		private String getLocalizableInstance(final @NotNull PsiClass anInterface, final @NotNull PsiLiteralExpression context)
		{
			PsiClassType type = JavaPsiFacade.getInstance(context.getProject()).getElementFactory().createType(anInterface);
			Set<String> expressions = JavaI18nUtil.suggestExpressionOfType(type, context);
			Iterator<String> iterator = expressions.iterator();
			if(iterator.hasNext())
			{
				return iterator.next();
			}
			return MessageFormat.format(GET_LOCALIZABLE_INSTANCE_TEMPLATE, anInterface.getQualifiedName());
		}

		public String getI18nizedConcatenationText(final String propertyKey, final String parametersString, final @Nullable PropertiesFile propertiesFile,
				final PsiLiteralExpression context)
		{
			String qualifier = null;
			if(propertiesFile != null)
			{
				PsiClass anInterface = myI18nManager.getPropertiesInterface(propertiesFile);
				if(anInterface != null)
				{
					qualifier = getLocalizableInstance(anInterface, context);
				}
			}

			return getI18nizedText(qualifier, propertyKey, context, parametersString);
		}
	}

	private class GwtPropertyCreationHandler implements PropertyCreationHandler
	{
		public void createProperty(final Project project, final Collection<PropertiesFile> propertiesFiles, final String key, final String value,
				final PsiExpression[] parameters) throws IncorrectOperationException
		{
			JavaI18nUtil.DEFAULT_PROPERTY_CREATION_HANDLER.createProperty(project, propertiesFiles, key, value, parameters);
			Iterator<PropertiesFile> iterator = propertiesFiles.iterator();
			if(iterator.hasNext())
			{
				PropertiesFile propertiesFile = iterator.next();
				PsiClass anInterface = myI18nManager.getPropertiesInterface(propertiesFile);
				if(anInterface != null)
				{
					addMethod(anInterface, key, parameters);
				}
			}
		}
	}
}
