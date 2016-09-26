package consulo.gwt.module.extension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import consulo.annotations.RequiredReadAction;
import consulo.gwt.module.extension.path.GwtLibraryPathProvider;

/**
 * @author VISTALL
 * @since 25.08.14
 */
public class GwtModuleExtensionUtil
{
	@Nullable
	public static GoogleGwtModuleExtension<?> findModuleExtension(@NotNull Project project, @Nullable VirtualFile file)
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

	public static boolean hasModuleExtension(final @NotNull Project project, final @Nullable VirtualFile file)
	{
		return findModuleExtension(project, file) != null;
	}

	@NotNull
	@RequiredReadAction
	public static GwtVersion getVersion(@Nullable PsiElement psiElement)
	{
		if(psiElement == null)
		{
			return GwtVersionImpl.VERSION_1_6_OR_LATER;
		}
		return getVersion(ModuleUtilCore.findModuleForPsiElement(psiElement));
	}

	@NotNull
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

	@NotNull
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
