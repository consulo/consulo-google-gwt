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

package consulo.gwt.jakartaee.maven;

import com.intellij.gwt.base.sdk.GwtVersionImpl;
import consulo.annotation.component.ExtensionImpl;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.jakartaee.module.extension.JavaEEGoogleGwtModuleExtension;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.maven.rt.server.common.model.MavenArtifact;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.util.collection.ArrayUtil;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import org.jdom.Element;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 25-May-16
 */
@ExtensionImpl(id = "mavenJavaEE", order = "before defaultGwt")
public class JavaEEGwtLibraryPathProvider implements GwtLibraryPathProvider
{
	@Nullable
	@Override
	public Info resolveInfo(@Nonnull GoogleGwtModuleExtension<?> extension)
	{
		if(!(extension instanceof JavaEEGoogleGwtModuleExtension))
		{
			return null;
		}

		ModuleExtension mavenExtension = extension.getModule().getExtension("maven");
		if(mavenExtension == null)
		{
			return null;
		}

		MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(extension.getProject());
		MavenProject project = mavenProjectsManager.findProject(extension.getModule());
		if(project == null)
		{
			return null;
		}

		List<MavenArtifact> dependencies = project.findDependencies("com.google.gwt", "gwt-user");
		if(dependencies.isEmpty())
		{
			return new Info(GwtVersionImpl.VERSION_1_6_OR_LATER, null, null);
		}

		MavenArtifact mavenArtifact = dependencies.get(0);
		final File file = mavenArtifact.getFile();

		VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
		if(virtualFile == null)
		{
			return new Info(GwtVersionImpl.VERSION_1_6_OR_LATER, null, null);
		}

		VirtualFile archiveRootForLocalFile = ArchiveVfsUtil.getArchiveRootForLocalFile(virtualFile);
		if(archiveRootForLocalFile == null)
		{
			return new Info(GwtVersionImpl.VERSION_1_6_OR_LATER, null, null);
		}

		String ver = mavenArtifact.getVersion();
		final String devPrefix = "com/google/gwt/gwt-dev/" + ver + "/" + "gwt-dev-" + ver;
		final File localRepository = mavenProjectsManager.getLocalRepository();
		File devJar = new File(localRepository, devPrefix + ".jar");
		return new Info(GwtVersionImpl.VERSION_1_6_OR_LATER, file.getPath(), devJar.getPath())
		{
			/**
			 * gwt from maven central not contains asm library inside - but have dependency to last asm library
			 * we need found it and add to compilation classpath
			 */
			@Nonnull
			@Override
			public List<String> getAdditionalClasspath()
			{
				List<String> paths = new ArrayList<String>();
				File pomFile = new File(localRepository, devPrefix + ".pom");
				if(pomFile.exists())
				{
					try
					{
						Element element = JDOMUtil.load(pomFile);
						for(Element dependencies : element.getChildren("dependencies", null))
						{
							for(Element dependency : dependencies.getChildren("dependency", null))
							{
								String groupId = dependency.getChildText("groupId", null);
								String artifactId = dependency.getChildText("artifactId", null);

								File directory = new File(localRepository, groupId.replace(".", "/") + "/" + artifactId);
								if(directory.exists())
								{
									String[] list = directory.list();
									Arrays.sort(list);
									String lastElement = ArrayUtil.getLastElement(list);
									if(lastElement == null)
									{
										continue;
									}

									File jarFile = new File(directory, lastElement + "/" + artifactId + "-" + lastElement + ".jar");
									if(jarFile.exists())
									{
										paths.add(jarFile.getPath());
									}
								}
							}
						}
					}
					catch(Exception ignored)
					{
					}
				}
				return paths;
			}
		};
	}

	public boolean canChooseBundle(@Nonnull ModuleRootLayer layer)
	{
		ModuleExtension mavenExtension = layer.getExtension("maven");
		return mavenExtension == null;
	}
}
