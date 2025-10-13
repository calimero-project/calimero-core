/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2015, 2025 B. Malinowsky

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

package io.calimero.link;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.calimero.CloseEvent;
import io.calimero.KNXAddress;
import io.calimero.KNXException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.KNXRemoteException;
import io.calimero.KNXTimeoutException;
import io.calimero.Priority;
import io.calimero.cemi.CEMILData;
import io.calimero.internal.Executor;
import io.calimero.link.medium.KNXMediumSettings;
import io.calimero.link.medium.PLSettings;
import io.calimero.link.medium.RFSettings;
import io.calimero.log.LogService;

/**
 * Connector for KNX network links.
 *
 * @author B. Malinowsky
 */
public final class Connector
{
	// TODO with functional interfaces, we should move away from checked exceptions
	@FunctionalInterface
	public interface TSupplier<T>
	{
		T get() throws KNXException, InterruptedException;
	}

	private boolean onSend = true;
	private long reconnectDelay = 4000; // [ms]
	// reconnect on disconnect caused by:
	private boolean initialError;
	private boolean serverError = true;
	private boolean internalError = true;

	// runs out earliest after (2^63-1)/1000/3600/24/365 ~ 3×10^8 years. Safe bet KNX got canned by then
	public static final long NoMaxAttempts = Long.MAX_VALUE;
	private long maxAttempts = NoMaxAttempts;

	private volatile Consumer<Boolean> connectionStatusChanged = __ -> {};

	public Connector() {}

	// copy ctor
	private Connector(final Connector rhs)
	{
		this.onSend = rhs.onSend;
		this.reconnectDelay = rhs.reconnectDelay;
		this.initialError = rhs.initialError;
		this.serverError = rhs.serverError;
		this.internalError = rhs.internalError;
		this.maxAttempts = rhs.maxAttempts;
		this.connectionStatusChanged = rhs.connectionStatusChanged;
	}

	// on successful connection, the attempts are reset to maxAttempts
	public Connector maxConnectAttempts(final long maxAttempts)
	{
		if (maxAttempts < 0)
			throw new KNXIllegalArgumentException("max. connect attempts " + maxAttempts + " < 0");
		this.maxAttempts = maxAttempts;
		return this;
	}

	public Connector connectOnSend(final boolean connect)
	{
		onSend = connect;
		return this;
	}

	public Connector reconnectDelay(final Duration delay) {
		reconnectDelay = delay.toMillis();
		return this;
	}

	public Connector reconnectOn(final boolean errorOnCreation, final boolean serverDisconnect,
		final boolean internalDisconnect)
	{
		initialError = errorOnCreation;
		serverError = serverDisconnect;
		internalError = internalDisconnect;
		return this;
	}

	public Connector connectionStatusNotifier(final Consumer<Boolean> connected) {
		connectionStatusChanged = connected;
		return this;
	}

	/**
	 * Returns a new KNXNetworkLink with the specified behavior for (re-)connection to the KNX network.
	 *
	 * @param creator supplies the specific KNX network link
	 * @return a new KNX network link with the specified (re-)connection behavior configured
	 * @throws KNXException on error creating the network link
	 * @throws InterruptedException on interrupted thread
	 */
	public KNXNetworkLink newLink(final TSupplier<? extends KNXNetworkLink> creator)
		throws KNXException, InterruptedException
	{
		return new Link<KNXNetworkLink>(creator, this);
	}

	/**
	 * Returns a new KNXNetworkMonitor with the specified behavior for (re-)connection to the KNX network.
	 *
	 * @param creator supplies the specific KNX network monitor
	 * @return a new KNX network monitor with the specified (re-)connection behavior configured
	 * @throws KNXException on error creating the monitor link
	 * @throws InterruptedException on interrupted thread
	 */
	public KNXNetworkMonitor newMonitor(final TSupplier<? extends KNXNetworkMonitor> creator)
		throws KNXException, InterruptedException
	{
		return new Link<KNXNetworkMonitor>(creator, this);
	}

	// interruption policy: close link resource
	public static final class Link<T extends AutoCloseable>
			implements KNXNetworkLink, KNXNetworkMonitor, NetworkLinkListener
	{
		private volatile T impl;
		private final List<LinkListener> listeners = new CopyOnWriteArrayList<>();
		private volatile int hopCount;
		// monitor: decode raw frames
		private volatile boolean decodeRawFrames;

		private final TSupplier<? extends T> creator;

		// we save a copy of the connector options that won't get modified
		private final Connector connector;

		private volatile boolean closed;
		private volatile Future<?> f = CompletableFuture.completedFuture(Void.TYPE);
		private final AtomicBoolean connecting = new AtomicBoolean();
		private final Object lock = new Object();

		private Link(final TSupplier<? extends T> creator, final Connector options)
			throws KNXException, InterruptedException
		{
			this.creator = creator;
			connector = new Connector(options);
			try {
				connect();
			}
			catch (final KNXException e) {
				if (!connector.initialError || connector.maxAttempts <= 1)
					throw e;
				logger().log(ERROR, "initial connection attempt", e);
				scheduleConnect(connector.maxAttempts - 1);
			}
		}

		/**
		 * @return the currently used connection instance of type {@link KNXNetworkLink} or
		 *         {@link KNXNetworkMonitor}, or null
		 */
		public AutoCloseable target()
		{
			return impl;
		}

		@SuppressWarnings("removal")
		public void setKNXMedium(final KNXMediumSettings settings) {}

		@Override
		public KNXMediumSettings getKNXMedium()
		{
			final T t = impl;
			if (t instanceof final KNXNetworkLink link)
				return link.getKNXMedium();
			if (t instanceof final KNXNetworkMonitor monitor)
				return monitor.getKNXMedium();
			return null;
		}

		@Override
		public void addLinkListener(final NetworkLinkListener l)
		{
			if (impl instanceof final KNXNetworkLink link)
				link.addLinkListener(l);
			listeners.add(l);
		}

		@Override
		public void removeLinkListener(final NetworkLinkListener l)
		{
			listeners.remove(l);
			if (impl instanceof final KNXNetworkLink link)
				link.removeLinkListener(l);
		}

		@Override
		public void addMonitorListener(final LinkListener l)
		{
			if (impl instanceof final KNXNetworkMonitor monitor)
				monitor.addMonitorListener(l);
			listeners.add(l);
		}

		@Override
		public void removeMonitorListener(final LinkListener l)
		{
			listeners.remove(l);
			if (impl instanceof final KNXNetworkMonitor monitor)
				monitor.removeMonitorListener(l);
		}

		@Override
		public void setDecodeRawFrames(final boolean decode)
		{
			decodeRawFrames = decode;
			if (impl instanceof final KNXNetworkMonitor monitor)
				monitor.setDecodeRawFrames(decode);
		}

		@Override
		public void setHopCount(final int count)
		{
			if (impl instanceof final KNXNetworkLink link)
				link.setHopCount(count);
			hopCount = count;
		}

		@Override
		public int getHopCount()
		{
			return impl != null ? ((KNXNetworkLink) impl).getHopCount() : hopCount;
		}

		@Override
		public void sendRequest(final KNXAddress dst, final Priority p, final byte... nsdu)
			throws KNXTimeoutException, KNXLinkClosedException
		{
			link().sendRequest(dst, p, nsdu);
		}

		@Override
		public void sendRequestWait(final KNXAddress dst, final Priority p, final byte... nsdu)
			throws KNXTimeoutException, KNXLinkClosedException
		{
			link().sendRequestWait(dst, p, nsdu);
		}

		@Override
		public void send(final CEMILData msg, final boolean waitForCon) throws KNXTimeoutException,
			KNXLinkClosedException
		{
			link().send(msg, waitForCon);
		}

		@Override
		public String getName()
		{
			final T t = impl;
			if (t instanceof final KNXNetworkLink link)
				return link.getName();
			if (t instanceof final KNXNetworkMonitor monitor)
				return monitor.getName();
			return "connector";
		}

		@Override
		public boolean isOpen()
		{
			return !closed;
		}

		@Override
		public void close()
		{
			closed = true;
			f.cancel(true);
			try {
				if (impl != null)
					impl.close();
			}
			catch (final Exception ignore) {}
		}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			connector.connectionStatusChanged.accept(false);
			if ((e.getInitiator() == CloseEvent.INTERNAL && connector.internalError)
					|| (e.getInitiator() == CloseEvent.SERVER_REQUEST && connector.serverError))
				scheduleConnect(connector.maxAttempts);
		}

		@Override
		public String toString() {
			final var v = impl;
			return v != null ? v.toString() : "link connecting...";
		}

		// only called for network link send
		private KNXNetworkLink link() throws KNXLinkClosedException
		{
			try {
				final KNXNetworkLink l = (KNXNetworkLink) impl;
				// ??? should immediate connects here also count for scheduled connects, and
				// increment the attempt counter?
				return (KNXNetworkLink) (l == null ? connect()
						: l.isOpen() ? l : connector.onSend ? connect() : l);
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new KNXLinkClosedException("interrupt on reconnecting link", e);
			}
			catch (final KNXLinkClosedException e) {
				throw e;
			}
			catch (final KNXException e) {
				throw new KNXLinkClosedException("reconnecting link", e);
			}
		}

		private void scheduleConnect(final long remainingAttempts)
		{
			if (closed)
				return;
			if (remainingAttempts <= 0) {
				closed = true;
				final var closed = new CloseEvent(this, CloseEvent.INTERNAL,
						"reached max. connect attempts of " + connector.maxAttempts);
				listeners.forEach(l -> l.linkClosed(closed));
				return;
			}
			final long max = connector.maxAttempts;
			final long remaining = remainingAttempts - 1;
			final long attempt = max - remaining;
			final Runnable s = () -> {
				Thread.currentThread().setName("Calimero Connector");
				try {
					final String maxSuffix;
					if (connector.maxAttempts == NoMaxAttempts)
						maxSuffix = " (no max)";
					else
						maxSuffix = "/" + max + " (" + remaining + " remaining)";
					logger().log(DEBUG, "execute scheduled connect {0}{1}", attempt, maxSuffix);
					connect();
				}
				catch (KNXException | RuntimeException | InterruptedException e) {
					if (attempt == 1)
						logger().log(WARNING, "connection attempt " + attempt, e);
					else {
						final Throwable cause = e.getCause();
						final String detail = cause != null && cause.getMessage() != null ? " (" + cause.getMessage() + ")" : "";
						logger().log(WARNING, "connection attempt {0}: {1}{2}", attempt, e.getMessage(), detail);
					}
					scheduleConnect(remaining);
				}
			};
			f = Executor.scheduledExecutor().schedule(s, connector.reconnectDelay, TimeUnit.MILLISECONDS);
		}

		private AutoCloseable connect() throws InterruptedException, KNXException
		{
			if (targetOpen()) {
				return impl;
			}
			// if currently no connection attempt is active, we create one
			if (connecting.compareAndSet(false, true)) {
				try {
					final T t = creator.get();
					if (t instanceof final KNXNetworkLink link) {
						final var old = getKNXMedium();
						if (old == null) {
							hopCount = link.getHopCount();
						}
						else {
							// adjust some medium settings of the new link with the old ones if appropriate
							final var adjust = link.getKNXMedium();
							if (!adjust.getDeviceAddress().equals(old.getDeviceAddress()))
								adjust.setDeviceAddress(old.getDeviceAddress());
							if (adjust.maxApduLength() != old.maxApduLength())
								adjust.setMaxApduLength(old.maxApduLength());
							if (adjust instanceof final PLSettings pl && old instanceof final PLSettings plOld
									&& !Arrays.equals(pl.getDomainAddress(), plOld.getDomainAddress()))
								pl.setDomainAddress(plOld.getDomainAddress());
							if (adjust instanceof final RFSettings rf && old instanceof final RFSettings rfOld
									&& !Arrays.equals(rf.getDomainAddress(), rfOld.getDomainAddress()))
								rf.setDomainAddress(rfOld.getDomainAddress());
						}
						link.setHopCount(hopCount);
						link.addLinkListener(this);
						if (link instanceof final AbstractLink<?> abstractLink)
							abstractLink.wrappedByConnector = true;
						listeners.forEach(l -> link.addLinkListener((NetworkLinkListener) l));
					}
					else if (t instanceof final KNXNetworkMonitor monitor) {
						monitor.setDecodeRawFrames(decodeRawFrames);
						monitor.addMonitorListener(this);
						if (monitor instanceof final AbstractMonitor<?> abstractLink)
							abstractLink.wrappedByConnector = true;
						listeners.forEach(monitor::addMonitorListener);
					}
					impl = t;
				}
				catch (final KNXRemoteException e) {
					throw new KNXLinkClosedException(e.getMessage(), e);
				}
				finally {
					connecting.set(false);
					synchronized (lock) {
						lock.notifyAll();
					}
				}
				connector.connectionStatusChanged.accept(true);
			}
			else {
				// if a connection attempt is active, we use that one
				synchronized (lock) {
					while (connecting.get())
						lock.wait();
				}
				if (!targetOpen())
					throw new KNXLinkClosedException("ongoing connect attempt failed");
			}
			return impl;
		}

		private boolean targetOpen()
		{
			final T t = impl;
			if (t instanceof final KNXNetworkLink link)
				return link.isOpen();
			if (t instanceof final KNXNetworkMonitor monitor)
				return monitor.isOpen();
			return false;
		}

		private Logger logger()
		{
			return LogService.getLogger("io.calimero.link." + getName());
		}
	}
}
