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

package tuwien.auto.calimero.serial;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;

import tuwien.auto.calimero.KNXException;

/**
 * Adapter to access a serial communication port using some serial I/O library.
 * <p>
 * Subtypes of this class implementing the access to a specific library have to declare a public
 * constructor expecting a String and an <code>int</code> argument, i.e.,<br>
 * <code>public MyAdapter(String portID, int baudrate)</code> for a class named "MyAdapter". The
 * <code>portID</code> argument identifies the communication port on which to open the connection,
 * the <code>baudrate</code> argument specifies the requested baud rate for communication.<br>
 * Invoking that constructor will open the serial port according the supplied arguments.
 * <p>
 * After closing a library adapter, method behavior is undefined.
 *
 * @author B. Malinowsky
 */
public abstract class LibraryAdapter implements Closeable
{
	/**
	 * The log service to use, supplied in the constructor; if a sub-class of LibraryAdapter does
	 * not use logger, it might be set to null.
	 */
	protected final Logger logger;

	/** Returns all available serial communication port identifiers. */
	public static List<String> getPortIdentifiers()
	{
		try {
			final String ports = System.getProperty("microedition.commports");
			if (ports != null)
				return Arrays.asList(ports.split(",")).stream().collect(Collectors.toList());
		}
		catch (final SecurityException e) {}
		if (SerialComAdapter.isAvailable()) {
			final List<String> ports = new ArrayList<>();
			Arrays.asList(defaultPortPrefixes()).forEach(p -> IntStream.range(0, 20)
					.filter(i -> SerialComAdapter.portExists(p + i)).forEach(i -> ports.add(p + i)));
			return ports;
		}
		try {
			final Class<?> c = Class.forName("tuwien.auto.calimero.serial.RxtxAdapter");
			@SuppressWarnings("unchecked")
			final List<String> ports = (List<String>) c.getMethod("getPortIdentifiers").invoke(null);
			return ports;
		}
		catch (Exception | NoClassDefFoundError e) {}
		return Collections.emptyList();
	}

	private static String[] defaultPortPrefixes()
	{
		return System.getProperty("os.name").toLowerCase().indexOf("windows") > -1 ? new String[] { "\\\\.\\COM" }
				: new String[] { "/dev/ttyS", "/dev/ttyACM", "/dev/ttyUSB" };
	}

	/**
	 * Factory method to open a serial connection using one of the available library adapters.
	 *
	 * @param logger logger
	 * @param portId serial port identifier
	 * @param baudrate baudrate
	 * @param idleTimeout idle timeout in milliseconds
	 * @return adapter to access serial communication port, port resource is in open state
	 * @throws KNXException on failure to open or configure serial port, or no adapter available
	 */
	public static LibraryAdapter open(final Logger logger, final String portId, final int baudrate,
		final int idleTimeout) throws KNXException
	{
		Throwable t = null;
		// check for Java ME Embedded platform and available serial communication port,
		// protocol support for communication ports is optional
		if (CommConnectionAdapter.isAvailable()) {
			logger.debug("open Java ME serial port connection (CommConnection) for {}", portId);
			try {
				return new CommConnectionAdapter(logger, portId, baudrate);
			}
			catch (final KNXException e) {
				t = e;
			}
		}
		// check internal support for serial port access
		// protocol support available for Win 32/64 platforms
		// (so we provide serial port access at least on platforms with ETS)
		if (SerialComAdapter.isAvailable()) {
			logger.debug("open Calimero native serial port connection (serialcom) for {}", portId);
			SerialComAdapter conn = null;
			try {
				conn = new SerialComAdapter(logger, portId);
				conn.setBaudRate(baudrate);
				//final int idleTimeout = idleTimeout(conn.getBaudRate());
				conn.setTimeouts(new SerialComAdapter.Timeouts(idleTimeout, 0, 250, 0, 0));
				conn.setParity(SerialComAdapter.PARITY_EVEN);
				conn.setControl(SerialComAdapter.STOPBITS, SerialComAdapter.ONE_STOPBIT);
				conn.setControl(SerialComAdapter.DATABITS, 8);
				conn.setControl(SerialComAdapter.FLOWCTRL, SerialComAdapter.FLOWCTRL_NONE);
				logger.info("setup serial port: baudrate " + conn.getBaudRate() + ", even parity, "
						+ conn.getControl(SerialComAdapter.DATABITS) + " databits, "
						+ conn.getControl(SerialComAdapter.STOPBITS) + " stopbits, timeouts: "
						+ conn.getTimeouts());
				return conn;
			}
			catch (final IOException e) {
				if (conn != null)
					conn.close();
				t = e;
			}
		}
		try {
			final Class<?> c = Class.forName("tuwien.auto.calimero.serial.RxtxAdapter");
			logger.debug("using rxtx library for serial port access");
			final Class<? extends LibraryAdapter> adapter = LibraryAdapter.class;
			return adapter.cast(c.getConstructors()[0]
					.newInstance(new Object[] { logger, portId, Integer.valueOf(baudrate) }));
		}
		catch (final ClassNotFoundException e) {
			logger.warn("no rxtx library adapter found");
		}
		catch (final Exception | NoClassDefFoundError e) {
			t = e instanceof InvocationTargetException ? e.getCause() : e;
		}

		if (t instanceof KNXException)
			throw (KNXException) t;
		if (t != null)
			throw new KNXException("failed to open serial port " + portId, t);
		throw new KNXException("no serial adapter available to open " + portId);
	}

	/**
	 * Creates a new library adapter.
	 *
	 * @param logService the log service to use for this adapter
	 */
	protected LibraryAdapter(final Logger logService)
	{
		logger = logService;
	}

	/**
	 * Returns the output stream for the opened serial communication port.
	 * <p>
	 * Subsequent invocations might return the same or a new stream object.
	 *
	 * @return the OutputStream object
	 */
	public abstract OutputStream getOutputStream();

	/**
	 * Returns the input stream for the opened serial communication port.
	 * <p>
	 * Subsequent invocations might return the same or a new stream object.
	 *
	 * @return the InputStream object
	 */
	public abstract InputStream getInputStream();

	/**
	 * Sets a new baud rate for this connection.
	 *
	 * @param baudrate requested baud rate [Bit/s], 0 &lt; baud rate
	 */
	public void setBaudRate(final int baudrate)
	{
		try {
			invoke(this, "setBaudRate", new Object[] { Integer.valueOf(baudrate) });
		}
		catch (final Exception e) {}
	}

	/**
	 * Returns the currently used baud rate.
	 * <p>
	 *
	 * @return baud rate in bit/s
	 */
	public int getBaudRate()
	{
		try {
			return ((Integer) invoke(this, "getBaudRate", null)).intValue();
		}
		catch (final Exception e) {}
		return 0;
	}

	@Override
	public abstract void close();

	/**
	 * Invokes <code>method</code> name on object <code>obj</code> with arguments <code>args</code>.
	 * <p>
	 * Arguments wrapped in an object of type Integer are replaced with the primitive int type when
	 * looking up the method name.
	 *
	 * @param obj object on which to invoke the method
	 * @param method method name
	 * @param args list of arguments
	 * @return the result of the invoked method
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @see Class#getMethod(String, Class[])
	 * @see Method#invoke(Object, Object[])
	 */
	protected Object invoke(final Object obj, final String method, final Object[] args)
		throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
	{
		final Class<?>[] c = new Class<?>[args == null ? 0 : args.length];
		for (int i = 0; i < c.length; ++i) {
			c[i] = args[i].getClass();
			if (c[i] == Integer.class)
				c[i] = int.class;
		}
		try {
			if (obj instanceof Class)
				return ((Class<?>) obj).getMethod(method, c).invoke(null, args);
			return obj.getClass().getMethod(method, c).invoke(obj, args);
		}
		catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("illegal argument on invoking "
					+ obj.getClass().getName() + "." + method + ": " + e.getMessage());
		}
	}
}
