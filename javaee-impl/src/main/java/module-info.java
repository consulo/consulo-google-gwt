/**
 * @author VISTALL
 * @since 25/01/2023
 */
module com.intellij.gwt.jakartaee.impl
{
	requires transitive consulo.ide.api;
	requires transitive com.intellij.gwt.base;
	requires transitive consulo.jakartaee.web.api;
	requires transitive consulo.java.execution.api;
	requires consulo.java;

	// TODO remove in future
	requires java.desktop;
	requires forms.rt;

	exports com.intellij.gwt.jakartaee;
	exports com.intellij.gwt.jakartaee.actions;
	exports com.intellij.gwt.jakartaee.junit;
	exports com.intellij.gwt.jakartaee.rpc;
	exports com.intellij.gwt.jakartaee.run;
	exports consulo.gwt.jakartaee.module.extension;
	exports consulo.gwt.jakartaee.run;
}