package com.intellij.gwt.base.rpc;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiElementFactory;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiType;
import com.intellij.java.language.psi.javadoc.PsiDocComment;
import com.intellij.java.language.psi.javadoc.PsiDocTag;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author nik
 */
public class GwtGenericsUtil
{
	@NonNls
	public static final String TYPE_ARGS_TAG = "gwt.typeArgs";

	private GwtGenericsUtil()
	{
	}

	public static List<PsiType> getTypeParameters(@Nonnull PsiElement context, @Nullable String typeParametersString)
	{
		if(typeParametersString == null)
		{
			return Collections.emptyList();
		}

		ArrayList<PsiType> types = new ArrayList<PsiType>();
		StringTokenizer tokenizer = new StringTokenizer(typeParametersString, "<>,");
		PsiElementFactory elementFactory = JavaPsiFacade.getInstance(context.getProject()).getElementFactory();
		while(tokenizer.hasMoreTokens())
		{
			String type = tokenizer.nextToken();
			try
			{
				types.add(elementFactory.createTypeFromText(type, context));
			}
			catch(IncorrectOperationException e)
			{
			}
		}
		return types;
	}

	@Nullable
	public static String getReturnTypeParametersString(final PsiMethod method)
	{
		return getTypeParametersString(method, null);
	}

	@Nullable
	public static String getTypeParametersString(final PsiMethod method, final @Nullable String parameterName)
	{
		PsiDocComment comment = method.getDocComment();
		if(comment == null)
		{
			return null;
		}

		PsiDocTag[] tags = comment.findTagsByName(TYPE_ARGS_TAG);
		for(PsiDocTag tag : tags)
		{
			PsiElement[] elements = tag.getDataElements();
			if(parameterName != null && elements.length >= 2 && parameterName.equals(elements[0].getText()))
			{
				return elements[1].getText();
			}
			if(parameterName == null && elements.length >= 1)
			{
				String text = elements[0].getText().trim();
				if(StringUtil.startsWithChar(text, '<') && StringUtil.endsWithChar(text, '>'))
				{
					return text;
				}
			}
		}
		return null;
	}

	public static void removeTypeArgsJavadocTags(final PsiMethod method) throws IncorrectOperationException
	{
		removeJavadocTags(method, TYPE_ARGS_TAG);
	}

	public static void removeJavadocTags(final PsiMethod method, final String tagName) throws IncorrectOperationException
	{
		PsiDocComment comment = method.getDocComment();
		if(comment != null)
		{
			PsiDocTag[] tags = comment.findTagsByName(tagName);
			for(PsiDocTag tag : tags)
			{
				tag.delete();
			}
			if(comment.getTags().length == 0)
			{
				boolean deleteComment = true;
				for(PsiElement element : comment.getDescriptionElements())
				{
					deleteComment &= CharArrayUtil.containsOnlyWhiteSpaces(element.getText());
				}
				if(deleteComment)
				{
					comment.delete();
				}
			}
		}
	}
}
