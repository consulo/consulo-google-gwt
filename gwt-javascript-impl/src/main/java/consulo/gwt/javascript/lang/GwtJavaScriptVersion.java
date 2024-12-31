package consulo.gwt.javascript.lang;

import com.intellij.lang.javascript.DialectOptionHolder;
import com.intellij.lang.javascript.JavaScriptParsingFlexLexer;
import com.intellij.lang.javascript.highlighting.JSHighlighter;
import consulo.annotation.component.ExtensionImpl;
import consulo.gwt.javascript.ide.highlight.GwtSyntaxHighlighter;
import consulo.gwt.javascript.lang.parsing.GwtJavaScriptParser;
import consulo.javascript.lang.BaseJavaScriptLanguageVersion;
import consulo.javascript.language.JavaScriptLanguage;
import consulo.language.lexer.Lexer;
import consulo.language.parser.PsiParser;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 05.03.2015
 */
@ExtensionImpl
public class GwtJavaScriptVersion extends BaseJavaScriptLanguageVersion
{
	@Nonnull
	public static GwtJavaScriptVersion getInstance()
	{
		return JavaScriptLanguage.INSTANCE.findVersionByClass(GwtJavaScriptVersion.class);
	}

	private final DialectOptionHolder myDialectOptionHolder = new DialectOptionHolder(false, true);

	public GwtJavaScriptVersion()
	{
		super("GWT");
	}

	@Nonnull
	@Override
	public PsiParser createParser()
	{
		return new GwtJavaScriptParser();
	}

	@Nonnull
	@Override
	public Lexer createLexer()
	{
		return new JavaScriptParsingFlexLexer(myDialectOptionHolder);
	}

	@Nonnull
	@Override
	public JSHighlighter getSyntaxHighlighter()
	{
		return new GwtSyntaxHighlighter(myDialectOptionHolder);
	}
}
