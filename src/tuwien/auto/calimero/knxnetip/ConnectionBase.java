/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2011 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EventListener;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.exception.KNXAckTimeoutException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.knxnetip.servicetype.DisconnectRequest;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.RoutingIndication;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest;
import tuwien.auto.calimero.knxnetip.util.HPAI;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Generic implementation of a KNXnet/IP connection, used for tunneling, device management
 * and routing.
 * <p>
 * 
 * @author B. Malinowsky
 */
public abstract class ConnectionBase implements KNXnetIPConnection
{
	/**
	 * Status code of communication: waiting for service acknowledgment after send, no
	 * error, not ready to send.
	 * <p>
	 */
	public static final int ACK_PENDING = 2;

	/**
	 * Status code of communication: in idle state, received a service acknowledgment
	 * error as response, ready to send.
	 * <p>
	 */
	public static final int ACK_ERROR = 3;

	// KNXnet/IP client SHALL wait 10 seconds for a connect response frame from server
	static final int CONNECT_REQ_TIMEOUT = 10;

	/** local control endpoint socket */
	protected DatagramSocket ctrlSocket;
	/** local data endpoint socket */
	protected DatagramSocket socket;

	/** remote control endpoint */
	protected InetSocketAddress ctrlEndpt;
	/** remote data endpoint */
	protected InetSocketAddress dataEndpt;

	/** connection KNX channel identifier */
	protected int channelId;
	/** use network address translation (NAT) aware communication */
	protected boolean useNat;

	/** service request code used for this connection type */
	protected final int serviceRequest;
	/** acknowledgment service type used for this connection type */
	protected final int serviceAck;
	/** container for event listeners */
	protected final EventListeners listeners = new EventListeners();
	/** logger for this connection */
	protected LogService logger;

	// current state visible to the user
	// a state < 0 indicates severe error state
	volatile int state = CLOSED;
	/** internal state, so we can use states not visible to the user */
	protected volatile int internalState = CLOSED;
	// should an internal state change update the state member
	volatile boolean updateState = true;

	// flag to ensure close is invoked only once
	volatile int closing;

	// number of maximum sends (= retries + 1)
	final int maxSendAttempts;
	// timeout for response message in seconds
	final int responseTimeout;
	
	private ReceiverLoop receiver;
	
	// lock object to do wait() on for protocol timeouts
	Object lock = new Object();

	// send/receive sequence numbers
	private int seqRcv;
	private int seqSend;

	private final Semaphore sendWaitQueue = new Semaphore();

	/**
	 * Base constructor to assign the supplied arguments.
	 * <p>
	 * 
	 * @param serviceRequest
	 * @param serviceAck
	 * @param maxSendAttempts
	 * @param responseTimeout
	 */
	protected ConnectionBase(final int serviceRequest, final int serviceAck,
		final int maxSendAttempts, final int responseTimeout)
	{
		this.serviceRequest = serviceRequest;
		this.serviceAck = serviceAck;
		this.maxSendAttempts = maxSendAttempts;
		this.responseTimeout = responseTimeout;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.KNXnetIPConnection#addConnectionListener
	 * (tuwien.auto.calimero.KNXListener)
	 */
	public void addConnectionListener(final KNXListener l)
	{
		listeners.add(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.KNXnetIPConnection#removeConnectionListener
	 * (tuwien.auto.calimero.KNXListener)
	 */
	public void removeConnectionListener(final KNXListener l)
	{
		listeners.remove(l);
	}

	/**
	 * <p>
	 * If <code>mode</code> is {@link KNXnetIPConnection#WAIT_FOR_CON} or
	 * {@link KNXnetIPConnection#WAIT_FOR_ACK}, the sequence order of more
	 * {@link #send(CEMI, tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode)}
	 * calls from different threads is being maintained according to invocation order
	 * (FIFO).<br>
	 * A call of this method blocks until (possible) previous invocations are done, then
	 * does communication according to the protocol and waits for response (either ACK or
	 * cEMI confirmation), timeout or an error condition.<br>
	 * Note that, for now, when using blocking mode any ongoing nonblocking invocation is
	 * not detected or considered for waiting until completion.
	 * <p>
	 * If mode is {@link KNXnetIPConnection#NONBLOCKING}, sending is only permitted if no
	 * other send is currently being done, otherwise throws a
	 * {@link KNXIllegalStateException}. In this mode, a user has to check the state (
	 * {@link #getState()} on its own.
	 */
	public void send(final CEMI frame, final BlockingMode mode)
		throws KNXTimeoutException, KNXConnectionClosedException
	{
		// send state | blocking | nonblocking
		// -----------------------------------
		// OK         |send+wait | send+return
		// WAIT       |wait+s.+w.| throw
		// ACK_ERROR  |send+wait | send+return
		// ERROR      |throw     | throw

		if (state == CLOSED) {
			logger.warn("send invoked on closed connection - aborted");
			throw new KNXConnectionClosedException("connection closed");
		}
		if (state < 0) {
			logger.error("send invoked in error state " + state + " - aborted");
			throw new KNXIllegalStateException("in error state, send aborted");
		}
		// arrange into line for blocking mode
		if (mode != NONBLOCKING)
			sendWaitQueue.acquire();
		synchronized (lock) {
			if (mode == NONBLOCKING && state != OK && state != ACK_ERROR) {
				logger.warn("nonblocking send invoked while waiting for data response in state "
						+ state + " - aborted");
				throw new KNXIllegalStateException("waiting for data response");
			}
			try {
				if (state == CLOSED) {
					logger.warn("send invoked on closed connection - aborted");
					throw new KNXConnectionClosedException("connection closed");
				}
				updateState = mode == NONBLOCKING;
				final byte[] buf;
				if (serviceRequest == KNXnetIPHeader.ROUTING_IND)
					buf = PacketHelper.toPacket(new RoutingIndication(frame));
				else
					buf = PacketHelper.toPacket(new ServiceRequest(serviceRequest, channelId,
							getSeqSend(), frame));
				final DatagramPacket p = new DatagramPacket(buf, buf.length,
						dataEndpt.getAddress(), dataEndpt.getPort());
				int attempt = 0;
				for (; attempt < maxSendAttempts; ++attempt) {
					if (logger.isLoggable(LogLevel.TRACE))
						logger.trace("sending cEMI frame, " + mode + ", attempt " + (attempt + 1));

					socket.send(p);
					// shortcut for routing, don't switch into 'ack-pending'
					if (serviceRequest == KNXnetIPHeader.ROUTING_IND)
						return;
					internalState = ACK_PENDING;
					// always forward this state to user
					state = ACK_PENDING;
					if (mode == NONBLOCKING)
						return;
					waitForStateChange(ACK_PENDING, responseTimeout);
					if (internalState == ClientConnection.CEMI_CON_PENDING || internalState == OK)
						break;
				}
				// close connection on no service ack from server
				if (attempt == maxSendAttempts) {
					final KNXAckTimeoutException e = new KNXAckTimeoutException(
						"maximum send attempts, no service acknowledgment received");
					close(CloseEvent.INTERNAL, "maximum send attempts", LogLevel.ERROR, e);
					throw e;
				}
				// always forward this state to user
				state = internalState;
				if (mode != WAIT_FOR_ACK)
					doExtraBlockingModes();
			}
			catch (final InterruptedException e) {
				close(CloseEvent.USER_REQUEST, "interrupted", LogLevel.WARN, e);
				Thread.currentThread().interrupt();
				throw new KNXConnectionClosedException("interrupted connection got closed");
			}
			catch (final IOException e) {
				close(CloseEvent.INTERNAL, "communication failure", LogLevel.ERROR, e);
				throw new KNXConnectionClosedException("connection closed");
			}
			finally {
				updateState = true;
				setState(internalState);
				if (mode != NONBLOCKING)
					sendWaitQueue.release();
			}
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.KNXnetIPConnection#getRemoteAddress()
	 */
	public final InetSocketAddress getRemoteAddress()
	{
		if (state == CLOSED)
			return new InetSocketAddress(0);
		return ctrlEndpt;
	}

	public final int getState()
	{
		return state;
	}

	public String getName()
	{
		// only the control endpoint is set when our logger is initialized (the data
		// endpoint gets assigned later in connect)
		// to keep the name short, avoid a prepended host name as done by InetAddress
		return ctrlEndpt.getAddress().getHostAddress() + ":" + ctrlEndpt.getPort();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.knxnetip.KNXnetIPConnection#close()
	 */
	public final void close()
	{
		close(CloseEvent.USER_REQUEST, "user request", LogLevel.INFO, null);
	}

	/**
	 * Returns the protocol's current receive sequence number.
	 * <p>
	 * 
	 * @return receive sequence number as int
	 */
	protected synchronized int getSeqRcv()
	{
		return seqRcv;
	}

	/**
	 * Increments the protocol's receive sequence number, with increment on sequence
	 * number 255 resulting in 0.
	 * <p>
	 */
	protected synchronized void incSeqRcv()
	{
		seqRcv = (seqRcv + 1) & 0xFF;
	}

	/**
	 * Returns the protocol's current send sequence number.
	 * <p>
	 * 
	 * @return send sequence number as int
	 */
	protected synchronized int getSeqSend()
	{
		return seqSend;
	}

	/**
	 * Increments the protocol's send sequence number, with increment on sequence number
	 * 255 resulting in 0.
	 * <p>
	 */
	protected synchronized void incSeqSend()
	{
		seqSend = (seqSend + 1) & 0xFF;
	}

	/**
	 * Fires a frame received event ({@link KNXListener#frameReceived(FrameEvent)}) for
	 * the supplied cEMI <code>frame</code>.
	 * 
	 * @param frame the cEMI to generate the event for
	 */
	protected void fireFrameReceived(final CEMI frame)
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

	/**
	 * This stub always returns false.
	 * <p>
	 * 
	 * @param h received KNXnet/IP header
	 * @param data received datagram data
	 * @param offset datagram data start offset
	 * @param src sender IP address
	 * @param port sender UDP port
	 * @return <code>true</code> if service type was known and handled (successfully or
	 *         not), <code>false</code> otherwise
	 * @throws KNXFormatException on service type parsing or data format errors
	 * @throws IOException on socket problems
	 */
	protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data,
		final int offset, final InetAddress src, final int port) throws KNXFormatException,
		IOException
	{
		// at this subtype level, we don't care about any service type
		return false;
	}

	/**
	 * Request to set this connection into a new connection state.
	 * <p>
	 * 
	 * @param newState new state to set
	 */
	protected final void setState(final int newState)
	{
		if (closing < 2) {
			//  detect and ignore order of arrival inversion for tunneling.ack/cEMI.con
			if (internalState == OK && newState == ClientConnection.CEMI_CON_PENDING)
				return;
			internalState = newState;
			if (updateState)
				state = newState;
		}
		else
			state = internalState = CLOSED;
	}

	/**
	 * See {@link #setState(int)}, with additional notification of internal threads.
	 * <p>
	 * 
	 * @param newState new state to set
	 */
	protected final void setStateNotify(final int newState)
	{
		synchronized (lock) {
			setState(newState);
			// worst case: we notify 2 threads, the closing one and 1 sending
			lock.notifyAll();
		}
	}

	/**
	 * Handler method forwarded to on calling {@link #close()}.
	 * <p>
	 * It contains all protocol specific actions to close a connection. Before this method
	 * returns, {@link #cleanup(int, String, LogLevel, Throwable)} is called.
	 * 
	 * @param initiator one of the constants of {@link CloseEvent}
	 * @param reason short text statement why close was called on this connection
	 * @param level log level to use for logging, adjust this to the reason of closing
	 *        this connection
	 * @param t a throwable, to pass to the logger if the close event was caused by some
	 *        error, can be <code>null</code>
	 */
	protected void close(final int initiator, final String reason, final LogLevel level,
		final Throwable t)
	{
		synchronized (this) {
			if (closing > 0)
				return;
			closing = 1;
		}
		try {
			synchronized (lock) {
				final byte[] buf = PacketHelper.toPacket(new DisconnectRequest(channelId,
						new HPAI(HPAI.IPV4_UDP, useNat ? null : (InetSocketAddress) ctrlSocket
								.getLocalSocketAddress())));
				final DatagramPacket p = new DatagramPacket(buf, buf.length, ctrlEndpt);
				ctrlSocket.send(p);
				long remaining = CONNECT_REQ_TIMEOUT * 1000;
				final long end = System.currentTimeMillis() + remaining;
				while (closing == 1 && remaining > 0) {
					lock.wait(remaining);
					remaining = end - System.currentTimeMillis();
				}
			}
		}
		catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (final IOException e) {
			logger.error("send disconnect failed", e);
		}
		catch (final RuntimeException e) {
			// we have to do this strange catch here, since if socket already failed
			// before close(), getLocalSocketAddress() might throw illegal argument
			// exception or return the wildcard address, indicating a messed up socket
			logger.error("send disconnect failed, socket problem");
		}
		cleanup(initiator, reason, level, t);
	}

	/**
	 * @param initiator
	 * @param reason
	 * @param level
	 * @param t
	 */
	protected void cleanup(final int initiator, final String reason,
		final LogLevel level, final Throwable t)
	{
		setState(CLOSED);
		fireConnectionClosed(initiator, reason);
		listeners.removeAll();
		LogManager.getManager().removeLogService(logger.getName());
	}

	/**
	 * Validates channel id received in a packet against the one assigned to this
	 * connection.
	 * <p>
	 * 
	 * @param id received id to check
	 * @param svcType packet service type
	 * @return <code>true</code> if valid, <code>false</code> otherwise
	 */
	protected boolean checkChannelId(final int id, final String svcType)
	{
		if (id == channelId)
			return true;
		logger.warn("received service " + svcType + " with wrong channel ID " + id + ", expected "
				+ channelId + " - ignored");
		return false;
	}

	/**
	 * Extracts the service request out of the supplied packet data.
	 * <p>
	 * 
	 * @param h packet KNXnet/IP header
	 * @param data contains the data following the KNXnet/IP header
	 * @param offset offset into <code>data</code> to message structure past KNXnet/IP
	 *        header
	 * @return the service request
	 * @throws KNXFormatException on failure to extract (even an empty) service request
	 */
	protected ServiceRequest getServiceRequest(final KNXnetIPHeader h, final byte[] data,
		final int offset) throws KNXFormatException
	{
		try {
			return PacketHelper.getServiceRequest(h, data, offset);
		}
		catch (final KNXFormatException e) {
			// check if at least the connection header of the service request
			// is correct and try to get its values
			final ServiceRequest req = PacketHelper.getEmptyServiceRequest(h, data, offset);
			logger.warn("received request with unknown cEMI data "
					+ DataUnitBuilder.toHex(DataUnitBuilder.copyOfRange(data, offset + 4,
					offset + h.getTotalLength() - h.getStructLength()), " "), e);
			return req;
		}
	}

	final void startReceiver()
	{
		if (receiver == null) {
			final ReceiverLoop looper = new ReceiverLoop(this, socket, 0x200);
			final Thread t = new Thread(looper, "KNXnet/IP receiver");
			t.setDaemon(true);
			t.start();
			receiver = looper;
		}
	}
	
	final void stopReceiver()
	{
		if (receiver != null)
			receiver.quit();
	}
	
	boolean waitForStateChange(final int initialState, final int timeout)
		throws InterruptedException
	{
		boolean changed = false;
		long remaining = timeout * 1000L;
		final long end = System.currentTimeMillis() + remaining;
		synchronized (lock) {
			while (internalState == initialState && remaining > 0) {
				lock.wait(remaining);
				remaining = end - System.currentTimeMillis();
			}
		}
		changed = remaining > 0;
		return changed;
	}

	/**
	 * Give chance to perform additional blocking modes called if mode is set to a
	 * blocking mode not equal to NON_BLOCKING and WAIT_FOR_ACK. This method is called
	 * from send() after WAIT_FOR_ACK was completed.
	 * 
	 * @throws KNXTimeoutException
	 * @throws InterruptedException
	 */
	void doExtraBlockingModes() throws KNXTimeoutException, InterruptedException
	{}

	private void fireConnectionClosed(final int initiator, final String reason)
	{
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

	// a semaphore with fair use behavior (FIFO)
	// acquire and its associated release don't have to be invoked by same thread
	private static final class Semaphore
	{
		private static final class Node
		{
			Node next;
			boolean blocked;

			Node(final Node n)
			{
				next = n;
				blocked = true;
			}
		}

		private Node head;
		private Node tail;
		private int cnt;

		Semaphore()
		{
			cnt = 1;
		}

		void acquire()
		{
			Node n;
			boolean interrupted = false;
			synchronized (this) {
				if (cnt > 0 && tail == null) {
					--cnt;
					return;
				}
				n = enqueue();
			}
			synchronized (n) {
				while (n.blocked)
					try {
						n.wait();
					}
					catch (final InterruptedException e) {
						interrupted = true;
					}
			}
			synchronized (this) {
				dequeue();
				--cnt;
			}
			if (interrupted)
				Thread.currentThread().interrupt();
		}

		synchronized void release()
		{
			if (++cnt > 0)
				notifyNext();
		}

		private Node enqueue()
		{
			final Node n = new Node(null);
			if (tail == null)
				tail = n;
			else
				head.next = n;
			return head = n;
		}

		private void notifyNext()
		{
			if (tail != null)
				synchronized (tail) {
					tail.blocked = false;
					tail.notify();
				}
		}

		private void dequeue()
		{
			tail = tail.next;
			if (tail == null)
				head = null;
		}
	}
}
