package com.intellij.gwt.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.gwt.GwtBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public abstract class BaseGwtLocalQuickFix implements LocalQuickFix {
  private String myName;

  protected BaseGwtLocalQuickFix(final String name) {
    myName = name;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public String getFamilyName() {
    return GwtBundle.message("quick.fixes.gwt.family.name");
  }
}
