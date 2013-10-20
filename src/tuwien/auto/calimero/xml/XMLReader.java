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

package tuwien.auto.calimero.xml;

import java.io.Reader;

/**
 * XML reader interface for pull based reading of XML documents.
 * <p>
 * The reader only does non-validating processing, and resolves predefined XML entities.
 * <br>
 * This interface does not support XML namespaces for now, and has no methods for setting
 * and requesting XML reader features/properties.<br>
 * A reader based on pull parsing style only reads a little piece of a document at once,
 * waiting for the user application to request the next chunk of data. So the application
 * controls the progress of reading a XML document. The reader does not create nor
 * maintain a representation of the entire document for the user (like a model parser
 * would).
 * <p>
 * XML reader implementations don't need to be thread safe.
 * 
 * @author B. Malinowsky
 */
public interface XMLReader
{
	// The XML reader was using the
	// Extensible Markup Language (XML) 1.0 (Fourth Edition)
	// W3C Recommendation 16 August 2006, edited in place 29 September 2006,
	// but the current implementation status of the default reader is done only
	// up to give necessary support to handle library XML stuff and not more.
	// If support for the XML schema for ETS configuration data is added,
	// I will either have a more in-depth look to complete the one or other part, or
	// provide an adapter to an external, full-fledged XML parser.

	/** No input specified. */
	int NO_INPUT = 0;
	/** Start of XML document. */
	int START_DOC = 1;
	/** End of XML document. */
	int END_DOC = 2;
	/** Start of XML tag. */
	int START_TAG = 3;
	/** End of XML tag. */
	int END_TAG = 4;
	/**
	 * Character data of an element.
	 * <p>
	 * This position identifier is especially important on mixed content in an element.<br>
	 * (Definition: An element type has mixed content when elements of that type may
	 * contain character data, optionally interspersed with child elements.)<br>
	 * So this position only occurs between {@link #START_TAG} and {@link #END_TAG}.
	 * Anyway, usually there is no mixed content and {@link #complete(Element)} will be
	 * used after a START_TAG to read all character data into the supplied current
	 * element.
	 */
	int CHAR_DATA = 5;

	/**
	 * Sets the input source for this XML reader.
	 * <p>
	 * If this XML reader was already closed, setting a new input has no effect.
	 * 
	 * @param input a reader with input, like obtained from
	 *        {@link EntityResolver#getInputReader(java.io.InputStream)}
	 * @param close <code>true</code> if the specified input reader should be closed on
	 *        {@link #close()}, <code>false</code> to leave it open
	 */
	void setInput(Reader input, boolean close);

	/**
	 * Reads to the next XML element tag, a CDATA section or character data.
	 * <p>
	 * The logical position returned by this method can also be obtained with
	 * {@link #getPosition()}.<br>
	 * Comments and processing instructions can be ignored, i.e., skipped on reading.
	 * 
	 * @return the current logical position after reading
	 * @throws KNXMLException on read error or a not well-formed XML document
	 */
	int read() throws KNXMLException;

	/**
	 * Reads until end of element <code>e</code>.
	 * <p>
	 * All gathered relevant information is stored into <code>e</code>.
	 * 
	 * @param e the element to be completed
	 * @throws KNXMLException if document is not well-formed, e.g. end of input or end-tag
	 *         of parent element was reached before end of specified element
	 */
	void complete(Element e) throws KNXMLException;

	/**
	 * Returns the current element read with the last invocation of {@link #read()}.
	 * <p>
	 * 
	 * @return current element or <code>null</code> if no element is available
	 */
	Element getCurrent();

	/**
	 * Returns the current logical position in a XML document for this XML reader.
	 * <p>
	 * The position is given with constants like {@link #START_DOC} or similar. If no
	 * input was set or the reader is closed, {@link #NO_INPUT} is returned.
	 * 
	 * @return the logical position in the document
	 */
	int getPosition();

	/**
	 * Returns the line number for the current position in a XML input source processed by
	 * this XML reader.
	 * <p>
	 * On no input source, or before an input source is first read, 0 is returned.
	 * 
	 * @return line number, or 0 if no line number is available
	 */
	int getLineNumber();

	/**
	 * Closes this XML reader.
	 * <p>
	 * If this XML reader is already closed, no action is performed. If for an input
	 * source was specified to get closed on invocation of this method, it is closed first
	 * (calling {@link Reader#close()}.
	 * 
	 * @throws KNXMLException
	 */
	void close() throws KNXMLException;
}
