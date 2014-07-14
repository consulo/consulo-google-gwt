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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mustbe.consulo.google.gwt.module.extension.JavaEEGoogleGwtMutableModuleExtension;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;

/**
 * @author nik
 */
public class GwtFacetEditor extends JPanel
{
	private JComboBox myOutputStyleBox;
	private JPanel myMainPanel;
	private JCheckBox myRunGwtCompilerCheckbox;
	private JTextField myCompilerHeapSizeField;
	private JLabel myCompilerHeapSizeLabel;
	private JPanel myPackagingPathsPanel;
	private JTextField myAdditionalCompilerParametersField;
	private JLabel myAdditionalCompilerParametersLabel;
	private TextFieldWithBrowseButton myCompilerOutputDirField;
	private JLabel myCompilerOutputDirLabel;

	private final TableView<ModulePackagingInfo> myTableView;
	private ArrayList<ModulePackagingInfo> myModulePackagingInfos;
	private final ListTableModel<ModulePackagingInfo> myTableModel;
	private JavaEEGoogleGwtMutableModuleExtension myExtension;

	public GwtFacetEditor(final JavaEEGoogleGwtMutableModuleExtension extension)
	{
		myExtension = extension;
		myCompilerOutputDirLabel.setLabelFor(myCompilerOutputDirField.getTextField());
		final Module module = extension.getModule();
		myCompilerOutputDirField.addBrowseFolderListener(extension.getProject(), new CompilerOutputBrowseFolderActionListener(extension.getProject(),
				module,
				myCompilerOutputDirField));

		for(GwtJavaScriptOutputStyle style : GwtJavaScriptOutputStyle.values())
		{
			myOutputStyleBox.addItem(style);
		}
		myOutputStyleBox.setSelectedItem(extension.getOutputStyle());
		myRunGwtCompilerCheckbox.setSelected(extension.isRunGwtCompilerOnMake());
		myAdditionalCompilerParametersField.setText(extension.getAdditionalCompilerParameters());
		myCompilerOutputDirField.setText(extension.getCompilerOutputPath());
		myCompilerHeapSizeField.setText(String.valueOf(extension.getCompilerMaxHeapSize()));


		myRunGwtCompilerCheckbox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
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

		myOutputStyleBox.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				extension.setOutputStyle((GwtJavaScriptOutputStyle) myOutputStyleBox.getSelectedItem());
			}
		});
	}

	public JComboBox getOutputStyleBox()
	{
		return myOutputStyleBox;
	}

	public JTextField getCompilerHeapSizeField()
	{
		return myCompilerHeapSizeField;
	}

	private GwtJavaScriptOutputStyle getOutputStyle()
	{
		return (GwtJavaScriptOutputStyle) myOutputStyleBox.getSelectedItem();
	}

	private JPanel getMainPanel()
	{
		return myMainPanel;
	}

	public JComponent createComponent()
	{
		return getMainPanel();
	}

	private void onCompilerCheckboxChanged()
	{
		boolean enabled = myRunGwtCompilerCheckbox.isSelected();

		myExtension.setRunGwtCompilerOnMake(enabled);
		myCompilerHeapSizeLabel.setEnabled(enabled);
		myCompilerHeapSizeField.setEnabled(enabled);
		myAdditionalCompilerParametersLabel.setEnabled(enabled);
		myAdditionalCompilerParametersField.setEnabled(enabled);
		myCompilerOutputDirLabel.setEnabled(enabled);
		myCompilerOutputDirField.setEnabled(enabled);

		updatePackagingTable();
	}

	private void updatePackagingTable()
	{
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
		myConfiguration.setAdditionalCompilerParameters(myAdditionalCompilerParametersField.getText().trim());
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


	private ArrayList<ModulePackagingInfo> getPackagingInfos(final Module module)
	{
		ArrayList<ModulePackagingInfo> list = new ArrayList<ModulePackagingInfo>();
		GwtModule[] modules = GwtModulesManager.getInstance(module.getProject()).getGwtModules(module);
		for(GwtModule gwtModule : modules)
		{
			list.add(new ModulePackagingInfo(gwtModule.getQualifiedName(), myExtension.getPackagingRelativePath(gwtModule)));
		}
		return list;
	}

	public void disposeUIResources()
	{
	}

	private static class ModulePackagingInfo
	{
		private final String myModuleName;
		private String myOutputPath;

		private ModulePackagingInfo(final String moduleName, final String outputPath)
		{
			myModuleName = moduleName;
			myOutputPath = outputPath;
		}

		@Override
		public boolean equals(final Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(o == null || getClass() != o.getClass())
			{
				return false;
			}

			final ModulePackagingInfo that = (ModulePackagingInfo) o;
			return myModuleName.equals(that.myModuleName) && myOutputPath.equals(that.myOutputPath);

		}

		@Override
		public int hashCode()
		{
			return 31 * myModuleName.hashCode() + myOutputPath.hashCode();
		}
	}

	private static final ColumnInfo<ModulePackagingInfo, String> MODULE_NAME_COLUMN = new ColumnInfo<ModulePackagingInfo,
			String>(GwtBundle.message("table.column.name.gwt.module"))
	{
		@Override
		public String valueOf(final ModulePackagingInfo modulePackagingInfo)
		{
			return modulePackagingInfo.myModuleName;
		}
	};

	private static final ColumnInfo<ModulePackagingInfo, String> OUTPUT_PATH_COLUMN = new ColumnInfo<ModulePackagingInfo,
			String>(GwtBundle.message("table.column.name.output.relative.path"))
	{
		@Override
		public String valueOf(final ModulePackagingInfo modulePackagingInfo)
		{
			return modulePackagingInfo.myOutputPath;
		}

		@Override
		public boolean isCellEditable(final ModulePackagingInfo modulePackagingInfo)
		{
			return true;
		}

		@Override
		public void setValue(final ModulePackagingInfo modulePackagingInfo, final String value)
		{
			modulePackagingInfo.myOutputPath = value;
		}
	};

	private static class CompilerOutputBrowseFolderActionListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField>
	{
		private final Module myModule;

		public CompilerOutputBrowseFolderActionListener(final Project project, final Module module, final TextFieldWithBrowseButton textField)
		{
			super(null, GwtBundle.message("file.chooser.description.select.output.directory.for.files.generated.by.gwt.compiler"), textField, project,
					FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
			myModule = module;
		}

		@Override
		protected VirtualFile getInitialFile()
		{
			if(StringUtil.isEmpty(getComponentText()))
			{
				VirtualFile[] roots = ModuleRootManager.getInstance(myModule).getContentRoots();
				if(roots.length > 0)
				{
					return roots[0];
				}
			}
			return super.getInitialFile();
		}
	}

	private void createUIComponents()
	{
		myMainPanel = this;
	}
}
