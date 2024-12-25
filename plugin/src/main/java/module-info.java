/**
 * @author VISTALL
 * @since 25/01/2023
 */
module com.intellij.gwt
{
    requires com.intellij.gwt.api;
    requires com.intellij.gwt.base;

    requires consulo.java;
    requires com.intellij.properties;
    requires consulo.java.properties.impl;

    opens com.intellij.gwt.impl.inspections to consulo.util.xml.serializer;

    // TODO remove in future
    requires java.desktop;
    requires forms.rt;
}