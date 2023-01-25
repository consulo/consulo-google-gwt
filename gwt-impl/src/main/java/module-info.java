/**
 * @author VISTALL
 * @since 25/01/2023
 */
module com.intellij.gwt.base
{
	requires transitive com.intellij.gwt.api;
	requires transitive consulo.java.analysis.impl;
	requires consulo.java;
	requires com.intellij.properties;

	// TODO remove in future
	requires java.desktop;
	requires forms.rt;

	exports com.intellij.gwt.base.actions;
	exports com.intellij.gwt.base.i18n;
	exports com.intellij.gwt.base.inspections;
	exports com.intellij.gwt.base.make;
	exports com.intellij.gwt.base.module.index;
	exports com.intellij.gwt.base.rpc;
	exports com.intellij.gwt.base.sdk;
	exports com.intellij.gwt.base.templates;
	exports consulo.google.gwt.base.icon;
	exports consulo.gwt.base.module.extension;
	exports consulo.gwt.base.module.extension.impl;
	exports consulo.gwt.base.module.extension.path;
	exports consulo.gwt.base.sdk;
}