package consulo.gwt.javascript.lang;

import javax.annotation.Nonnull;

import com.intellij.lang.PsiParser;
import com.intellij.lang.javascript.DialectOptionHolder;
import com.intellij.lang.javascript.JavaScriptParsingFlexLexer;
import com.intellij.lang.javascript.highlighting.JSHighlighter;
import com.intellij.lexer.Lexer;
import consulo.gwt.javascript.ide.highlight.GwtSyntaxHighlighter;
import consulo.gwt.javascript.lang.parsing.GwtJavaScriptParser;
import consulo.javascript.lang.BaseJavaScriptLanguageVersion;
import consulo.javascript.lang.JavaScriptLanguage;

/**
 * @author VISTALL
 * @since 05.03.2015
 */
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
