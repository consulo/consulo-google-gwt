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

package com.intellij.gwt.jsinject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.google.gwt.module.extension.GoogleGwtModuleExtensionUtil;
import com.intellij.codeInsight.completion.util.MethodParenthesesHandler;
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.impl.JSChangeUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * @author nik
 */
public class GwtClassMemberReference extends PsiReferenceBase<JSGwtReferenceExpressionImpl>
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.gwt.jsinject.GwtClassMemberReference");
	private static final Key<CachedValue<Map<String, PsiMember>>> CACHED_MEMBER_MAP_KEY = Key.create("cached_member_signatures");
	@NonNls
	private static final String NEW_EXPRESSION = "new";
	@NonNls
	private static Map<PsiType, String> ourPrimitiveTypes = new HashMap<PsiType, String>();

	static
	{
		ourPrimitiveTypes.put(PsiType.BYTE, "B");
		ourPrimitiveTypes.put(PsiType.CHAR, "C");
		ourPrimitiveTypes.put(PsiType.DOUBLE, "D");
		ourPrimitiveTypes.put(PsiType.FLOAT, "F");
		ourPrimitiveTypes.put(PsiType.INT, "I");
		ourPrimitiveTypes.put(PsiType.LONG, "J");
		ourPrimitiveTypes.put(PsiType.SHORT, "S");
		ourPrimitiveTypes.put(PsiType.BOOLEAN, "Z");
	}

	private final PsiReference myClassReference;

	public GwtClassMemberReference(final JSGwtReferenceExpressionImpl element, final @Nullable PsiReference classReference, final TextRange range)
	{
		super(element, range);
		myClassReference = classReference;
	}

	@Override
	@Nullable
	public PsiElement resolve()
	{
		PsiClass psiClass = resolveQualifier();
		if(psiClass == null)
		{
			return null;
		}

		Map<String, PsiMember> map = getMembersMap(psiClass);
		return map != null ? map.get(getValue()) : null;
	}

	@Nullable
	public PsiClass resolveQualifier()
	{
		if(myClassReference == null)
		{
			return null;
		}

		PsiElement element = myClassReference.resolve();
		return element instanceof PsiClass ? (PsiClass) element : null;

	}

	@Nullable
	private Map<String, PsiMember> getMembersMap(final @NotNull PsiClass aClass)
	{
		CachedValue<Map<String, PsiMember>> value = aClass.getUserData(CACHED_MEMBER_MAP_KEY);
		if(value == null)
		{
			value = CachedValuesManager.getManager(getElement().getProject()).createCachedValue(new CachedValueProvider<Map<String, PsiMember>>()
			{
				@Override
				public Result<Map<String, PsiMember>> compute()
				{
					final Map<String, PsiMember> map = buildMembersMap(aClass);
					return new Result<Map<String, PsiMember>>(map, PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
				}
			}, false);
			aClass.putUserData(CACHED_MEMBER_MAP_KEY, value);
		}
		return value.getValue();
	}

	private static Map<String, PsiMember> buildMembersMap(final PsiClass aClass)
	{
		@NonNls HashMap<String, PsiMember> map = new HashMap<String, PsiMember>();

		for(PsiMethod psiMethod : aClass.getAllMethods())
		{
			if(psiMethod.isConstructor())
			{
				continue;
			}

			StringBuilder signature = new StringBuilder(psiMethod.getName());
			signature.append('(');
			if(!appendParameterTypes(signature, psiMethod))
			{
				continue;
			}
			signature.append(')');
			map.put(signature.toString(), psiMethod);
		}

		GwtVersion gwtVersion = GoogleGwtModuleExtensionUtil.getVersion(aClass);
		if(gwtVersion.isNewExpressionInJavaScriptSupported())
		{
			for(PsiMethod constructor : aClass.getConstructors())
			{
				StringBuilder signature = new StringBuilder(NEW_EXPRESSION);
				signature.append('(');
				if(!appendEnclosingClassType(signature, constructor.getContainingClass()))
				{
					continue;
				}
				if(!appendParameterTypes(signature, constructor))
				{
					continue;
				}
				signature.append(')');
				map.put(signature.toString(), constructor);
			}
			if(aClass.getConstructors().length == 0)
			{
				StringBuilder signature = new StringBuilder(NEW_EXPRESSION);
				signature.append('(');
				if(appendEnclosingClassType(signature, aClass))
				{
					signature.append(')');
					map.put(signature.toString(), aClass);
				}
			}
		}

		if(aClass.isEnum())
		{
			PsiElementFactory elementFactory = JavaPsiFacade.getInstance(aClass.getProject()).getElementFactory();
			try
			{
				final PsiMethod valuesMethod = elementFactory.createMethodFromText("public static " + aClass.getName() + "[] values() {}", aClass);
				map.put("values()", valuesMethod);
				final PsiMethod valueOfMethod = elementFactory.createMethodFromText("public static " + aClass.getName() + " valueOf(String name) {}", aClass);
				map.put("valueOf(Ljava/lang/String;)", valueOfMethod);
			}
			catch(IncorrectOperationException e)
			{
				LOG.info(e);
			}
		}

		for(PsiField psiField : aClass.getFields())
		{
			map.put(psiField.getName(), psiField);
		}

		return map;
	}

	private static boolean appendEnclosingClassType(final @NotNull StringBuilder signature, final @NotNull PsiClass psiClass)
	{
		PsiClass containingClass = psiClass.getContainingClass();
		PsiModifierList modifierList = psiClass.getModifierList();
		if(containingClass != null && (modifierList == null || !modifierList.hasModifierProperty(PsiModifier.STATIC)))
		{
			String type = getClassTypeSignature(containingClass);
			if(type == null)
			{
				return false;
			}
			signature.append(type);
		}
		return true;
	}

	private static boolean appendParameterTypes(final StringBuilder signature, final PsiMethod psiMethod)
	{
		for(PsiParameter psiParameter : psiMethod.getParameterList().getParameters())
		{
			String type = getTypeSignature(psiParameter.getType());
			if(type == null)
			{
				return false;
			}
			signature.append(type);
		}
		return true;
	}

	@Nullable
	@NonNls
	private static String getTypeSignature(final PsiType type)
	{
		if(type instanceof PsiArrayType)
		{
			return "[" + getTypeSignature(((PsiArrayType) type).getComponentType());
		}
		if(type instanceof PsiPrimitiveType)
		{
			return ourPrimitiveTypes.get(type);
		}
		if(type instanceof PsiClassType)
		{
			PsiClass psiClass = ((PsiClassType) type).resolve();
			if(psiClass == null)
			{
				return null;
			}
			return getClassTypeSignature(psiClass);
		}
		return null;
	}

	@Nullable
	@NonNls
	private static String getClassTypeSignature(final PsiClass psiClass)
	{
		String name = getJvmClassName(psiClass);
		if(name == null)
		{
			return null;
		}
		return "L" + name + ";";
	}

	@Nullable
	private static String getJvmClassName(final @NotNull PsiClass psiClass)
	{
		PsiClass parent = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class, true);
		if(parent != null)
		{
			return getJvmClassName(parent) + "$" + psiClass.getName();
		}
		String qualifiedName = psiClass.getQualifiedName();
		return qualifiedName != null ? qualifiedName.replace('.', '/') : null;
	}

	@NotNull
	@Override
	public Object[] getVariants()
	{
		PsiClass psiClass = resolveQualifier();
		if(psiClass == null)
		{
			return ArrayUtil.EMPTY_OBJECT_ARRAY;
		}

		Map<String, PsiMember> map = getMembersMap(psiClass);
		if(map == null)
		{
			return ArrayUtil.EMPTY_OBJECT_ARRAY;
		}

		List<LookupElementBuilder> lookupItems = new ArrayList<LookupElementBuilder>();
		for(Map.Entry<String, PsiMember> entry : map.entrySet())
		{
			PsiMember member = entry.getValue();
			LookupElementBuilder item = LookupElementBuilder.create(member, entry.getKey());
			if(member instanceof PsiMethod)
			{
				item.withInsertHandler(new MethodParenthesesHandler((PsiMethod) member, true));
			}
			else if(member instanceof PsiClass)
			{
				item.withInsertHandler(ParenthesesInsertHandler.NO_PARAMETERS);
			}
			lookupItems.add(item);
		}
		return lookupItems.toArray();
	}

	@Override
	public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException
	{
		String oldText = myElement.getText();
		String oldElementName = getRangeInElement().substring(oldText);
		int i = oldElementName.indexOf('(');
		if(i != -1)
		{
			newElementName += oldElementName.substring(i);
		}
		String newText = getRangeInElement().replace(oldText, newElementName);
		JSExpression newNode = JSChangeUtil.createExpressionFromText(myElement.getProject(), newText);
		return myElement.replace(newNode);
	}

	@Override
	public PsiElement bindToElement(@NotNull final PsiElement element) throws IncorrectOperationException
	{
		if(element instanceof PsiMember)
		{
			return handleElementRename(((PsiMember) element).getName());
		}
		return super.bindToElement(element);
	}
}
