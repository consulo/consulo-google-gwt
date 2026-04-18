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
	requires com.intellij.xml.html.api;

	requires consulo.application.api;
	requires consulo.application.content.api;
	requires consulo.compiler.api;
	requires consulo.container.api;
	requires consulo.file.chooser.api;
	requires consulo.file.template.api;
	requires consulo.index.io;
	requires consulo.language.api;
	requires consulo.language.code.style.api;
	requires consulo.language.editor.api;
	requires consulo.localize.api;
	requires consulo.logging.api;
	requires consulo.module.api;
	requires consulo.module.content.api;
	requires consulo.platform.api;
	requires consulo.project.api;
	requires consulo.ui.api;
	requires consulo.ui.ex.api;
	requires consulo.ui.ex.awt.api;
	requires consulo.util.io;
	requires consulo.util.lang;
	requires consulo.virtual.file.system.api;

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