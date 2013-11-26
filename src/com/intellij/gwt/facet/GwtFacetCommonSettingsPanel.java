package com.intellij.gwt.facet;

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;

/**
 * @author nik
 */
public class GwtFacetCommonSettingsPanel
{
	private JPanel myMainPanel;
	private JPanel mySdkPathPanel;
	private JComboBox myOutputStyleComboBox;
	private JTextField myCompilerHeapSizeField;
	private GwtSdkPathEditor mySdkPathEditor;

	public GwtFacetCommonSettingsPanel(@NotNull Project project)
	{
		mySdkPathEditor = new GwtSdkPathEditor(project);
		mySdkPathPanel.add(mySdkPathEditor.getMainComponent(), BorderLayout.CENTER);

		for(GwtJavaScriptOutputStyle outputStyle : GwtJavaScriptOutputStyle.values())
		{
			myOutputStyleComboBox.addItem(outputStyle);
		}
	}

	public JPanel getMainPanel()
	{
		return myMainPanel;
	}

	public JComboBox getOutputStyleComboBox()
	{
		return myOutputStyleComboBox;
	}

	public JTextField getCompilerHeapSizeField()
	{
		return myCompilerHeapSizeField;
	}

	public GwtSdkPathEditor getSdkPathEditor()
	{
		return mySdkPathEditor;
	}
}
