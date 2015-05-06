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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;

/**
 * Creates XML stream readers for working with XML resources. The factory tries to obtain a platform
 * reader implementation; if that fails, it might fall back on an internal minimal stream reader
 * implementation.
 *
 * @author B. Malinowsky
 */
public final class XmlInputFactory // extends XMLInputFactory
{
	public static boolean INTERNAL_ONLY = false;

	private static final XmlInputFactory f = new XmlInputFactory();

	private final Map<String, Object> config = new HashMap<>();

	private XmlInputFactory() {}

	/**
	 * Gets the XML factory.
	 * <p>
	 *
	 * @return XmlInputFactory object
	 */
	// new instance
	public static XmlInputFactory getInstance()
	{
		return f;
	}

	/**
	 * Creates a {@link XmlReader} to read the XML resource located by the specified identifier.
	 * <p>
	 * On closing the created XML reader, the {@link Reader} set as input for the XML reader will
	 * get closed as well.
	 *
	 * @param baseUri location identifier of the XML resource
	 * @return XML reader
	 * @throws KNXMLException if creation of the reader failed or XML resource can't be resolved
	 */
	// XXX not from stream API
	public XmlReader createXMLReader(final String baseUri) throws KNXMLException
	{
		final EntityResolver res = new EntityResolver();
		final InputStream is = (InputStream) res.resolveEntity(null, null, baseUri, null);
		return create(res, is);
	}

	private Object createReader(final String className)
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

	private XmlReader create(final EntityResolver resolver, final InputStream is)
	{
		if (!INTERNAL_ONLY) {
			System.out.println("lookup system-provided XMLStreamReader");
			try {
				final XmlStreamReaderProxy r = XmlStreamReaderProxy.createXmlReader(is);
				System.out.println("using StaX XMLStreamReader " + r.r.getClass().getName());
				return r;
			}
			catch (Exception | Error e) {
				e.printStackTrace();
				// we fall back on our own minimal implementation
				throw new KNXMLException("no StaX implementation found", e);
			}
		}
//		System.out.println("using internal minimal XMLStreamReader implementation");
//		final DefaultXmlReader r = new DefaultXmlReader();
//		r.setInput(resolver.getInputReader(is));
//		return r;
		throw new KNXMLException("no XML stream reader available");
	}

	public XmlReader createXMLStreamReader(final Reader reader)
	{
		if (!INTERNAL_ONLY) {
			System.out.println("lookup system-provided XMLStreamReader");
			try {
				final XmlStreamReaderProxy r = XmlStreamReaderProxy.createXmlReader(reader);
				System.out.println("using StaX XMLStreamReader " + r.r.getClass().getName());
				return r;
			}
			catch (Exception | Error e) {
				e.printStackTrace();
				// we fall back on our own minimal implementation
				throw new KNXMLException("no StaX implementation found", e);
			}
		}
//		final DefaultXmlReader r = new DefaultXmlReader();
//		r.setInput(reader);
//		return r;
		throw new KNXMLException("no XML stream reader available");
	}

	public XmlReader createXMLStreamReader(final InputStream stream)
	{
		return createXMLStreamReader(new InputStreamReader(stream));
	}

	public XmlReader createXMLStreamReader(final InputStream stream, final String encoding)
	{
		try {
			return createXMLStreamReader(new InputStreamReader(stream, encoding));
		}
		catch (final UnsupportedEncodingException e) {
			throw new KNXMLException("XML stream reader with encoding " + encoding, e);
		}
	}

	public XmlReader createXMLStreamReader(final String systemId, final InputStream stream)
	{
		return createXMLStreamReader(stream);
	}

	public XmlReader createXMLStreamReader(final String systemId, final Reader reader)
	{
		return createXMLStreamReader(reader);
	}

	public XmlReader createFilteredReader(final XmlReader reader, final StreamFilter filter)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public XMLResolver getXMLResolver()
	{
		return (XMLResolver) config.get(XMLInputFactory.RESOLVER);
	}

	public void setXMLResolver(final XMLResolver resolver)
	{
		config.put(XMLInputFactory.RESOLVER, resolver);
	}

	public void setProperty(final String name, final Object value) throws IllegalArgumentException
	{
		config.put(name, value);
	}

	public Object getProperty(final String name) throws IllegalArgumentException
	{
		return config.get(name);
	}

	public boolean isPropertySupported(final String name)
	{
		return config.containsKey(name) && config.get(name).equals(Boolean.TRUE);
	}
}
