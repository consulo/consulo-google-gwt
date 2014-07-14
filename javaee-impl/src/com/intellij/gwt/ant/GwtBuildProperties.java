package com.intellij.gwt.ant;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.google.gwt.module.extension.JavaEEGoogleGwtModuleExtension;
import com.intellij.compiler.ant.BuildProperties;

/**
 * @author nik
 */
public class GwtBuildProperties
{
	private GwtBuildProperties()
	{
	}

	@NotNull
	@NonNls
	public static String getGwtSdkHomeProperty()
	{
		return "gwt.sdk.home";
	}

	@NotNull
	@NonNls
	public static String getGwtSdkHomeProperty(@NotNull JavaEEGoogleGwtModuleExtension facet)
	{
		return "gwt.sdk.home." + getConvertedName(facet);
	}

	@NotNull
	@NonNls
	public static String getGwtSdkDevJarNameProperty()
	{
		return "gwt.sdk.dev.jar.name";
	}

	private static String getConvertedName(final JavaEEGoogleGwtModuleExtension facet)
	{
		return BuildProperties.convertName(facet.getModule().getName());
	}

	@NotNull
	@NonNls
	public static String getCompileGwtTargetName(@NotNull JavaEEGoogleGwtModuleExtension facet)
	{
		return "compile.gwt." + getConvertedName(facet);
	}

	@NotNull
	@NonNls
	public static String getRunGwtCompilerTargetName(@NotNull JavaEEGoogleGwtModuleExtension facet)
	{
		return "run.gwt.compiler." + getConvertedName(facet);
	}

	@NotNull
	@NonNls
	public static String getGwtCompilerOutputPropertyName(@NotNull JavaEEGoogleGwtModuleExtension facet)
	{
		return "gwt.compiler.output." + getConvertedName(facet);
	}

	@NotNull
	@NonNls
	public static String getOutputDirParameter()
	{
		return "gwt.output.dir";
	}

	@NotNull
	@NonNls
	public static String getGwtModuleParameter()
	{
		return "gwt.module.name";
	}
}
