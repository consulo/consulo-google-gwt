package com.intellij.gwt.inspections;

import javax.annotation.Nonnull;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.gwt.GwtBundle;

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
