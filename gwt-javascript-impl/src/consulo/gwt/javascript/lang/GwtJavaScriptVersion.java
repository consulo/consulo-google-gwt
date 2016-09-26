package consulo.gwt.javascript.lang;

import org.jetbrains.annotations.NotNull;
import com.intellij.lang.PsiParser;
import com.intellij.lang.javascript.DialectOptionHolder;
import com.intellij.lang.javascript.JavaScriptParsingFlexLexer;
import com.intellij.lang.javascript.highlighting.JSHighlighter;
import com.intellij.lexer.Lexer;
import consulo.gwt.javascript.ide.highlight.GwtSyntaxHighlighter;
import consulo.gwt.javascript.lang.parsing.GwtJavaScriptParser;
import consulo.javascript.lang.BaseJavaScriptLanguageVersion;
import consulo.javascript.lang.JavaScriptLanguage;
import consulo.lombok.annotations.Lazy;

/**
 * @author VISTALL
 * @since 05.03.2015
 */
public class GwtJavaScriptVersion extends BaseJavaScriptLanguageVersion
{
	@NotNull
	@Lazy
	public static GwtJavaScriptVersion getInstance()
	{
		return JavaScriptLanguage.INSTANCE.findVersionByClass(GwtJavaScriptVersion.class);
	}

	private final DialectOptionHolder myDialectOptionHolder = new DialectOptionHolder(false, true);

	public GwtJavaScriptVersion()
	{
		super("GWT");
	}

	@NotNull
	@Override
	public PsiParser createParser()
	{
		return new GwtJavaScriptParser();
	}

	@NotNull
	@Override
	public Lexer createLexer()
	{
		return new JavaScriptParsingFlexLexer(myDialectOptionHolder);
	}

	@NotNull
	@Override
	public JSHighlighter getSyntaxHighlighter()
	{
		return new GwtSyntaxHighlighter(myDialectOptionHolder);
	}
}
