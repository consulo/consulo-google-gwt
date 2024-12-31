package com.intellij.gwt.impl.inspections;

import com.intellij.gwt.GwtBundle;
import consulo.language.editor.inspection.LocalQuickFix;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class BaseGwtLocalQuickFix implements LocalQuickFix
{
	private String myName;

	protected BaseGwtLocalQuickFix(final String name)
	{
		myName = name;
	}

	@Override
	@Nonnull
	public String getName()
	{
		return myName;
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return GwtBundle.message("quick.fixes.gwt.family.name");
	}
}
