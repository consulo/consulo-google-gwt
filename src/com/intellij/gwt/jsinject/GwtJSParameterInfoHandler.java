package com.intellij.gwt.jsinject;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.api.impls.MethodParameterInfoHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.javascript.JSParameterInfoHandler;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithTabActionSupport;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.ParameterInfoUtils;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;

/**
 * @author nik
 */
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
	public void showParameterInfo(@NotNull final JSArgumentList element, final CreateParameterInfoContext context)
	{
		context.showHint(element, element.getTextOffset(), this);
	}

	@Override
	public JSArgumentList findElementForUpdatingParameterInfo(final UpdateParameterInfoContext context)
	{
		return JSParameterInfoHandler.findArgumentList(context.getFile(), context.getOffset());
	}

	@Override
	public void updateParameterInfo(@NotNull final JSArgumentList o, final UpdateParameterInfoContext context)
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
	@NotNull
	public JSExpression[] getActualParameters(@NotNull final JSArgumentList o)
	{
		return o.getArguments();
	}

	@Override
	@NotNull
	public IElementType getActualParameterDelimiterType()
	{
		return JSTokenTypes.COMMA;
	}

	@Override
	@NotNull
	public IElementType getActualParametersRBraceType()
	{
		return JSTokenTypes.RBRACE;
	}

	@Override
	@NotNull
	public Set<Class> getArgumentListAllowedParentClasses()
	{
		return Collections.singleton((Class) JSCallExpression.class);
	}

	@Override
	@NotNull
	public Class<JSArgumentList> getArgumentListClass()
	{
		return JSArgumentList.class;
	}
}
