/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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

package tuwien.auto.calimero.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Vector;

import junit.framework.TestCase;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.xml.def.DefaultXMLWriter;

/**
 * @author B. Malinowsky
 */
public class XmlWriterTest extends TestCase
{
	private static final String file = Util.getPath() + "write.xml";
	private Writer out;
	private XMLWriter w, w2;
	private ByteArrayOutputStream stream;

	/**
	 * @param name name of test case
	 */
	public XmlWriterTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		out = new FileWriter(file);
		w = new DefaultXMLWriter(out, true);
		stream = new ByteArrayOutputStream();
		final OutputStreamWriter buf = new OutputStreamWriter(stream);
		w2 = new DefaultXMLWriter(buf, true);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		w.close();
		out.close();
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.xml.def.DefaultXMLWriter#DefaultXMLWriter
	 * (Writer, boolean)}.
	 */
	public void testXmlWriter()
	{
		try {
			final XMLWriter w = new DefaultXMLWriter(out, true);
			w.endAllElements();
		}
		catch (final KNXMLException e) {
			fail("on creation of xml writer");
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.def.DefaultXMLWriter#endElement()}.
	 * 
	 * @throws KNXMLException
	 */
	public void testEndElement() throws KNXMLException
	{
		try {
			w.endElement();
			fail("illegal state");
		}
		catch (final KNXIllegalStateException e) {}
		w.endAllElements();
		assertEquals(0, new File(file).length());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.def.DefaultXMLWriter#endElement()}.
	 * 
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testEndElement2() throws IOException, KNXMLException
	{
		w.writeElement("element", null, "test text");
		w.endElement();
		w.endAllElements();
		assertTrue(new File(file).length() > 0);

		w2.writeElement("element", null, "test text");
		w2.endElement();
		w2.endAllElements();

		final BufferedReader r = getInput("<element>test text</element>");
		final BufferedReader r2 = getInput(stream.toString());
		assertEquals(r.readLine(), r2.readLine());

	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.def.DefaultXMLWriter#endElement()}.
	 * 
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testEndElement3() throws IOException, KNXMLException
	{
		final List att = new Vector();
		att.add(new Attribute("att1", "value1"));

		w2.writeElement("root", null, "test text");
		w2.writeElement("child1", att, "test text");
		att.add(new Attribute("att2", "value2"));
		w2.writeElement("child2", att, "test text");
		w2.endElement();
		att.add(new Attribute("att3", "value3"));
		w2.writeElement("child3", att, "test text");
		w2.endElement();
		w2.endElement();
		w2.endElement();
		try {
			w2.endElement();
			fail("illegal state");
		}
		catch (final KNXIllegalStateException e) {}
		w2.endAllElements();

		final BufferedReader r =
			getInput("<root>test text\n" + "    <child1 att1=\"value1\">test text\n"
				+ "        <child2 att1=\"value1\" att2=\"value2\">test text</child2>\n"
				+ "        <child3 att1=\"value1\" att2=\"value2\" att3=\"value3\">"
				+ "test text</child3>\n    </child1>\n" + "</root>\n");
		final BufferedReader r2 = getInput(stream.toString());
		while (true) {
			final String s = r.readLine();
			final String s2 = r2.readLine();
			if (s == null && s2 == null)
				break;
			if (s == null || s2 == null)
				fail("one input ended earlier than the other");
			assertEquals(s, s2);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.xml.def.DefaultXMLWriter#endAllElements()}.
	 * 
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testEndAllElements() throws IOException, KNXMLException
	{
		final List att = new Vector();
		att.add(new Attribute("att1", "value1"));

		w2.writeEmptyElement("root", null);
		w2.writeEmptyElement("child1", att);
		w2.writeElement("child2", att, "");
		w2.writeElement("child3", att, "test text");
		w2.endAllElements();
		w2.endAllElements();

		final BufferedReader r =
			getInput("<root />\n" + "<child1 att1=\"value1\" />\n"
				+ "<child2 att1=\"value1\">\n"
				+ "    <child3 att1=\"value1\">test text</child3>\n" + "</child2>");
		final BufferedReader r2 = getInput(stream.toString());
		while (true) {
			final String s = r.readLine();
			final String s2 = r2.readLine();
			if (s == null && s2 == null)
				break;
			if (s == null || s2 == null)
				fail("one input ended earlier than the other");
			assertEquals(s, s2);
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.def.DefaultXMLWriter#writeElement
	 * (String, List, String)}.
	 * 
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testWriteElement() throws IOException, KNXMLException
	{
		final List att = new Vector();
		att.add(new Attribute("att1", "value1"));

		w2.writeElement("root", null, "test text");
		w2.writeElement("child1", att, "xyz");
		w2.writeElement("child2", null, "");
		w2.writeEmptyElement("child3", att);
		w2.endElement();
		w2.writeEmptyElement("child3", att);
		w2.writeElement("child3", null, "");
		w2.endElement();
		w2.endAllElements();
		w2.writeEmptyElement("root", null);
		w2.endAllElements();
		final BufferedReader r =
			getInput("<root>test text\n" + "    <child1 att1=\"value1\">xyz\n"
				+ "        <child2>\n" + "            <child3 att1=\"value1\" />\n"
				+ "        </child2>\n" + "        <child3 att1=\"value1\" />\n"
				+ "        <child3></child3>\n" + "    </child1>\n" + "</root>\n"
				+ "<root />\n");

		final BufferedReader r2 = getInput(stream.toString());
		while (true) {
			final String s = r.readLine();
			final String s2 = r2.readLine();
			if (s == null && s2 == null)
				break;
			if (s == null || s2 == null)
				fail("one input ended earlier than the other");
			assertEquals(s, s2);
		}
	}

	/**
	 * @throws KNXMLException
	 */
	public void testWriteCommentCharData() throws KNXMLException
	{
		w.writeElement("root", null, null);
		w.writeComment("character data follows");
		w.writeCharData("hello XML! ", false);
		w.writeCharData("hello tags &lt; < &gt; > ", true);
		w.endAllElements();
		w.close();
		final XMLReader r = XMLFactory.getInstance().createXMLReader(file);
		r.read();
		r.complete(r.getCurrent());
		assertEquals("\n    hello XML! hello tags &lt; < &gt; > ", r.getCurrent().getCharacterData());
	}

	private BufferedReader getInput(final String text)
	{
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes())));
	}

}
