package consulo.gwt.base.module.extension;

import com.intellij.gwt.base.sdk.GwtVersionImpl;
import com.intellij.gwt.sdk.GwtVersion;
import consulo.annotation.access.RequiredReadAction;
import consulo.gwt.module.extension.GoogleGwtModuleExtension;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25.08.14
 */
public class GwtModuleExtensionUtil
{
	@Nullable
	public static GoogleGwtModuleExtension<?> findModuleExtension(@Nonnull Project project, @Nullable VirtualFile file)
	{
		if(file == null)
		{
			return null;
		}

		final Module module = ModuleUtilCore.findModuleForFile(file, project);
		if(module == null)
		{
			return null;
		}

		return ModuleUtilCore.getExtension(module, GoogleGwtModuleExtension.class);
	}

	public static boolean hasModuleExtension(final @Nonnull Project project, final @Nullable VirtualFile file)
	{
		return findModuleExtension(project, file) != null;
	}

	@Nonnull
	@RequiredReadAction
	public static GwtVersion getVersion(@Nullable PsiElement psiElement)
	{
		if(psiElement == null)
		{
			return GwtVersionImpl.VERSION_1_6_OR_LATER;
		}
		return getVersion(ModuleUtilCore.findModuleForPsiElement(psiElement));
	}

	@Nonnull
	@RequiredReadAction
	public static GwtVersion getVersion(@Nullable Module module)
	{
		if(module == null)
		{
			return GwtVersionImpl.VERSION_1_6_OR_LATER;
		}
		GoogleGwtModuleExtension extension = ModuleUtilCore.getExtension(module, GoogleGwtModuleExtension.class);
		return getVersion(extension);
	}

	@Nonnull
	@RequiredReadAction
	public static GwtVersion getVersion(@Nullable GoogleGwtModuleExtension<?> extension)
	{
		if(extension == null)
		{
			return GwtVersionImpl.VERSION_1_6_OR_LATER;
		}
		GwtLibraryPathProvider.Info info = GwtLibraryPathProvider.EP_NAME.computeSafeIfAny(it -> it.resolveInfo(extension));
		assert info != null;
		return info.getVersion();
	}
}
