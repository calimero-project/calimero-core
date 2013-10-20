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

package tuwien.auto.calimero.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.log.LogService;

/**
 * Adapter for Java ME CDC javax.microedition.io.CommConnection.
 * <p>
 * 
 * @author B. Malinowsky
 */
class CommConnectionAdapter extends LibraryAdapter
{
	private static final Class connector;

	private Object conn;
	private InputStream is;
	private OutputStream os;

	static {
		Class clazz = null;
		try {
			clazz = Class.forName("javax.microedition.io.Connector");
		}
		catch (final ClassNotFoundException e) {}
		connector = clazz;
	}

	CommConnectionAdapter(final LogService logger, final String portId, final int baudrate)
		throws KNXException
	{
		super(logger);
		if (!isAvailable())
			throw new KNXException("no ME CDC environment, Connector factory missing");
		open(portId, baudrate);
	}

	/**
	 * Returns whether the ME CDC Connector class is available or not, and therefore,
	 * checks if CommConnectionAdapter functionality can be used.
	 * <p>
	 * This method does not check if the actual serial protocol connection is supported.
	 * 
	 * @return <code>true</code>, if running in a ME CDC environment, and CommConnection
	 *         can be queried, <code>false</code> otherwise
	 */
	static boolean isAvailable()
	{
		return connector != null;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.serial.LibraryAdapter#close()
	 */
	public void close() throws IOException
	{
		try {
			invoke(conn, "close", null);
		}
		catch (final InvocationTargetException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
		}
		catch (final Exception ignore) {}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.serial.LibraryAdapter#getInputStream()
	 */
	public InputStream getInputStream()
	{
		return is;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.serial.LibraryAdapter#getOutputStream()
	 */
	public OutputStream getOutputStream()
	{
		return os;
	}

	private void open(final String portID, final int baudrate) throws KNXException
	{
		Object cc = null;
		try {
			// query a CommConnection instance
			cc = invoke(connector, "open", new String[] { "comm:" + portID + ";baudrate="
					+ baudrate + ";bitsperchar=8;stopbits=1;parity=even;autocts=off;autorts=off" });
			is = (InputStream) invoke(cc, "openInputStream", null);
			os = (OutputStream) invoke(cc, "openOutputStream", null);
			conn = cc;
			return;
		}
		catch (final SecurityException e) {
			logger.error("CommConnection access denied", e);
		}
		catch (final InvocationTargetException e) {
			// usually, the cause will hold a ConnectionNotFoundException
			logger.error("CommConnection: " + e.getCause().getMessage());
		}
		// NoSuchMethodException, IllegalAccessException, IllegalArgumentException
		catch (final Exception e) {}
		try {
			invoke(cc, "close", null);
			is.close();
			os.close();
		}
		catch (final Exception mainlyNPE) {}
		throw new KNXException("failed to open CommConnection");
	}
}
