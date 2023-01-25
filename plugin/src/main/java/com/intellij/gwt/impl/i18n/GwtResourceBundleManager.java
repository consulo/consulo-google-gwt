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

package com.intellij.gwt.impl.i18n;

import com.intellij.gwt.base.i18n.GwtI18nUtil;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.impl.codeInsight.daemon.impl.quickfix.CreateFromUsageUtils;
import com.intellij.java.language.LanguageLevel;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiUtil;
import com.intellij.lang.properties.PropertiesFileProcessor;
import com.intellij.lang.properties.PropertiesFilesManager;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.fileEditor.FileEditorManager;
import consulo.gwt.base.module.extension.GwtModuleExtensionUtil;
import consulo.gwt.base.module.extension.path.GwtSdkUtil;
import consulo.java.analysis.impl.util.JavaI18nUtil;
import consulo.java.properties.impl.i18n.JavaPropertiesUtil;
import consulo.java.properties.impl.psi.I18nizedTextGenerator;
import consulo.java.properties.impl.psi.PropertyCreationHandler;
import consulo.java.properties.impl.psi.ResourceBundleManager;
import consulo.language.editor.CodeInsightUtilCore;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateBuilder;
import consulo.language.editor.template.TemplateBuilderFactory;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtResourceBundleManager extends ResourceBundleManager
{
	private static final Logger LOG = Logger.getInstance(GwtResourceBundleManager.class);
	private final PsiManager myPsiManager;
	private final GwtI18nManager myI18nManager;
	private final GwtModulesManager myGwtModulesManager;

	@Inject
	public GwtResourceBundleManager(final Project project, PsiManager psiManager, GwtI18nManager gwtI18nManager, GwtModulesManager gwtModulesManager)
	{
		super(project);
		myPsiManager = psiManager;
		myI18nManager = gwtI18nManager;
		myGwtModulesManager = gwtModulesManager;
	}

	@Override
	@Nullable
	public PsiClass getResourceBundle()
	{
		return null;
	}

	@Override
	@NonNls
	public String getTemplateName()
	{
		return null;
	}

	@Override
	@NonNls
	public String getConcatenationTemplateName()
	{
		return null;
	}

	@Override
	public boolean isActive(final PsiFile context) throws ResourceBundleNotFoundException
	{
		return myGwtModulesManager.isUnderGwtModule(context.getVirtualFile());
	}

	@Override
	public boolean canShowJavaCodeInfo()
	{
		return false;
	}

	@Override
	public String suggestPropertyKey(final @Nonnull String value)
	{
		return GwtI18nUtil.suggetsPropertyKey(value, PsiNameHelper.getInstance(myProject), LanguageLevel.HIGHEST);
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
		FileModificationService.getInstance().prepareFileForWrite(psiFile);
		final VirtualFile virtualFile = psiFile.getVirtualFile();
		LOG.assertTrue(virtualFile != null);

		GwtVersion gwtVersion = GwtModuleExtensionUtil.getVersion(anInterface);
		PsiMethod method = GwtI18nUtil.addMethod(anInterface, key, gwtVersion);
		if(parameters.length > 0)
		{
			TemplateBuilder builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(method);
			CreateFromUsageUtils.setupMethodParameters(method, builder, parameters[0], PsiSubstitutor.EMPTY, parameters);
			method = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(method);

			final OpenFileDescriptor descriptor = OpenFileDescriptorFactory.getInstance(myProject).builder(virtualFile).offset(method.getTextRange().getStartOffset()).build();

			Document document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
			LOG.assertTrue(document != null);
			RangeMarker methodRange = document.createRangeMarker(method.getTextRange());
			final Editor editor = FileEditorManager.getInstance(myProject).openTextEditor(descriptor, true);
			final Template template = TemplateManager.getInstance(myProject).createTemplate("", "");

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

		@Override
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

		private String getLocalizableInstance(final @Nonnull PsiClass anInterface, final @Nonnull PsiLiteralExpression context)
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

		@Override
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
		@Override
		public void createProperty(final Project project, final Collection<PropertiesFile> propertiesFiles, final String key, final String value,
				final PsiExpression[] parameters) throws IncorrectOperationException
		{
			JavaPropertiesUtil.DEFAULT_PROPERTY_CREATION_HANDLER.createProperty(project, propertiesFiles, key, value, parameters);
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
