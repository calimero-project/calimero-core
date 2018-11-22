/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2018 B. Malinowsky

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.mgmt.Destination.AggregatorProxy;

/**
 * Implementation of the transport layer protocol.
 * <p>
 * All sending is done blocking on the attached network link, so an eventual confirmation
 * response is implicit by return of a send method, there are no explicit confirmation
 * notifications.
 * <p>
 * Once this transport layer has been {@link TransportLayer#detach()}ed, it can't be used
 * for any further layer 4 communication, and it can't be attached to a new network link.
 * <br>
 * All methods invoked after a detach of the network link used for communication are
 * allowed to throw {@link IllegalStateException}.
 *
 * @author B. Malinowsky
 */
public class TransportLayerImpl implements TransportLayer
{
	private final class NLListener implements NetworkLinkListener
	{
		NLListener()
		{}

		@Override
		public void confirmation(final FrameEvent e)
		{}

		@Override
		public void indication(final FrameEvent e)
		{
			final CEMILData f = (CEMILData) e.getFrame();
			final int ctrl = f.getPayload()[0] & 0xfc;
			if (ctrl == 0) {
				final KNXAddress dst = f.getDestination();
				if (dst instanceof GroupAddress)
					// check for broadcast or group
					fireFrameType(f, dst.getRawAddress() == 0 ? 0 : 1);
				else
					// individual
					fireFrameType(f, 2);
			}
			else {
				final IndividualAddress src = f.getSource();
				// are we waiting for ack?
				synchronized (indications) {
					if (active != null && active.getDestination().getAddress().equals(src)) {
						indications.add(e);
						indications.notify();
						return;
					}
				}
				AggregatorProxy ap = null;
				synchronized (proxies) {
					ap = proxies.get(src);
				}
				try {
					handleConnected(f, ap);
				}
				catch (KNXLinkClosedException | KNXTimeoutException ignore) {
					// we get notified with link-closed event, possible timeouts on sending ack don't matter
				}
				catch (final RuntimeException rte) {
					logger.error("{}: {}", ap != null ? ap.getDestination() : "destination n/a", f, rte);
				}
			}
		}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			logger.info("attached link was closed");
			closeDestinations(true);
			fireLinkClosed(e);
		}
	}

	private static final int CONNECT = 0x80;
	private static final int DISCONNECT = 0x81;
	private static final int ACK = 0xC2;
	private static final int NACK = 0xC3;
	private static final int DATA_CONNECTED = 0x40;
	// group, broadcast and individual request service codes are 0

	// acknowledge timeout in seconds
	private static final int ACK_TIMEOUT = 3;
	// maximum repetitions of send in connected mode
	private static final int MAX_REPEAT = 3;

	private static final GroupAddress broadcast = new GroupAddress(0);
	// used as default on incoming conn.oriented messages from unknown remote devices
	private final Destination unknownPartner = new Destination(new AggregatorProxy(this),
		new IndividualAddress(0), true);

	private final Logger logger;

	// are we representing server side of this transport layer connection
	private final boolean serverSide;

	private volatile boolean detached;
	private final KNXNetworkLink lnk;
	private final NetworkLinkListener lnkListener = new NLListener();
	private final Deque<FrameEvent> indications = new ArrayDeque<>();
	private final EventListeners<TransportListener> listeners;

	// holds the mapping of connection destination address to proxy
	private final Map<IndividualAddress, AggregatorProxy> proxies = new HashMap<>();
	private final Map<IndividualAddress, AggregatorProxy> incomingProxies = new HashMap<>();
	private AggregatorProxy active;

	private volatile int repeated;
	private final Object lock = new Object();

	/**
	 * Creates a new client-side transport layer end-point attached to the supplied KNX network
	 * link.
	 *
	 * @param link network link used for communication with a KNX network
	 * @throws KNXLinkClosedException if the network link is closed
	 */
	public TransportLayerImpl(final KNXNetworkLink link) throws KNXLinkClosedException
	{
		this(link, false);
	}

	/**
	 * Creates a new transport layer end-point attached to the supplied KNX network link.
	 *
	 * @param link network link used for communication with a KNX network
	 * @param serverEndpoint does this instance represent the client-side (<code>false</code>),
	 *        or server-side (<code>true</code>) end-point
	 * @throws KNXLinkClosedException if the network link is closed
	 */
	public TransportLayerImpl(final KNXNetworkLink link, final boolean serverEndpoint)
		throws KNXLinkClosedException
	{
		if (!link.isOpen())
			throw new KNXLinkClosedException(
					"cannot initialize transport layer using closed link " + link.getName());
		lnk = link;
		lnk.addLinkListener(lnkListener);
		logger = LogService.getLogger("calimero.mgmt." + getName());
		listeners = new EventListeners<>(logger);
		serverSide = serverEndpoint;
	}

	/**
	 * {@inheritDoc} Only one destination can be created per remote address. If a
	 * destination with the supplied remote address already exists for this transport
	 * layer, a {@link KNXIllegalArgumentException} is thrown.<br>
	 * A transport layer can only handle one connection per destination, because it can't
	 * distinguish incoming messages between more than one connection.
	 */
	@Override
	public Destination createDestination(final IndividualAddress remote,
		final boolean connectionOriented)
	{
		return createDestination(remote, connectionOriented, false, false);
	}

	/**
	 * {@inheritDoc} Only one destination can be created per remote address. If a
	 * destination with the supplied remote address already exists for this transport
	 * layer, a {@link KNXIllegalArgumentException} is thrown.<br>
	 * A transport layer can only handle one connection per destination, because it can't
	 * distinguish incoming messages between more than one connection.
	 */
	@Override
	public Destination createDestination(final IndividualAddress remote,
		final boolean connectionOriented, final boolean keepAlive, final boolean verifyMode)
	{
		if (detached)
			throw new IllegalStateException("TL detached");
		synchronized (proxies) {
			if (proxies.containsKey(remote))
				throw new KNXIllegalArgumentException("destination already created: " + remote);
			final AggregatorProxy p = new AggregatorProxy(this);
			final Destination d = new Destination(p, remote, connectionOriented, keepAlive, verifyMode);
			proxies.put(remote, p);
			logger.trace("created {} destination for {}", (connectionOriented ? "co" : "cl"), remote);
			return d;
		}
	}

	/**
	 * Returns the destination object for the remote individual address, if such exists.
	 *
	 * @param remote the remote address to look up
	 * @return the destination for that address, or <code>null</code> if no destination
	 *         is currently maintained by the transport layer
	 */
	public Destination getDestination(final IndividualAddress remote)
	{
		final AggregatorProxy proxy = proxies.get(remote);
		return proxy != null ? proxy.getDestination() : null;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#destroyDestination
	 * (tuwien.auto.calimero.mgmt.Destination)
	 */
	@Override
	public void destroyDestination(final Destination d)
	{
		// method invocation is idempotent
		synchronized (proxies) {
			final AggregatorProxy p = proxies.get(d.getAddress());
			if (p == null)
				return;
			if (p.getDestination() == d) {
				d.destroy();
				proxies.remove(d.getAddress());
				synchronized (indications) {
					indications.notify();
				}
			}
			else
				logger.warn("not owner of " + d.getAddress());
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#addTransportListener
	 * (tuwien.auto.calimero.mgmt.TransportListener)
	 */
	@Override
	public void addTransportListener(final TransportListener l)
	{
		listeners.add(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#removeTransportListener
	 * (tuwien.auto.calimero.mgmt.TransportListener)
	 */
	@Override
	public void removeTransportListener(final TransportListener l)
	{
		listeners.remove(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#connect
	 * (tuwien.auto.calimero.mgmt.Destination)
	 */
	@Override
	public void connect(final Destination d) throws KNXTimeoutException, KNXLinkClosedException
	{
		final AggregatorProxy p = getProxy(d);
		if (!d.isConnectionOriented()) {
			logger.error("destination not connection-oriented: " + d.getAddress());
			return;
		}
		if (d.getState() != Disconnected)
			return;
		p.setState(Connecting);
		final byte[] tpdu = new byte[] { (byte) CONNECT };
		lnk.sendRequestWait(d.getAddress(), Priority.SYSTEM, tpdu);
		p.setState(OpenIdle);
		logger.trace("connected with {}", d.getAddress());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#disconnect
	 * (tuwien.auto.calimero.mgmt.Destination)
	 */
	@Override
	public void disconnect(final Destination d) throws KNXLinkClosedException
	{
		if (detached)
			throw new IllegalStateException("TL detached");
		if (d.getState() != Destroyed && d.getState() != Disconnected)
			disconnectIndicate(getProxy(d), true);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#sendData
	 * (tuwien.auto.calimero.mgmt.Destination, tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void sendData(final Destination d, final Priority p, final byte[] tsdu)
		throws KNXDisconnectException, KNXLinkClosedException
	{
		final AggregatorProxy ap = getProxy(d);
		if (d.getState() == Disconnected) {
			final KNXDisconnectException e = new KNXDisconnectException("no connection opened for "
					+ d.getAddress(), d);
			logger.warn("send failed", e);
			throw e;
		}
		tsdu[0] = (byte) (tsdu[0] & 0x03 | DATA_CONNECTED | ap.getSeqSend() << 2);
		// the entry lock guards between send and return (only one at a time)
		synchronized (lock) {
			// on indications we do wait() for incoming messages
			synchronized (indications) {
				try {
					active = ap;
					for (repeated = 0; repeated < MAX_REPEAT + 1; ++repeated) {
						try {
							logger.trace("sending data connected to {}, attempt {}", d.getAddress(), (repeated + 1));
							// set state and timer
							ap.setState(OpenWait);
							lnk.sendRequestWait(d.getAddress(), p, tsdu);
							if (waitForAck())
								return;
						}
						catch (final KNXTimeoutException e) {}
						// cancel repetitions if detached or destroyed
						if (detached || d.getState() == Destroyed)
							throw new KNXDisconnectException("send data connected failed", d);
					}
				}
				finally {
					active = null;
					repeated = 0;
				}
			}
		}
		disconnectIndicate(ap, true);
		throw new KNXDisconnectException("send data connected failed", d);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#sendData
	 * (tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void sendData(final KNXAddress addr, final Priority p, final byte[] tsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		if (detached)
			throw new IllegalStateException("TL detached");
		tsdu[0] &= 0x03;
		lnk.sendRequestWait(addr, p, tsdu);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#broadcast
	 * (boolean, tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void broadcast(final boolean system, final Priority p, final byte[] tsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		sendData(system ? null : broadcast, p, tsdu);
	}

	/**
	 * {@inheritDoc} If {@link #detach()} was invoked for this layer, "TL (detached)" is
	 * returned.
	 */
	@Override
	public String getName()
	{
		return "TL " + (detached ? "(detached)" : lnk.getName());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.TransportLayer#detach()
	 */
	@Override
	public synchronized KNXNetworkLink detach()
	{
		if (detached)
			return null;
		closeDestinations(false);
		lnk.removeLinkListener(lnkListener);
		detached = true;
		fireDetached();
		logger.debug("detached from " + lnk.getName());
		return lnk;
	}

	private AggregatorProxy getProxy(final Destination d)
	{
		if (detached)
			throw new IllegalStateException("TL detached");
		synchronized (proxies) {
			final AggregatorProxy p = proxies.get(d.getAddress());
			// TODO at this point, proxy might also be null because destination just got destroyed
			// check identity, too, to prevent destination with only same address
			if (p == null || p.getDestination() != d)
				throw new KNXIllegalArgumentException("not the owner of " + d.toString());
			return p;
		}
	}

	private void handleConnected(final CEMILData frame, final AggregatorProxy p)
		throws KNXLinkClosedException, KNXTimeoutException
	{
		final IndividualAddress sender = frame.getSource();
		final byte[] tpdu = frame.getPayload();
		final int ctrl = tpdu[0] & 0xFF;
		final int seq = (tpdu[0] & 0x3C) >>> 2;

		// on proxy null (no destination found for sender) use 'no partner' placeholder
		@SuppressWarnings("resource")
		final Destination d = p != null ? p.getDestination() : unknownPartner;

		if (ctrl == CONNECT) {
			if (serverSide) {
				AggregatorProxy proxy = p;
				// if we receive a new connect, but an old destination is still
				// here configured as connection-less, we get a problem with setting
				// the connection timeout (connectionless has no support for that)
				if (proxy != null && !d.isConnectionOriented()) {
					logger.warn(d + ": recreate for conn-oriented");
					d.destroy();
					proxy = null;
				}

				// allow incoming connect requests (server)
				if (proxy == null) {
					proxy = new AggregatorProxy(this);
					// constructor of destination assigns itself to ap
					new Destination(proxy, sender, true);
					incomingProxies.put(sender, proxy);
					proxies.put(sender, proxy);
					proxy.setState(OpenIdle);
				}
				else {
					// reset the sequence counters
					proxy.setState(Connecting);
					// restart disconnect timer
					proxy.setState(OpenIdle);
				}
			}
			else {
				// don't allow (client side)
				if (d.getState() == Disconnected)
					checkSendDisconnect(frame);
			}
		}
		else if (ctrl == DISCONNECT) {
			if (d.getState() != Disconnected && sender.equals(d.getAddress()))
				disconnectIndicate(p, false);
		}
		else if ((ctrl & 0xC0) == DATA_CONNECTED) {
			if (d.getState() == Disconnected || !sender.equals(d.getAddress())) {
				checkSendDisconnect(frame);
			}
			else {
				Objects.requireNonNull(p);
				p.restartTimeout();
				if (seq == p.getSeqReceive()) {
					lnk.sendRequest(sender, Priority.SYSTEM, new byte[] { (byte) (ACK | p.getSeqReceive() << 2) });
					p.incSeqReceive();
					fireFrameType(frame, 3);
				}
				else if (seq == (p.getSeqReceive() - 1 & 0xF))
					lnk.sendRequest(sender, Priority.SYSTEM, new byte[] { (byte) (ACK | seq << 2) });
				else
					lnk.sendRequest(sender, Priority.SYSTEM, new byte[] { (byte) (NACK | seq << 2) });
			}
		}
		else if ((ctrl & 0xC3) == ACK) {
			if (d.getState() == Disconnected || !sender.equals(d.getAddress()))
				checkSendDisconnect(frame);
			else if (d.getState() == OpenWait && seq == Objects.requireNonNull(p).getSeqSend()) {
				p.incSeqSend();
				p.setState(OpenIdle);
				logger.trace("positive ack by {}", d.getAddress());
			}
			else
				disconnectIndicate(p, true);
		}
		else if ((ctrl & 0xC3) == NACK) {
			if (d.getState() == Disconnected || !sender.equals(d.getAddress()))
				checkSendDisconnect(frame);
			else if (d.getState() == OpenWait && seq == Objects.requireNonNull(p).getSeqSend()
					&& repeated < MAX_REPEAT) {
				; // do nothing, we will send message again
			}
			else
				disconnectIndicate(p, true);
		}
	}

	private boolean waitForAck() throws KNXTimeoutException, KNXDisconnectException, KNXLinkClosedException
	{
		boolean interrupted = false;
		try {
			long remaining = ACK_TIMEOUT * 1000L;
			final long end = System.currentTimeMillis() + remaining;
			final Destination d = active.getDestination();
			while (remaining > 0) {
				try {
					while (indications.size() > 0)
						handleConnected((CEMILData) indications.remove().getFrame(), active);
					if (d.getState() == Disconnected || d.getState() == Destroyed)
						throw new KNXDisconnectException(d.getAddress() + " disconnected while awaiting ACK", d);
					if (d.getState() == OpenIdle)
						return true;
					indications.wait(remaining);
					if (d.getState() == Disconnected || d.getState() == Destroyed)
						throw new KNXDisconnectException(d.getAddress() + " disconnected while awaiting ACK", d);
				}
				catch (final InterruptedException e) {
					interrupted = true;
				}
				remaining = end - System.currentTimeMillis();
			}
		}
		finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
		return false;
	}

	private void closeDestinations(final boolean skipSendDisconnect)
	{
		// we can't use proxies default iterator due to concurrent modifications in
		// destroyDestination(), called by d.destroy()
		AggregatorProxy[] allProxies = new AggregatorProxy[proxies.size()];
		synchronized (proxies) {
			allProxies = proxies.values().toArray(allProxies);
		}
		for (int i = 0; i < allProxies.length; i++) {
			final AggregatorProxy p = allProxies[i];
			final Destination d = p.getDestination();
			if (skipSendDisconnect && d.getState() != Disconnected) {
				p.setState(Disconnected);
				fireDisconnected(d);
			}
			d.destroy();
		}
	}

	private void disconnectIndicate(final AggregatorProxy p,
		final boolean sendDisconnectReq) throws KNXLinkClosedException
	{
		p.setState(Disconnected);
		// TODO add initiated by user and refactor into a method
		p.getDestination().disconnectedBy = sendDisconnectReq ? Destination.LOCAL_ENDPOINT
				: Destination.REMOTE_ENDPOINT;
		try {
			if (sendDisconnectReq)
				sendDisconnect(p.getDestination().getAddress());
		}
		finally {
			fireDisconnected(p.getDestination());
			logger.trace("disconnected from {}", p.getDestination().getAddress());
		}
	}

	private void checkSendDisconnect(final CEMILData frame) throws KNXLinkClosedException {
		final IndividualAddress device = lnk.getKNXMedium().getDeviceAddress();
		if (device.getRawAddress() == 0 || device.equals(frame.getDestination()))
			sendDisconnect(frame.getSource());
	}

	private void sendDisconnect(final IndividualAddress addr) throws KNXLinkClosedException
	{
		final byte[] tpdu = { (byte) DISCONNECT };
		try {
			logger.trace("send disconnect to {}", addr);
			lnk.sendRequestWait(addr, Priority.SYSTEM, tpdu);
		}
		catch (final KNXTimeoutException ignore) {
			// do a warning, but otherwise can be ignored
			logger.warn("disconnected not gracefully (timeout)", ignore);
		}
	}

	private void fireDisconnected(final Destination d)
	{
		listeners.fire(l -> l.disconnected(d));
	}

	// type 0 = broadcast, 1 = group, 2 = individual, 3 = connected
	private void fireFrameType(final CEMI frame, final int type)
	{
		final FrameEvent e = new FrameEvent(this, frame);
		final Consumer<? super TransportListener> c;
		if (type == 0)
			c = l -> l.broadcast(e);
		else if (type == 1)
			c = l -> l.group(e);
		else if (type == 2)
			c = l -> l.dataIndividual(e);
		else if (type == 3)
			c = l -> l.dataConnected(e);
		else
			return;
		listeners.fire(c);
	}

	private void fireDetached()
	{
		final DetachEvent e = new DetachEvent(this);
		listeners.fire(l -> l.detached(e));
	}

	private void fireLinkClosed(final CloseEvent e)
	{
		listeners.fire(l -> l.linkClosed(e));
	}
}
