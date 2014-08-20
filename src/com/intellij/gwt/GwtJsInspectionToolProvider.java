package com.intellij.gwt;

import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.gwt.inspections.GwtJavaScriptReferencesInspection;

/**
 * @author VISTALL
 * @since 20.08.14
 */
public class GwtJsInspectionToolProvider implements InspectionToolProvider
{
	@Override
	public Class[] getInspectionClasses()
	{
		return new Class[]{GwtJavaScriptReferencesInspection.class};
	}
}
