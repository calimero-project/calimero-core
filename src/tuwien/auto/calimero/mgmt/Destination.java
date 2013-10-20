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

package tuwien.auto.calimero.mgmt;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.link.KNXLinkClosedException;

/**
 * Represents a transport layer logical connection destination.
 * <p>
 * It keeps the settings used for communication with a destination and maintains the logical
 * connection state. In connection oriented mode, a timer is used to detect the connection
 * timeout and send a disconnect.<br>
 * The actual layer 4 communication is done by a {@link TransportLayer} (the aggregator
 * for the destination) specified with the {@link AggregatorProxy}.
 * <p>
 * A destination object is usually created and maintained by a TransportLayer instance or
 * {@link ManagementClient} instance.<br>
 * After a destination got destroyed, it can't be used for communication to that
 * destination anymore, i.e., it's not possible to change the connection state.
 * 
 * @author B. Malinowsky
 * @see TransportLayer
 * @see ManagementClient
 */
public class Destination
{
	/**
	 * An aggregator proxy is associated with one destination and is supplied at the
	 * creation of a new destination object.
	 * <p>
	 * Used by the owner of a destination handling the communication and used to modify
	 * destination state and obtain internal connection settings.
	 * <p>
	 * By default, a proxy is created by a transport layer implementation.
	 * 
	 * @author B. Malinowsky
	 */
	public static final class AggregatorProxy implements Runnable
	{
		private final TransportLayer aggr;
		private Destination d;

		/**
		 * Creates a new aggregator proxy.
		 * <p>
		 * 
		 * @param aggregator the transport layer serving the destination associated with
		 *        this proxy and handles necessary transport layer communication
		 */
		public AggregatorProxy(final TransportLayer aggregator)
		{
			aggr = aggregator;
		}

		/**
		 * Returns the destination associated with this proxy.
		 * <p>
		 * 
		 * @return the Destination
		 */
		public Destination getDestination()
		{
			return d;
		}

		/**
		 * Returns the receive sequence number of the connection.
		 * <p>
		 * 
		 * @return sequence number, 0 &lt;= number &lt;= 15
		 */
		public synchronized int getSeqReceive()
		{
			return d.seqRcv;
		}

		/**
		 * Increments the receive sequence number by one.
		 * <p>
		 * The new sequence number is the next expected receive sequence number, with
		 * increment on sequence number 15 resulting in 0.
		 */
		public synchronized void incSeqReceive()
		{
			d.seqRcv = ++d.seqRcv & 0x0f;
		}

		/**
		 * Returns the send sequence number of the connection.
		 * <p>
		 * 
		 * @return sequence number, 0 &lt;= number &lt;= 15
		 */
		public synchronized int getSeqSend()
		{
			return d.seqSend;
		}

		/**
		 * Increments the send sequence number by one.
		 * <p>
		 * The new sequence number is the next expected send sequence number, with
		 * increment on sequence number 15 resulting in 0.
		 */
		public synchronized void incSeqSend()
		{
			d.seqSend = ++d.seqSend & 0x0f;
		}

		/**
		 * Restarts the connection timeout used for the destination connection.
		 * <p>
		 * This method is only used in connection oriented communication mode.
		 * 
		 * @throws KNXIllegalStateException if invoked on not connection oriented mode
		 */
		public void restartTimeout()
		{
			d.restartTimer(this);
		}

		/**
		 * Sets a new destination connection state.
		 * <p>
		 * If necessary, the connection timeout for the destination is started, restarted
		 * or deactivated according the state transition.<br>
		 * If the state of destination is {@link Destination#DESTROYED}, setting of a new
		 * state is ignored.
		 * 
		 * @param newState new destination state
		 */
		public void setState(final int newState)
		{
			d.setState(newState, this);
		}
		
		void setDestination(final Destination dst)
		{
			d = dst;
		}
		
		/**
		 * The proxy acts as notifiable to the disconnect timer. If notified, the
		 * connection timed out, and it will ensure the destination gets disconnected.
		 */
		public void run()
		{
			final int state = d.getState();
			if (state != DISCONNECTED && state != DESTROYED)
				try {
					aggr.disconnect(d);
				}
				catch (final KNXLinkClosedException e) {}
		}
	}

	// idle timeout for a connection in milliseconds
	private static final int TIMEOUT = 6000;
	
	// a disconnect timer for all active destination objects
	private static TimerQueue disconnectTimer = new TimerQueue();

	/**
	 * Destination is destroyed.
	 * <p>
	 * 
	 * @see Destination#getState
	 */
	public static final int DESTROYED = 0;

	/**
	 * Connection state is disconnected.
	 * <p>
	 * 
	 * @see Destination#getState
	 */
	public static final int DISCONNECTED = 1;

	/**
	 * Connection state is connecting.
	 * <p>
	 * 
	 * @see Destination#getState
	 */
	public static final int CONNECTING = 2;

	/**
	 * Connection state is open and communication is in idle state.
	 * <p>
	 * 
	 * @see Destination#getState
	 */
	public static final int OPEN_IDLE = 3;

	/**
	 * Connection state is open and communication is in waiting state for Layer 4
	 * acknowledgment.
	 * <p>
	 * 
	 * @see Destination#getState
	 */
	public static final int OPEN_WAIT = 4;

	
	static final int USER_REQUEST = 0;
	static final int REMOTE_ENDPOINT = 1;
	static final int LOCAL_ENDPOINT = 2;
	
	volatile int disconnectedBy = -1;
	
	private final TransportLayer tl;
	private final IndividualAddress addr;
	private volatile int state = DISCONNECTED;
	private int seqRcv;
	private int seqSend;
	private final boolean co;
	private final boolean alive;

	// see 03/05/01 Resources: Verify Mode Control
	private final boolean verify;

	/**
	 * Creates a new destination.
	 * <p>
	 * Verify mode defaults to false and keep alive is not used.
	 * 
	 * @param aggregator aggregator proxy to associate with this destination
	 * @param remote KNX remote address specifying the connection destination
	 * @param connectionOriented <code>true</code> for connection oriented mode,
	 *        <code>false</code> to use connectionless mode
	 */
	public Destination(final AggregatorProxy aggregator, final IndividualAddress remote,
		final boolean connectionOriented)
	{
		this(aggregator, remote, connectionOriented, false, false);
	}

	/**
	 * Creates a new destination with all available destination connection settings.
	 * <p>
	 * Keep alive of a logical connection is only available in connection oriented mode,
	 * in connectionless mode keep alive is always disabled.<br>
	 * <b>Implementation note</b>: the keep alive option is not implemented by now and not
	 * used by this destination. Nevertheless, it is set and might be queried using
	 * {@link Destination#isKeepAlive()}.<br>
	 * The verify mode refers to the verify mode control in application layer services and
	 * specifies whether the specified destination to communicate with supports verified
	 * writing of data.
	 * 
	 * @param aggregator aggregator proxy to associate with this destination
	 * @param remote KNX remote address specifying the connection destination
	 * @param connectionOriented <code>true</code> for connection oriented mode,
	 *        <code>false</code> to use connectionless mode
	 * @param keepAlive <code>true</code> to prevent a timing out of the logical
	 *        connection in connection oriented mode, <code>false</code> to use default
	 *        connection timeout
	 * @param verifyMode <code>true</code> to indicate the destination has verify mode
	 *        enabled, <code>false</code> otherwise
	 */
	public Destination(final AggregatorProxy aggregator, final IndividualAddress remote,
			final boolean connectionOriented, final boolean keepAlive, final boolean verifyMode)
	{
		tl = aggregator.aggr;
		aggregator.setDestination(this);
		addr = remote;
		co = connectionOriented;
		alive = co ? keepAlive : false;
		verify = verifyMode;
	}

	/**
	 * Returns the destination address.
	 * 
	 * @return the destination individual address
	 */
	public IndividualAddress getAddress()
	{
		return addr;
	}

	/**
	 * Returns the state of this destination.
	 * <p>
	 * The returned value is one of the destination state constants.
	 * 
	 * @return destination state
	 */
	public final int getState()
	{
		return state;
	}

	/**
	 * Returns whether this destination uses connection oriented mode or connectionless
	 * mode.
	 * 
	 * @return <code>true</code> for connection oriented mode, <code>false</code>
	 *         otherwise
	 */
	public final boolean isConnectionOriented()
	{
		return co;
	}

	/**
	 * Returns whether keep alive of connection is specified.
	 * <p>
	 * 
	 * @return <code>true</code> if keep alive is specified and connection oriented mode
	 *         is used, <code>false</code> otherwise
	 */
	public final boolean isKeepAlive()
	{
		return alive;
	}

	/**
	 * Returns whether verify mode is supported by the destination.
	 * <p>
	 * 
	 * @return <code>true</code> for verify mode enabled, <code>false</code> otherwise
	 */
	public final boolean isVerifyMode()
	{
		return verify;
	}

	/**
	 * Destroys this destination.
	 * <p>
	 * If the connection state is connected, it will be disconnected. The connection state
	 * is set to {@link #DESTROYED}. The associated transport layer is notified through
	 * {@link TransportLayer#destroyDestination(Destination)}. <br>
	 * On an already destroyed destination, no action is performed.
	 */
	public synchronized void destroy()
	{
		if (state == DESTROYED)
			return;
		if (state != DISCONNECTED)
			try {
				tl.disconnect(this);
			}
			catch (final KNXLinkClosedException e) {
				// we already should've been destroyed on catching this exception
			}
		setState(DESTROYED, null);
		tl.destroyDestination(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		final String s = "destination " + addr + " (" + tl.getName() + ") "
			+ getStateString();
		if (state == DESTROYED)
			return s;
		return s + (co ? "" : " not") + " conn.oriented, " + (alive ? "" : "no")
			+ " keep alive, " + (verify ? "" : "no") + " verify mode";
	}

	int getDisconnectedBy()
	{
		return disconnectedBy;
	}
	
	private String getStateString()
	{
		switch (state) {
		case DISCONNECTED:
			return "disconnected";
		case CONNECTING:
			return "connecting";
		case OPEN_IDLE:
			return "open idle";
		case OPEN_WAIT:
			return "open wait";
		case DESTROYED:
			return "destroyed";
		default:
			return "unknown";
		}
	}

	private synchronized void setState(final int newState, final Runnable notify)
	{
		if (state == DESTROYED)
			return;
		state = newState;
		if (state == CONNECTING) {
			seqSend = 0;
			seqRcv = 0;
		}
		else if (state == OPEN_IDLE)
			restartTimer(notify);
		else if (state == OPEN_WAIT)
			restartTimer(notify);
		else if (state == DISCONNECTED)
			disconnectTimer.cancel(notify);
		else if (state == DESTROYED)
			disconnectTimer.cancel(notify);
	}

	private void restartTimer(final Runnable notify)
	{
		if (!co)
			throw new KNXIllegalStateException("no timer if not connection oriented");
		if (state == DESTROYED)
			return;

		if (!disconnectTimer.isAlive())
			try {
				disconnectTimer.start();
			}
			catch (final IllegalStateException ignore) {
				// might occur due to concurrency when started by >1 destinations
			}
		disconnectTimer.cancel(notify);
		disconnectTimer.submit(notify, System.currentTimeMillis() + TIMEOUT);
	}
}
