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
 * XML reader interface to be used with {@link javax.xml.stream.XMLStreamReader}. This interface was
 * created by extending the XMLStreamReader class, without adding any new methods. It does not
 * provide methods which are not supported on Java ME Embedded.
 *
 * @author B. Malinowsky
 */
public interface XmlReader extends AutoCloseable/*, XMLStreamReader*/
{
	int START_ELEMENT = 1;

	int END_ELEMENT = 2;

	int PROCESSING_INSTRUCTION = 3;

	int CHARACTERS = 4;

	int COMMENT = 5;

	int SPACE = 6;

	int START_DOCUMENT = 7;

	int END_DOCUMENT = 8;

	int ENTITY_REFERENCE = 9;

	int ATTRIBUTE = 10;

	int DTD = 11;

	int CDATA = 12;

	int NAMESPACE = 13;

	int NOTATION_DECLARATION = 14;

	int ENTITY_DECLARATION = 15;

	Object getProperty(java.lang.String name);

	int next() throws KNXMLException;

	void require(int type, String namespaceURI, String localName) throws KNXMLException;

	String getElementText() throws KNXMLException;

	int nextTag() throws KNXMLException;

	boolean hasNext() throws KNXMLException;

	String getNamespaceURI(String prefix);

	boolean isWhiteSpace();

	String getAttributeValue(String namespaceURI, String localName);

	int getAttributeCount();

	String getAttributeNamespace(int index);

	String getAttributeLocalName(int index);

	String getAttributePrefix(int index);

	String getAttributeType(int index);

	String getAttributeValue(int index);

	boolean isAttributeSpecified(int index);

	int getNamespaceCount();

	String getNamespacePrefix(int index);

	String getNamespaceURI(int index);

	int getEventType();

	String getText();

	char[] getTextCharacters();

	int getTextStart();

	int getTextLength();

	String getEncoding();

	StreamLocation getLocation();

	String getLocalName();

	String getNamespaceURI();

	String getPrefix();

	String getVersion();

	boolean isStandalone();

	boolean standaloneSet();

	String getCharacterEncodingScheme();

	String getPITarget();

	String getPIData();

	@Override
	void close() throws KNXMLException;
}
