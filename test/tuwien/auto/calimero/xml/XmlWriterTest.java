/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

import junit.framework.TestCase;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class XmlWriterTest extends TestCase
{
	private static final String file = Util.getTargetPath() + "write.xml";
	private Writer out;
	private XmlWriter w, w2;
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
	@Override
	protected void setUp() throws Exception
	{
		out = new FileWriter(file);
		w = XmlOutputFactory.newInstance().createXMLStreamWriter(out);
		stream = new ByteArrayOutputStream();
		final OutputStreamWriter buf = new OutputStreamWriter(stream);
		w2 = XmlOutputFactory.newInstance().createXMLStreamWriter(buf);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception
	{
		w.close();
		out.close();
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.xml.DefaultXmlWriter#DefaultXmlWriter
	 * (Writer, boolean)}.
	 */
	public void testXmlWriter()
	{
		try (final XmlWriter w = new DefaultXmlWriter(out)) {
			w.writeEndDocument();
		}
		catch (final KNXMLException e) {
			fail("on creation of xml writer");
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.DefaultXmlWriter#writeEndElement()}.
	 *
	 * @throws KNXMLException
	 */
	public void testEndElement() throws KNXMLException
	{
		try {
			w.writeEndElement();
			fail("illegal state");
		}
		catch (final KNXMLException e) {}
		w.writeEndDocument();
		assertEquals(0, new File(file).length());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.DefaultXmlWriter#writeEndElement()}.
	 *
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testEndElement2() throws IOException, KNXMLException
	{
		w.writeStartElement("element");
		w.writeCharacters("test text");
		w.writeEndElement();
		w.writeEndDocument();
		w.close();
		assertTrue(new File(file).length() > 0);

		w2.writeStartElement("element");
		w2.writeCharacters("test text");
		w2.writeEndElement();
		w2.writeEndDocument();
		w2.close();
		final BufferedReader r = getInput("<element>test text</element>");
		final BufferedReader r2 = getInput(stream.toString());
		assertEquals(r.readLine(), r2.readLine());

	}

	/**
	 * Test method for {@link tuwien.auto.calimero.xml.DefaultXmlWriter#writeEndElement()}.
	 *
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testEndElement3() throws IOException, KNXMLException
	{
		w2.writeStartElement("root");
		w2.writeCharacters("test text");
		w2.writeStartElement("child1");
		w2.writeAttribute("att1", "value1");
		w2.writeCharacters("test text");
		w2.writeStartElement("child2");
		w2.writeAttribute("att1", "value1");
		w2.writeAttribute("att2", "value2");
		w2.writeCharacters("test text");
		w2.writeEndElement();
		w2.writeStartElement("child3");
		w2.writeAttribute("att1", "value1");
		w2.writeAttribute("att2", "value2");
		w2.writeAttribute("att3", "value3");
		w2.writeCharacters("test text");
		w2.writeEndElement();
		w2.writeEndElement();
		w2.writeEndElement();
		try {
			w2.writeEndElement();
			fail("illegal state");
		}
		catch (final KNXMLException e) {}
		w2.writeEndDocument();
		w2.close();
		final BufferedReader r =
			getInput("<root>test text<child1 att1=\"value1\">test text"
				+ "<child2 att1=\"value1\" att2=\"value2\">test text</child2>"
				+ "<child3 att1=\"value1\" att2=\"value2\" att3=\"value3\">"
				+ "test text</child3></child1>" + "</root>");
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
	 * {@link tuwien.auto.calimero.xml.DefaultXmlWriter#writeEndDocument()}.
	 *
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testEndAllElements() throws IOException, KNXMLException
	{
		w2.writeEmptyElement("root");
		w2.writeEmptyElement("child1");
		w2.writeAttribute("att1", "value1");
//		w2.writeEndElement();
		w2.writeStartElement("child2");
		w2.writeAttribute("att1", "value1");
		w2.writeStartElement("child3");
		w2.writeAttribute("att1", "value1");
		w2.writeCharacters("test text");
		w2.writeEndDocument();
		w2.close();
		final BufferedReader r =
			getInput("<root/>" + "<child1 att1=\"value1\"/>"
				+ "<child2 att1=\"value1\">"
				+ "<child3 att1=\"value1\">test text</child3>" + "</child2>");
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
	 * Test method for {@link tuwien.auto.calimero.xml.DefaultXmlWriter#writeStartElement
	 * (String, List, String)}.
	 *
	 * @throws IOException
	 * @throws KNXMLException
	 */
	public void testWriteElement() throws IOException, KNXMLException
	{
		w2.writeStartElement("root");
		w2.writeCharacters("test text");
		w2.writeStartElement("child1");
		w2.writeAttribute("att1", "value1");
		w2.writeCharacters("xyz");
		w2.writeStartElement("child2");
		w2.writeCharacters("");
		w2.writeEmptyElement("child3");
		w2.writeAttribute("att1", "value1");
		w2.writeEndElement();
		w2.writeEmptyElement("child3");
		w2.writeAttribute("att1", "value1");
		w2.writeStartElement("child3");
		w2.writeEndElement();
		w2.writeEndDocument();
		w2.writeEmptyElement("root");
		w2.writeEndDocument();
		w2.close();
		final BufferedReader r =
			getInput("<root>test text" + "<child1 att1=\"value1\">xyz"
				+ "<child2>" + "<child3 att1=\"value1\"/>"
				+ "</child2>" + "<child3 att1=\"value1\"/>"
				+ "<child3></child3>" + "</child1>" + "</root>"
				+ "<root/>");

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
		w.writeStartElement("root");
		w.writeComment("character data follows");
		w.writeCharacters("hello XML! ");
		w.writeCData("hello tags &lt; < &gt; > ");
		w.writeEndDocument();
		w.close();
		final XmlReader r = XmlInputFactory.newInstance().createXMLReader(file);
		r.nextTag();
		final String s = "\n    hello XML! hello tags &lt; < &gt; > ".replace("\n    ", "");
		assertEquals(s, r.getElementText());
	}

	private BufferedReader getInput(final String text)
	{
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes())));
	}

}
