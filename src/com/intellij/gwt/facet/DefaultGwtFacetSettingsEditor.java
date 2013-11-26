package com.intellij.gwt.facet;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.facet.ui.DefaultFacetSettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author nik
 */
public class DefaultGwtFacetSettingsEditor extends DefaultFacetSettingsEditor {
  private final GwtFacetConfiguration myConfiguration;
  private final GwtFacetCommonSettingsPanel myPanel;

  public DefaultGwtFacetSettingsEditor(@NotNull final Project project, @NotNull GwtFacetConfiguration configuration) {
    myConfiguration = configuration;
    myPanel = new GwtFacetCommonSettingsPanel(project);
  }

  public JComponent createComponent() {
    return myPanel.getMainPanel();
  }

  public void disposeUIResources() {
  }

  public void reset() {
    myPanel.getSdkPathEditor().setPath(myConfiguration.getGwtSdkPath());
    myPanel.getOutputStyleComboBox().setSelectedItem(myConfiguration.getOutputStyle());
    myPanel.getCompilerHeapSizeField().setText(String.valueOf(myConfiguration.getCompilerMaxHeapSize()));
  }

  public void apply() throws ConfigurationException {
    myConfiguration.setGwtSdkUrl(myPanel.getSdkPathEditor().getUrl());
    myConfiguration.setOutputStyle(getOutputStyle());
    try {
      myConfiguration.setCompilerMaxHeapSize(Integer.parseInt(myPanel.getCompilerHeapSizeField().getText()));
    }
    catch (NumberFormatException e) {
    }
  }

  public boolean isModified() {
    return !myConfiguration.getGwtSdkPath().equals(myPanel.getSdkPathEditor().getPath()) ||
           !myConfiguration.getOutputStyle().equals(getOutputStyle()) ||
           !String.valueOf(myConfiguration.getCompilerMaxHeapSize()).equals(myPanel.getCompilerHeapSizeField().getText());
  }

  private GwtJavaScriptOutputStyle getOutputStyle() {
    return (GwtJavaScriptOutputStyle)myPanel.getOutputStyleComboBox().getSelectedItem();
  }
}
