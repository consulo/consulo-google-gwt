package consulo.vaadin.maven;

import com.intellij.openapi.module.Module;
import consulo.maven.importing.MavenImporterFromBuildPlugin;
import consulo.vaadin.module.extension.JavaEEVaadinModuleExtension;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-22
 */
public class ConsuloInternalVaadinImporter extends MavenImporterFromBuildPlugin
{
	public ConsuloInternalVaadinImporter()
	{
		super("consulo.internal.vaadin", "vaadin-maven-plugin");
	}

	@Override
	public void preProcess(Module module, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges, MavenModifiableModelsProvider mavenModifiableModelsProvider)
	{
	}

	@Override
	public void process(MavenModifiableModelsProvider mavenModifiableModelsProvider,
						Module module,
						MavenRootModelAdapter mavenRootModelAdapter,
						MavenProjectsTree mavenProjectsTree,
						MavenProject mavenProject,
						MavenProjectChanges mavenProjectChanges,
						Map<MavenProject, String> map,
						List<MavenProjectsProcessorTask> list)
	{
		enableModuleExtension(module, mavenModifiableModelsProvider, JavaEEVaadinModuleExtension.class);
	}
}
