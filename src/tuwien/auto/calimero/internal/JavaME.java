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

package tuwien.auto.calimero.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.microedition.io.Connector;

/**
 * @author B. Malinowsky
 */
public final class JavaME
{
	static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("calimero");
	
	public static <T> List<T> synchronizedList()
	{
		return new Vector<>();
	}

	public static <T> List<T> synchronizedList(final Collection<T> c)
	{
		return new Vector<>(c);
	}

	public static <K, V> Map<K, V> synchronizedMap(final Map<K, V> map)
	{
		// NYI Java8ME
		return map;
//		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static <K, V> Map<K, V> synchronizedMap()
	{
		// NYI Java8ME
		return new HashMap<>();
//		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static <T> List<T> unmodifiableList(final List<T> list)
	{
		return new ArrayList(list);
	}

	public static <T> Collection<T> unmodifiableCollection(final Collection<T> coll)
	{
		return coll;
		// NYI Java8ME
//		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static int hashCode(final byte a[])
	{
		if (a == null)
			return 0;
		final int prime = 31;
		int result = 1;
		for (final byte e : a)
			result = prime * result + e;
		return result;
	}

	public static OutputStream newFileOutputStream(final String fileName) throws IOException
	{
		final OutputStream os = Connector.openOutputStream("file://localhost" + fileName);
		return os;
	}

	public static InputStream newFileInputStream(final String fileName) throws IOException
	{
		final InputStream is = Connector.openInputStream("file://localhost" + fileName);
		return is;
	}

	public static OutputStream newUrlOutputStream(final String resource) throws IOException
	{
		final OutputStream os = Connector.openOutputStream(resource);
		return os;
	}

	public static InputStream newUrlInputStream(final String resource) throws IOException
	{
		final InputStream is = Connector.openInputStream(resource);
		return is;
	}
}
