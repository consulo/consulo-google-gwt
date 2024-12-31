/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package consulo.gwt.javascript.ide.highlight;

import com.intellij.java.analysis.impl.ide.highlighter.JavaHighlightingColors;
import com.intellij.lang.javascript.DialectOptionHolder;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.highlighting.JSHighlighter;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.ast.IElementType;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author nik
 */
public class GwtSyntaxHighlighter extends JSHighlighter
{
	private Map<IElementType, TextAttributesKey> myKeysMap = new HashMap<IElementType, TextAttributesKey>();

	public GwtSyntaxHighlighter(DialectOptionHolder dialectOptionHolder)
	{
		super(dialectOptionHolder);
		myKeysMap.put(JSTokenTypes.COLON_COLON, JS_OPERATION_SIGN);
		myKeysMap.put(JSTokenTypes.GWT_FIELD_OR_METHOD, JavaHighlightingColors.METHOD_CALL_ATTRIBUTES);
		myKeysMap.put(JSTokenTypes.AT, JS_OPERATION_SIGN);
		myKeysMap.put(JSTokenTypes.IDENTIFIER, JavaHighlightingColors.CLASS_NAME_ATTRIBUTES);
	}

	@Override
	@Nonnull
	public TextAttributesKey[] getTokenHighlights(final IElementType tokenType)
	{
		if(myKeysMap.containsKey(tokenType))
		{
			return pack(myKeysMap.get(tokenType));
		}
		return super.getTokenHighlights(tokenType);
	}
}