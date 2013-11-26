package com.intellij.gwt.references;

import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class GwtModuleInStringLiteralReference extends GwtModuleReference<PsiLiteralExpression> {
  public GwtModuleInStringLiteralReference(final PsiLiteralExpression element) {
    super(element);
  }

  @Nullable
  protected String getStringValue() {
    Object value = myElement.getValue();
    return value instanceof String ? (String)value : null;
  }
}
