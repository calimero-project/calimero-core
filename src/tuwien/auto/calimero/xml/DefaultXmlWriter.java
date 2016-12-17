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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import tuwien.auto.calimero.KNXIllegalStateException;

/**
 * Default XML writer implementation of the XmlWriter interface.
 * <p>
 * Does not add any feature not already documented in the implemented interface.<br>
 * This writer is not thread safe.
 *
 * @author B. Malinowsky
 */
public class DefaultXmlWriter implements XmlWriter
{
	// default indentation
	private static final int indentWidth = 4;
	private static final String quote = "\"";

	private static final String lt = "<";
	private static final String gt = ">";
	private static final String equal = "=";
	private static final String slash = "/";
	private static final String space = " ";

	private final Map<String, Object> config = new HashMap<>();

	private BufferedWriter w;
	// xml layout stack
	private Stack<Tag> layout;
	// current layout indentation
	private int indent;
	private boolean newTag;
	private boolean empty;


	/**
	 * Creates a new XML writer with output <code>w</code>.
	 * <p>
	 * The writer is buffered by this XML writer.
	 *
	 * @param w the output {@link Writer}
	 */
	public DefaultXmlWriter(final Writer w)
	{
		this.w = new BufferedWriter(w);
		layout = new Stack<>();
		indent = 0;
		newTag = false;
	};

	private void writeDeclaration(final boolean standalone, final String encoding,
		final String version) throws KNXMLException
	{
		write("<?xml version=", quote, version, quote);
		write(" standalone=", quote, (standalone ? "yes" : "no"), quote);
		if (encoding != null && encoding.length() > 0)
			write(" encoding=", quote, encoding, quote);
		write("?>");
		try {
			w.newLine();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	@Override
	public void writeComment(final String comment) throws KNXMLException
	{
		closeStartElement();
		try {
			if (newTag)
				w.newLine();
			indent();
			write("<!--", comment, "-->");
			if (!newTag)
				w.newLine();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage(), e);
		}
		newTag = false;
	}

	@Override
	public void close() throws KNXMLException
	{
		writeEndDocument();
	}

	private BufferedWriter indent() throws IOException
	{
		for (int i = 0; i < indent; ++i)
			w.write(' ');
		return w;
	}

	// helper class for tag writing
	private final class Tag
	{
		private final String name;

		Tag(final String localName)
		{
			name = localName;
		}

		void addAttribute(final String localName, final String value)
		{
			write(space, localName, equal, quote, References.replace(value, true), quote);
		}

		void endTag()
		{
			try {
				if (name != null) {
					indent -= indentWidth;
					if (!newTag)
						indent();
					newTag = false;
					if (empty)
						write(" ", slash, gt);
					else
						w.write(lt + slash + name + gt);
					w.newLine();
					empty = false;
				}
			}
			catch (final IOException e) {
				throw new KNXMLException("endTag", e);
			}
		}
	}

	@Override
	public void writeStartElement(final String localName)
	{
		writeStartElement(null, localName, null);
	}

	@Override
	public void writeStartElement(final String namespaceURI, final String localName)
	{
		writeStartElement(null, localName, namespaceURI);
	}

	@Override
	public void writeStartElement(final String prefix, final String localName,
		final String namespaceURI)
	{
		closeStartElement();
		try {
			if (newTag) {
				w.newLine();
			}
			indent();
		}
		catch (final IOException e) {
			throw new KNXMLException("write start element", e);
		}
		write(lt);
		if (prefix != null)
			write(prefix, ":");
		write(localName);
		newTag = true;
		empty = false;
		layout.push(new Tag(localName));
	}

	private void closeStartElement()
	{
		if (empty)
			layout.pop().endTag();
		if (newTag)
			write(gt);
	}

	@Override
	public void writeEmptyElement(final String namespaceURI, final String localName)
	{
		writeEmptyElement(null, localName, namespaceURI);
	}

	@Override
	public void writeEmptyElement(final String prefix, final String localName,
		final String namespaceURI)
	{
		writeStartElement(prefix, localName, namespaceURI);
		empty = true;
	}

	@Override
	public void writeEmptyElement(final String localName)
	{
		writeEmptyElement(null, localName, null);
	}

	@Override
	public void writeEndElement()
	{
		closeStartElement();
		if (layout.empty())
			throw new KNXIllegalStateException("no elements to end");
		try {
			layout.pop().endTag();
			if (layout.empty())
				w.flush();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	@Override
	public void writeEndDocument()
	{
		try {
			while (!layout.empty())
				layout.pop().endTag();
			w.flush();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	@Override
	public void flush()
	{
		try {
			w.flush();
		}
		catch (final IOException e) {
			throw new KNXMLException("close", e);
		}
	}

	@Override
	public void writeAttribute(final String localName, final String value)
	{
		writeAttribute(null, null, localName, value);
	}

	@Override
	public void writeAttribute(final String prefix, final String namespaceURI,
		final String localName, final String value)
	{
		layout.peek().addAttribute(localName, value);
	}

	@Override
	public void writeAttribute(final String namespaceURI, final String localName, final String value)
	{
		writeAttribute(null, null, localName, value);
	}

	@Override
	public void writeNamespace(final String prefix, final String namespaceURI)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void writeDefaultNamespace(final String namespaceURI)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void writeProcessingInstruction(final String target)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void writeProcessingInstruction(final String target, final String data)

	{
		// TODO Auto-generated method stub
	}

	@Override
	public void writeCData(final String data)
	{
		write("<![CDATA[", data, "]]>");
	}

	@Override
	public void writeDTD(final String dtd)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void writeEntityRef(final String name)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void writeStartDocument()
	{
		writeDeclaration(true, "utf-8", "1.0");
	}

	@Override
	public void writeStartDocument(final String version)
	{
		writeDeclaration(true, "utf-8", version);
	}

	@Override
	public void writeStartDocument(final String encoding, final String version)
	{
		writeDeclaration(true, encoding, version);
	}

	@Override
	public void writeCharacters(final String text)
	{
		closeStartElement();
		write(References.replace(text, true));
		newTag = false;
	}

	@Override
	public void writeCharacters(final char[] text, final int start, final int len)
	{
		final String s = new String(Arrays.copyOfRange(text, start, start + len));
		writeCharacters(s);
	}

	@Override
	public String getPrefix(final String uri)
	{
		return (String) config.get("prefix");
	}

	@Override
	public void setPrefix(final String prefix, final String uri)
	{
		config.put("prefix", uri);
	}

	@Override
	public void setDefaultNamespace(final String uri)
	{
		config.put("namespace", uri);
	}

	@Override
	public Object getProperty(final String name) throws IllegalArgumentException
	{
		return config.get(name);
	}

	private void write(final String... items)
	{
		try {
			for (final String s : items)
				if (s != null)
					w.write(s);
		}
		catch (final IOException e) {
			throw new KNXMLException("write", e);
		}
	}
}
