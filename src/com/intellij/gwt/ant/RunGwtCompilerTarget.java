package com.intellij.gwt.ant;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.Tag;
import com.intellij.compiler.ant.taskdefs.PathElement;
import com.intellij.compiler.ant.taskdefs.PathRef;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.make.GwtCompiler;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author nik
 */
public class RunGwtCompilerTarget extends Target
{
	private final GwtFacet myFacet;
	private final GwtFacetConfiguration myConfiguration;

	public RunGwtCompilerTarget(GwtFacet facet, final GenerationOptions genOptions)
	{
		super(GwtBuildProperties.getRunGwtCompilerTargetName(facet), null, GwtBundle.message("ant.target.name.run.gwt.compiler"), null);
		myFacet = facet;
		myConfiguration = myFacet.getConfiguration();
		addJavaTag(genOptions);
	}

	private void addJavaTag(final GenerationOptions genOptions)
	{
		Module module = myFacet.getModule();
		List<Pair> options = new ArrayList<Pair>();
		options.add(pair("fork", "true"));
		String chunkName = genOptions.getChunkByModule(module).getName();
		options.add(pair("jvm", BuildProperties.propertyRef(BuildProperties.getModuleChunkJdkBinProperty(chunkName)) + "/java"));
		final GwtVersion sdkVersion = myFacet.getSdkVersion();
		options.add(pair("classname", sdkVersion.getCompilerClassName()));

		Tag java = new Tag("java", options.toArray(new Pair[options.size()]));

		java.add(jvmarg("-Xmx" + String.valueOf(myConfiguration.getCompilerMaxHeapSize()) + "m"));
		String jvmParameters = myConfiguration.getAdditionalCompilerParameters();
		if(!StringUtil.isEmpty(jvmParameters))
		{
			java.add(jvmarg(jvmParameters));
		}

		Tag classpath = new Tag("classpath");
		classpath.add(new PathElement(BuildProperties.propertyRef(GwtBuildProperties.getGwtSdkHomeProperty(myFacet)) + "/" + BuildProperties.propertyRef
				(GwtBuildProperties.getGwtSdkDevJarNameProperty())));
		classpath.add(new PathRef(BuildProperties.getSourcepathProperty(chunkName)));
		classpath.add(new PathRef(BuildProperties.getRuntimeClasspathProperty(chunkName)));
		java.add(classpath);

		java.add(arg(GwtCompiler.LOG_LEVEL_ARGUMENT));
		java.add(arg("WARN"));
		java.add(arg(sdkVersion.getCompilerOutputDirParameterName()));
		java.add(arg(BuildProperties.propertyRef(GwtBuildProperties.getGwtCompilerOutputPropertyName(myFacet))));
		java.add(arg(GwtCompiler.STYLE_ARGUMENT));
		java.add(arg(myConfiguration.getOutputStyle().getId()));
		java.add(arg(BuildProperties.propertyRef(GwtBuildProperties.getGwtModuleParameter())));
		add(java);
	}

	private static Tag arg(final @NotNull @NonNls String arg)
	{
		return new Tag("arg", pair("value", arg));
	}

	private static Tag jvmarg(final @NotNull @NonNls String arg)
	{
		return new Tag("jvmarg", pair("line", arg));
	}
}
