package com.intellij.gwt.sdk.impl;

import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.gwt.sdk.GwtVersion;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public enum GwtVersionImpl implements GwtVersion{
  VERSION_1_0,
  VERSION_FROM_1_1_TO_1_3,
  VERSION_1_4,
  VERSION_1_5,
  VERSION_1_6_OR_LATER;
  @NonNls public static final String GWT_15_COMPILER_MAIN_CLASS = "com.google.gwt.dev.GWTCompiler";
  @NonNls public static final String GWT_16_COMPILER_MAIN_CLASS = "com.google.gwt.dev.Compiler";

  private boolean isGwt14OrLater() {
    return this == VERSION_1_4 || isGwt15OrLater();
  }

  @NotNull
  public String getGwtModuleHtmlTemplate() {
    return isGwt14OrLater() ? GwtTemplates.GWT_MODULE_HTML_1_4 : GwtTemplates.GWT_MODULE_HTML;
  }

  @NotNull
  public String getGwtServiceJavaTemplate() {
    return this == VERSION_1_0 ? GwtTemplates.GWT_SERVICE_JAVA_1_0 : GwtTemplates.GWT_SERVICE_JAVA;
  }

  public boolean isJavaIoSerializableSupported() {
    return isGwt14OrLater();
  }

  public boolean isPrivateNoArgConstructorInSerializableClassAllowed() {
    return isGwt15OrLater();
  }

  private boolean isGwt15OrLater() {
    return this == VERSION_1_5 || this == VERSION_1_6_OR_LATER;
  }

  public boolean isGenericsSupported() {
    return isGwt15OrLater();
  }

  public boolean isNewExpressionInJavaScriptSupported() {
    return isGwt15OrLater();
  }

  @NotNull
  public String getCompilerClassName() {
    return this == VERSION_1_6_OR_LATER ? GWT_16_COMPILER_MAIN_CLASS : GWT_15_COMPILER_MAIN_CLASS;
  }

  @NotNull
  public String getShellClassName() {
    //todo[nik] 
    //return this == VERSION_1_6_OR_LATER ? "com.google.gwt.dev.HostedMode" : "com.google.gwt.dev.GWTShell";
    return "com.google.gwt.dev.GWTShell";
  }

  @NotNull
  public String getCompilerOutputDirParameterName() {
    return this == VERSION_1_6_OR_LATER ? "-war" : "-out";
  }
}
