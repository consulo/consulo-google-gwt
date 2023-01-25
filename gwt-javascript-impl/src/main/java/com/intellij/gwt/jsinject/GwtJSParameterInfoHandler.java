package com.intellij.gwt.jsinject;

import com.intellij.java.impl.codeInsight.hint.api.impls.MethodParameterInfoHandler;
import com.intellij.java.language.psi.PsiMember;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiSubstitutor;
import com.intellij.javascript.JSParameterInfoHandler;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.javascript.language.JavaScriptLanguage;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.parameterInfo.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.util.collection.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtJSParameterInfoHandler implements ParameterInfoHandlerWithTabActionSupport<JSArgumentList, PsiMember, JSExpression>
{
	@Override
	public boolean couldShowInLookup()
	{
		return false;
	}

	@Override
	public Object[] getParametersForLookup(final LookupElement item, final ParameterInfoContext context)
	{
		return null;
	}

	@Override
	public Object[] getParametersForDocumentation(final PsiMember p, final ParameterInfoContext context)
	{
		if(p instanceof PsiMethod)
		{
			return ((PsiMethod) p).getParameterList().getParameters();
		}
		return ArrayUtil.EMPTY_OBJECT_ARRAY;
	}

	@Override
	public JSArgumentList findElementForParameterInfo(final CreateParameterInfoContext context)
	{
		JSArgumentList argumentList = JSParameterInfoHandler.findArgumentList(context.getFile(), context.getOffset());
		if(argumentList != null)
		{
			PsiElement parent = argumentList.getParent();
			if(parent instanceof JSCallExpression)
			{
				JSExpression expression = ((JSCallExpression) parent).getMethodExpression();
				GwtClassMemberReference gwtReference = getGwtClassMemberReference(expression);
				if(gwtReference != null)
				{
					PsiElement element = gwtReference.resolve();
					if(element != null)
					{
						context.setItemsToShow(new Object[]{element});
						return argumentList;
					}
				}
			}
		}
		return null;
	}

	@Nullable
	private static GwtClassMemberReference getGwtClassMemberReference(@Nullable JSExpression expression)
	{
		if(expression == null)
		{
			return null;
		}

		if(!(expression instanceof JSGwtReferenceExpressionImpl))
		{
			PsiElement child = expression.getLastChild();
			if(child instanceof JSGwtReferenceExpressionImpl)
			{
				expression = (JSGwtReferenceExpressionImpl) child;
			}
			else
			{
				return null;
			}
		}

		PsiReference[] references = expression.getReferences();
		PsiReference last = references[references.length - 1];
		return last instanceof GwtClassMemberReference ? (GwtClassMemberReference) last : null;
	}

	@Override
	public void showParameterInfo(@Nonnull final JSArgumentList element, final CreateParameterInfoContext context)
	{
		context.showHint(element, element.getTextOffset(), this);
	}

	@Override
	public JSArgumentList findElementForUpdatingParameterInfo(final UpdateParameterInfoContext context)
	{
		return JSParameterInfoHandler.findArgumentList(context.getFile(), context.getOffset());
	}

	@Override
	public void updateParameterInfo(@Nonnull final JSArgumentList o, final UpdateParameterInfoContext context)
	{
		context.setCurrentParameter(ParameterInfoUtils.getCurrentParameterIndex(o.getNode(), context.getOffset(), JSTokenTypes.COMMA));
	}

	@Override
	public String getParameterCloseChars()
	{
		return ParameterInfoUtils.DEFAULT_PARAMETER_CLOSE_CHARS;
	}

	@Override
	public boolean tracksParameterIndex()
	{
		return true;
	}

	@Override
	public void updateUI(final PsiMember p, final ParameterInfoUIContext context)
	{
		if(p instanceof PsiMethod)
		{
			MethodParameterInfoHandler.updateMethodPresentation((PsiMethod) p, PsiSubstitutor.EMPTY, context);
		}
		else
		{
			context.setupUIComponentPresentation(CodeInsightBundle.message("parameter.info.no.parameters"), -1, -1, false, false, false,
					context.getDefaultParameterColor());
		}
	}

	@Override
	@Nonnull
	public JSExpression[] getActualParameters(@Nonnull final JSArgumentList o)
	{
		return o.getArguments();
	}

	@Override
	@Nonnull
	public IElementType getActualParameterDelimiterType()
	{
		return JSTokenTypes.COMMA;
	}

	@Override
	@Nonnull
	public IElementType getActualParametersRBraceType()
	{
		return JSTokenTypes.RBRACE;
	}

	@Override
	@Nonnull
	public Set<Class<?>> getArgumentListAllowedParentClasses()
	{
		return Collections.singleton(JSCallExpression.class);
	}

	@Nonnull
	@Override
	public Set<? extends Class<?>> getArgListStopSearchClasses()
	{
		return Collections.emptySet();
	}

	@Override
	@Nonnull
	public Class<JSArgumentList> getArgumentListClass()
	{
		return JSArgumentList.class;
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return JavaScriptLanguage.INSTANCE;
	}
}
