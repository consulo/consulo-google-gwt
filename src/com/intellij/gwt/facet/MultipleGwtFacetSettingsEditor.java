package com.intellij.gwt.facet;

import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.MultipleFacetEditorHelper;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author nik
 */
public class MultipleGwtFacetSettingsEditor extends MultipleFacetSettingsEditor {
  private GwtFacetCommonSettingsPanel myCommonSettingsPanel;
  private MultipleFacetEditorHelper myHelper;

  public MultipleGwtFacetSettingsEditor(final Project project, final FacetEditor[] editors) {
    myCommonSettingsPanel = new GwtFacetCommonSettingsPanel(project);

    myHelper = FacetEditorsFactory.getInstance().createMultipleFacetEditorHelper();
    myHelper.bind(myCommonSettingsPanel.getCompilerHeapSizeField(), editors, new NotNullFunction<FacetEditor, JTextField>() {
      @NotNull
      public JTextField fun(final FacetEditor facetEditor) {
        return facetEditor.getEditorTab(GwtFacetEditor.class).getCompilerHeapSizeField();
      }
    });
    myHelper.bind(myCommonSettingsPanel.getOutputStyleComboBox(), editors, new NotNullFunction<FacetEditor, JComboBox>() {
      @NotNull
      public JComboBox fun(final FacetEditor facetEditor) {
        return facetEditor.getEditorTab(GwtFacetEditor.class).getOutputStyleBox();
      }
    });
    myHelper.bind(myCommonSettingsPanel.getSdkPathEditor().getPathTextField(), editors, new NotNullFunction<FacetEditor, JTextField>() {
      @NotNull
      public JTextField fun(final FacetEditor facetEditor) {
        return facetEditor.getEditorTab(GwtFacetEditor.class).getGwtPathEditor().getPathTextField();
      }
    });
  }

  public JComponent createComponent() {
    return myCommonSettingsPanel.getMainPanel();
  }

  public void disposeUIResources() {
    myHelper.unbind();
  }
}
