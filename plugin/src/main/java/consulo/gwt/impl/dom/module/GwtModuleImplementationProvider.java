package consulo.gwt.impl.dom.module;

import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.impl.module.model.impl.GwtModuleImpl;
import consulo.annotation.component.ExtensionImpl;
import consulo.xml.dom.DomElementImplementationProvider;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24/01/2023
 */
@ExtensionImpl
public class GwtModuleImplementationProvider implements DomElementImplementationProvider<GwtModule, GwtModuleImpl>
{
	@Nonnull
	@Override
	public Class<GwtModule> getInterfaceClass()
	{
		return GwtModule.class;
	}

	@Nonnull
	@Override
	public Class<GwtModuleImpl> getImplementationClass()
	{
		return GwtModuleImpl.class;
	}
}
