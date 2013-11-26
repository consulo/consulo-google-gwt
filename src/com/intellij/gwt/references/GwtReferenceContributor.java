package com.intellij.gwt.references;

import com.intellij.gwt.i18n.GwtI18nUtil;
import com.intellij.gwt.i18n.GwtPropertyReference;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtServlet;
import com.intellij.patterns.*;
import static com.intellij.patterns.PsiJavaPatterns.*;
import static com.intellij.patterns.PsiJavaPatterns.string;
import static com.intellij.patterns.XmlPatterns.*;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class GwtReferenceContributor extends PsiReferenceContributor {
  public void registerReferenceProviders(final PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(literalExpression().annotationParam(GwtI18nUtil.KEY_ANNOTATION_CLASS, "value"), new PsiReferenceProvider() {
      @NotNull
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        if (element instanceof PsiLiteralExpression) {
          Object value = ((PsiLiteralExpression)element).getValue();
          if (value instanceof String) {
            PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (psiClass != null) {
              return new PsiReference[]{new GwtPropertyReference((String)value, element, psiClass)};
            }
          }
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });

    XmlAttributeValuePattern inheritsTag =
        xmlAttributeValue(xmlAttribute("name").withParent(xmlTag().withLocalName("inherits").withParent(xmlTag().withLocalName("module")
            .inFile(psiFile().withName(string().endsWith(GwtModulesManager.GWT_XML_SUFFIX))))));
    ElementPattern<? extends PsiElement> returnInTestCase =
        literalExpression().withParent(psiReturnStatement().insideMethod("getModuleName", "com.google.gwt.junit.client.GWTTestCase"));
    registrar.registerReferenceProvider(inheritsTag, new GwtModuleReferencesProvider());
    registrar.registerReferenceProvider(returnInTestCase, new GwtModuleReferencesProvider());

    registrar.registerReferenceProvider(
        literalExpression().and(psiExpression().methodCallParameter(0, psiMethod().withName("get")
            .definedInClass("com.google.gwt.user.client.ui.RootPanel"))), new GwtToHtmlReferencesProvider());

    StringPattern methods = string().oneOf("addStyleName", "removeStyleName", "setStyleName", "setStylePrimaryName");
    registrar.registerReferenceProvider(
        literalExpression().andOr(
          psiExpression().methodCallParameter(0, psiMethod().withName(methods).definedInClass("com.google.gwt.user.client.ui.UIObject")),
          psiExpression().methodCallParameter(2, psiMethod().withName(methods).definedInClass("com.google.gwt.user.client.ui.HTMLTable.CellFormatter"))
        ), new GwtToCssClassReferenceProvider());

    final PsiMethodPattern setEntryPointMethodPattern =
      psiMethod().withName("setServiceEntryPoint").definedInClass("com.google.gwt.user.client.rpc.ServiceDefTarget");
    registrar.registerReferenceProvider(
      literalExpression()
        .withParent(psiBinaryExpression().operation(PsiJavaPatterns.psiElement(JavaTokenType.PLUS))
                     .and(psiExpression().methodCallParameter(0, setEntryPointMethodPattern))), new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
        if (element instanceof PsiLiteralExpression) {
          Object value = ((PsiLiteralExpression)element).getValue();
          if (value instanceof String) {
            return new PsiReference[] {new GwtServletPathReference((String)value, (PsiLiteralExpression)element)};
          }
        }
        return PsiReference.EMPTY_ARRAY;
      }
    });
  }

  private static class GwtServletPathReference extends BaseGwtReference {
    private String myValue;

    public GwtServletPathReference(final String value, final PsiLiteralExpression element) {
      super(element);
      myValue = value;
    }

    public PsiElement resolve() {
      final GwtModule module = findGwtModule();
      if (module != null) {
        for (GwtServlet servlet : module.getServlets()) {
          final String path = servlet.getPath().getValue();
          if (path != null && (path.equals(myValue) || path.equals("/" + myValue))) {
            return servlet.getXmlTag();
          }
        }
      }
      return null;
    }

    @Override
    public boolean isSoft() {
      return true;
    }

    public Object[] getVariants() {
      return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }
  }
}
