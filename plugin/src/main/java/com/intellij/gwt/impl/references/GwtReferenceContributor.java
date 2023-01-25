package com.intellij.gwt.impl.references;

import com.intellij.gwt.base.i18n.GwtI18nUtil;
import com.intellij.gwt.impl.i18n.GwtPropertyReference;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.java.language.patterns.PsiJavaPatterns;
import com.intellij.java.language.patterns.PsiMethodPattern;
import com.intellij.java.language.psi.JavaTokenType;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ArrayUtil;
import consulo.xml.patterns.XmlAttributeValuePattern;

import javax.annotation.Nonnull;

import static com.intellij.java.language.patterns.PsiJavaPatterns.string;
import static com.intellij.java.language.patterns.PsiJavaPatterns.*;
import static consulo.xml.patterns.XmlPatterns.*;

/**
 * @author nik
 */
@ExtensionImpl
public class GwtReferenceContributor extends PsiReferenceContributor
{
	@Override
	public void registerReferenceProviders(final PsiReferenceRegistrar registrar)
	{
		registrar.registerReferenceProvider(literalExpression().annotationParam(GwtI18nUtil.KEY_ANNOTATION_CLASS, "value"), new PsiReferenceProvider()
		{
			@Override
			@Nonnull
			public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull final ProcessingContext context)
			{
				if(element instanceof PsiLiteralExpression)
				{
					Object value = ((PsiLiteralExpression) element).getValue();
					if(value instanceof String)
					{
						PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
						if(psiClass != null)
						{
							return new PsiReference[]{new GwtPropertyReference((String) value, element, psiClass)};
						}
					}
				}
				return PsiReference.EMPTY_ARRAY;
			}
		});

		XmlAttributeValuePattern inheritsTag = xmlAttributeValue(xmlAttribute("name").withParent(xmlTag().withLocalName("inherits").withParent(xmlTag()
				.withLocalName("module").inFile(psiFile().withName(string().endsWith(GwtModulesManager.GWT_XML_SUFFIX))))));
		ElementPattern<? extends PsiElement> returnInTestCase = literalExpression().withParent(psiReturnStatement().insideMethod("getModuleName",
				"com.google.gwt.junit.client.GWTTestCase"));
		registrar.registerReferenceProvider(inheritsTag, new GwtModuleReferencesProvider());
		registrar.registerReferenceProvider(returnInTestCase, new GwtModuleReferencesProvider());

		registrar.registerReferenceProvider(literalExpression().and(psiExpression().methodCallParameter(0, psiMethod().withName("get").definedInClass("com" +
				".google.gwt.user.client.ui.RootPanel"))), new GwtToHtmlReferencesProvider());

	/*	StringPattern methods = string().oneOf("addStyleName", "removeStyleName", "setStyleName", "setStylePrimaryName");
		registrar.registerReferenceProvider(literalExpression().andOr(psiExpression().methodCallParameter(0, psiMethod().withName(methods).definedInClass
				("com.google.gwt.user.client.ui.UIObject")), psiExpression().methodCallParameter(2, psiMethod().withName(methods).definedInClass("com.google.gwt" +
				".user.client.ui.HTMLTable.CellFormatter"))), new GwtToCssClassReferenceProvider());   */

		final PsiMethodPattern setEntryPointMethodPattern = psiMethod().withName("setServiceEntryPoint").definedInClass("com.google.gwt.user.client.rpc" +
				".ServiceDefTarget");
		registrar.registerReferenceProvider(literalExpression().withParent(psiBinaryExpression().operation(PsiJavaPatterns.psiElement(JavaTokenType.PLUS))
				.and(psiExpression().methodCallParameter(0, setEntryPointMethodPattern))), new PsiReferenceProvider()
		{
			@Nonnull
			@Override
			public PsiReference[] getReferencesByElement(@Nonnull final PsiElement element, @Nonnull final ProcessingContext context)
			{
				if(element instanceof PsiLiteralExpression)
				{
					Object value = ((PsiLiteralExpression) element).getValue();
					if(value instanceof String)
					{
						return new PsiReference[]{new GwtServletPathReference((String) value, (PsiLiteralExpression) element)};
					}
				}
				return PsiReference.EMPTY_ARRAY;
			}
		});
	}

	private static class GwtServletPathReference extends BaseGwtReference
	{
		private String myValue;

		public GwtServletPathReference(final String value, final PsiLiteralExpression element)
		{
			super(element);
			myValue = value;
		}

		@Override
		public PsiElement resolve()
		{
			final GwtModule module = findGwtModule();
			if(module != null)
			{
				for(GwtServlet servlet : module.getServlets())
				{
					final String path = servlet.getPath().getValue();
					if(path != null && (path.equals(myValue) || path.equals("/" + myValue)))
					{
						return servlet.getXmlTag();
					}
				}
			}
			return null;
		}

		@Override
		public boolean isSoft()
		{
			return true;
		}

		@Override
		public Object[] getVariants()
		{
			return ArrayUtil.EMPTY_OBJECT_ARRAY;
		}
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return Language.ANY;
	}
}
