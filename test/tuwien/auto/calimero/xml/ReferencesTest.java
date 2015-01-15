/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

package tuwien.auto.calimero.xml;

import junit.framework.TestCase;

/**
 * Tests commented out because class References has default visibility.
 *
 * @author B. Malinowsky
 */
public class ReferencesTest extends TestCase
{

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.References#replaceFromRef(java.lang.String)}.
	 */
	public void testReplaceFromRef()
	{
		String s = "text &amp; text &#38; text &#x26; text";
		String out = References.replaceFromRef(s);
		assertEquals("text & text & text & text", out);

		s = "&lt; text &#60; text &#x3C;";
		out = References.replaceFromRef(s);
		assertEquals("< text < text <", out);

		s = "&gt; text &#62; text &#x3E;";
		out = References.replaceFromRef(s);
		assertEquals("> text > text >", out);

		s = "&quot; text &#34; text &#x22; text \"";
		out = References.replaceFromRef(s);
		assertEquals("\" text \" text \" text \"", out);

		s = "&apos;&#39;&#x27;'";
		out = References.replaceFromRef(s);
		assertEquals("''''", out);

		s = "& < > \" '";
		out = References.replaceFromRef(s);
		System.out.println(out);
		assertEquals("& < > \" '", out);

		s = "&<>\"';";
		out = References.replaceFromRef(s);
		System.out.println(out);
		assertEquals("&<>\"';", out);

		s = "a text without any entities to replace";
		out = References.replaceFromRef(s);
		assertEquals(s, out);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.References#replaceWithRef(java.lang.String)}.
	 */
	public void testReplaceWithRef()
	{
		String s = "& < > \" '";
		String out = References.replaceWithRef(s);
		assertEquals("&amp; &lt; &gt; &quot; &apos;", out);

		s = "a text without any entities to replace";
		out = References.replaceWithRef(s);
		assertEquals(s, out);

		s = "&&&<<<>\"'''>>";
		out = References.replaceWithRef(s);
		System.out.println(out);
		assertEquals("&amp;&amp;&amp;&lt;&lt;&lt;&gt;&quot;&apos;&apos;&apos;&gt;&gt;", out);

		s = " text & text < text > text \" text ' text ";
		out = References.replaceWithRef(s);
		System.out.println(out);
		assertEquals(" text &amp; text &lt; text &gt; text &quot; text &apos; text ", out);

	}
}
