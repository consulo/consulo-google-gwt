/**
 * @author VISTALL
 * @since 25/01/2023
 */
module com.intellij.gwt.jakartaee.impl {
    requires transitive consulo.ide.api;
    requires transitive com.intellij.gwt.base;
    requires transitive consulo.jakartaee.web.api;
    requires transitive consulo.java.execution.api;
    requires com.intellij.xml.html.api;
    requires consulo.java;

    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.configurable.api;
    requires consulo.disposer.api;
    requires consulo.execution.api;
    requires consulo.file.chooser.api;
    requires consulo.language.api;
    requires consulo.localize.api;
    requires consulo.logging.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.module.ui.api;
    requires consulo.process.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.util.io;
    requires consulo.util.jdom;
    requires consulo.util.lang;
    requires consulo.util.xml.serializer;
    requires consulo.virtual.file.system.api;

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