package com.intellij.gwt.impl.inspections;

import consulo.language.editor.inspection.InspectionToolState;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-08-23
 */
public class GwtSerializableInspectionStateProvider implements InspectionToolState<GwtSerializableInspectionState> {
    private GwtSerializableInspectionState myState = new GwtSerializableInspectionState();

    @Nullable
    @Override
    public GwtSerializableInspectionState getState() {
        return myState;
    }

    @Override
    public void loadState(GwtSerializableInspectionState state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
}
