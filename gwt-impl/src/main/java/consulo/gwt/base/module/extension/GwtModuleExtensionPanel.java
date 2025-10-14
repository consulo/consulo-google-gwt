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

package consulo.gwt.base.module.extension;

import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.gwt.module.extension.GoogleGwtMutableModuleExtension;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.table.TableView;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

/**
 * @author nik
 */
public class GwtModuleExtensionPanel extends JPanel {
    private JComboBox myOutputStyleBox;
    private JPanel myMainPanel;
    private JCheckBox myRunGwtCompilerCheckbox;
    private JFormattedTextField myCompilerHeapSizeField;
    private JLabel myCompilerHeapSizeLabel;
    private JPanel myPackagingPathsPanel;
    private JTextField myAdditionalCompilerParametersField;
    private JTextField myAdditionalCompilerVmParametersField;
    private JLabel myAdditionalCompilerParametersLabel;
    private TextFieldWithBrowseButton myCompilerOutputDirField;
    private JLabel myCompilerOutputDirLabel;

    private final TableView<ModulePackagingInfo> myTableView;
    private ArrayList<ModulePackagingInfo> myModulePackagingInfos;
    private final ListTableModel<ModulePackagingInfo> myTableModel;
    private GoogleGwtMutableModuleExtension myExtension;

    public GwtModuleExtensionPanel(final GoogleGwtMutableModuleExtension extension) {
        myExtension = extension;
        $$$setupUI$$$();
        myCompilerOutputDirLabel.setLabelFor(myCompilerOutputDirField.getTextField());
        final Module module = extension.getModule();
        myCompilerOutputDirField.addBrowseFolderListener(extension.getProject(), new CompilerOutputBrowseFolderActionListener(extension.getProject(),
            module,
            myCompilerOutputDirField));

        for (GwtJavaScriptOutputStyle style : GwtJavaScriptOutputStyle.values()) {
            myOutputStyleBox.addItem(style);
        }
        myOutputStyleBox.setSelectedItem(extension.getOutputStyle());
        myRunGwtCompilerCheckbox.setSelected(extension.isRunGwtCompilerOnMake());
        myAdditionalCompilerParametersField.setText(extension.getAdditionalCompilerParameters());
        myAdditionalCompilerVmParametersField.setText(extension.getAdditionalVmCompilerParameters());
        String compilerOutputUrl = extension.getCompilerOutputUrl();
        myCompilerOutputDirField.setText(StringUtil.isEmpty(compilerOutputUrl) ? null : VirtualFileManager.extractPath(compilerOutputUrl));
        myCompilerOutputDirField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                String path = StringUtil.nullize(myCompilerOutputDirField.getText(), true);

                extension.setCompilerOutputUrl(path == null ? null : VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, FileUtil.toSystemIndependentName(path)));
            }
        });
        myCompilerHeapSizeField.setText(String.valueOf(extension.getCompilerMaxHeapSize()));
        myCompilerHeapSizeField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                Object value = myCompilerHeapSizeField.getValue();
                if (value == null) {
                    return;
                }
                extension.setCompilerMaxHeapSize(((Number) value).intValue());
            }
        });

        myAdditionalCompilerParametersField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                Document document = documentEvent.getDocument();
                try {
                    extension.setAdditionalCompilerParameters(document.getText(0, document.getLength()));
                }
                catch (BadLocationException ignored) {
                    //
                }
            }
        });

        myAdditionalCompilerVmParametersField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                Document document = documentEvent.getDocument();
                try {
                    extension.setAdditionalCompilerVmParameters(document.getText(0, document.getLength()));
                }
                catch (BadLocationException ignored) {
                    //
                }
            }
        });

        myRunGwtCompilerCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                onCompilerCheckboxChanged();
            }
        });

        final ColumnInfo[] columns = {
            MODULE_NAME_COLUMN,
            OUTPUT_PATH_COLUMN
        };
        myModulePackagingInfos = new ArrayList<ModulePackagingInfo>();
        myModulePackagingInfos = getPackagingInfos(module);
        myTableModel = new ListTableModel<ModulePackagingInfo>(columns, myModulePackagingInfos, 0);
        myTableView = new TableView<ModulePackagingInfo>(myTableModel);
        myPackagingPathsPanel.add(ScrollPaneFactory.createScrollPane(myTableView), BorderLayout.CENTER);

        int gwtModulesCount = GwtModulesManager.getInstance(module.getProject()).getGwtModules(module).length;
        int height = (myTableView.getRowHeight() + myTableView.getRowMargin()) * gwtModulesCount + myTableView.getTableHeader().getPreferredSize().height
            + 2;
        height = Math.max(height, myPackagingPathsPanel.getMinimumSize().height);
        height = Math.min(height, 300);
        myPackagingPathsPanel.setPreferredSize(new Dimension(myPackagingPathsPanel.getPreferredSize().width, height));

        myOutputStyleBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                extension.setOutputStyle((GwtJavaScriptOutputStyle) myOutputStyleBox.getSelectedItem());
            }
        });
    }

    public JComboBox getOutputStyleBox() {
        return myOutputStyleBox;
    }

    public JTextField getCompilerHeapSizeField() {
        return myCompilerHeapSizeField;
    }

    private GwtJavaScriptOutputStyle getOutputStyle() {
        return (GwtJavaScriptOutputStyle) myOutputStyleBox.getSelectedItem();
    }

    private JPanel getMainPanel() {
        return myMainPanel;
    }

    public JComponent createComponent() {
        return getMainPanel();
    }

    private void onCompilerCheckboxChanged() {
        boolean enabled = myRunGwtCompilerCheckbox.isSelected();

        myExtension.setRunGwtCompilerOnMake(enabled);
        myCompilerHeapSizeLabel.setEnabled(enabled);
        myCompilerHeapSizeField.setEnabled(enabled);
        myAdditionalCompilerParametersLabel.setEnabled(enabled);
        myAdditionalCompilerParametersField.setEnabled(enabled);
        myAdditionalCompilerVmParametersField.setEnabled(enabled);
        myCompilerOutputDirLabel.setEnabled(enabled);
        myCompilerOutputDirField.setEnabled(enabled);

        updatePackagingTable();
    }

    private void updatePackagingTable() {
        boolean enable = myRunGwtCompilerCheckbox.isSelected();
        myTableView.setEnabled(enable);
    }

/*	private boolean isPackagingSettingsModified()
    {
		Module module = myEditorContext.getModule();
		if(module == null)
		{
			return false;
		}

		ArrayList<ModulePackagingInfo> list = getPackagingInfos(module);
		return !Comparing.haveEqualElements(list, myModulePackagingInfos);
	}
         */
/*	public void apply() throws ConfigurationException
    {
		String gwtUrl = myGwtPathEditor.getUrl();

		myConfiguration.setGwtSdkUrl(gwtUrl);
		GwtSdkManager.getInstance().registerGwtSdk(gwtUrl);
		myConfiguration.setOutputStyle(getOutputStyle());
		myConfiguration.setWebFacetName(getSelectedWebFacet());
		myConfiguration.setRunGwtCompilerOnMake(myRunGwtCompilerCheckbox.isSelected());
		myConfiguration.setAdditionalCompilerParameters(myAdditionalCompilerVmParametersField.getText().trim());
		myConfiguration.setCompilerOutputPath(FileUtil.toSystemIndependentName(myCompilerOutputDirField.getText().trim()));
		try
		{
			myConfiguration.setCompilerMaxHeapSize(Integer.parseInt(myCompilerHeapSizeField.getText().trim()));
		}
		catch(NumberFormatException e)
		{
			//todo[nik] show warning
		}
		Module module = myEditorContext.getModule();
		if(module != null)
		{
			for(ModulePackagingInfo packagingInfo : myModulePackagingInfos)
			{
				myConfiguration.setPackagingRelativePath(packagingInfo.myModuleName, packagingInfo.myOutputPath);
			}
		}
	}     */


    private ArrayList<ModulePackagingInfo> getPackagingInfos(final Module module) {
        ArrayList<ModulePackagingInfo> list = new ArrayList<ModulePackagingInfo>();
        GwtModule[] modules = GwtModulesManager.getInstance(module.getProject()).getGwtModules(module);
        for (GwtModule gwtModule : modules) {
            list.add(new ModulePackagingInfo(gwtModule.getQualifiedName(), myExtension.getPackagingRelativePath(gwtModule)));
        }
        return list;
    }

    /**
     * Method generated by Consulo GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        myMainPanel.setLayout(new GridLayoutManager(7, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, GwtLocalize.labelSelectScriptOutputStyleText().get());
        myMainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myOutputStyleBox = new JComboBox();
        myMainPanel.add(myOutputStyleBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, -1), null, 0, false));
        myRunGwtCompilerCheckbox = new JCheckBox();
        myRunGwtCompilerCheckbox.setSelected(true);
        this.$$$loadButtonText$$$(myRunGwtCompilerCheckbox, GwtLocalize.checkboxRunGwtCompilerOnMake().get());
        myMainPanel.add(myRunGwtCompilerCheckbox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myCompilerHeapSizeLabel = new JLabel();
        this.$$$loadLabelText$$$(myCompilerHeapSizeLabel, GwtLocalize.labelTextGwtCompilerHeapSize().get());
        myMainPanel.add(myCompilerHeapSizeLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myMainPanel.add(myCompilerHeapSizeField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(25, -1), null, 0, false));
        myPackagingPathsPanel = new JPanel();
        myPackagingPathsPanel.setLayout(new BorderLayout(0, 0));
        myMainPanel.add(myPackagingPathsPanel, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 50), null, null, 0, false));
        myAdditionalCompilerParametersLabel = new JLabel();
        this.$$$loadLabelText$$$(myAdditionalCompilerParametersLabel, GwtLocalize.labelTextAdditionalVmCompilerParameters().get());
        myMainPanel.add(myAdditionalCompilerParametersLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myAdditionalCompilerVmParametersField = new JTextField();
        myMainPanel.add(myAdditionalCompilerVmParametersField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        myMainPanel.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        myCompilerOutputDirLabel = new JLabel();
        myCompilerOutputDirLabel.setText("Output directory:");
        myMainPanel.add(myCompilerOutputDirLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myCompilerOutputDirField = new TextFieldWithBrowseButton();
        myMainPanel.add(myCompilerOutputDirField, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, GwtLocalize.labelTextAdditionalCompilerParameters().get());
        myMainPanel.add(label2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        myAdditionalCompilerParametersField = new JTextField();
        myMainPanel.add(myAdditionalCompilerParametersField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        label1.setLabelFor(myOutputStyleBox);
        myCompilerHeapSizeLabel.setLabelFor(myCompilerHeapSizeField);
        myAdditionalCompilerParametersLabel.setLabelFor(myAdditionalCompilerVmParametersField);
        label2.setLabelFor(myAdditionalCompilerVmParametersField);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) {
                    break;
                }
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return myMainPanel;
    }

    private static class ModulePackagingInfo {
        private final String myModuleName;
        private String myOutputPath;

        private ModulePackagingInfo(final String moduleName, final String outputPath) {
            myModuleName = moduleName;
            myOutputPath = outputPath;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ModulePackagingInfo that = (ModulePackagingInfo) o;
            return myModuleName.equals(that.myModuleName) && myOutputPath.equals(that.myOutputPath);

        }

        @Override
        public int hashCode() {
            return 31 * myModuleName.hashCode() + myOutputPath.hashCode();
        }
    }

    private static final ColumnInfo<ModulePackagingInfo, String> MODULE_NAME_COLUMN = new ColumnInfo<ModulePackagingInfo,
        String>(GwtLocalize.tableColumnNameGwtModule().get()) {
        @Override
        public String valueOf(final ModulePackagingInfo modulePackagingInfo) {
            return modulePackagingInfo.myModuleName;
        }
    };

    private static final ColumnInfo<ModulePackagingInfo, String> OUTPUT_PATH_COLUMN = new ColumnInfo<ModulePackagingInfo,
        String>(GwtLocalize.tableColumnNameOutputRelativePath().get()) {
        @Override
        public String valueOf(final ModulePackagingInfo modulePackagingInfo) {
            return modulePackagingInfo.myOutputPath;
        }

        @Override
        public boolean isCellEditable(final ModulePackagingInfo modulePackagingInfo) {
            return true;
        }

        @Override
        public void setValue(final ModulePackagingInfo modulePackagingInfo, final String value) {
            modulePackagingInfo.myOutputPath = value;
        }
    };

    private static class CompilerOutputBrowseFolderActionListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> {
        private final Module myModule;

        public CompilerOutputBrowseFolderActionListener(final Project project, final Module module, final TextFieldWithBrowseButton textField) {
            super(null, GwtLocalize.fileChooserDescriptionSelectOutputDirectoryForFilesGeneratedByGwtCompiler().get(), textField, project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
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

    private void createUIComponents() {
        myMainPanel = this;
        myCompilerHeapSizeField = new JFormattedTextField(new NumberFormatter());
    }
}
