package com.intellij.gwt.inspections;

import org.jetbrains.annotations.NotNull;
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
	@NotNull
	public String getName()
	{
		return myName;
	}

	@Override
	@NotNull
	public String getFamilyName()
	{
		return GwtBundle.message("quick.fixes.gwt.family.name");
	}
}
