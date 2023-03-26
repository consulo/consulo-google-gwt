package com.intellij.gwt.impl.inspections;

import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.ConfigurableBuilderState;
import consulo.configurable.UnnamedConfigurable;
import consulo.google.gwt.localize.GwtLocalize;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Tag;

import javax.annotation.Nullable;

public class GwtSerializableInspectionState implements InspectionToolState<GwtSerializableInspectionState>
{
	private boolean myReportInterfaces = true;

	@Tag("report-interfaces")
	public boolean isReportInterfaces()
	{
		return myReportInterfaces;
	}

	public void setReportInterfaces(final boolean reportInterfaces)
	{
		myReportInterfaces = reportInterfaces;
	}

	@Nullable
	@Override
	public UnnamedConfigurable createConfigurable()
	{
		ConfigurableBuilder<ConfigurableBuilderState> builder = ConfigurableBuilder.newBuilder();
		builder.checkBox(GwtLocalize.checkboxTextReportInterfaces(), this::isReportInterfaces, this::setReportInterfaces);
		return builder.buildUnnamed();
	}

	@Nullable
	@Override
	public GwtSerializableInspectionState getState()
	{
		return this;
	}

	@Override
	public void loadState(GwtSerializableInspectionState state)
	{
		XmlSerializerUtil.copyBean(state, this);
	}
}
