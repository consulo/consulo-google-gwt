package com.intellij.gwt.facet;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.ide.util.BrowseFilesListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.ComboboxWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public class GwtSdkPathEditor {
  private final ComboboxWithBrowseButton myPathEditor;

  public GwtSdkPathEditor(@Nullable Project project) {
    myPathEditor = new ComboboxWithBrowseButton();
    myPathEditor.addBrowseFolderListener(GwtBundle.message("gwt.installation.chooser.title"), GwtBundle.message("gwt.installation.chooser.description"),
                                         project, BrowseFilesListener.SINGLE_DIRECTORY_DESCRIPTOR, TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT);
    JComboBox comboBox = myPathEditor.getComboBox();
    comboBox.setEditable(true);
    for (String path : GwtSdkManager.getInstance().getAllSdkPaths()) {
      comboBox.addItem(path);
    }
  }

  public ComboboxWithBrowseButton getMainComponent() {
    return myPathEditor;
  }

  public JComboBox getComboBox() {
    return myPathEditor.getComboBox();
  }

  public String getPath() {
    return (String)myPathEditor.getComboBox().getEditor().getItem();
  }

  public void setPath(final String path) {
    myPathEditor.getComboBox().setSelectedItem(path);
  }

  public String getUrl() {
    return VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(getPath()));
  }

  public JTextField getPathTextField() {
    return (JTextField)getComboBox().getEditor().getEditorComponent();
  }
}
