/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.impl.ui.FacetEditorContextBase;
import com.intellij.facet.ui.*;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtSdk;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
public class GwtFacetEditor extends FacetEditorTab {
  @NonNls private static final String NONE = GwtBundle.message("gwt.package.web.facet.none");
  private JComboBox myOutputStyleBox;
  private JPanel myMainPanel;
  private JCheckBox myRunGwtCompilerCheckbox;
  private JComboBox myWebFacetBox;
  private JPanel myWebFacetPanel;
  private JTextField myCompilerHeapSizeField;
  private JLabel myCompilerHeapSizeLabel;
  private JPanel myGwtPathPanel;
  private JPanel myPackagingPathsPanel;
  private JLabel myWebFacetLabel;
  private JTextField myAdditionalCompilerParametersField;
  private JLabel myAdditionalCompilerParametersLabel;
  private TextFieldWithBrowseButton myCompilerOutputDirField;
  private JLabel myCompilerOutputDirLabel;
  private final FacetEditorContext myEditorContext;
  private final GwtSdkPathEditor myGwtPathEditor;
  private final FacetValidatorsManager myValidatorsManager;
  private final GwtFacetConfiguration myConfiguration;
  private final TableView<ModulePackagingInfo> myTableView;
  private ArrayList<ModulePackagingInfo> myModulePackagingInfos;
  private final ListTableModel<ModulePackagingInfo> myTableModel;

  public GwtFacetEditor(final FacetEditorContext editorContext, final FacetValidatorsManager validatorsManager, GwtFacetConfiguration configuration) {
    myEditorContext = editorContext;
    myValidatorsManager = validatorsManager;
    myConfiguration = configuration;

    Project project = editorContext.getProject();
    myGwtPathEditor = new GwtSdkPathEditor(project);
    myGwtPathPanel.add(myGwtPathEditor.getMainComponent(), BorderLayout.CENTER);

    myCompilerOutputDirLabel.setLabelFor(myCompilerOutputDirField.getTextField());
    final Module module = editorContext.getModule();
    myCompilerOutputDirField.addBrowseFolderListener(project, new CompilerOutputBrowseFolderActionListener(project, module,
                                                                                                           myCompilerOutputDirField));

    if (module != null) {
      myWebFacetBox.addItem(NONE);
      for (WebFacet webFacet : editorContext.getFacetsProvider().getFacetsByType(module, WebFacet.ID)) {
        myWebFacetBox.addItem(webFacet.getName());
      }
      myWebFacetBox.setSelectedIndex(0);
    }
    else {
      myWebFacetPanel.setVisible(false);
    }

    for (GwtJavaScriptOutputStyle style : GwtJavaScriptOutputStyle.values()) {
      myOutputStyleBox.addItem(style);
    }

    myWebFacetBox.setRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list,
                                                    final Object value,
                                                    final int index, final boolean isSelected, final boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof String) {
          setText((String)value);
          WebFacet webFacet = FacetManager.getInstance(module).findFacet(WebFacet.ID, (String)value);
          if (webFacet != null) {
            setIcon(webFacet.getType().getIcon());
          }
        }
        return this;
      }
    });
    myWebFacetBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        updatePackagingTable();
      }
    });

    validatorsManager.registerValidator(new FacetEditorValidator() {
      public ValidationResult check() {
        String path = myGwtPathEditor.getPath();
        ValidationResult result = GwtSdkUtil.checkGwtSdkPath(path);
        if (result.isOk()) {
          result = checkUserJarLibrary(path);
        }
        return result;
      }
    }, myGwtPathEditor.getComboBox());

    myRunGwtCompilerCheckbox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        onCompilerCheckboxChanged();
      }
    });

    final ColumnInfo[] columns = {MODULE_NAME_COLUMN, OUTPUT_PATH_COLUMN};
    myModulePackagingInfos = new ArrayList<ModulePackagingInfo>();
    myTableModel = new ListTableModel<ModulePackagingInfo>(columns, myModulePackagingInfos, 0);
    myTableView = new TableView<ModulePackagingInfo>(myTableModel);
    myPackagingPathsPanel.add(ScrollPaneFactory.createScrollPane(myTableView), BorderLayout.CENTER);

    if (module != null) {
      int gwtModulesCount = GwtModulesManager.getInstance(module.getProject()).getGwtModules(module).length;
      int height = (myTableView.getRowHeight()+myTableView.getRowMargin()) * gwtModulesCount + myTableView.getTableHeader().getPreferredSize().height + 2;
      height = Math.max(height, myPackagingPathsPanel.getMinimumSize().height);
      height = Math.min(height, 300);
      myPackagingPathsPanel.setPreferredSize(new Dimension(myPackagingPathsPanel.getPreferredSize().width, height));
    }
    else {
      myPackagingPathsPanel.setVisible(false);
    }
  }

  public JComboBox getOutputStyleBox() {
    return myOutputStyleBox;
  }

  public JTextField getCompilerHeapSizeField() {
    return myCompilerHeapSizeField;
  }

  public GwtSdkPathEditor getGwtPathEditor() {
    return myGwtPathEditor;
  }

  private ValidationResult checkUserJarLibrary(final String sdkPath) {
    final String userJarPath = GwtSdkUtil.getUserJarPath(sdkPath);
    final VirtualFile userJar = JarFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(userJarPath) + JarFileSystem.JAR_SEPARATOR);
    ModuleRootModel rootModel = myEditorContext.getRootModel();
    if (rootModel == null || userJar == null) return ValidationResult.OK;

    LibrarySearchingPolicy searchingPolicy = new LibrarySearchingPolicy(userJar);
    boolean found = searchingPolicy.containsLibrary(rootModel);
    if (found) {
      return ValidationResult.OK;
    }

    String errorMessage;
    final LibraryOrderEntry gwtLibrary = searchingPolicy.getGwtLibrary();
    if (gwtLibrary == null) {
      errorMessage = GwtBundle.message("error.message.gwt.user.jar.library.not.found.in.dependencies.of.module");
    }
    else {
      errorMessage = GwtBundle.message("error.message.gwt.user.jar.in.library.0.doesn.t.correspond.to.selected.gwt.installation",
                                       gwtLibrary.getPresentableName());
    }

    return new ValidationResult(errorMessage, new FacetConfigurationQuickFix() {
      public void run(final JComponent place) {
        ModifiableRootModel modifiableRootModel = myEditorContext.getModifiableRootModel();
        if (modifiableRootModel != null) {
          if (gwtLibrary != null && gwtLibrary.isValid()) {
            modifiableRootModel.removeOrderEntry(gwtLibrary);
          }
          final LibrariesContainer container = ((FacetEditorContextBase)myEditorContext).getContainer();
          Library library = GwtSdkUtil.findOrCreateGwtUserLibrary(container, userJar);
          modifiableRootModel.addLibraryEntry(library);
          myValidatorsManager.validate();
        }
      }
    });
  }

  private GwtJavaScriptOutputStyle getOutputStyle() {
    return (GwtJavaScriptOutputStyle)myOutputStyleBox.getSelectedItem();
  }

  private JPanel getMainPanel() {
    return myMainPanel;
  }

  public JComponent createComponent() {
    return getMainPanel();
  }

  private void onCompilerCheckboxChanged() {
    boolean enabled = myRunGwtCompilerCheckbox.isSelected();
    myCompilerHeapSizeLabel.setEnabled(enabled);
    myCompilerHeapSizeField.setEnabled(enabled);
    myAdditionalCompilerParametersLabel.setEnabled(enabled);
    myAdditionalCompilerParametersField.setEnabled(enabled);
    myCompilerOutputDirLabel.setEnabled(enabled);
    myCompilerOutputDirField.setEnabled(enabled);
    myWebFacetBox.setEnabled(enabled);
    myWebFacetLabel.setEnabled(enabled);
    updatePackagingTable();
  }

  private void updatePackagingTable() {
    boolean enable = myRunGwtCompilerCheckbox.isSelected() && getSelectedWebFacet() != null;
    myTableView.setEnabled(enable);
  }

  public boolean isModified() {
    return !myConfiguration.getGwtSdkPath().equals(myGwtPathEditor.getPath())
           || myConfiguration.getOutputStyle() != getOutputStyle()
           || myConfiguration.isRunGwtCompilerOnMake() != myRunGwtCompilerCheckbox.isSelected()
           || !Comparing.equal(myConfiguration.getWebFacetName(), getSelectedWebFacet())
           || !Comparing.equal(myConfiguration.getCompilerOutputPath(), FileUtil.toSystemIndependentName(myCompilerOutputDirField.getText()))
           || !myConfiguration.getAdditionalCompilerParameters().equals(myAdditionalCompilerParametersField.getText())
           || !String.valueOf(myConfiguration.getCompilerMaxHeapSize()).equals(myCompilerHeapSizeField.getText())
           || isPackagingSettingsModified() || myTableView.isEditing();
  }

  private boolean isPackagingSettingsModified() {
    Module module = myEditorContext.getModule();
    if (module == null) return false;

    ArrayList<ModulePackagingInfo> list = getPackagingInfos(module);
    return !Comparing.haveEqualElements(list, myModulePackagingInfos);
  }

  public void apply() throws ConfigurationException {
    String gwtUrl = myGwtPathEditor.getUrl();

    myConfiguration.setGwtSdkUrl(gwtUrl);
    GwtSdkManager.getInstance().registerGwtSdk(gwtUrl);
    myConfiguration.setOutputStyle(getOutputStyle());
    myConfiguration.setWebFacetName(getSelectedWebFacet());
    myConfiguration.setRunGwtCompilerOnMake(myRunGwtCompilerCheckbox.isSelected());
    myConfiguration.setAdditionalCompilerParameters(myAdditionalCompilerParametersField.getText().trim());
    myConfiguration.setCompilerOutputPath(FileUtil.toSystemIndependentName(myCompilerOutputDirField.getText().trim()));
    try {
      myConfiguration.setCompilerMaxHeapSize(Integer.parseInt(myCompilerHeapSizeField.getText().trim()));
    }
    catch (NumberFormatException e) {
      //todo[nik] show warning
    }
    Module module = myEditorContext.getModule();
    if (module != null) {
      for (ModulePackagingInfo packagingInfo : myModulePackagingInfos) {
        myConfiguration.setPackagingRelativePath(packagingInfo.myModuleName, packagingInfo.myOutputPath);
      }
    }
  }

  @Nullable
  private String getSelectedWebFacet() {
    String name = (String)myWebFacetBox.getSelectedItem();
    return NONE.equals(name) ? null : name;
  }


  public void reset() {
    myGwtPathEditor.setPath(myConfiguration.getGwtSdkPath());
    myOutputStyleBox.setSelectedItem(myConfiguration.getOutputStyle());
    myRunGwtCompilerCheckbox.setSelected(myConfiguration.isRunGwtCompilerOnMake());
    String webFacetName = myConfiguration.getWebFacetName();
    myWebFacetBox.setSelectedItem(webFacetName != null ? webFacetName : NONE);
    myAdditionalCompilerParametersField.setText(myConfiguration.getAdditionalCompilerParameters());
    myCompilerOutputDirField.setText(myConfiguration.getCompilerOutputPath());
    myCompilerHeapSizeField.setText(String.valueOf(myConfiguration.getCompilerMaxHeapSize()));
    onCompilerCheckboxChanged();

    Module module = myEditorContext.getModule();
    if (module != null) {
      myModulePackagingInfos = getPackagingInfos(module);
      myTableModel.setItems(myModulePackagingInfos);
      myTableView.revalidate();
      myTableView.repaint();
    }

    if (myEditorContext.isNewFacet() && myConfiguration.getGwtSdkPath().length() == 0) {
      GwtSdk gwtSdk = GwtSdkManager.getInstance().suggestGwtSdk();
      if (gwtSdk != null) {
        myGwtPathEditor.setPath(VfsUtil.urlToPath(gwtSdk.getHomeDirectoryUrl()));
      }
    }
  }

  private ArrayList<ModulePackagingInfo> getPackagingInfos(final Module module) {
    ArrayList<ModulePackagingInfo> list = new ArrayList<ModulePackagingInfo>();
    GwtModule[] modules = GwtModulesManager.getInstance(module.getProject()).getGwtModules(module);
    for (GwtModule gwtModule : modules) {
      list.add(new ModulePackagingInfo(gwtModule.getQualifiedName(), myConfiguration.getPackagingRelativePath(gwtModule)));
    }
    return list;
  }

  public void disposeUIResources() {
  }


  @Nls
  public String getDisplayName() {
    return GwtBundle.message("google.web.toolkit.title");
  }

  @Nullable
  @NonNls
  public String getHelpTopic() {
    return "reference.settings.project.modules.gwt.facet";
  }

  @Override
  public void onFacetInitialized(@NotNull final Facet facet) {
    ((GwtFacet)facet).updateCompilerOutputWatchRequest();
  }

  private class LibrarySearchingPolicy extends RootPolicy<Boolean> {
    private final VirtualFile myUserJar;
    private final Set<Module> myProcessedModules = new HashSet<Module>();
    private final List<LibraryOrderEntry> myGwtLibraries = new ArrayList<LibraryOrderEntry>();

    public LibrarySearchingPolicy(final VirtualFile userJar) {
      myUserJar = userJar;
    }

    public Boolean visitLibraryOrderEntry(final LibraryOrderEntry libraryOrderEntry, Boolean value) {
      Library library = libraryOrderEntry.getLibrary();
      if (library != null) {
        VirtualFile[] files = myEditorContext.getLibraryFiles(library, OrderRootType.CLASSES);
        for (VirtualFile file : files) {
          if (file.equals(myUserJar)) {
            return true;
          }
        }
        if (files.length == 1 && LibraryUtil.isClassAvailableInLibrary(new VirtualFile[]{files[0]}, GwtSdkUtil.GWT_CLASS_NAME)) {
          myGwtLibraries.add(libraryOrderEntry);
        }
      }
      return value;
    }

    public Boolean visitModuleOrderEntry(final ModuleOrderEntry moduleOrderEntry, final Boolean value) {
      Module module = moduleOrderEntry.getModule();
      if (module != null && myProcessedModules.add(module)) {
        ModuleRootModel dependency = myEditorContext.getModulesProvider().getRootModel(module);
        if (dependency != null) {
          return dependency.processOrder(this, value);
        }
      }
      return value;
    }

    @Nullable
    public LibraryOrderEntry getGwtLibrary() {
      return myGwtLibraries.size() == 1 ? myGwtLibraries.get(0) : null;
    }

    public boolean containsLibrary(final ModuleRootModel rootModel) {
      return rootModel.processOrder(this, false);
    }
  }

  private static class ModulePackagingInfo {
    private final String myModuleName;
    private String myOutputPath;

    private ModulePackagingInfo(final String moduleName, final String outputPath) {
      myModuleName = moduleName;
      myOutputPath = outputPath;
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ModulePackagingInfo that = (ModulePackagingInfo)o;
      return myModuleName.equals(that.myModuleName) && myOutputPath.equals(that.myOutputPath);

    }

    public int hashCode() {
      return 31 * myModuleName.hashCode() + myOutputPath.hashCode();
    }
  }

  private static final ColumnInfo<ModulePackagingInfo, String> MODULE_NAME_COLUMN = new ColumnInfo<ModulePackagingInfo, String>(GwtBundle.message("table.column.name.gwt.module")) {
    public String valueOf(final ModulePackagingInfo modulePackagingInfo) {
      return modulePackagingInfo.myModuleName;
    }
  };

  private static final ColumnInfo<ModulePackagingInfo, String> OUTPUT_PATH_COLUMN = new ColumnInfo<ModulePackagingInfo, String>(GwtBundle.message("table.column.name.output.relative.path")) {
    public String valueOf(final ModulePackagingInfo modulePackagingInfo) {
      return modulePackagingInfo.myOutputPath;
    }

    public boolean isCellEditable(final ModulePackagingInfo modulePackagingInfo) {
      return true;
    }

    public void setValue(final ModulePackagingInfo modulePackagingInfo, final String value) {
      modulePackagingInfo.myOutputPath = value;
    }
  };

  private static class CompilerOutputBrowseFolderActionListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> {
    private final Module myModule;

    public CompilerOutputBrowseFolderActionListener(final Project project, final Module module, final TextFieldWithBrowseButton textField) {
      super(null, GwtBundle.message("file.chooser.description.select.output.directory.for.files.generated.by.gwt.compiler"), textField,
            project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
      myModule = module;
    }

    @Override
    protected VirtualFile getInitialFile() {
      if (StringUtil.isEmpty(getComponentText())) {
        VirtualFile[] roots = ModuleRootManager.getInstance(myModule).getContentRoots();
        if (roots.length > 0) {
          return roots[0];
        }
      }
      return super.getInitialFile();
    }
  }

}
