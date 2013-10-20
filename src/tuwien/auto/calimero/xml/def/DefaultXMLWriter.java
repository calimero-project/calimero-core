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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.xml.Attribute;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLWriter;

/**
 * Default XML writer implementation of the XMLWriter interface.
 * <p>
 * Does not add any feature not already documented in the implemented interface.<br>
 * This writer is not thread safe.
 * 
 * @author B. Malinowsky
 */
public class DefaultXMLWriter implements XMLWriter
{
	// default indentation
	private static final int indentWidth = 4;
	private static final String quote = "\"";

	private BufferedWriter w;
	private boolean closeWriter;
	// xml layout stack
	private Stack layout;
	// current layout indentation
	private int indent;
	// controls indentation for new tags
	private boolean newTag;

	/**
	 * Creates a new XML writer.
	 * <p>
	 */
	public DefaultXMLWriter()
	{}

	/**
	 * Creates a new XML writer with output <code>w</code>.
	 * <p>
	 * The writer is buffered by this XML writer.
	 * 
	 * @param w the output {@link Writer}
	 * @param close <code>true</code> to close <code>w</code> if XML writer is closed,
	 *        <code>false</code> otherwise
	 * @see XMLWriter#setOutput(Writer, boolean)
	 */
	public DefaultXMLWriter(final Writer w, final boolean close)
	{
		setOutput(w, close);
	};

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#setOutput(java.io.Writer, boolean)
	 */
	public void setOutput(final Writer output, final boolean close)
	{
		reset();
		w = new BufferedWriter(output);
		closeWriter = close;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#writeDeclaration(boolean, java.lang.String)
	 */
	public void writeDeclaration(final boolean standalone, final String encoding)
		throws KNXMLException
	{
		try {
			w.write("<?xml version=" + quote + "1.0" + quote);
			w.write(" standalone=" + quote + (standalone ? "yes" : "no") + quote);
			if (encoding != null && encoding.length() > 0)
				w.write(" encoding=" + quote + encoding + quote);
			w.write("?>");
			w.newLine();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#writeElement
	 * (java.lang.String, java.util.List, java.lang.String)
	 */
	public void writeElement(final String name, final List att, final String content)
		throws KNXMLException
	{
		try {
			final Tag tag = new Tag(name, att, content, false);
			layout.push(tag);
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#writeEmptyElement
	 * (java.lang.String, java.util.List)
	 */
	public void writeEmptyElement(final String name, final List att)
		throws KNXMLException
	{
		try {
			final Tag tag = new Tag(name, att, null, true);
			tag.endTag();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#writeCharData(java.lang.String, boolean)
	 */
	public void writeCharData(final String text, final boolean isCDATASection)
		throws KNXMLException
	{
		try {
			if (isCDATASection) {
				w.write("<![CDATA[");
				w.write(text);
				w.write("]]>");
			}
			else
				w.write(References.replace(text, true));
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#writeComment(java.lang.String)
	 */
	public void writeComment(final String comment) throws KNXMLException
	{
		try {
			if (newTag)
				w.newLine();
			indent();
			w.write("<!--");
			w.write(comment);
			w.write("-->");
			if (!newTag)
				w.newLine();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#endElement()
	 */
	public void endElement() throws KNXMLException
	{
		if (layout.empty())
			throw new KNXIllegalStateException("no elements to end");
		try {
			((Tag) layout.pop()).endTag();
			if (layout.empty())
				w.flush();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#endAllElements()
	 */
	public void endAllElements() throws KNXMLException
	{
		try {
			while (!layout.empty())
				((Tag) layout.pop()).endTag();
			w.flush();
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLWriter#close()
	 */
	public void close() throws KNXMLException
	{
		if (!layout.isEmpty())
			endAllElements();
		if (closeWriter)
			try {
				w.close();
			}
			catch (final IOException e) {
				throw new KNXMLException(e.getMessage());
			}
	}

	private BufferedWriter indent() throws IOException
	{
		for (int i = 0; i < indent; ++i)
			w.write(' ');
		return w;
	}

	private void reset()
	{
		layout = new Stack();
		indent = 0;
		newTag = false;
	}

	// helper class for tag writing
	private final class Tag
	{
		private static final String lt = "<";
		private static final String gt = ">";
		private static final String equal = "=";
		private static final String slash = "/";
		private static final String space = " ";
		private String name;

		Tag(final String name, final List att, final String cnt, final boolean empty)
			throws IOException
		{
			if (newTag)
				w.newLine();
			indent().write(lt + name);
			if (att != null)
				for (final Iterator i = att.iterator(); i.hasNext();) {
					final Attribute a = (Attribute) i.next();
					w.write(space + a.getName() + equal + quote
							+ References.replace(a.getValue(), true) + quote);
				}
			if (empty) {
				w.write(space + slash + gt);
				w.newLine();
				newTag = false;
			}
			else {
				newTag = true;
				if (cnt == null || cnt.length() == 0)
					w.write(gt);
				else
					w.write(gt + References.replace(cnt, true));
				this.name = name;
				indent += indentWidth;
			}
		}

		void endTag() throws IOException
		{
			if (name != null) {
				indent -= indentWidth;
				if (!newTag)
					indent();
				newTag = false;
				w.write(lt + slash + name + gt);
				w.newLine();
			}
		}
	}
}
