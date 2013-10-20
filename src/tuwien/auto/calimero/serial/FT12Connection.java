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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.StringTokenizer;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.exception.KNXAckTimeoutException;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Provides a connection based on the FT1.2 protocol for communication with a BCU2 device.
 * <p>
 * It supports establishing a connection over serial port identifiers recognized and
 * supported by the underlying operating system.
 * <p>
 * Access to log event information:<br>
 * See the corresponding constructors of this class for how to get the associated log
 * service.
 * 
 * @author B. Malinowsky
 */
public class FT12Connection
{
	/**
	 * State of communication: in idle state, no error, ready to send.
	 * <p>
	 */
	public static final int OK = 0;

	/**
	 * State of communication: in closed state, no send possible.
	 * <p>
	 */
	public static final int CLOSED = 1;

	/**
	 * Status code of communication: waiting for acknowledge after send, no error, not
	 * ready to send.
	 * <p>
	 */
	public static final int ACK_PENDING = 2;

	// default used baud rate for BAU
	private static final int DEFAULT_BAUDRATE = 19200;

	// primary station control field
	private static final int DIR_FROM_BAU = 0x80;
	private static final int INITIATOR = 0x40;
	private static final int FRAMECOUNT_BIT = 0x20;

	// frame count valid is always set on sending user data
	private static final int FRAMECOUNT_VALID = 0x10;
	private static final int ACK = 0xE5;
	// primary station service codes
	private static final int RESET = 0x00;
	private static final int USER_DATA = 0x03;
	private static final int REQ_STATUS = 0x09;
	// secondary station control field (used only for response to REQ_STATUS)
	// reserved bit, always 0
	// private static final int RESERVED = 0x20;
	// secondary station service codes
	// private static final int CONFIRM_ACK = 0x00;
	// private static final int CONFIRM_NACK = 0x01;
	// private static final int RESPONSE_STATUS = 0x0B;

	// timeout for end of frame exchange [Bits]
	private static final int EXCHANGE_TIMEOUT = 512;
	// limit for retransmissions of discarded frames
	private static final int REPEAT_LIMIT = 3;
	// maximum time between two frame characters, minimum time to indicate error [Bits]
	private static final int IDLE_TIMEOUT = 33;

	// frame delimiter characters
	private static final int START = 0x68;
	private static final int START_FIXED = 0x10;
	private static final int END = 0x16;

	private final LogService logger;

	// adapter for the used serial I/O connection:
	// - on ME CDC platforms with CommConnection available, CommConnectionAdapter is used
	// - for Calimero 2 native I/O, SerialComAdapter is used
	// - with rx/tx available, RxtxAdapter is used
	// - or some external serial I/O library adapter
	private LibraryAdapter adapter;

	private String port;
	private InputStream is;
	private OutputStream os;
	private volatile int state = CLOSED;

	private Receiver receiver;
	private final Object lock = new Object();
	private int sendFrameCount;
	private int rcvFrameCount;
	private int exchangeTimeout;
	private int idleTimeout;

	private final EventListeners listeners = new EventListeners();

	/**
	 * Creates a new connection to a BCU2 using the FT1.2 protocol.
	 * <p>
	 * If the port to use can not be told just by the number, use
	 * {@link FT12Connection#FT12Connection(String)}.<br>
	 * The baud rate is set to 19200.<br>
	 * The associated log service to which the created instance will output logging events
	 * is named "FT1.2 <code>portNumber</code>", with <code>portNumber</code> being the
	 * supplied port parameter value. If a log writer wants to receive all log events
	 * created during establishment of this FT1.2 connection, use
	 * {@link LogManager#getLogService(String)} before invoking this constructor and add
	 * the writer.
	 * 
	 * @param portNumber port number of the serial communication port to use; mapped to
	 *        the default port identifier using this number (device and platform specific)
	 * @throws KNXException on port not found or access error, initializing port settings
	 *         failed, if reset of BCU2 failed
	 */
	public FT12Connection(final int portNumber) throws KNXException
	{
		this(Integer.toString(portNumber), defaultPortPrefixes()[0] + portNumber, DEFAULT_BAUDRATE);
	}

	/**
	 * Creates a new connection to a BCU2 using the FT1.2 protocol.
	 * <p>
	 * The baud rate is set to 19200.<br>
	 * The associated log service to which the created instance will output logging events
	 * is named "FT1.2 <code>portId</code>", with <code>portId</code> being the supplied
	 * port identifier. If a log writer wants to receive all log events created during
	 * establishment of this FT1.2 connection, use
	 * {@link LogManager#getLogService(String)} before invoking this constructor and add
	 * the writer.
	 * 
	 * @param portId port identifier of the serial communication port to use
	 * @throws KNXException on port not found or access error, initializing port settings
	 *         failed, if reset of BCU2 failed
	 */
	public FT12Connection(final String portId) throws KNXException
	{
		this(portId, portId, DEFAULT_BAUDRATE);
	}

	/**
	 * Creates a new connection to a BCU2 using the FT1.2 protocol, and set the baud rate
	 * for communication.
	 * <p>
	 * If the requested baud rate is not supported, it may get substituted with a valid
	 * baud rate by default.<br>
	 * For access to the log service, see {@link #FT12Connection(String)}.
	 * 
	 * @param portId port identifier of the serial communication port to use
	 * @param baudrate baud rate to use for communication, 0 &lt; baud rate
	 * @throws KNXException on port not found or access error, initializing port settings
	 *         failed, if reset of BCU2 failed
	 */
	public FT12Connection(final String portId, final int baudrate) throws KNXException
	{
		this(portId, portId, baudrate);
	}

	private FT12Connection(final String originalPortId, final String portId,
		final int baudrate) throws KNXException
	{
		logger = LogManager.getManager().getLogService("FT1.2 " + originalPortId);
		open(portId, baudrate);
		try {
			sendReset();
		}
		catch (final KNXAckTimeoutException e) {
			close(false, "acknowledgment timeout on sending reset");
			throw e;
		}
	}

	/**
	 * Attempts to gets the available serial communication ports on the host.
	 * <p>
	 * At first, the Java system property "microedition.commports" is queried. If there is no
	 * property with that key, and the Calimero library has access to serial ports, the lowest 10
	 * port numbers of each of the default system name prefixes are checked if present.<br>
	 * The empty array is returned if no ports are discovered.
	 * 
	 * @return array of strings with found port identifiers
	 */
	public static String[] getPortIdentifiers()
	{
		String ports = null;
		try {
			ports = System.getProperty("microedition.commports");
		}
		catch (final SecurityException e) {}
		if (ports != null) {
			final StringTokenizer st = new StringTokenizer(ports, ",");
			final String[] portIds = new String[st.countTokens()];
			for (int i = 0; i < portIds.length; ++i)
				portIds[i] = st.nextToken();
			return portIds;
		}
		if (SerialComAdapter.isAvailable()) {
			final String[] prefixes = defaultPortPrefixes();
			final List l = new ArrayList(10);
			for (int k = 0; k < prefixes.length; k++) {
				final String prefix = prefixes[k];
				for (int i = 0; i < 10; ++i)
					if (SerialComAdapter.portExists(prefix + i))
						l.add(prefix + i);
			}
			return (String[]) l.toArray(new String[l.size()]);
		}
		// skip other possible adapters for now, and return empty list...
		return new String[0];
	}

	/**
	 * Adds the specified event listener <code>l</code> to receive events from this
	 * connection.
	 * <p>
	 * If <code>l</code> was already added as listener, no action is performed.
	 * 
	 * @param l the listener to add
	 */
	public void addConnectionListener(final KNXListener l)
	{
		listeners.add(l);
	}

	/**
	 * Removes the specified event listener <code>l</code>, so it does no longer receive
	 * events from this connection.
	 * <p>
	 * If <code>l</code> was not added in the first place, no action is performed.
	 * 
	 * @param l the listener to remove
	 */
	public void removeConnectionListener(final KNXListener l)
	{
		listeners.remove(l);
	}

	/**
	 * Returns the port identifier in use for this connection.
	 * <p>
	 * After the connection is closed, the returned identifier will always be the empty
	 * string.
	 * 
	 * @return port identifier as string, or empty string
	 */
	public final String getPortID()
	{
		return state == CLOSED ? "" : port;
	}

	/**
	 * Sets a new baud rate for this connection.
	 * <p>
	 * 
	 * @param baud requested baud rate [bit/s], 0 &lt; baud rate
	 */
	public void setBaudrate(final int baud)
	{
		adapter.setBaudRate(baud);
	}

	/**
	 * Returns the currently used baud rate.
	 * <p>
	 * After closing the connection, the returned baud rate is 0 by default.
	 * 
	 * @return baud rate in Bit/s
	 */
	public final int getBaudRate()
	{
		return adapter.getBaudRate();
	}

	/**
	 * Returns information about the current FT1.2 communication state.
	 * 
	 * @return state enumeration
	 */
	public final int getState()
	{
		return state;
	}

	/**
	 * Sends an EMI frame to the BCU2 connected with this endpoint.
	 * <p>
	 * In blocking mode, all necessary retransmissions of the sent frame will be done
	 * automatically according to the protocol specification (i.e., in case of timeout).<br>
	 * If a communication failure occurs on the port, {@link #close()} is called. A send
	 * timeout does not lead to closing of this connection.<br>
	 * In blocking send mode, on successfully receiving a confirmation, all listeners are
	 * guaranteed to get notified before this method returns. The communication state (see
	 * {@link #getState()}) is reset to {@link #OK} when the notification completed, so to
	 * prevent another send call from a listener.
	 * 
	 * @param frame EMI message to send, length of frame &lt; 256 bytes
	 * @param blocking <code>true</code> to block for confirmation (ACK),
	 *        <code>false</code> to immediately return after send
	 * @throws KNXAckTimeoutException in <code>blocking</code> mode, if a timeout
	 *         regarding the frame acknowledgment message was encountered
	 * @throws KNXPortClosedException if no communication was established in the first
	 *         place or communication was closed
	 * @throws InterruptedException on thread interruption during sending the frame; the
	 *         connection is not closed because of the interruption
	 */
	public void send(final byte[] frame, final boolean blocking)
		throws KNXAckTimeoutException, KNXPortClosedException, InterruptedException
	{
		boolean ack = false;
		try {
			for (int i = 0; i <= REPEAT_LIMIT; ++i) {
				if (logger.isLoggable(LogLevel.TRACE))
					logger.trace("sending FT1.2 frame, " + (blocking ? "" : "non-")
							+ "blocking, attempt " + (i + 1));
				sendData(frame);
				if (!blocking || waitForAck()) {
					ack = true;
					break;
				}
			}
			sendFrameCount ^= FRAMECOUNT_BIT;
			if (state == ACK_PENDING)
				state = OK;
			if (!ack)
				throw new KNXAckTimeoutException("no acknowledge reply received");
		}
		catch (final InterruptedIOException e) {
			throw new InterruptedException(e.getMessage());
		}
		catch (final IOException e) {
			close(false, e.getMessage());
			throw new KNXPortClosedException(e.getMessage());
		}
	}

	/**
	 * Ends communication with the BCU2 as specified by the FT1.2 protocol.
	 * <p>
	 * The BCU is always switched back into normal mode.<br>
	 * All registered event listeners get notified. The close event is the last event the
	 * listeners receive. <br>
	 * If this connection endpoint is already closed, no action is performed.
	 */
	public void close()
	{
		close(true, "client request");
	}

	private void close(final boolean user, final String reason)
	{
		if (state == CLOSED)
			return;
		logger.info("close serial port " + port + " - " + reason);
		state = CLOSED;
		if (receiver != null)
			receiver.quit();
		try {
			is.close();
			os.close();
			adapter.close();
		}
		catch (final Exception e) {
			logger.warn("failed to close all serial I/O resources", e);
		}
		fireConnectionClosed(user, reason);
		LogManager.getManager().removeLogService(logger.getName());
	}

	private void open(final String portId, final int baudrate) throws KNXException
	{
		adapter = createAdapter(portId, baudrate);
		port = portId;
		is = adapter.getInputStream();
		os = adapter.getOutputStream();
		calcTimeouts(adapter.getBaudRate());
		(receiver = new Receiver()).start();
		state = OK;
		logger.info("access supported, opened serial port " + portId);
	}

	private LibraryAdapter createAdapter(final String portId, final int baudrate)
		throws KNXException
	{
		boolean available = false;
		// check for ME CDC platform and available serial communication port
		// protocol support for communication ports is optional in CDC
		if (CommConnectionAdapter.isAvailable()) {
			available = true;
			logger.info("open ME CDC serial port connection (CommConnection)");
			try {
				return new CommConnectionAdapter(logger, portId, baudrate);
			}
			catch (final KNXException e) {
				logger.warn("ME CDC serial port access failed", e);
			}
		}
		// check internal support for serial port access
		// protocol support available for Win 32/64 platforms
		// (so we provide serial port access at least on platforms with ETS)
		if (SerialComAdapter.isAvailable()) {
			available = true;
			logger.info("open Calimero 2 native serial port connection (serialcom)");
			SerialComAdapter conn = null;
			try {
				conn = new SerialComAdapter(logger, portId);
				conn.setBaudRate(baudrate);
				calcTimeouts(conn.getBaudRate());
				// In Windows Embedded CE, the read interval timeout starts immediately
				conn.setTimeouts(new SerialComAdapter.Timeouts(idleTimeout, 0, 0, 0, 0));
				conn.setParity(SerialComAdapter.PARITY_EVEN);
				conn.setControl(SerialComAdapter.STOPBITS, SerialComAdapter.ONE_STOPBIT);
				conn.setControl(SerialComAdapter.DATABITS, 8);
				conn.setControl(SerialComAdapter.FLOWCTRL, SerialComAdapter.FLOWCTRL_NONE);
				logger.info("setup serial port: baudrate " + conn.getBaudRate()
					+ ", even parity, " + conn.getControl(SerialComAdapter.DATABITS)
					+ " databits, " + conn.getControl(SerialComAdapter.STOPBITS)
					+ " stopbits, timeouts: " + conn.getTimeouts());
				return conn;
			}
			catch (final IOException e) {
				if (conn != null)
					try {
						conn.close();
					}
					catch (final IOException ignore) {}
				logger.warn("native serial port access failed", e);
			}
		}
		try {
			final Class c = Class.forName("tuwien.auto.calimero.serial.RxtxAdapter");
			available = true;
			// check whether a rxtx library is hanging around somewhere
			logger.info("open rxtx library serial port connection");
			return (LibraryAdapter) c.getConstructors()[0].newInstance(new Object[] {
				logger, portId, new Integer(baudrate) });
		}
		catch (final ClassNotFoundException e) {
			logger.warn("rxtx library adapter not found");
		}
		catch (final SecurityException e) {
			logger.error("rxtx library adapter access denied", e);
		}
		catch (final InvocationTargetException e) {
			logger.error("initalizing rxtx serial port", e.getCause());
		}
		catch (final Exception e) {
			// InstantiationException, NoSuchMethodException,
			// IllegalAccessException, IllegalArgumentException
			logger.warn("rxtx serial port access failed", e);
		}
		catch (final NoClassDefFoundError e) {
			logger.error("no rxtx library classes found", e);
		}
		if (available)
			throw new KNXException("can not open serial port " + portId);
		throw new KNXException("no serial adapter available");
	}

	private void sendReset() throws KNXPortClosedException, KNXAckTimeoutException
	{
		try {
			final byte[] reset = new byte[] { START_FIXED, INITIATOR | RESET, INITIATOR | RESET,
				END };
			for (int i = 0; i <= REPEAT_LIMIT; ++i) {
				logger.trace("send reset to BCU");
				state = ACK_PENDING;
				os.write(reset);
				os.flush();
				if (waitForAck())
					return;
			}
			throw new KNXAckTimeoutException("resetting BCU failed (no acknowledge reply received)");
		}
		catch (final InterruptedException e) {
			close(true, "send reset interruption, " + e.getMessage());
			throw new KNXPortClosedException("interrupted during send reset");
		}
		catch (final IOException e) {
			close(false, e.getMessage());
			throw new KNXPortClosedException(e.getMessage());
		}
		finally {
			sendFrameCount = FRAMECOUNT_BIT;
			rcvFrameCount = FRAMECOUNT_BIT;
		}
	}

	private void sendData(final byte[] data) throws IOException, KNXPortClosedException
	{
		if (data.length > 255)
			throw new KNXIllegalArgumentException("data length > 255 bytes");
		if (state == CLOSED)
			throw new KNXPortClosedException("connection closed");
		final byte[] buf = new byte[data.length + 7];
		int i = 0;
		buf[i++] = START;
		buf[i++] = (byte) (data.length + 1);
		buf[i++] = (byte) (data.length + 1);
		buf[i++] = START;
		buf[i++] = (byte) (INITIATOR | sendFrameCount | FRAMECOUNT_VALID | USER_DATA);
		for (int k = 0; k < data.length; ++k)
			buf[i++] = data[k];
		buf[i++] = checksum(buf, 4, data.length + 1);
		buf[i++] = END;

		state = ACK_PENDING;
		os.write(buf);
		os.flush();
	}

	private void sendAck() throws IOException
	{
		os.write(ACK);
		os.flush();
	}

	private boolean waitForAck() throws InterruptedException
	{
		long remaining = exchangeTimeout;
		final long now = System.currentTimeMillis();
		final long end = now + remaining;
		synchronized (lock) {
			while (state == ACK_PENDING && remaining > 0) {
				lock.wait(remaining);
				remaining = end - System.currentTimeMillis();
			}
		}
		return remaining > 0;
	}

	private void fireConnectionClosed(final boolean user, final String reason)
	{
		final int initiator = user ? CloseEvent.USER_REQUEST : CloseEvent.INTERNAL;
		final CloseEvent ce = new CloseEvent(this, initiator, reason);
		final EventListener[] el = listeners.listeners();
		for (int i = 0; i < el.length; i++) {
			final KNXListener l = (KNXListener) el[i];
			try {
				l.connectionClosed(ce);
			}
			catch (final RuntimeException e) {
				removeConnectionListener(l);
				logger.error("removed event listener", e);
			}
		}
	}

	private void calcTimeouts(final int baudrate)
	{
		// with some serial driver/BCU/OS combinations, the calculated
		// timeouts are just too short, so add some milliseconds just as it fits
		// this little extra time usually doesn't hurt
		final int xTolerance = 5;
		final int iTolerance = 15;
		exchangeTimeout = Math.round(1000f * EXCHANGE_TIMEOUT / baudrate) + xTolerance;
		idleTimeout = Math.round(1000f * IDLE_TIMEOUT / baudrate) + iTolerance;
	}

	private static byte checksum(final byte[] data, final int offset, final int length)
	{
		byte chk = 0;
		for (int i = 0; i < length; ++i)
			chk += data[offset + i];
		return chk;
	}

	private static String[] defaultPortPrefixes()
	{
		return System.getProperty("os.name").toLowerCase().indexOf("windows") > -1
				? new String[]{ "\\\\.\\COM" } : new String[]{ "/dev/ttyS", "/dev/ttyUSB" };
	}

	private final class Receiver extends Thread
	{
		private volatile boolean quit;
		private int lastChecksum;

		Receiver()
		{
			super("FT1.2 receiver");
			setDaemon(true);
		}

		public void run()
		{
			try {
				while (!quit) {
					final int c = is.read();
					if (c > -1) {
						if (c == ACK) {
							if (state == ACK_PENDING)
								synchronized (lock) {
									state = OK;
									lock.notify();
								}
						}
						else if (c == START)
							readFrame();
						else if (c == START_FIXED)
							readShortFrame();
						else
							logger.trace("received unexpected start byte 0x"
									+ Integer.toHexString(c) + " - ignored");
					}
				}
			}
			catch (final IOException e) {
				if (!quit)
					close(false, "receiver communication failure");
			}
		}

		void quit()
		{
			quit = true;
			interrupt();
			if (currentThread() == this)
				return;
			try {
				join(50);
			}
			catch (final InterruptedException e) {}
		}

		private boolean readShortFrame() throws IOException
		{
			final byte[] buf = new byte[3];
			if (is.read(buf) == 3 && buf[0] == buf[1] && (buf[2] & 0xff) == END) {
				// for our purposes (reset and status), FRAMECOUNT_VALID is never set
				if ((buf[0] & 0x30) == 0) {
					sendAck();
					final int fc = buf[0] & 0x0f;
					logger.trace("received " + (fc == RESET ? "reset" : fc == REQ_STATUS
							? "status" : "unknown function code "));
					return true;
				}
			}
			return false;
		}

		private boolean readFrame() throws IOException
		{
			final int len = is.read();
			final byte[] buf = new byte[len + 4];
			// read rest of frame, check header, ctrl, and end tag
			final int read = is.read(buf);
			if (read == len + 4 && (buf[0] & 0xff) == len && (buf[1] & 0xff) == START
					&& (buf[len + 3] & 0xff) == END) {
				final byte chk = buf[buf.length - 2];
				if (!checkCtrlField(buf[2] & 0xff, chk))
					return false;

				if (checksum(buf, 2, len) != chk)
					logger.warn("invalid checksum in frame " + DataUnitBuilder.toHex(buf, " "));
				else {
					sendAck();
					lastChecksum = chk;
					rcvFrameCount ^= FRAMECOUNT_BIT;
					final byte[] ldata = new byte[len - 1];
					for (int i = 0; i < ldata.length; ++i)
						ldata[i] = buf[3 + i];

					fireFrameReceived(ldata);
					return true;
				}
			}
			else
				logger.warn("invalid frame, discarded " + read + " bytes: "
						+ DataUnitBuilder.toHex(buf, " "));
			return false;
		}

		private boolean checkCtrlField(final int c, final byte chk)
		{
			if ((c & (DIR_FROM_BAU | INITIATOR)) != (DIR_FROM_BAU | INITIATOR)) {
				logger.warn("unexpected ctrl field 0x" + Integer.toHexString(c));
				return false;
			}
			if ((c & FRAMECOUNT_VALID) == FRAMECOUNT_VALID) {
				if ((c & FRAMECOUNT_BIT) != rcvFrameCount) {
					// ignore repeated frame
					if (chk == lastChecksum) {
						logger.trace("framecount and checksum indicate a repeated "
								+ "frame - ignored");
						return false;
					}
					// protocol discrepancy (Merten Instabus coupler)
					logger.warn("toggle frame count bit");
					rcvFrameCount ^= FRAMECOUNT_BIT;
				}
			}
			if ((c & 0x0f) == USER_DATA && (c & FRAMECOUNT_VALID) == 0)
				return false;
			return true;
		}

		/**
		 * Fires a frame received event ({@link KNXListener#frameReceived(FrameEvent)})
		 * for the supplied EMI2 <code>frame</code>.
		 * 
		 * @param frame the EMI2 L-data frame to generate the event for
		 */
		private void fireFrameReceived(final byte[] frame)
		{
			final FrameEvent fe = new FrameEvent(this, frame);
			final EventListener[] el = listeners.listeners();
			for (int i = 0; i < el.length; i++) {
				final KNXListener l = (KNXListener) el[i];
				try {
					l.frameReceived(fe);
				}
				catch (final RuntimeException e) {
					removeConnectionListener(l);
					logger.error("removed event listener", e);
				}
			}
		}
	}
}
