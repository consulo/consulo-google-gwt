/**
 * @author VISTALL
 * @since 25/01/2023
 */
module com.intellij.gwt.api
{
    requires transitive consulo.ide.api;
    requires transitive consulo.java.language.api;
    requires transitive com.intellij.xml;

    exports com.intellij.gwt.facet;
    exports com.intellij.gwt.module;
    exports com.intellij.gwt.module.model;
    exports com.intellij.gwt.sdk;
    exports consulo.gwt.module.extension;
    exports consulo.gwt.module.extension.path;
    exports consulo.google.gwt.localize;
}