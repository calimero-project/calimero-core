/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

    Linking this library statically or dynamically with other modules is
    making a combined work based on this library. Thus, the terms and
    conditions of the GNU General Public License cover the whole
    combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent
    modules, and to copy and distribute the resulting executable under terms
    of your choice, provided that you also meet, for each linked independent
    module, the terms and conditions of the license of that module. An
    independent module is a module which is not derived from or based on
    this library. If you modify this library, you may extend this exception
    to your version of the library, but you are not obligated to do so. If
    you do not wish to do so, delete this exception statement from your
    version.
*/

package io.calimero.xml;

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
	private static final Map<String, String> map;

	static {
		map = new HashMap<>(25);
		for (int i = 0; i < 5; ++i)
			for (int k = 0; k < 3; ++k)
				map.put(entityTable[i][k], entityTable[i][3]);
	}

	private References()
	{}

	static String replaceFromRef(final String text)
	{
		final StringBuilder mod = new StringBuilder(text.length());
		int pos = 0;
		int oldpos = 0;
		while ((pos = text.indexOf('&', pos)) != -1) {
			final int end = text.indexOf(';', pos + 1);
			if (end == -1)
				break;
			final String entity = map.get(text.substring(pos, end + 1));
			if (entity != null) {
				mod.append(text, oldpos, pos);
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
		final StringBuilder mod = new StringBuilder((int) (1.5f * text.length()));
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
