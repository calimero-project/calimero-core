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

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import tuwien.auto.calimero.xml.Attribute;
import tuwien.auto.calimero.xml.Element;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;

/**
 * Default XML reader implementation of the XMLReader interface.
 * <p>
 * Does not add any feature not already documented in the implemented interface.<br>
 * This reader is not thread safe.
 * 
 * @author B. Malinowsky
 */
public class DefaultXMLReader implements XMLReader
{
	private Reader r;
	private boolean closeReader;
	private Element elem;
	private final Stack openElems = new Stack();
	private int pos;
	private int line;

	/**
	 * Creates a new XML reader.
	 * <p>
	 */
	public DefaultXMLReader()
	{}

	/**
	 * Creates a new XML reader with input <code>r</code>.
	 * <p>
	 * The {@link Reader} should already be buffered or wrapped with a buffered reader, if
	 * necessary (e.g. when reading from a file).
	 * 
	 * @param r a {@link Reader} for input
	 * @param close <code>true</code> to close <code>r</code> if XML reader is closed,
	 *        <code>false</code> otherwise
	 * @see XMLReader#setInput(Reader, boolean)
	 */
	public DefaultXMLReader(final Reader r, final boolean close)
	{
		reset();
		setInput(r, close);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#setInput(java.io.Reader, boolean)
	 */
	public void setInput(final Reader input, final boolean close)
	{
		if (r != null)
			reset();
		r = input;
		closeReader = close;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#read()
	 */
	public int read() throws KNXMLException
	{
		while (canRead()) {
			// init line counter on first read
			if (line == 0)
				line = 1;

			read('<');
			String str = read('>');
			// if no comment and no closing tag
			if (str.length() == 0 || skipComment(str) || skipInstruction(str))
				continue;
			final StringBuffer buf = new StringBuffer();
			if (readCDATASection(str, buf)) {
				elem = new DefaultElement((String) openElems.peek());
				elem.setCharacterData(buf.toString());
				pos = CHAR_DATA;
				return pos;
			}
			str = str.trim();
			// extract element name
			final String name = splitOnSpace(str);
			if (name.charAt(0) == '/') {
				if (!name.substring(1).equals(openElems.peek()))
					throw new KNXMLException("element end tag does not match start tag",
						name.substring(1), line);
				elem = new DefaultElement((String) openElems.pop());
				pos = END_TAG;
				return pos;
			}
			elem = new DefaultElement(name);
			extractAttributes(str.substring(name.length()));
			if (!elem.isEmptyElementTag())
				openElems.push(name);
			pos = START_TAG;
			return pos;
		}
		if (!openElems.empty())
			throw new KNXMLException("end of XML input with elements left open");
		pos = END_DOC;
		return pos;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#complete(tuwien.auto.calimero.xml.Element)
	 */
	public void complete(final Element e) throws KNXMLException
	{
		if (e.isEmptyElementTag())
			return;
		final int index = openElems.lastIndexOf(e.getName());
		if (index == -1)
			throw new KNXMLException("element tag not read before", e.getName(), line);
		String end = null;
		final StringBuffer content = new StringBuffer(50);
		while (canRead()) {
			// read text content
			final String s = read('<');
			final boolean current = openElems.peek().equals(e.getName());
			// if character data is for current element, append it
			if (current && s.length() > 0)
				content.append(References.replace(s, false));
			// read a possible end tag
			end = read('>');
			if (skipComment(end))
				;
			else if (current && readCDATASection(end, content))
				;
			else if (end.length() > 0 && end.charAt(0) == '/') {
				// actually, no white space is allowed between '/' and tag name
				// but there might be some between end of name and '>'
				final String tag = end.substring(1).trim();
				// got end tag?
				if (!tag.equals(openElems.peek()))
					throw new KNXMLException("element end tag does not match start tag", tag, line);
				openElems.pop();
				if (tag.equals(e.getName())) {
					e.setCharacterData(content.toString());
					pos = END_TAG;
					return;
				}
			}
			else if (end.length() > 0)
				// don't push empty element tags
				if (end.charAt(end.length() - 1) != '/') {
					final String tag = splitOnSpace(end);
					if (tag.length() > 0)
						openElems.push(tag);
				}
		}
		throw new KNXMLException("end of XML input with elements left open", end, line);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#getCurrent()
	 */
	public final Element getCurrent()
	{
		return elem;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#getPosition()
	 */
	public final int getPosition()
	{
		return pos;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#getLineNumber()
	 */
	public final int getLineNumber()
	{
		return line;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.xml.XMLReader#close()
	 */
	public void close() throws KNXMLException
	{
		if (closeReader)
			try {
				r.close();
			}
			catch (final IOException e) {
				throw new KNXMLException(e.getMessage());
			}
	}

	private boolean canRead()
	{
		try {
			return r.ready();
		}
		catch (final IOException e) {
			return false;
		}
	}

	private String read(final char delimiter) throws KNXMLException
	{
		final StringBuffer buf = new StringBuffer(50);
		try {
			boolean cr = false;
			for (int c = 0; (c = r.read()) != -1 && c != delimiter;) {
				if (c == '\n')
					++line;
				else if (cr) {
					++line;
					buf.append('\n');
				}
				cr = c == '\r';
				if (!cr)
					buf.append((char) c);
			}
		}
		catch (final IOException e) {
			throw new KNXMLException(e.getMessage(), buf.toString(), line);
		}
		return buf.toString();
	}

	// adds all available attributes to current element
	private void extractAttributes(final String attributes)
	{
		String s = attributes.trim();
		if (s.length() > 0 && s.charAt(s.length() - 1) == '/')
			elem.setEmptyElementTag(true);

		while (s.length() != 0) {
			final int equal = s.indexOf('=');
			if (equal == -1 || equal == s.length() - 1)
				break;
			// cut off attribute name
			final String att = s.substring(0, equal).trim();
			s = s.substring(equal + 1).trim();
			if (s.length() == 0)
				break;
			final boolean quote = s.charAt(0) == '\'' || s.charAt(0) == '\"';
			if (att.length() > 0 && quote) {
				String value = s.substring(1);
				final int end = value.indexOf(s.charAt(0));
				if (end >= 0)
					value = value.substring(0, end);
				elem.addAttribute(new Attribute(att, References.replace(value, false)));
			}
			final int i = s.indexOf(quote ? s.charAt(0) : ' ', 1);
			s = i == -1 ? "" : s.substring(i + 1);
		}
	}

	private boolean readCDATASection(final String s, final StringBuffer buf) throws KNXMLException
	{
		if (!s.startsWith("![CDATA["))
			return false;
		buf.append(s.substring(8));
		String cdata = s;
		while (!cdata.endsWith("]]")) {
			buf.append('>');
			cdata = read('>');
			buf.append(cdata);
		}
		// trim CDEnd ('>' is not included)
		buf.delete(buf.length() - 2, buf.length());
		return true;
	}
	
	// checks if '<' marks begin of a comment, and if so skips over it
	private boolean skipComment(final String s) throws KNXMLException
	{
		if (s.startsWith("!--")) {
			String comment = s;
			while (canRead() && !comment.endsWith("--"))
				comment = read('>');
			return true;
		}
		return false;
	}

	private boolean skipInstruction(final String tag)
	{
		// is this a processing instruction
		if (tag.charAt(0) == '?' && tag.charAt(tag.length() - 1) == '?')
			return true;
		return false;
	}

	private String splitOnSpace(final String s)
	{
		for (int i = 0; i < s.length(); ++i)
			if (Character.isSpaceChar(s.charAt(i)))
				return s.substring(0, i);
		return s;
	}

	private void reset()
	{
		elem = null;
		openElems.clear();
		pos = START_DOC;
		line = 0;
	}
}
