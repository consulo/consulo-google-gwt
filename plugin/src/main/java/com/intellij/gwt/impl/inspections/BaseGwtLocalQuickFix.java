package com.intellij.gwt.impl.inspections;

import consulo.google.gwt.localize.GwtLocalize;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class BaseGwtLocalQuickFix implements LocalQuickFix {
    private final LocalizeValue myName;

    protected BaseGwtLocalQuickFix(final LocalizeValue name) {
        myName = name;
    }

    @Override
    @Nonnull
    public LocalizeValue getName() {
        return myName;
    }

    @Nonnull
    public LocalizeValue getFamilyName() {
        return GwtLocalize.quickFixesGwtFamilyName();
    }
}
