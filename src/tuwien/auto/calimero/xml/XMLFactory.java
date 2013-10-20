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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * Creates XML reader and XML writer for working with XML resources.
 * <p>
 * A {@link XMLWriter} and {@link XMLReader} is created using its default constructor,
 * i.e., every implementation is required to supply such a constructor.<br>
 * By default, the factory creates instances of types located in the <code>def</code>
 * sub package.<br>
 * The factory uses an {@link EntityResolver} to resolve XML resources.
 * <p>
 * Note that for now only one default XML reader/writer is available in this factory, and
 * therefore can't be changed.
 * 
 * @author B. Malinowsky
 */
public final class XMLFactory
{
	// if we have more than one reader/writer implementation or wrappers for other
	// XML processing tools, appropriate selection methods will be provided

	private static final String DEFAULT_READER =
		"tuwien.auto.calimero.xml.def.DefaultXMLReader";
	private static final String DEFAULT_WRITER =
		"tuwien.auto.calimero.xml.def.DefaultXMLWriter";
	private static final String DEFAULT_RESOLVER =
		"tuwien.auto.calimero.xml.def.DefaultEntityResolver";

	private XMLFactory()
	{}

	/**
	 * Gets the XML factory.
	 * <p>
	 * 
	 * @return XMLFactory object
	 */
	public static XMLFactory getInstance()
	{
		return FactoryHolder.f;
	}

	/**
	 * Creates a {@link XMLReader} to read the XML resource located by the specified
	 * identifier.
	 * <p>
	 * On closing the created XML reader, the {@link Reader} set as input for the XML
	 * reader will get closed as well.
	 * 
	 * @param systemID location identifier of the XML resource
	 * @return XML reader
	 * @throws KNXMLException if creation of the reader failed or XML resource can't be
	 *         resolved
	 */
	public XMLReader createXMLReader(final String systemID) throws KNXMLException
	{
		final XMLReader r = (XMLReader) create(DEFAULT_READER);
		final EntityResolver res = (EntityResolver) create(DEFAULT_RESOLVER);
		final InputStream is = res.resolveInput(systemID);
		try {
			r.setInput(res.getInputReader(is), true);
		}
		catch (final KNXMLException e) {
			try {
				is.close();
			}
			catch (final IOException ignore) {}
			throw e;
		}
		return r;
	}

	/**
	 * Creates a {@link XMLWriter} to write into the XML resource located by the specified
	 * identifier.
	 * <p>
	 * 
	 * @param systemID location identifier of the XML resource
	 * @return XML writer
	 * @throws KNXMLException if creation of the writer failed or XML resource can't be
	 *         resolved
	 */
	public XMLWriter createXMLWriter(final String systemID) throws KNXMLException
	{
		final XMLWriter w = (XMLWriter) create(DEFAULT_WRITER);
		final EntityResolver res = (EntityResolver) create(DEFAULT_RESOLVER);
		final OutputStream os = res.resolveOutput(systemID);
		try {
			w.setOutput(new OutputStreamWriter(os, "UTF-8"), true);
			return w;
		}
		catch (final UnsupportedEncodingException e) {
			try {
				os.close();
			}
			catch (final IOException ignore) {}
			throw new KNXMLException("encoding UTF-8 unknown");
		}
	}

	private Object create(final String className) throws KNXMLException
	{
		try {
			return Class.forName(className).newInstance();
		}
		catch (final SecurityException e) {
			throw new KNXMLException("loading XML handler not allowed, " + e.getMessage());
		}
		catch (final Exception e) {
			// InstantiationException, IllegalAccessException, ClassNotFoundException
			throw new KNXMLException("failed to create XML handler, " + e.getMessage());
		}
	}

	private static final class FactoryHolder
	{
		static final XMLFactory f = new XMLFactory();

		private FactoryHolder()
		{}
	}
}
