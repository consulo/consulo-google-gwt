package consulo.gwt.javascript.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.javascript.lang.BaseJavaScriptLanguageVersion;
import org.mustbe.consulo.javascript.lang.JavaScriptLanguage;
import com.intellij.lang.PsiParser;
import com.intellij.lang.javascript.DialectOptionHolder;
import com.intellij.lang.javascript.JavaScriptParsingFlexLexer;
import com.intellij.lang.javascript.highlighting.JSHighlighter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import consulo.gwt.javascript.ide.highlight.GwtSyntaxHighlighter;
import consulo.gwt.javascript.lang.parsing.GwtJavaScriptParser;
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
	public PsiParser createParser(@Nullable Project project)
	{
		return new GwtJavaScriptParser();
	}

	@NotNull
	@Override
	public Lexer createLexer(@Nullable Project project)
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
