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

package tuwien.auto.calimero.mgmt;

import static tuwien.auto.calimero.mgmt.Destination.State.Connecting;
import static tuwien.auto.calimero.mgmt.Destination.State.Destroyed;
import static tuwien.auto.calimero.mgmt.Destination.State.Disconnected;
import static tuwien.auto.calimero.mgmt.Destination.State.OpenIdle;
import static tuwien.auto.calimero.mgmt.Destination.State.OpenWait;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXIllegalStateException;
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
 * If a destination is closed, it changes to state destroyed. After a destination got destroyed, it can't be used
 * for communication to that destination anymore, i.e., it's not possible to change the connection state.
 *
 * @author B. Malinowsky
 * @see TransportLayer
 * @see ManagementClient
 */
public class Destination implements AutoCloseable
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
		 * If the state of destination is {@link State#Destroyed}, setting of a new
		 * state is ignored.
		 *
		 * @param newState new destination state
		 */
		public void setState(final State newState)
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
		@Override
		public void run()
		{
			// with keep-alive we do not disconnect on timeout
			if (d.alive)
				return;

			final State state = d.getState();
			if (state != Disconnected && state != Destroyed)
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

	/** Destination state. */
	public enum State {
		/**
		 * Destination is destroyed.
		 */
		Destroyed,
		/**
		 * Connection state is disconnected.
		 */
		Disconnected,
		/**
		 * Connection state is connecting.
		 */
		Connecting,
		/**
		 * Connection state is open and communication is in idle state.
		 */
		OpenIdle,
		/**
		 * Connection state is open and communication is in waiting state for Layer 4 acknowledgment.
		 */
		OpenWait
	};


	static final int USER_REQUEST = 0;
	static final int REMOTE_ENDPOINT = 1;
	static final int LOCAL_ENDPOINT = 2;

	volatile int disconnectedBy = -1;

	private final TransportLayer tl;
	private final IndividualAddress addr;
	private volatile State state = Disconnected;
	private int seqRcv;
	private int seqSend;
	private final boolean co;
	private final boolean alive;

	// see 03/05/01 Resources: Verify Mode Control
	private final boolean verify;

	/**
	 * Creates a new destination, verify mode defaults to false and keep alive is not used.
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
	public final IndividualAddress getAddress()
	{
		return addr;
	}

	/**
	 * Returns the state of this destination.
	 *
	 * @return destination state
	 */
	public final State getState()
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
	 * is set to {@link State#Destroyed}. The associated transport layer is notified through
	 * {@link TransportLayer#destroyDestination(Destination)}. <br>
	 * On an already destroyed destination, no action is performed.
	 */
	public synchronized void destroy()
	{
		if (state == Destroyed)
			return;
		if (state != Disconnected)
			try {
				tl.disconnect(this);
			}
			catch (final KNXLinkClosedException e) {
				// we already should've been destroyed on catching this exception
			}
		setState(Destroyed, null);
		tl.destroyDestination(this);
	}

	@Override
	public void close()
	{
		destroy();
	}

	@Override
	public String toString()
	{
		String s = "destination " + addr + " (" + tl.getName() + ") ";
		if (state == Destroyed)
			return s + getStateString();
		// keep-alive and detailed state only apply in CO mode
		if (co)
			s = s + getStateString() +  ", conn.-oriented," + (alive ? "w/" : " no") + " keep-alive, ";
		else
			s = s + "connectionless, ";
		return s + (verify ? "" : "no") + " verify mode";
	}

	int getDisconnectedBy()
	{
		return disconnectedBy;
	}

	private String getStateString()
	{
		return state.name();
	}

	private synchronized void setState(final State newState, final Runnable notify)
	{
		if (state == Destroyed)
			return;
		state = newState;
		if (state == Connecting) {
			seqSend = 0;
			seqRcv = 0;
		}
		else if (state == OpenIdle)
			restartTimer(notify);
		else if (state == OpenWait)
			restartTimer(notify);
		else if (state == Disconnected) {
			disconnectTimer.cancel(notify);
			// required for server-side, otherwise server would expect old sequence and return NAK
			seqSend = 0;
			seqRcv = 0;
		}
		else if (state == Destroyed)
			disconnectTimer.cancel(notify);
	}

	private void restartTimer(final Runnable notify)
	{
		if (!co)
			throw new KNXIllegalStateException("no timer if not connection oriented");
		if (state == Destroyed)
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
