package com.intellij.gwt.impl.i18n;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.references.PropertyReferenceBase;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class GwtPropertyReference extends PropertyReferenceBase
{
	private GwtI18nManager myGwtI18nManager;
	private final PsiClass myPropertiesInterface;

	public GwtPropertyReference(@Nonnull final String key, @Nonnull final PsiElement element, @Nonnull PsiClass propertiesInterface)
	{
		super(key, false, element);
		myPropertiesInterface = propertiesInterface;
		myGwtI18nManager = GwtI18nManager.getInstance(propertiesInterface.getProject());
	}

	@Override
	protected List<PropertiesFile> getPropertiesFiles()
	{
		return Arrays.asList(myGwtI18nManager.getPropertiesFiles(myPropertiesInterface));
	}
}
