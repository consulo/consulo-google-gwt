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

package com.intellij.gwt.run;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.index.GwtHtmlFileIndex;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.javaee.DeploymentDescriptorsConstants;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.RawCommandLineEditor;
import consulo.gwt.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.javaee.module.extension.JavaWebModuleExtension;

public class GwtRunConfigurationEditor extends SettingsEditor<GwtRunConfiguration>
{
	private DefaultComboBoxModel myModulesModel;
	private DefaultComboBoxModel myPagesModel;
	private JComboBox myModulesBox;
	private JPanel myMainPanel;
	private ComboboxWithBrowseButton myHtmlPageBox;
	private JLabel myHtmlToOpenLabel;
	private RawCommandLineEditor myVMParameters;
	private RawCommandLineEditor myGwtShellParameters;
	private JCheckBox myPatchWebXmlCheckBox;
	private TextFieldWithBrowseButton myWebXmlField;
	private Project myProject;
	private GwtModulesManager myGwtModulesManager;

	public GwtRunConfigurationEditor(Project project)
	{
		myProject = project;
		myGwtModulesManager = GwtModulesManager.getInstance(myProject);
		myVMParameters.setDialogCaption(GwtBundle.message("dialog.caption.vm.parameters"));
		myGwtShellParameters.setDialogCaption(GwtBundle.message("dialog.caption.gwt.shell.parameters"));
	}

	@Override
	public void resetEditorFrom(GwtRunConfiguration configuration)
	{
		myVMParameters.setText(configuration.VM_PARAMETERS);
		myGwtShellParameters.setText(configuration.SHELL_PARAMETERS);

		myModulesModel.removeAllElements();
		for(Module module : configuration.getValidModules())
		{
			myModulesModel.addElement(module);
		}
		Module module = configuration.getModule();
		myModulesModel.setSelectedItem(module);

		boolean customWebXml = configuration.CUSTOM_WEB_XML != null;
		myPatchWebXmlCheckBox.setSelected(customWebXml);
		myWebXmlField.setEnabled(customWebXml);
		if(customWebXml)
		{
			setCustomWebXml(configuration.CUSTOM_WEB_XML);
		}
		updateWebXmlPanel(module);

		fillPages(module);
		String pagePath = configuration.getPage();
		if(pagePath == null)
		{
			pagePath = "";
		}
		myHtmlPageBox.getComboBox().getEditor().setItem(pagePath);
	}

	private void setCustomWebXml(final String url)
	{
		myWebXmlField.setText(FileUtil.toSystemDependentName(VfsUtil.urlToPath(url)));
	}

	@Nullable
	private VirtualFile getFileByPagePath(final Module module, final String pagePath)
	{
		final int index = pagePath.indexOf('/');
		if(index == -1)
		{
			return null;
		}

		GwtModule gwtModule = myGwtModulesManager.findGwtModuleByName(pagePath.substring(0, index), GlobalSearchScope.moduleWithDependenciesScope
				(module));
		if(gwtModule == null)
		{
			return null;
		}

		String name = pagePath.substring(index + 1);
		final List<VirtualFile> publicRoots = gwtModule.getPublicRoots();
		for(VirtualFile root : publicRoots)
		{
			final VirtualFile file = root.findFileByRelativePath(name);
			if(file != null)
			{
				return file;
			}
		}
		return null;
	}

	@Nullable
	private String getPath(@NotNull GwtModule gwtModule, @NotNull VirtualFile file)
	{
		final String path = myGwtModulesManager.getPathFromPublicRoot(gwtModule, file);
		return path != null ? getPath(gwtModule, path) : null;
	}

	@NotNull
	public static String getPath(@NotNull GwtModule gwtModule, @NotNull String relativePath)
	{
		return gwtModule.getQualifiedName() + "/" + relativePath;
	}

	private void fillPages(final Module module)
	{
		myPagesModel.removeAllElements();
		if(module == null)
		{
			return;
		}

		final GwtModule[] modules = myGwtModulesManager.getGwtModules(module);
		for(GwtModule gwtModule : modules)
		{
			final Collection<VirtualFile> htmlFiles = GwtHtmlFileIndex.getHtmlFilesByModule(myProject, gwtModule.getQualifiedName());
			for(VirtualFile htmlFile : htmlFiles)
			{
				String path = getPath(gwtModule, htmlFile);
				if(path != null)
				{
					myPagesModel.addElement(path);
				}
			}
		}
	}

	@Override
	public void applyEditorTo(GwtRunConfiguration configuration) throws ConfigurationException
	{
		configuration.setModule(getSelectedModule());
		final String path = (String) myHtmlPageBox.getComboBox().getEditor().getItem();
		configuration.setPage(path);
		configuration.VM_PARAMETERS = myVMParameters.getText();
		configuration.SHELL_PARAMETERS = myGwtShellParameters.getText();
		if(myPatchWebXmlCheckBox.isSelected())
		{
			configuration.CUSTOM_WEB_XML = VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(myWebXmlField.getText()));
		}
		else
		{
			configuration.CUSTOM_WEB_XML = null;
		}
	}

	private Module getSelectedModule()
	{
		return (Module) myModulesBox.getSelectedItem();
	}

	@Override
	@NotNull
	public JComponent createEditor()
	{
		myModulesModel = new DefaultComboBoxModel();
		myModulesBox.setModel(myModulesModel);
		myPagesModel = new DefaultComboBoxModel();
		final JComboBox comboBox = myHtmlPageBox.getComboBox();
		comboBox.setEditable(true);
		comboBox.setModel(myPagesModel);
		myHtmlToOpenLabel.setLabelFor(comboBox);

		myPatchWebXmlCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				myWebXmlField.setEnabled(myPatchWebXmlCheckBox.isSelected());
			}
		});
		myWebXmlField.addBrowseFolderListener(null, null, myProject, createWebXmlChooserDescriptor());

		myModulesBox.setRenderer(new ListCellRendererWrapper()
		{
			@Override
			public void customize(JList jList, Object value, int i, boolean b, boolean b2)
			{
				final Module module = (Module) value;
				if(module != null)
				{
					setIcon(AllIcons.Nodes.Module);
					setText(module.getName());
				}
			}
		});

		myModulesBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Module module = (Module) myModulesModel.getSelectedItem();
				fillPages(module);
				updateWebXmlPanel(module);
			}
		});

		myHtmlPageBox.addBrowseFolderListener(myProject, new HtmlPageActionListener());

		return myMainPanel;
	}

	private FileChooserDescriptor createWebXmlChooserDescriptor()
	{
		FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
		{
			@Override
			public boolean isFileVisible(final VirtualFile file, final boolean showHiddenFiles)
			{
				return super.isFileVisible(file, showHiddenFiles) && (file.isDirectory() || file.getName().equals(DeploymentDescriptorsConstants
						.WEB_XML_META_DATA.getFileName()));
			}
		};
		final VirtualFile[] roots = ProjectRootManager.getInstance(myProject).getContentRoots();
		descriptor.setRoots(roots);
		return descriptor;
	}

	private void updateWebXmlPanel(final @Nullable Module module)
	{
		boolean visible = updateWebXmlField(module);
		myWebXmlField.setVisible(visible);
		myPatchWebXmlCheckBox.setVisible(visible);
	}

	private boolean updateWebXmlField(final @Nullable Module module)
	{
		if(module == null)
		{
			return false;
		}
		JavaWebModuleExtension javaWebModuleExtension = ModuleUtilCore.getExtension(module, JavaWebModuleExtension.class);
		JavaEEGoogleGwtModuleExtension facet = ModuleUtilCore.getExtension(module, JavaEEGoogleGwtModuleExtension.class);
		if(javaWebModuleExtension == null || facet == null)
		{
			return false;
		}

		if(myWebXmlField.getText().trim().length() == 0)
		{
			 /*
			ConfigFile descriptor = javaWebModuleExtension.getWebXmlDescriptor();
			if(descriptor != null)
			{
				setCustomWebXml(descriptor.getUrl());
			}  */
		}
		return true;
	}

	private FileChooserDescriptor createHtmlFileChooserDescriptor()
	{
		final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
		{
			@Override
			public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles)
			{
				return super.isFileVisible(file, showHiddenFiles) && (file.isDirectory() || FileTypeManager.getInstance().getFileTypeByFile(file) ==
						HtmlFileType.INSTANCE);
			}
		};
		final VirtualFile[] sourceRoots = ProjectRootManager.getInstance(myProject).getContentSourceRoots();
		descriptor.setRoots(sourceRoots);
		return descriptor;
	}

	@Override
	public void disposeEditor()
	{
	}

	private class RunPageComponentAccessor implements TextComponentAccessor<JComboBox>
	{
		@Override
		public String getText(final JComboBox component)
		{
			String pagePath = component.getEditor().getItem().toString();
			VirtualFile file = getFileByPagePath(getSelectedModule(), pagePath);
			return file != null ? file.getPath() : "";
		}

		@Override
		public void setText(final JComboBox component, final String text)
		{
			throw new UnsupportedOperationException();
		}
	}

	private class HtmlPageActionListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JComboBox>
	{
		public HtmlPageActionListener()
		{
			super(null, null, myHtmlPageBox, myProject, createHtmlFileChooserDescriptor(), new RunPageComponentAccessor());
		}

		@Override
		protected void onFileChosen(final VirtualFile chosenFile)
		{
			List<Pair<GwtModule, String>> pairs = myGwtModulesManager.findGwtModulesByPublicFile(chosenFile);
			Pair<GwtModule, String> pair = null;
			if(pairs.size() == 1)
			{
				pair = pairs.get(0);
			}
			else
			{
				String[] gwtModules = new String[pairs.size()];
				for(int i = 0; i < pairs.size(); i++)
				{
					gwtModules[i] = pairs.get(i).getFirst().getQualifiedName();
				}
				int answer = Messages.showChooseDialog(myMainPanel, GwtBundle.message("choose.text.select.gwt.module"),
						GwtBundle.message("dialog.title.choose.gwt.module"), gwtModules, gwtModules[0], null);
				if(answer >= 0)
				{
					pair = pairs.get(answer);
				}
			}
			if(pair != null)
			{
				myHtmlPageBox.getComboBox().getEditor().setItem(getPath(pair.getFirst(), pair.getSecond()));
			}
		}
	}
}
