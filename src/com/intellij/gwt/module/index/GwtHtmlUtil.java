package com.intellij.gwt.module.index;

import com.intellij.lexer.HtmlLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlTokenType;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

/**
 * @author nik
 */
public class GwtHtmlUtil {
  @NonNls private static final String META_TAG_NAME = "meta";
  @NonNls private static final String GWT_MODULE_META_NAME = "gwt:module";
  @NonNls private static final String SCRIPT_TAG_NAME = "script";
  @NonNls private static final String JAVASCRIPT_LANGUAGE_NAME = "javascript";
  @NonNls private static final String JAVASCRIPT_TYPE = "text/javascript";
  @NonNls private static final String NO_CACHE_SUFFIX = ".nocache.js";

  private GwtHtmlUtil() {
  }

  public static void collectGwtModules(CharSequence fileText, Map<String, Void> result) {
    HtmlLexer lexer = new HtmlLexer();
    lexer.start(fileText, 0, fileText.length(), 0);
    IElementType tokenType;
    String currentTag = null;
    Map<String, String> attributes = new THashMap<String, String>();
    while ((tokenType = lexer.getTokenType()) != null) {
      //System.out.println(tokenType + ":" + lexer.getTokenStart() + "-" + lexer.getTokenEnd() + "='" + getTokenText(lexer) + "'");
      if (tokenType == XmlTokenType.XML_START_TAG_START) {
        currentTag = null;
        lexer.advance();
        if (lexer.getTokenType() == XmlTokenType.XML_NAME) {
          currentTag = getTokenText(lexer).toLowerCase();
          lexer.advance();
        }
      }
      else if (tokenType == XmlTokenType.XML_NAME) {
        String attributeName = getTokenText(lexer);
        skipWhiteSpaces(lexer);
        if (lexer.getTokenType() == XmlTokenType.XML_EQ) {
          skipWhiteSpaces(lexer);
          if (lexer.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER) {
            lexer.advance();
            if (lexer.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
              String attributeValue = getTokenText(lexer);
              lexer.advance();
              if (lexer.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER) {
                attributes.put(attributeName, attributeValue);
                lexer.advance();
              }
            }
          }
        }
      }
      else if (tokenType == XmlTokenType.XML_TAG_END) {
        if (META_TAG_NAME.equals(currentTag) && GWT_MODULE_META_NAME.equals(attributes.get("name"))) {
          final String content = attributes.get("content");
          if (content != null) {
            result.put(content, null);
          }
        }
        else if (SCRIPT_TAG_NAME.equals(currentTag) && (JAVASCRIPT_LANGUAGE_NAME.equalsIgnoreCase(attributes.get("language")) ||
                  JAVASCRIPT_TYPE.equalsIgnoreCase(attributes.get("type")))) {
          String src = attributes.get("src");
          if (src != null && src.endsWith(NO_CACHE_SUFFIX)) {
            int start = Math.max(src.lastIndexOf('/'), src.lastIndexOf('\\'));
            result.put(src.substring(start + 1, src.length() - NO_CACHE_SUFFIX.length()), null);
          }
        }
        currentTag = null;
        attributes.clear();
        lexer.advance();
      }
      else {
        lexer.advance();
      }
    }
  }

  private static String getTokenText(HtmlLexer lexer) {
    return lexer.getBufferSequence().subSequence(lexer.getTokenStart(), lexer.getTokenEnd()).toString();
  }

  private static void skipWhiteSpaces(HtmlLexer lexer) {
    lexer.advance();
    while (lexer.getTokenType() == XmlTokenType.XML_WHITE_SPACE) {
      lexer.advance();
    }
  }
}
