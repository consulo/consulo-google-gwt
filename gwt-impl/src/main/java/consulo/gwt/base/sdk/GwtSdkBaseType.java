/*
 * Copyright 2013-2016 must-be.org
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

package consulo.gwt.base.sdk;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.java.language.impl.JarArchiveFileType;
import com.intellij.java.language.projectRoots.JavaSdkType;
import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkType;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 25-May-16
 */
public abstract class GwtSdkBaseType extends SdkType {
    private static final Set<String> ourAllowedRootTypeIds = Set.of(
        BinariesOrderRootType.ID,
        SourcesOrderRootType.ID
    );

    public GwtSdkBaseType(@Nonnull String id, @Nonnull LocalizeValue displayName, @Nonnull Image icon) {
        super(id, displayName, icon);
    }

    @Override
    public void setupSdkPaths(Sdk sdk) {
        SdkModificator sdkModificator = sdk.getSdkModificator();

        VirtualFile homeDirectory = sdk.getHomeDirectory();
        if (homeDirectory == null) {
            sdkModificator.commitChanges();
            return;
        }

        for (VirtualFile virtualFile : homeDirectory.getChildren()) {
            String name = virtualFile.getName();

            // skip compiler library
            if (StringUtil.startsWith(name, "vaadin-client-compiler")) {
                continue;
            }

            if (virtualFile.getFileType() == JarArchiveFileType.INSTANCE) {
                VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
                if (archiveRootForLocalFile == null) {
                    continue;
                }

                sdkModificator.addRoot(archiveRootForLocalFile, BinariesOrderRootType.ID);

                if (StringUtil.startsWith(name, "vaadin-client")) {
                    sdkModificator.addRoot(archiveRootForLocalFile, SourcesOrderRootType.ID);
                }
            }
        }

        VirtualFile libFile = homeDirectory.findChild("lib");
        if (libFile != null) {
            for (VirtualFile virtualFile : libFile.getChildren()) {
                if (virtualFile.getFileType() == JarArchiveFileType.INSTANCE) {
                    VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
                    if (archiveRootForLocalFile == null) {
                        continue;
                    }

                    sdkModificator.addRoot(archiveRootForLocalFile, BinariesOrderRootType.ID);
                }
            }
        }

        sdkModificator.commitChanges();
    }

    @Override
    public boolean isRootTypeApplicable(String type) {
        return ourAllowedRootTypeIds.contains(type);
    }

    @Nonnull
    public abstract GwtVersion getVersion(Sdk sdk);

    @Nullable
    public abstract String getDevJarPath(Sdk sdk);

    @Nullable
    public abstract String getUserJarPath(Sdk sdk);
}
