package consulo.gwt.module.extension;

import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import consulo.annotation.access.RequiredReadAction;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;

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

		final Module module = ModuleUtil.findModuleForFile(file, project);
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
		GwtLibraryPathProvider.Info info = GwtLibraryPathProvider.EP_NAME.composite().resolveInfo(extension);
		assert info != null;
		return info.getVersion();
	}
}
