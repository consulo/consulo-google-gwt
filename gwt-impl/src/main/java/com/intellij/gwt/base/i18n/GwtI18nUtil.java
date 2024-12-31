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

package com.intellij.gwt.base.i18n;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.LanguageLevel;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.javadoc.PsiDocComment;
import com.intellij.java.language.psi.javadoc.PsiDocTag;
import com.intellij.java.language.psi.javadoc.PsiDocTagValue;
import com.intellij.java.language.psi.util.PsiUtil;
import com.intellij.lang.properties.IProperty;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.text.MessageFormat;
import java.util.List;

/**
 * @author nik
 */
public class GwtI18nUtil
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.i18n.GwtI18nUtil");

	@NonNls
	public static final String CONSTANTS_INTERFACE_NAME = "com.google.gwt.i18n.client.Constants";
	@NonNls
	public static final String MESSAGES_INTERFACE_NAME = "com.google.gwt.i18n.client.Messages";
	@NonNls
	public static final String LOCALIZABLE_INTERFACE_NAME = "com.google.gwt.i18n.client.LocalizableResource";
	@NonNls
	private static final String GWT_PROPERTY_KEY_JAVADOC = "/**\n*@gwt.key {0}\n*/";
	@NonNls
	public static final String KEY_ANNOTATION_CLASS = "com.google.gwt.i18n.client.LocalizableResource.Key";
	@NonNls
	public static final String GWT_KEY_TAG = "gwt.key";


	private GwtI18nUtil()
	{
	}

	public static void navigateToProperty(@Nonnull IProperty property)
	{
		property.navigate(true);
	}

	public static String suggetsPropertyKey(@Nonnull String value, final PsiNameHelper nameHelper, final LanguageLevel languageLevel)
	{
		List<String> words = StringUtil.getWordsIn(value);
		StringBuilder key = new StringBuilder();
		for(String word : words)
		{
			if(key.length() > 0)
			{
				word = StringUtil.capitalize(word);
			}
			key.append(word);
		}
		return addPrefixIfNeeded(key.toString(), "property", nameHelper, languageLevel);
	}

	public static String convertPropertyName2MethodName(String propertyName, final PsiNameHelper nameHelper, final LanguageLevel languageLevel)
	{
		if(nameHelper.isIdentifier(propertyName, languageLevel))
		{
			return propertyName;
		}

		final String[] words = propertyName.split("\\.");
		StringBuilder builder = new StringBuilder();
		for(String word : words)
		{
			String id = convert2Id(word);
			if(id.length() > 0)
			{
				if(builder.length() > 0)
				{
					id = StringUtil.capitalize(id);
				}
				builder.append(id);
			}
		}

		return addPrefixIfNeeded(builder.toString(), "getProperty", nameHelper, languageLevel);
	}

	private static String addPrefixIfNeeded(String id, final @NonNls String prefix, final PsiNameHelper nameHelper, final LanguageLevel languageLevel)
	{
		if(!nameHelper.isIdentifier(id, languageLevel))
		{
			id = prefix + StringUtil.capitalize(id);
			if(!nameHelper.isIdentifier(id, languageLevel))
			{
				id = prefix;
			}
		}
		return id;
	}

	private static String convert2Id(final String word)
	{
		final StringBuilder builder = new StringBuilder();
		for(int i = 0; i < word.length(); i++)
		{
			char c = word.charAt(i);
			if(builder.length() == 0 && Character.isJavaIdentifierStart(c) || builder.length() > 0 && Character.isJavaIdentifierPart(c))
			{
				builder.append(c);
			}
		}
		return builder.toString();
	}

	public static void addMethod(PsiClass aClass, String propertyName, @Nullable String propertyValue, final GwtVersion gwtVersion)
	{
		try
		{
			PsiMethod method = addMethod(aClass, propertyName, gwtVersion);
			PsiElementFactory psiElementFactory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();
			int parametersCount = getParametersCount(propertyValue);
			PsiClassType javaLangString = PsiType.getJavaLangString(method.getManager(), GlobalSearchScope.allScope(method.getProject()));
			for(int i = 0; i < parametersCount; i++)
			{
				final PsiParameter psiParameter = psiElementFactory.createParameter("p" + i, javaLangString);
				if(aClass.isInterface())
				{
					psiParameter.getModifierList().setModifierProperty(PsiModifier.FINAL, false);
				}
				method.getParameterList().add(psiParameter);
			}
		}
		catch(IncorrectOperationException e)
		{
			LOG.error(e);
		}
	}

	public static int getParametersCount(final @Nullable String propertyValue)
	{
		if(propertyValue == null)
		{
			return 0;
		}

		int maxParameter = -1;
		int i = propertyValue.indexOf('{');
		while(i != -1)
		{
			int end = propertyValue.indexOf('}', i);
			if(end == -1)
			{
				break;
			}

			int comma = propertyValue.indexOf(',', i);
			if(comma != -1 && comma < end)
			{
				end = comma;
			}

			try
			{
				int parameter = Integer.parseInt(propertyValue.substring(i + 1, end));
				maxParameter = Math.max(maxParameter, parameter);
			}
			catch(NumberFormatException e)
			{
			}
			i = propertyValue.indexOf('{', end);
		}
		return maxParameter + 1;
	}

	public static PsiMethod addMethod(PsiClass aClass, String propertyName, final GwtVersion gwtVersion) throws IncorrectOperationException
	{
		final PsiManager psiManager = aClass.getManager();
		String methodName = convertPropertyName2MethodName(propertyName, JavaPsiFacade.getInstance(psiManager.getProject()).getNameHelper(),
				PsiUtil.getLanguageLevel(aClass));

		final PsiClassType javaLangString = PsiType.getJavaLangString(psiManager, aClass.getResolveScope());
		PsiElementFactory elementFactory = JavaPsiFacade.getInstance(psiManager.getProject()).getElementFactory();
		final PsiMethod method = elementFactory.createMethod(methodName, javaLangString);
		method.getModifierList().setModifierProperty(PsiModifier.PUBLIC, false);
		final PsiCodeBlock body = method.getBody();
		LOG.assertTrue(body != null);
		body.delete();
		final PsiMethod addedMethod = (PsiMethod) aClass.add(method);

		if(!propertyName.equals(methodName))
		{
			if(gwtVersion.isGenericsSupported())
			{
				addKeyAnnotation(propertyName, addedMethod, elementFactory);
			}
			else
			{
				final String commentText = MessageFormat.format(GWT_PROPERTY_KEY_JAVADOC, propertyName);
				PsiComment comment = elementFactory.createCommentFromText(commentText, aClass);
				addedMethod.addBefore(comment, addedMethod.getFirstChild());
			}
		}
		return addedMethod;
	}

	public static void addKeyAnnotation(final String propertyName, final PsiMethod method, final PsiElementFactory elementFactory) throws
			IncorrectOperationException
	{
		final String annotationText = "@" + KEY_ANNOTATION_CLASS + "(\"" + propertyName + "\")";
		PsiAnnotation annotation = elementFactory.createAnnotationFromText(annotationText, method);
		method.getModifierList().add(annotation);
	}

	public static String getPropertyName(final PsiMethod method)
	{
		PsiAnnotation annotation = method.getModifierList().findAnnotation(KEY_ANNOTATION_CLASS);
		if(annotation != null)
		{
			PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
			if(attributes.length == 1)
			{
				String key = AnnotationUtil.getStringAttributeValue(annotation, "value");
				if(key != null)
				{
					return key;
				}
			}
		}

		final PsiDocComment docComment = method.getDocComment();
		if(docComment != null)
		{
			final PsiDocTag tag = docComment.findTagByName(GWT_KEY_TAG);
			if(tag != null)
			{
				final PsiDocTagValue psiDocTagValue = tag.getValueElement();
				if(psiDocTagValue != null)
				{
					final String text = psiDocTagValue.getText();
					if(text != null)
					{
						return text;
					}
				}
			}
		}
		return method.getName();
	}
}
