/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015 B. Malinowsky

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

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Proxies the XML stream writer interface {@link XMLStreamWriter} with the derived Calimero stream
 * writer interface. This avoids a direct dependency on javax.xml.stream when using the Java SE
 * compact1 profile, or on Java ME Embedded.
 *
 * @author B. Malinowsky
 */
public final class XmlStreamWriterProxy implements XmlWriter
{
	final XMLStreamWriter w;

	public static XmlStreamWriterProxy createXmlStreamWriter(final OutputStream stream)
		throws XMLStreamException, FactoryConfigurationError
	{
		return new XmlStreamWriterProxy(XMLOutputFactory.newInstance()
				.createXMLStreamWriter(stream));
	}

	public static XmlStreamWriterProxy createXmlStreamWriter(final Writer stream)
		throws XMLStreamException, FactoryConfigurationError
	{
		return new XmlStreamWriterProxy(XMLOutputFactory.newInstance()
				.createXMLStreamWriter(stream));
	}

	public XmlStreamWriterProxy(final XMLStreamWriter writer)
	{
		w = writer;
	}

	@Override
	public void writeStartElement(final String localName)
	{
		try {
			w.writeStartElement(localName);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeStartElement", e);
		}
	}

	@Override
	public void writeStartElement(final String namespaceURI, final String localName)
	{
		try {
			w.writeStartElement(namespaceURI, localName);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeStartElement", e);
		}
	}

	@Override
	public void writeStartElement(final String prefix, final String localName,
		final String namespaceURI)
	{
		try {
			w.writeStartElement(prefix, localName, namespaceURI);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeStartElement", e);
		}
	}

	@Override
	public void writeEmptyElement(final String namespaceURI, final String localName)
	{
		try {
			w.writeEmptyElement(namespaceURI, localName);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeEmptyElement", e);
		}
	}

	@Override
	public void writeEmptyElement(final String prefix, final String localName,
		final String namespaceURI)
	{
		try {
			w.writeEmptyElement(prefix, localName, namespaceURI);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeEmptyElement", e);
		}
	}

	@Override
	public void writeEmptyElement(final String localName)
	{
		try {
			w.writeEmptyElement(localName);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeEmptyElement", e);
		}
	}

	@Override
	public void writeEndElement()
	{
		try {
			w.writeEndElement();
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeEndElement", e);
		}
	}

	@Override
	public void writeEndDocument()
	{
		try {
			w.writeEndDocument();
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeEndDocument", e);
		}
	}

	@Override
	public void close()
	{
		try {
			w.close();
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("close", e);
		}
	}

	@Override
	public void flush()
	{
		try {
			w.flush();
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("flush", e);
		}
	}

	@Override
	public void writeAttribute(final String localName, final String value)
	{
		try {
			w.writeAttribute(localName, value);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeAttribute", e);
		}
	}

	@Override
	public void writeAttribute(final String prefix, final String namespaceURI,
		final String localName, final String value)
	{
		try {
			w.writeAttribute(prefix, namespaceURI, localName, value);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeAttribute", e);
		}
	}

	@Override
	public void writeAttribute(final String namespaceURI, final String localName, final String value)
	{
		try {
			w.writeAttribute(namespaceURI, localName, value);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeAttribute", e);
		}
	}

	@Override
	public void writeNamespace(final String prefix, final String namespaceURI)
	{
		try {
			w.writeNamespace(prefix, namespaceURI);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeNamespace", e);
		}
	}

	@Override
	public void writeDefaultNamespace(final String namespaceURI)
	{
		try {
			w.writeDefaultNamespace(namespaceURI);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeDefaultNamespace", e);
		}
	}

	@Override
	public void writeComment(final String data)
	{
		try {
			w.writeComment(data);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeComment", e);
		}
	}

	@Override
	public void writeProcessingInstruction(final String target)
	{
		try {
			w.writeProcessingInstruction(target);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeProcessingInstruction", e);
		}
	}

	@Override
	public void writeProcessingInstruction(final String target, final String data)
	{
		try {
			w.writeProcessingInstruction(target, data);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeProcessingInstruction", e);
		}
	}

	@Override
	public void writeCData(final String data)
	{
		try {
			w.writeCData(data);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeCData", e);
		}
	}

	@Override
	public void writeDTD(final String dtd)
	{
		try {
			w.writeDTD(dtd);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeDTD", e);
		}
	}

	@Override
	public void writeEntityRef(final String name)
	{
		try {
			w.writeEntityRef(name);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeEntityRef", e);
		}
	}

	@Override
	public void writeStartDocument()
	{
		try {
			w.writeStartDocument();
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeStartDocument", e);
		}
	}

	@Override
	public void writeStartDocument(final String version)
	{
		try {
			w.writeStartDocument(version);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeStartDocument", e);
		}
	}

	@Override
	public void writeStartDocument(final String encoding, final String version)
	{
		try {
			w.writeStartDocument(encoding, version);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeStartDocument", e);
		}
	}

	@Override
	public void writeCharacters(final String text)
	{
		try {
			w.writeCharacters(text);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeCharacters", e);
		}
	}

	@Override
	public void writeCharacters(final char[] text, final int start, final int len)
	{
		try {
			w.writeCharacters(text, start, len);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("writeCharacters", e);
		}
	}

	@Override
	public String getPrefix(final String uri)
	{
		return null;
	}

	@Override
	public void setPrefix(final String prefix, final String uri)
	{
		try {
			w.setPrefix(prefix, uri);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("setPrefix", e);
		}
	}

	@Override
	public void setDefaultNamespace(final String uri)
	{
		try {
			w.setDefaultNamespace(uri);
		}
		catch (final XMLStreamException e) {
			throw new KNXMLException("setDefaultNamespace", e);
		}
	}

	@Override
	public Object getProperty(final String name)
	{
		return w.getProperty(name);
	}
}
