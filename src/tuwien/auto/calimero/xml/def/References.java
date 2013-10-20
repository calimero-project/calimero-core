/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package tuwien.auto.calimero.xml.def;

import java.util.HashMap;
import java.util.Map;

/**
 * Substitutes predefined XML entities.
 * <p>
 * Does the replacement to and from references in strings.<br>
 * Predefined entities exist for
 * {@literal '&', '<', '>', '"' (quote) and ''' (single quote)}.
 * 
 * @author B. Malinowsky
 */
final class References
{
	// TODO allow to add/remove user defined entities
	
	// entities predefined in XML:
	// char | entity ref | num ref | hex ref
	// &      &amp;        &#38;     &#x26;
	// <      &lt;         &#60;     &#x3C;
	// >      &gt;         &#62;     &#x3E;
	// "      &quot;       &#34;     &#x22;
	// '      &apos;       &#39;     &#x27;

	// substitution table
	private static final String[][] entityTable = {
		{ "&amp;", "&#38;", "&#x26;", "&" },
		{ "&lt;", "&#60;", "&#x3C;", "<" },
		{ "&gt;", "&#62;", "&#x3E;", ">" },
		{ "&quot;", "&#34;", "&#x22;", "\"" },
		{ "&apos;", "&#39;", "&#x27;", "'" },
	};
	// references to entity mapping
	private static final Map map;
	
	static
	{
		map = new HashMap(25);
		for (int i = 0; i < 5; ++i)
			for (int k = 0; k < 3; ++k)
				map.put(entityTable[i][k], entityTable[i][3]);
	}
	
	private References()
	{}

	static String replaceFromRef(final String text)
	{
		final StringBuffer mod = new StringBuffer(text.length());
		int pos = 0;
		int oldpos = 0;
		while ((pos = text.indexOf('&', pos)) != -1) {
			final int end = text.indexOf(';', pos + 1);
			if (end == -1)
				break;
			final String entity = (String) map.get(text.substring(pos, end + 1));
			if (entity != null) {
				mod.append(text.substring(oldpos, pos));
				mod.append(entity);
				oldpos = end + 1;
				pos = end;
			}
			++pos;
		}
		if (mod.length() != 0)
			return mod.append(text.substring(oldpos)).toString();
		return text;
	}

	static String replaceWithRef(final String text)
	{
		final StringBuffer mod = new StringBuffer((int)(1.5f * text.length()));
		for (int i = 0; i < text.length(); ++i) {
			final char c = text.charAt(i);
			if (c == '&')
				mod.append("&amp;");
			else if (c == '<')
				mod.append("&lt;");
			else if (c == '>')
				mod.append("&gt;");
			else if (c == '"')
				mod.append("&quot;");
			else if (c == '\'')
				mod.append("&apos;");
			else
				mod.append(c);
		}
		return mod.toString();
	}
	
	// wrapper for reference replacement
	static String replace(final String text, final boolean toReference)
	{
		return toReference ? replaceWithRef(text) : replaceFromRef(text);
	}
}
