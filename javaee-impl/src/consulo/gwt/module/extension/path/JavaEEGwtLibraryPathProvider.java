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

package consulo.gwt.module.extension.path;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.consulo.module.extension.ModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.mustbe.consulo.extensions.CompositeExtensionPointName;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.roots.ModuleRootLayer;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.util.ArchiveVfsUtil;
import com.intellij.util.ArrayUtil;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;

/**
 * @author VISTALL
 * @since 25-May-16
 */
public class JavaEEGwtLibraryPathProvider implements GwtLibraryPathProvider
{
	@Nullable
	@Override
	public Info resolveInfo(@NotNull GoogleGwtModuleExtension<?> extension)
	{
		ModuleExtension mavenExtension = extension.getModuleRootLayer().getExtension("maven");
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
			@NotNull
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

	@CompositeExtensionPointName.BooleanBreakResult(breakValue = false)
	public boolean canChooseBundle(@NotNull ModuleRootLayer layer)
	{
		ModuleExtension mavenExtension = layer.getExtension("maven");
		return mavenExtension == null;
	}
}
