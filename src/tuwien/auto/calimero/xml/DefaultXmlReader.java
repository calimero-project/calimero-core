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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Default XML reader implementation of the XmlReader interface.
 * <p>
 * Does not add any feature not already documented in the implemented interface.<br>
 * This reader is not thread safe.
 *
 * @author B. Malinowsky
 */
class DefaultXmlReader implements XmlReader
{
	private final Map<String, Object> config = new HashMap<>();

	private Reader r;

	private String elemName;
	private List<String> attributeName;
	private List<String> attributeValue;
	private String elemText;
	private boolean emptyTag;


	private final Stack<String> openElems = new Stack<>();
	private int event;
	private int line;

	// variables introduced for StaX API
	private boolean standalone;
	private String version;
	private String encoding;

	// TODO set variables
	private int textStart;
	private int textLength;


	/**
	 * Creates a new XML reader with input <code>r</code>.
	 * <p>
	 * The {@link Reader} should already be buffered or wrapped with a buffered reader, if necessary
	 * (e.g. when reading from a file).
	 *
	 * @param r a {@link Reader} for input
	 */
	DefaultXmlReader(final Reader r)
	{
		reset();
		setInput(r);
	}

	private void setInput(final Reader input)
	{
		if (r != null)
			reset();
		r = input;
	}

	private void setElement(final String name)
	{
		elemName = name;
		attributeName = new ArrayList<>();
		attributeValue = new ArrayList<>();
		elemText = null;
		emptyTag = false;
	}

	private int read() throws KNXMLException
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
				setElement(openElems.peek());
				elemText = buf.toString();
				event = XmlReader.CHARACTERS;
				return event;
			}
			str = str.trim();
			// extract element name
			String name = splitOnSpace(str);
			if (name.charAt(name.length() - 1) == '/')
				name = name.substring(0, name.length() - 1);
			if (name.charAt(0) == '/') {
				if (!name.substring(1).equals(openElems.peek()))
					throw new KNXMLException("element end tag does not match start tag",
							name.substring(1), line);
				setElement(openElems.pop());
				event = XmlReader.END_ELEMENT;
				return event;
			}
			setElement(name);
			extractAttributes(str.substring(name.length()));
			if (!emptyTag)
				openElems.push(name);
			event = XmlReader.START_ELEMENT;
			return event;
		}
		if (!openElems.empty())
			throw new KNXMLException("end of XML input with elements left open");
		event = XmlReader.END_DOCUMENT;
		return event;
	}

	private int w_read()
	{
		return read();
	}

	private void complete(final String name) throws KNXMLException
	{
		if (emptyTag)
			return;
		final int index = openElems.lastIndexOf(name);
		if (index == -1)
			throw new KNXMLException("no matching element open tag: " + name, this);
		String end = null;
		final StringBuffer content = new StringBuffer(50);
		while (canRead()) {
			// read text content
			final String s = read('<');
			final boolean current = openElems.peek().equals(name);
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
				if (tag.equals(name)) {
					elemText = content.toString();
					event = XmlReader.END_ELEMENT;
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

	private void w_complete(final String name)
	{
		complete(name);
	}

	@Override
	public void close()
	{
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
			emptyTag = true;

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
				attributeName.add(att);
				attributeValue.add(References.replace(value, false));
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
		if (tag.charAt(0) == '?' && tag.charAt(tag.length() - 1) == '?') {
			// check for decl section
			// TODO detection is already implemented in entity resolver, use that
			int idx = tag.indexOf("version");
			if (idx != -1) {
				idx = tag.indexOf("=", idx);
				version = tag.substring(idx + 2, tag.indexOf("\"", idx + 2));
			}
			idx = tag.indexOf("encoding");
			if (idx != -1) {
				idx = tag.indexOf("=", idx);
				encoding = tag.substring(idx + 2, tag.indexOf("\"", idx + 2));
			}

			return true;
		}
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
		openElems.clear();
		event = XmlReader.START_DOCUMENT;
		line = 0;
	}

	@Override
	public Object getProperty(final String name) throws IllegalArgumentException
	{
		return config.get(name);
	}

	@Override
	public int next()
	{
		return read();
	}

	@Override
	public void require(final int type, final String namespaceURI, final String localName)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String getElementText()
	{
		w_complete(elemName);
		return elemText;
	}

	@Override
	public int nextTag()
	{
		return w_read();
	}

	@Override
	public boolean hasNext()
	{
		return event != XmlReader.END_DOCUMENT;
	}

	@Override
	public String getNamespaceURI(final String prefix)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isWhiteSpace()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getAttributeValue(final String namespaceURI, final String localName)
	{
		inStartEvent();
		final int i = attributeName.indexOf(localName);
		if (i != -1)
			return attributeValue.get(i);
		return null;
	}

	private void inStartEvent()
	{
		if (event != XmlReader.START_ELEMENT)
			throw new IllegalStateException("not at XML start element");
	}

	@Override
	public int getAttributeCount()
	{
		inStartEvent();
		return attributeName.size();
	}

	@Override
	public String getAttributeNamespace(final int index)
	{
		inStartEvent();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAttributeLocalName(final int index)
	{
		inStartEvent();
		return attributeName.get(index);
	}

	@Override
	public String getAttributePrefix(final int index)
	{
		inStartEvent();
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAttributeType(final int index)
	{
		inStartEvent();
		return "CDATA";
	}

	@Override
	public String getAttributeValue(final int index)
	{
		inStartEvent();
		return attributeValue.get(index);
	}

	@Override
	public boolean isAttributeSpecified(final int index)
	{
		inStartEvent();
		return attributeName.size() > index;
	}

	@Override
	public int getNamespaceCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getNamespacePrefix(final int index)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNamespaceURI(final int index)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getEventType()
	{
		return event;
	}

	@Override
	public String getText()
	{
		return new String(getTextCharacters());
	}

	@Override
	public char[] getTextCharacters()
	{
		w_complete(elemName);
		return elemText.toCharArray();
	}

	@Override
	public int getTextStart()
	{
		return textStart;
	}

	@Override
	public int getTextLength()
	{
		return textLength;
	}

	@Override
	public String getEncoding()
	{
		if (r instanceof InputStreamReader) {
			final InputStreamReader isr = (InputStreamReader) r;
			return isr.getEncoding();
		}
		return encoding;
	}

	@Override
	public StreamLocation getLocation()
	{
		return new StreamLocation(line);
	}

	@Override
	public String getLocalName()
	{
		if (event != XmlReader.START_ELEMENT && event != XmlReader.END_ELEMENT
				&& event != XmlReader.ENTITY_REFERENCE)
			throw new IllegalStateException("no XML start/end element or entity reference");
		return elemName;
	}

	@Override
	public String getNamespaceURI()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrefix()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion()
	{
		return version;
	}

	@Override
	public boolean isStandalone()
	{
		return standalone;
	}

	@Override
	public boolean standaloneSet()
	{
		return config.containsKey("standaloneSet");
	}

	@Override
	public String getCharacterEncodingScheme()
	{
		return encoding;
	}

	@Override
	public String getPITarget()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPIData()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
