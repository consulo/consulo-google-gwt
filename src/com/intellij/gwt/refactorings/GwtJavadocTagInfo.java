package com.intellij.gwt.refactorings;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
*/
abstract class GwtJavadocTagInfo implements JavadocTagInfo {
  private String myName;

  GwtJavadocTagInfo(final @NonNls String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public boolean isInline() {
    return false;
  }

  public boolean isValidInContext(final PsiElement element) {
    return element instanceof PsiMethod && isValidFor((PsiMethod)element);
  }

  protected abstract boolean isValidFor(final @NotNull PsiMethod psiMethod);

  public Object[] getPossibleValues(final PsiElement context, final PsiElement place, final String prefix) {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public String checkTagValue(final PsiDocTagValue value) {
    return null;
  }

  public PsiReference getReference(final PsiDocTagValue value) {
    return null;
  }
}
