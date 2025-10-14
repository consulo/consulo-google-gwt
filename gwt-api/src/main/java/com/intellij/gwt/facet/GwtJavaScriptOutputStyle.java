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

import consulo.google.gwt.localize.GwtLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public enum GwtJavaScriptOutputStyle {
    OBFUSCATED("OBF", GwtLocalize.scriptOutputStyleObfuscated(), 1),
    PRETTY("PRETTY", GwtLocalize.scriptOutputStylePretty(), 2),
    DETAILED("DETAILED", GwtLocalize.scriptOutputStyleDetailed(), 3);
    private
    String myId;
    private int myNumericId;
    private LocalizeValue myPresentableName;

    GwtJavaScriptOutputStyle(final @NonNls String id, final LocalizeValue presentableName, int numericId) {
        myPresentableName = presentableName;
        myId = id;
        myNumericId = numericId;
    }

    public String getId() {
        return myId;
    }

    public int getNumericId() {
        return myNumericId;
    }

    @Nonnull
    public LocalizeValue getPresentableName() {
        return myPresentableName;
    }

    @Nullable
    public static GwtJavaScriptOutputStyle byId(@Nullable String id) {
        for (GwtJavaScriptOutputStyle style : values()) {
            if (style.getId().equals(id)) {
                return style;
            }
        }
        return null;
    }

    @Nullable
    public static GwtJavaScriptOutputStyle byId(final int id) {
        for (GwtJavaScriptOutputStyle style : values()) {
            if (style.getNumericId() == id) {
                return style;
            }
        }
        return null;
    }
}
