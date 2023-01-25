package com.intellij.gwt.base.sdk;

import com.intellij.gwt.base.templates.GwtTemplates;
import com.intellij.gwt.sdk.GwtVersion;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public enum GwtVersionImpl implements GwtVersion
{
	VERSION_1_0,
	VERSION_FROM_1_1_TO_1_3,
	VERSION_1_4,
	VERSION_1_5,
	VERSION_1_6_OR_LATER;
	@NonNls
	public static final String GWT_15_COMPILER_MAIN_CLASS = "com.google.gwt.dev.GWTCompiler";
	@NonNls
	public static final String GWT_16_COMPILER_MAIN_CLASS = "com.google.gwt.dev.Compiler";

	private boolean isGwt14OrLater()
	{
		return this == VERSION_1_4 || isGwt15OrLater();
	}

	@Override
	@Nonnull
	public String getGwtModuleHtmlTemplate()
	{
		return isGwt14OrLater() ? GwtTemplates.GWT_MODULE_HTML_1_4 : GwtTemplates.GWT_MODULE_HTML;
	}

	@Override
	@Nonnull
	public String getGwtServiceJavaTemplate()
	{
		return this == VERSION_1_0 ? GwtTemplates.GWT_SERVICE_JAVA_1_0 : GwtTemplates.GWT_SERVICE_JAVA;
	}

	@Override
	public boolean isJavaIoSerializableSupported()
	{
		return isGwt14OrLater();
	}

	@Override
	public boolean isPrivateNoArgConstructorInSerializableClassAllowed()
	{
		return isGwt15OrLater();
	}

	private boolean isGwt15OrLater()
	{
		return this == VERSION_1_5 || this == VERSION_1_6_OR_LATER;
	}

	@Override
	public boolean isGenericsSupported()
	{
		return isGwt15OrLater();
	}

	@Override
	public boolean isNewExpressionInJavaScriptSupported()
	{
		return isGwt15OrLater();
	}

	@Override
	@Nonnull
	public String getCompilerClassName()
	{
		return this == VERSION_1_6_OR_LATER ? GWT_16_COMPILER_MAIN_CLASS : GWT_15_COMPILER_MAIN_CLASS;
	}

	@Override
	@Nonnull
	public String getShellClassName()
	{
		//todo[nik]
		//return this == VERSION_1_6_OR_LATER ? "com.google.gwt.dev.HostedMode" : "com.google.gwt.dev.GWTShell";
		return "com.google.gwt.dev.GWTShell";
	}

	@Override
	@Nonnull
	public String getCompilerOutputDirParameterName()
	{
		return this == VERSION_1_6_OR_LATER ? "-war" : "-out";
	}
}
