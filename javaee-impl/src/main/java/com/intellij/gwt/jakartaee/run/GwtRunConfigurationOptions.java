/*
 * Copyright 2013-2026 consulo.io
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
package com.intellij.gwt.jakartaee.run;

import com.dslplatform.json.CompiledJson;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@CompiledJson
public class GwtRunConfigurationOptions extends RunConfigurationOptions {
    private @Nullable String myPage;
    private @Nullable String myVmParameters;
    private @Nullable String myShellParameters;
    private @Nullable String myCustomWebXml;

    @Attribute("page")
    public @Nullable String getPage() {
        return myPage;
    }

    public void setPage(@Nullable String page) {
        myPage = StringUtil.nullize(page, true);
    }

    @Attribute("vmParameters")
    public @Nullable String getVmParameters() {
        return myVmParameters;
    }

    public void setVmParameters(@Nullable String vmParameters) {
        myVmParameters = StringUtil.nullize(vmParameters, true);
    }

    @Attribute("shellParameters")
    public @Nullable String getShellParameters() {
        return myShellParameters;
    }

    public void setShellParameters(@Nullable String shellParameters) {
        myShellParameters = StringUtil.nullize(shellParameters, true);
    }

    /**
     * The URL of the web.xml whose content replaces the module one, {@code null} for the module web.xml.
     */
    @Attribute("customWebXml")
    public @Nullable String getCustomWebXml() {
        return myCustomWebXml;
    }

    public void setCustomWebXml(@Nullable String customWebXml) {
        myCustomWebXml = StringUtil.nullize(customWebXml, true);
    }
}
