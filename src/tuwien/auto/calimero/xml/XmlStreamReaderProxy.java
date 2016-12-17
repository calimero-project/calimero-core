/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Proxies the XML stream reader interface {@link XMLStreamReader} with the derived Calimero stream
 * reader interface. This avoids a direct dependency on javax.xml.stream when using the Java SE
 * compact1 profile, or on Java ME Embedded.
 *
 * @author B. Malinowsky
 */
public final class XmlStreamReaderProxy implements XmlReader
{
	public static XmlStreamReaderProxy createXmlReader(final InputStream is)
		throws XMLStreamException, FactoryConfigurationError {
		return new XmlStreamReaderProxy(XMLInputFactory.newInstance().createXMLStreamReader(is));
	}

	public static XmlStreamReaderProxy createXmlReader(final Reader reader)
		throws XMLStreamException, FactoryConfigurationError {
		return new XmlStreamReaderProxy(XMLInputFactory.newInstance().createXMLStreamReader(reader));
	}

	final XMLStreamReader r;

	public XmlStreamReaderProxy(final XMLStreamReader reader) { r = reader; }

	@Override
	public Object getProperty(final java.lang.String name)
		throws java.lang.IllegalArgumentException
	{
		return r.getProperty(name);
	}

	@Override
	public int next() {
		try { return r.next(); } catch (final XMLStreamException e) { throw wrapped(e); }
	}

	@Override
	public void require(final int type, final String namespaceURI, final String localName) {
		try { r.require(type, namespaceURI, localName); }
		catch (final XMLStreamException e) { throw wrapped(e); }
	}

	@Override
	public String getElementText() {
		try { return r.getElementText(); } catch (final XMLStreamException e) { throw wrapped(e); }
	}

	@Override
	public int nextTag() {
		try { return r.nextTag(); } catch (final XMLStreamException e) { throw wrapped(e); }
	}

	@Override
	public boolean hasNext() {
		try { return r.hasNext(); } catch (final XMLStreamException e) { throw wrapped(e); }
	}

	@Override
	public void close() {
		try { r.close(); } catch (final XMLStreamException e) { throw wrapped(e); }
	}

	@Override
	public String getNamespaceURI(final String prefix) { return r.getNamespaceURI(prefix); }

	@Override
	public boolean isWhiteSpace() { return r.isWhiteSpace(); }

	@Override
	public String getAttributeValue(final String namespaceURI, final String localName) {
		return r.getAttributeValue(namespaceURI, localName);
	}

	@Override
	public int getAttributeCount() { return r.getAttributeCount(); }

	@Override
	public String getAttributeNamespace(final int index) { return r.getAttributeNamespace(index); }

	@Override
	public String getAttributeLocalName(final int index) { return r.getAttributeLocalName(index); }

	@Override
	public String getAttributePrefix(final int index) { return r.getAttributePrefix(index); }

	@Override
	public String getAttributeType(final int index) { return r.getAttributeType(index); }

	@Override
	public String getAttributeValue(final int index) { return r.getAttributeValue(index); }

	@Override
	public boolean isAttributeSpecified(final int index) { return r.isAttributeSpecified(index); }

	@Override
	public int getNamespaceCount() { return r.getNamespaceCount(); }

	@Override
	public String getNamespacePrefix(final int index) { return r.getNamespacePrefix(index); }

	@Override
	public String getNamespaceURI(final int index) { return r.getNamespaceURI(index); }

	@Override
	public int getEventType() { return r.getEventType(); }

	@Override
	public String getText() { return r.getText(); }

	@Override
	public char[] getTextCharacters() { return r.getTextCharacters(); }

	@Override
	public int getTextStart() { return r.getTextStart(); }

	@Override
	public int getTextLength() { return r.getTextLength(); }

	@Override
	public String getEncoding() { return r.getEncoding(); }

	@Override
	public StreamLocation getLocation() { return new StreamLocation(r.getLocation().getLineNumber()); }

	@Override
	public String getLocalName() { return r.getLocalName(); }

	@Override
	public String getNamespaceURI() { return r.getNamespaceURI(); }

	@Override
	public String getPrefix() { return r.getPrefix(); }

	@Override
	public String getVersion() { return r.getVersion(); }

	@Override
	public boolean isStandalone() { return r.isStandalone(); }

	@Override
	public boolean standaloneSet() { return r.standaloneSet(); }

	@Override
	public String getCharacterEncodingScheme() { return r.getCharacterEncodingScheme(); }

	@Override
	public String getPITarget() { return r.getPITarget(); }

	@Override
	public String getPIData() { return r.getPIData(); }

	private KNXMLException wrapped(final XMLStreamException e) { return new KNXMLException(e.getMessage(), e); }
}
