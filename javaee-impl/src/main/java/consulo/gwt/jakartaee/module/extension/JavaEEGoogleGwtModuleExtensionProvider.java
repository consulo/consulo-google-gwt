package consulo.gwt.jakartaee.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.google.gwt.base.icon.GwtIconGroup;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
@ExtensionImpl
public class JavaEEGoogleGwtModuleExtensionProvider implements ModuleExtensionProvider<JavaEEGoogleGwtModuleExtension>
{
	@Nonnull
	@Override
	public String getId()
	{
		return "javaee-google-gwt";
	}

	@Nullable
	@Override
	public String getParentId()
	{
		return "java-web";
	}

	@Nonnull
	@Override
	public LocalizeValue getName()
	{
		return LocalizeValue.localizeTODO("GWT");
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return GwtIconGroup.gwt();
	}

	@Nonnull
	@Override
	public ModuleExtension<JavaEEGoogleGwtModuleExtension> createImmutableExtension(@Nonnull ModuleRootLayer moduleRootLayer)
	{
		return new JavaEEGoogleGwtModuleExtension(getId(), moduleRootLayer);
	}

	@Nonnull
	@Override
	public MutableModuleExtension<JavaEEGoogleGwtModuleExtension> createMutableExtension(@Nonnull ModuleRootLayer moduleRootLayer)
	{
		return new JavaEEGoogleGwtMutableModuleExtension(getId(), moduleRootLayer);
	}
}
