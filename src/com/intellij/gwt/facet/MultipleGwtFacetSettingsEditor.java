package com.intellij.gwt.facet;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.FacetEditorsFactory;
import com.intellij.facet.ui.MultipleFacetEditorHelper;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;

/**
 * @author nik
 */
public class MultipleGwtFacetSettingsEditor extends MultipleFacetSettingsEditor
{
	private GwtFacetCommonSettingsPanel myCommonSettingsPanel;
	private MultipleFacetEditorHelper myHelper;

	public MultipleGwtFacetSettingsEditor(final Project project, final FacetEditor[] editors)
	{
		myCommonSettingsPanel = new GwtFacetCommonSettingsPanel(project);

		myHelper = FacetEditorsFactory.getInstance().createMultipleFacetEditorHelper();
		myHelper.bind(myCommonSettingsPanel.getCompilerHeapSizeField(), editors, new NotNullFunction<FacetEditor, JTextField>()
		{
			@Override
			@NotNull
			public JTextField fun(final FacetEditor facetEditor)
			{
				return facetEditor.getEditorTab(GwtFacetEditor.class).getCompilerHeapSizeField();
			}
		});
		myHelper.bind(myCommonSettingsPanel.getOutputStyleComboBox(), editors, new NotNullFunction<FacetEditor, JComboBox>()
		{
			@Override
			@NotNull
			public JComboBox fun(final FacetEditor facetEditor)
			{
				return facetEditor.getEditorTab(GwtFacetEditor.class).getOutputStyleBox();
			}
		});
		myHelper.bind(myCommonSettingsPanel.getSdkPathEditor().getPathTextField(), editors, new NotNullFunction<FacetEditor, JTextField>()
		{
			@Override
			@NotNull
			public JTextField fun(final FacetEditor facetEditor)
			{
				return facetEditor.getEditorTab(GwtFacetEditor.class).getGwtPathEditor().getPathTextField();
			}
		});
	}

	public JComponent createComponent()
	{
		return myCommonSettingsPanel.getMainPanel();
	}

	public void disposeUIResources()
	{
		myHelper.unbind();
	}
}
