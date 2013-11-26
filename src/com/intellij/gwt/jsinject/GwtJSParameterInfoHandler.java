package com.intellij.gwt.jsinject;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.api.impls.MethodParameterInfoHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.javascript.JSParameterInfoHandler;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSArgumentList;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.parameterInfo.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author nik
 */
public class GwtJSParameterInfoHandler implements ParameterInfoHandlerWithTabActionSupport<JSArgumentList, PsiMember, JSExpression> {
  public boolean couldShowInLookup() {
    return false;
  }

  public Object[] getParametersForLookup(final LookupElement item, final ParameterInfoContext context) {
    return null;
  }

  public Object[] getParametersForDocumentation(final PsiMember p, final ParameterInfoContext context) {
    if (p instanceof PsiMethod) {
      return ((PsiMethod)p).getParameterList().getParameters();
    }
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public JSArgumentList findElementForParameterInfo(final CreateParameterInfoContext context) {
    JSArgumentList argumentList = JSParameterInfoHandler.findArgumentList(context.getFile(), context.getOffset());
    if (argumentList != null) {
      PsiElement parent = argumentList.getParent();
      if (parent instanceof JSCallExpression) {
        JSExpression expression = ((JSCallExpression)parent).getMethodExpression();
        GwtClassMemberReference gwtReference = getGwtClassMemberReference(expression);
        if (gwtReference != null) {
          PsiElement element = gwtReference.resolve();
          if (element != null) {
            context.setItemsToShow(new Object[]{element});
            return argumentList;
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private static GwtClassMemberReference getGwtClassMemberReference(@Nullable JSExpression expression) {
    if (expression == null) return null;

    if (!(expression instanceof JSGwtReferenceExpressionImpl)) {
      PsiElement child = expression.getLastChild();
      if (child instanceof JSGwtReferenceExpressionImpl) {
        expression = (JSGwtReferenceExpressionImpl)child;
      }
      else {
        return null;
      }
    }

    PsiReference[] references = expression.getReferences();
    PsiReference last = references[references.length - 1];
    return last instanceof GwtClassMemberReference ? (GwtClassMemberReference)last : null;
  }

  public void showParameterInfo(@NotNull final JSArgumentList element, final CreateParameterInfoContext context) {
    context.showHint(element, element.getTextOffset(), this);
  }

  public JSArgumentList findElementForUpdatingParameterInfo(final UpdateParameterInfoContext context) {
    return JSParameterInfoHandler.findArgumentList(context.getFile(), context.getOffset());
  }

  public void updateParameterInfo(@NotNull final JSArgumentList o, final UpdateParameterInfoContext context) {
    context.setCurrentParameter(ParameterInfoUtils.getCurrentParameterIndex(o.getNode(), context.getOffset(), JSTokenTypes.COMMA));
  }

  public String getParameterCloseChars() {
    return ParameterInfoUtils.DEFAULT_PARAMETER_CLOSE_CHARS;
  }

  public boolean tracksParameterIndex() {
    return true;
  }

  public void updateUI(final PsiMember p, final ParameterInfoUIContext context) {
    if (p instanceof PsiMethod) {
      MethodParameterInfoHandler.updateMethodPresentation((PsiMethod)p, PsiSubstitutor.EMPTY, context);
    }
    else {
      context.setupUIComponentPresentation(CodeInsightBundle.message("parameter.info.no.parameters"), -1, -1, false, false, false,
                                           context.getDefaultParameterColor());
    }
  }

  @NotNull
  public JSExpression[] getActualParameters(@NotNull final JSArgumentList o) {
    return o.getArguments();
  }

  @NotNull
  public IElementType getActualParameterDelimiterType() {
    return JSTokenTypes.COMMA;
  }

  @NotNull
  public IElementType getActualParametersRBraceType() {
    return JSTokenTypes.RBRACE;
  }

  @NotNull
  public Set<Class> getArgumentListAllowedParentClasses() {
    return Collections.singleton((Class)JSCallExpression.class);
  }

  @NotNull
  public Class<JSArgumentList> getArgumentListClass() {
    return JSArgumentList.class;
  }
}
