/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2014, 2016 B. Malinowsky

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAckTimeoutException;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.log.LogService;

/**
 * Provides a connection with a TP-UART-IC controller for transparent communication with a KNX TP1 network. The
 * connection supports cEMI L-Data communication and busmonitor mode.<br>
 * The host establishes a connection over a serial port, using any identifier recognized and supported by the serial
 * adapter and the operating system.
 * <p>
 * Interruption policy: any blocking sends are cancelled.
 *
 * @author B. Malinowsky
 */
public class TpuartConnection implements AutoCloseable
{
	// UART services

	private static final int Reset_req = 0x01;
	private static final int Reset_ind = 0x03;

	// we can use this service to distinguish TP-UART 1/2
	// TP-UART 1 doesn't support this service
//	private static final int ProductId_req = 0x20;

	// the ProductId_res sucks, as it is specified without msg code
	// | Bit 7 - 5 | Bit 4 - 0 |
	// |  Prod. Id |  Rev. no  |
	// TP-UART 2 Release a
//	private static final int V2ReleaseA = 0b01000001;

	private static final int State_req = 0x02;
	private static final int State_ind = 0x07;

	private static final int ActivateBusmon = 0x05;

	private static final int LData_con = 0x0b; // MSB is pos/neg confirm
	private static final int AckInfo = 0x10;

	private static final int LDataStart = 0x80; // plus L_Data byte index
	private static final int LDataEnd = 0x40; // plus L_Data.length

	// TP1 frame type constants

	private static final int AlwaysSet = 0x10;
	private static final int StdFrameFormat = 0x80 | AlwaysSet;
	private static final int ExtFrameFormat = AlwaysSet;
	private static final int RepeatFlag = 0x20;

	// Short acks in bus monitor mode
	private static final int Ack = 0xcc;
	private static final int Nak = 0x0c;
	private static final int Busy = 0xc0;

	// TP-UART send

	private static final int OK = 0;
	private static final int ConPending = 1;
	// set to 3 if tpuart is told not to repeat frames
//	private static int MaxSendAttempts = 1; //3;
	// XXX tune, includes receive timeout of 2-2.5 ms to detect end of packet
	private static final long ExchangeTimeout = 50;
	// time interval to check on TP UART state [ms]
	private static final long UartStateReadInterval = 10000;

	private final String portId;
	private final LibraryAdapter adapter;
	private final OutputStream os;
	private final InputStream is;

	private final Receiver receiver;
	private final Object lock = new Object();

	private volatile boolean idle;
	private final Object enterIdleLock = new Object();

	// NYI compare to received frame for .con, or just remove
	private volatile byte[] req;
	private volatile int state;

	private volatile boolean busmon;
	private volatile int busmonSequence;

	private final EventListeners<KNXListener> listeners = new EventListeners<>();

	private final Set<KNXAddress> addresses = Collections.synchronizedSet(new HashSet<>());

	private final Logger logger;

	/**
	 * Creates a new TP-UART connection using communication port <code>portId</code>, expecting a collection of KNX
	 * addresses for which the host shall acknowledge TP1 frame reception.
	 *
	 * @param portId the identifier of the communication port
	 * @param acknowledge a (possibly empty) collection of KNX addresses this endpoint shall issue a positive
	 *        acknowledgement for, on receiving a valid TP1 frame with its destination address being an element in
	 *        <code>acknowledge</code>. By default, this endpoint also won't acknowledge the default individual
	 *        address 0.2.ff on the TP1 network.
	 * @throws KNXException on error opening the communication port, or initializing the TP-UART controller
	 */
	public TpuartConnection(final String portId, final Collection<? extends KNXAddress> acknowledge) throws KNXException
	{
		this.portId = portId;
		logger = LogService.getAsyncLogger("calimero.serial.tpuart");
		adapter = LibraryAdapter.open(logger, portId, 19200, 0);
		os = adapter.getOutputStream();
		is = adapter.getInputStream();

		addresses.addAll(acknowledge);

		receiver = new Receiver();
		receiver.start();
		try {
			reset();
		}
		catch (final IOException e) {
			close();
			throw new KNXPortClosedException("on resetting TP-UART controller", portId, e);
		}
	}

	/**
	 * Adds the specified event listener <code>l</code> to receive events from this connection. If <code>l</code> was
	 * already added as listener, no action is performed.
	 *
	 * @param l the listener to add
	 */
	public final void addConnectionListener(final KNXListener l)
	{
		listeners.add(l);
	}

	/**
	 * Removes the specified event listener <code>l</code>, so it does no longer receive events from this connection. If
	 * <code>l</code> was not added in the first place, no action is performed.
	 *
	 * @param l the listener to remove
	 */
	public final void removeConnectionListener(final KNXListener l)
	{
		listeners.remove(l);
	}

	/**
	 * Activate busmonitor mode, the TP-UART controller is set completely passive.
	 *
	 * @throws IOException on I/O error
	 */
	public void activateBusmonitor() throws IOException
	{
		logger.debug("activate TP-UART busmonitor");
		os.write(ActivateBusmon);
		busmonSequence = 0;
		busmon = true;
	}

	/**
	 * Adds an address to the list of addresses acknowledged on the bus.
	 *
	 * @param ack the address to acknowledge
	 */
	public final void addAddress(final KNXAddress ack)
	{
		addresses.add(ack);
	}

	/**
	 * Removes an address from the list of addresses acknowledged on the bus.
	 *
	 * @param ack the address to no further acknowledge
	 */
	public final void removeAddress(final KNXAddress ack)
	{
		addresses.remove(ack);
	}

	/**
	 * Sends a cEMI L-Data frame, either waiting for confirmation or non-blocking. Sending is not-permitted in
	 * busmonitor mode. A cEMI frame for TP1 does not require any additional information, therefore is assumed to not
	 * contain any, i.e., the field with additional information length is 0.
	 *
	 * @param frame cEMI L-Data msg as byte array, <code>msg</code> cannot contain additional information types
	 *        (additional information length field equals 0)
	 * @param waitForCon wait for L_Data.con (blocking) or not (non-blocking)
	 * @throws KNXPortClosedException on closed communication port
	 * @throws KNXAckTimeoutException on send/receive timeout (or if no ACK from the bus was received)
	 * @throws InterruptedException on thread interrupt, a send waiting for L-Data confirmation will be cancelled
	 */
	// TODO sync concurrent sends
	public void send(final byte[] frame, final boolean waitForCon)
		throws KNXPortClosedException, KNXAckTimeoutException, InterruptedException
	{
		final boolean group = (frame[3] & 0x80) == 0x80;
		KNXAddress dst = null;
		if (group) {
			dst = new GroupAddress(new byte[] { frame[6], frame[7] });
			addresses.add(dst);
		}

		try {
			final byte[] data = toUartServices(cEmiToTP1(frame));
			if (logger.isTraceEnabled())
				logger.trace("create UART services {}", DataUnitBuilder.toHex(data, " "));
			req = frame.clone();
			final long start = System.nanoTime();
			synchronized (enterIdleLock) {
				while (!idle)
					enterIdleLock.wait();
			}
			logger.trace("UART ready for sending after {} us", (System.nanoTime() - start) / 1000);

			logger.debug("write UART services, {}", (waitForCon ? "waiting for .con" : "non-blocking"));
			os.write(data);
			state = ConPending;
			if (!waitForCon)
				return;
			if (waitForCon())
				return;
			throw new KNXAckTimeoutException("no ACK for L-Data.con");
		}
		catch (final InterruptedIOException e) {
			throw new InterruptedException(e.getMessage());
		}
		catch (final IOException e) {
			close();
			throw new KNXPortClosedException("I/O error", portId, e);
		}
		finally {
			req = null;
			addresses.remove(dst);
		}
	}

	@Override
	public void close()
	{
		close(CloseEvent.USER_REQUEST, "user request");
	}

	private void close(final int origin, final String reason)
	{
		// best-effort, as we might already have hit an I/O error
		try {
			reset();
		}
		catch (final InterruptedIOException e) {
			Thread.currentThread().interrupt();
		}
		catch (final IOException ignore) {}
		receiver.quit();
		adapter.close();
		fireConnectionClosed(origin, reason);
	}

	private void fireConnectionClosed(final int origin, final String reason)
	{
		final CloseEvent ce = new CloseEvent(this, origin, reason);
		listeners.fire(l -> l.connectionClosed(ce));
	}

	private void reset() throws IOException
	{
		logger.debug("reset TP-UART controller");
		busmon = false;
		busmonSequence = 0;
		os.write(Reset_req);
	}

	// returns a TP1 std or ext frame
	private byte[] cEmiToTP1(final byte[] frame)
	{
		// set frame type to std/ext
		final int StdMaxApdu = 15;
		final int cEmiPrefix = 10;
		final boolean std = frame.length <= cEmiPrefix + StdMaxApdu;

		final byte[] tp1;
		if (std) {
			tp1 = new byte[frame.length - 2];
			int i = 0;
			// set upper 6 bits of ctrl1 field, ensure not repeated std frame
			tp1[i++] = (byte) ((frame[2] & 0xfc) | StdFrameFormat | RepeatFlag);
			// src
			tp1[i++] = frame[4];
			tp1[i++] = frame[5];
			// dst
			tp1[i++] = frame[6];
			tp1[i++] = frame[7];
			// address type, npci, length
			final int len = frame[8];
			tp1[i++] = (byte) ((frame[3] & 0xf0) | len);
			// tpci
			tp1[i++] = frame[9];
			// apdu
			for (int k = 0; k < len; k++)
				tp1[i++] = frame[10 + k];
		}
		else {
			// total length = frame length - 1 byte mc + 1 byte checksum
			tp1 = new byte[frame.length];
			for (int i = 1; i < frame.length; i++)
				tp1[i - 1] = frame[i];

			// ensure not repeated ext frame
			tp1[0] |= (byte) (frame[0] | ExtFrameFormat | RepeatFlag);
		}
		// last byte is checksum
		tp1[tp1.length - 1] = (byte) checksum(tp1);
		return tp1;
	}

	private byte[] toUartServices(final byte[] tp1)
	{
		final ByteArrayOutputStream data = new ByteArrayOutputStream(tp1.length * 2);
		for (int i = 0; i < tp1.length - 1; i++) {
			data.write(LDataStart | i);
			data.write(tp1[i]);
		}
		// write end data with frame checksum
		data.write(LDataEnd | tp1.length - 1);
		data.write(tp1[tp1.length - 1]);

		return data.toByteArray();
	}

	private boolean waitForCon() throws InterruptedException
	{
		long remaining = ExchangeTimeout;
		final long now = System.currentTimeMillis();
		final long end = now + remaining;
		synchronized (lock) {
			while (state == ConPending && remaining > 0) {
				lock.wait(remaining);
				remaining = end - System.currentTimeMillis();
			}
		}
		logger.trace("ACK received after {} ms", ExchangeTimeout - remaining);
		return remaining > 0;
	}

	private static int checksum(final byte[] frame)
	{
		int cs = 0;
		for (final byte b : frame)
			cs ^= b;
		return ~cs;
	}

	private boolean isValidChecksum(final byte[] frame)
	{
		final byte[] copy = Arrays.copyOf(frame, frame.length - 1);
		final int cs = checksum(copy);
		final int expected = frame[frame.length - 1];
		final boolean valid = expected == cs;
		if (!valid)
			logger.warn("invalid L-Data checksum 0x{}, expected 0x{}", Integer.toHexString(cs & 0xff),
					Integer.toHexString(expected & 0xff));
		return valid;
	}

	private final class Receiver extends Thread
	{
		private volatile boolean quit;

		private final ByteArrayOutputStream in = new ByteArrayOutputStream();
		private long lastRead;
		private boolean extFrame;
		private boolean frameAcked;

		private long lastUartState = System.currentTimeMillis();
		private boolean uartStatePending;

		Receiver()
		{
			super("Calimero TP-UART receiver");
			setDaemon(true);
			// more or less useless on Linux (requires root with UseThreadPriorities flag)
			setPriority(Thread.MAX_PRIORITY);
		}

		@Override
		public void run()
		{
			// at first drain rx queue of any old frames
			// most likely, flushes out the reset.ind corresponding to our init reset, but that's ok
			int drained = 0;
			try {
				while (is.read() != -1)
					drained++;
			}
			catch (final IOException ignore) {}
			logger.trace("drain rx queue ({} bytes)", drained);

			long enterIdleTimestamp = 0; // [ns]
			while (!quit) {
				try {
					final long start = System.nanoTime();
					final int c = is.available() > 0 || idle ? is.read() : -1;

					if (c == -1) {
						if (lastUartState + UartStateReadInterval < System.currentTimeMillis())
							readUartState();

						// we transition to idle state after some time of inactivity, and notify a waiting sender
						final long inactivity = 10000; // [us]
						if (enterIdleTimestamp == 0)
							enterIdleTimestamp = start;
						else if ((start - enterIdleTimestamp) / 1000 > inactivity) {
							synchronized (enterIdleLock) {
								idle = true;
								enterIdleLock.notify();
							}
						}
						continue;
					}

					final long idlePeriod = (start - enterIdleTimestamp) / 1000;
					if (enterIdleTimestamp != 0 && idlePeriod > 100_000)
						logger.trace("return from extended idle period of {} us", idlePeriod);
					idle = false;
					enterIdleTimestamp = 0;

					if (parseFrame(c) || isLDataCon(c) || isUartStateInd(c))
						; // nothing to do
					else if (c == Reset_ind)
						logger.debug("TP-UART reset.ind");

//					final long loop = System.nanoTime() - start;
//					logger.trace("loop time = {} us", loop / 1000);
				}
				catch (final RuntimeException e) {
					e.printStackTrace();
				}
				catch (final IOException e) {
					if (!quit)
						close(CloseEvent.INTERNAL, "receiver communication failure");
					break;
				}
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
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		private boolean parseFrame(final int c) throws IOException
		{
			int size = in.size();
			// empty buffer if we didn't receive data for some time
			if (size > 0 && ((lastRead + 4000) < (System.nanoTime() / 1000))) {
				final byte[] buf = in.toByteArray();
				in.reset();
				size = 0;
				logger.debug("reset input buffer, discard partial frame (length {}) {}", buf.length,
						DataUnitBuilder.toHex(buf, " "));
			}

			if (size > 0) {
				in.write(c);
				lastRead = System.nanoTime() / 1000;
				final int minLength = extFrame ? 6 : 5;
				if (size + 1 > minLength) {
					final byte[] frame = in.toByteArray();
					ack(frame);

					// check if we got the expected frame size
					final int total;
					if (extFrame)
						total = 8 + (frame[6] & 0x3f) + 1;
					else
						total = 7 + (frame[5] & 0x0f) + 1;

					if (frame.length >= total) {
						try {
							final byte[] data = in.toByteArray();
							logger.debug("received TP1 L-Data (length {}): {}", frame.length,
									DataUnitBuilder.toHex(data, " "));

							if (busmon) {
								fireFrameReceived(createBusmonInd(data));
							}
							else
								fireFrameReceived(createLDataInd(data));
						}
						catch (final Exception e) {
							e.printStackTrace();
						}
						finally {
							in.reset();
						}
					}
				}
			}
			else if (isLDataStart(c)) {
				lastRead = System.nanoTime() / 1000;
				in.reset();
				in.write(c);
				frameAcked = false;
			}
			// busmon mode only: short acks
			else if (c == Ack || c == Nak || c == Busy)
				fireFrameReceived(createBusmonInd(new byte[] { (byte) c }));
			else
				return false;
			return true;
		}

		private void readUartState() throws IOException
		{
			uartStatePending = true;
			os.write(State_req);
		}

		private boolean isUartStateInd(final int c)
		{
			if (!uartStatePending)
				return false;
			final boolean ind = (c & State_ind) == State_ind;
			if (!ind)
				return false;
			uartStatePending = false;
			lastUartState = System.currentTimeMillis();
			final boolean slaveCollision = (c & 0x80) == 0x80;
			final boolean rxError = (c & 0x40) == 0x40; // checksum, parity, bit error
			final boolean txError = (c & 0x20) == 0x20; // send 0, receive 1
			final boolean protError = (c & 0x10) == 0x10; // illegal ctrl byte
			final boolean tempWarning = (c & 0x08) == 0x08; // too hot
			logger.debug("TP-UART status update: {}Temp. {}, Errors: Rx={} Tx={} Prot={}",
					slaveCollision ? "Slave collision, " : "", tempWarning ? "warning" : "OK", rxError, txError,
					protError);
			// NYI if controller sends warning, we have to pause tx for 1 sec
			if (tempWarning)
				logger.warn("TP-UART high temperature warning!");
			return true;
		}

		private boolean isLDataCon(final int c)
		{
			final boolean con = (c & 0x7f) == LData_con;
			if (con) {
				final boolean pos = (c & 0x80) == 0x80;
				onConfirmation(pos);
				final String status = pos ? "positive" : "negative";
				logger.debug("{} L_Data.con", status);
			}
			return con;
		}

		private boolean isLDataStart(final int c)
		{
			if ((c & 0x03) != 0)
				return false;
			final boolean start = (c & 0xd0) == StdFrameFormat || (c & 0xd0) == ExtFrameFormat;
			if (start) {
				final boolean repeated = (c & RepeatFlag) == 0;
				logger.trace("Start of frame 0x{}, repeated = {}", Integer.toHexString(c), repeated);
				extFrame = (c & 0xd0) == ExtFrameFormat;
			}
			return start;
		}

		// pre: we have a new .ind frame, length > 5
		// The ack service has to be sent at the latest 1.7 ms after receiving the
		// address-type bit of the L-Data.ind
		private void ack(final byte[] frame) throws IOException
		{
			if (busmon || frameAcked)
				return;
			final byte[] addr = new byte[] { frame[3], frame[4] };
			final boolean group = (frame[5] & 0x80) == 0x80;
			final KNXAddress dst = group ? new GroupAddress(addr) : new IndividualAddress(addr);

			// We can answer as follows:
			// ACK: if we got addressed
			// NAK: we don't care about that, because the TP-UART checks that for us
			// Busy: we're never busy
			int ack = AckInfo;
			final boolean oneOfUs = addresses.contains(dst);
			if (oneOfUs) {
				ack |= 0x01;
				os.write(new byte[] { (byte) ack });
				logger.trace("write ACK (0x{}) for {}", Integer.toHexString(ack), dst);
			}
			frameAcked = true;
		}

		// TODO We assemble a .con using our saved .req, and let the TP-UART L-Data frame bubble up
		// as .ind, which is useless. Use the received frame as L-Data.con, or throw it away.
		private void onConfirmation(final boolean pos)
		{
			// assemble a .con frame
			final byte[] frame = req;
			if (frame == null)
				return;
			frame[0] = CEMILData.MC_LDATA_CON;
			if (pos) {
				// set confirm bit to no error
				frame[2] &= 0xfe;
				// wake up one blocked sender, if any
				synchronized (lock) {
					state = OK;
					lock.notify();
				}
			}
			else {
				// set confirm bit to error
				frame[2] |= 0x01;
			}
			fireFrameReceived(frame);
		}

		// create a cEMI L-Data from a TP1 frame
		private byte[] createLDataInd(final byte[] tp1)
		{
			return createLData(CEMILData.MC_LDATA_IND, tp1);
		}

		// create a cEMI L-Data from a TP1 frame
		private byte[] createLData(final int mc, final byte[] tp1)
		{
			if (!isValidChecksum(tp1))
				return null;

			final boolean std = (tp1[0] & StdFrameFormat) == StdFrameFormat;
			final byte[] ind = new byte[tp1.length + (std ? 2 : 1)];
			ind[0] = (byte) mc;
			// with TP1, there is no additional information, i.e., 2nd cEMI byte is always 0
			ind[1] = 0;
			if (std) {
				ind[2] = tp1[0];
				ind[3] = (byte) (tp1[5] & 0xf0);
				// src
				ind[4] = tp1[1];
				ind[5] = tp1[2];
				// dst
				ind[6] = tp1[3];
				ind[7] = tp1[4];
				// len
				final int len = tp1[5] & 0x0f;
				ind[8] = (byte) len;
				// tpci
				ind[9] = tp1[6];
				// apdu
				for (int i = 0; i < len; i++)
					ind[10 + i] = tp1[7 + i];
			}
			else {
				for (int i = 0; i < tp1.length - 1; i++)
					ind[2 + i] = tp1[i];
			}
			return ind;
		}

		private byte[] createBusmonInd(final byte[] tp1)
		{
			final int seq = busmonSequence;
			busmonSequence = (busmonSequence + 1) % 8;
			// provide 32 bit timestamp with 1 us precision
			final long timestamp = (System.nanoTime() / 1000) & 0xFFFFFFFFL;
			// NYI we could at least set frame error in status
			return CEMIBusMon.newWithSequenceNumber(seq, timestamp, true, tp1).toByteArray();
		}

		private void fireFrameReceived(final byte[] frame)
		{
			if (frame == null)
				return;
			logger.trace("cEMI (length {}): {}", frame.length, DataUnitBuilder.toHex(frame, " "));
			try {
				final CEMI msg = CEMIFactory.create(frame, 0, frame.length);
				final FrameEvent fe = new FrameEvent(this, msg);
				listeners.fire(l -> l.frameReceived(fe));
			}
			catch (final KNXFormatException | RuntimeException e) {
				logger.error("invalid frame for cEMI: {}", DataUnitBuilder.toHex(frame, " "), e);
			}
		}
	}
}
