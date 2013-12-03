package org.mustbe.consulo.google.gwt.module.extension;

import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.jetbrains.annotations.NotNull;
import com.intellij.gwt.facet.GwtJavaScriptOutputStyle;

/**
 * @author VISTALL
 * @since 03.12.13.
 */
public interface GoogleGwtModuleExtension<T extends GoogleGwtModuleExtension<T>> extends ModuleExtensionWithSdk<T>
{
	@NotNull
	GwtJavaScriptOutputStyle getJavaScriptOutputStyle();
}
