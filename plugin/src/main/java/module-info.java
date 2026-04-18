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

    requires com.intellij.xml.html.api;

    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.code.editor.api;
    requires consulo.compiler.api;
    requires consulo.component.api;
    requires consulo.configurable.api;
    requires consulo.datacontext.api;
    requires consulo.document.api;
    requires consulo.file.editor.api;
    requires consulo.file.template.api;
    requires consulo.language.api;
    requires consulo.language.code.style.api;
    requires consulo.language.editor.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.editor.ui.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.navigation.api;
    requires consulo.process.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.usage.api;
    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.util.xml.serializer;
    requires consulo.virtual.file.system.api;

    opens com.intellij.gwt.impl.inspections to consulo.util.xml.serializer;

    // TODO remove in future
    requires java.desktop;
    requires forms.rt;
}