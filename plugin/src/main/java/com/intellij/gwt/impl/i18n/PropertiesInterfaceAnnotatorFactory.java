package com.intellij.gwt.impl.i18n;

import com.intellij.java.language.JavaLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25/01/2023
 */
@ExtensionImpl
public class PropertiesInterfaceAnnotatorFactory implements AnnotatorFactory
{
	@Nullable
	@Override
	public Annotator createAnnotator()
	{
		return new PropertiesInterfaceAnnotator();
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return JavaLanguage.INSTANCE;
	}
}
