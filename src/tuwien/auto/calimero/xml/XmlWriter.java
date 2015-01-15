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

/**
 * XML writer interface to be used with {@link javax.xml.stream.XMLStreamWriter}. This interface was
 * created by extending the XMLStreamWriter class, without adding any new methods. It does not
 * provide methods which are not supported on Java ME Embedded.
 *
 * @author B. Malinowsky
 */
public interface XmlWriter extends AutoCloseable //, XMLStreamWriter
{
	void writeStartElement(String localName);

	void writeStartElement(String namespaceURI, String localName);

	void writeStartElement(String prefix, String localName, String namespaceURI);

	void writeEmptyElement(String namespaceURI, String localName);

	void writeEmptyElement(String prefix, String localName, String namespaceURI);

	void writeEmptyElement(String localName);

	void writeEndElement();

	void writeEndDocument();

	void writeAttribute(String localName, String value);

	void writeAttribute(String prefix, String namespaceURI, String localName, String value);

	void writeAttribute(String namespaceURI, String localName, String value);

	void writeNamespace(String prefix, String namespaceURI);

	void writeDefaultNamespace(String namespaceURI);

	void writeComment(String data);

	void writeProcessingInstruction(String target);

	void writeProcessingInstruction(String target, String data);

	void writeCData(String data);

	void writeDTD(String dtd);

	void writeEntityRef(String name);

	void writeStartDocument();

	void writeStartDocument(String version);

	void writeStartDocument(String encoding, String version);

	void writeCharacters(String text);

	void writeCharacters(char[] text, int start, int len);

	String getPrefix(String uri);

	void setPrefix(String prefix, String uri);

	void setDefaultNamespace(String uri);

	Object getProperty(java.lang.String name);

	void flush();

	@Override
	void close();
}
