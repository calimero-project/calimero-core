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

package tuwien.auto.calimero.link;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

/**
 * Connector for KNX network links.
 *
 * @author B. Malinowsky
 */
public final class Connector
{
	// TODO cleanup exceptions, stack traces

	// TODO with functional interfaces, we should move away from checked exceptions
	@FunctionalInterface
	public interface TSupplier<T>
	{
		T get() throws KNXException, InterruptedException;
	}

	private boolean onCreation = true;
	private boolean onSend = true;
	private long reconnectWait = 2000; // [ms]
	// reconnect on disconnect caused by:
	private boolean initialError = false;
	private boolean serverError = true;
	private boolean internalError = true;

	// runs out earliest after (2^63-1)/1000/3600/24/365 ~ 3×10^8 years. Safe bet KNX got canned by then
	public static final long NoMaxAttempts = Long.MAX_VALUE;
	private long maxAttempts = 5;

	public Connector()
	{}

	// copy ctor
	private Connector(final Connector rhs)
	{
		this.onCreation = rhs.onCreation;
		this.onSend = rhs.onSend;
		this.reconnectWait = rhs.reconnectWait;
		this.initialError = rhs.initialError;
		this.serverError = rhs.serverError;
		this.internalError = rhs.internalError;
		this.maxAttempts = rhs.maxAttempts;
	}

	public Connector connectOnCreation(final boolean connect)
	{
		this.onCreation = connect;
		return this;
	}

	public Connector maxConnectAttempts(final long maxAttempts)
	{
		if (maxAttempts < 1)
			throw new KNXIllegalArgumentException("max. connect attempts " + maxAttempts + " < 1");
		this.maxAttempts = maxAttempts;
		return this;
	}

	public Connector connectOnSend(final boolean connect)
	{
		onSend = connect;
		return this;
	}

	public Connector reconnectWait(final long wait, final TimeUnit unit)
	{
		reconnectWait = unit.toMillis(wait);
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

	/**
	 * Returns a new KNXNetworkLink with the specified behavior for (re-)connection to the KNX
	 * network.
	 *
	 * @param creator supplies the specific KNX network link
	 * @return a new KNX network link with the specified (re-)connection behavior configured
	 * @throws KNXException
	 * @throws InterruptedException
	 */
	public <T> KNXNetworkLink newLink(final TSupplier<? extends KNXNetworkLink> creator)
		throws KNXException, InterruptedException
	{
		return new LinkProxy(creator, this);
	}

	// ??? add static factory method that directly creates link using supplier only
//	public static <T, U> KNXNetworkLink newLink(final TSupplier<? extends KNXNetworkLink> creator)
//		throws KNXException, InterruptedException
//	{
//		return creator.get();
//	}

	// interruption policy: close link resource
	private static final class LinkProxy implements KNXNetworkLink, NetworkLinkListener
	{
		private volatile KNXNetworkLink impl;
		// ??? right now we cannot access link internals, have to duplicate them here
		private final List<NetworkLinkListener> listeners = new ArrayList<>();
		private volatile KNXMediumSettings settings;
		private volatile int hopCount;

		private final TSupplier<? extends KNXNetworkLink> creator;

		// To avoid any kind of resource exhaustion, declare as thread pool executor which
		// implements ScheduledExecutorService. This way, we can set a reasonable bound
		// for the maximum thread pool size.
		// XXX use daemon threads, or shutdown orderly if Calimero exits
		private static ScheduledThreadPoolExecutor reconnect = new ScheduledThreadPoolExecutor(1);
		static {
			reconnect.setMaximumPoolSize(20);
		}

		// we save a copy of the connector options that won't get modified
		private final Connector connector;
		private volatile boolean closed;
		private volatile Future<?> f = CompletableFuture.completedFuture(null);
		private final AtomicBoolean connecting = new AtomicBoolean();

		private LinkProxy(final TSupplier<? extends KNXNetworkLink> creator, final Connector options)
			throws KNXException, InterruptedException
		{
			this.creator = creator;
			connector = new Connector(options);

			logger().warn("!!! Experimental feature: KNX network link via Connector");
			try {
				if (connector.onCreation)
					connect();
			}
			catch (final KNXException e) {
				if (!connector.initialError)
					throw e;
				scheduleConnect(connector.maxAttempts - 1);
				// TODO should we wait here until scheduled connects are done?
				// wait ... then check link:
				//if (impl == null)
				//	throw new KNXLinkClosedException("connect error");
			}
		}

		@Override
		public void setKNXMedium(final KNXMediumSettings settings)
		{
			if (impl != null)
				impl.setKNXMedium(settings);
			this.settings = settings;
		}

		@Override
		public KNXMediumSettings getKNXMedium()
		{
			return impl != null ? impl.getKNXMedium() : settings;
		}

		@Override
		public void addLinkListener(final NetworkLinkListener l)
		{
			if (impl != null)
				impl.addLinkListener(l);
			listeners.add(l);
		}

		@Override
		public void removeLinkListener(final NetworkLinkListener l)
		{
			listeners.remove(l);
			if (impl != null)
				impl.removeLinkListener(l);
		}

		@Override
		public void setHopCount(final int count)
		{
			if (impl != null)
				impl.setHopCount(count);
			hopCount = count;
		}

		@Override
		public int getHopCount()
		{
			return impl != null ? impl.getHopCount() : hopCount;
		}

		@Override
		public void sendRequest(final KNXAddress dst, final Priority p, final byte[] nsdu)
			throws KNXTimeoutException, KNXLinkClosedException
		{
			link().sendRequest(dst, p, nsdu);
		}

		@Override
		public void sendRequestWait(final KNXAddress dst, final Priority p, final byte[] nsdu)
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
			return impl != null ? impl.getName() : "calimero.link";
		}

		@Override
		public boolean isOpen()
		{
			// XXX that's basically lying? there should be a way to show the currently known state
			return !closed;
		}

		@Override
		public void close()
		{
			closed = true;
			final boolean canceled = f.cancel(true);
			System.out.println("cancellation of " + f + " returned " + canceled);
			if (impl != null)
				impl.close();
		}

		@Override
		public void linkClosed(final CloseEvent e)
		{
			if ((e.getInitiator() == CloseEvent.INTERNAL && connector.internalError)
					|| (e.getInitiator() == CloseEvent.SERVER_REQUEST && connector.serverError))
				scheduleConnect(connector.maxAttempts);
		}

		@Override
		public void indication(final FrameEvent e)
		{}

		@Override
		public void confirmation(final FrameEvent e)
		{}

		private KNXNetworkLink link() throws KNXLinkClosedException
		{
			final KNXLinkClosedException lce = new KNXLinkClosedException("reconnecting link");
			try {
				final KNXNetworkLink l = impl;
				// ??? should immediate connects here also count for scheduled connects, and
				// increment the attempt counter?
				return l == null ? connect() : l.isOpen() ? l : connector.onSend
						? connect() : l;
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				lce.initCause(e);
			}
			catch (final KNXLinkClosedException e) {
				throw e;
			}
			catch (final KNXException e) {
				lce.initCause(e);
			}
			throw lce;
		}

		private void scheduleConnect(final long remainingAttempts)
		{
			if (closed || remainingAttempts <= 0)
				return;
			final Runnable s = () -> {
				try {
					final long max = connector.maxAttempts;
					final long attempt = max - remainingAttempts + 1;
					if (connector.maxAttempts == NoMaxAttempts)
						logger().info("execute scheduled connect {} (no max)", attempt);
					else
						logger().info("execute scheduled connect {}/{} ({} remaining)", attempt,
								max, (remainingAttempts - 1));
					connect();
				}
				catch (final InterruptedException e) {
					// TODO The interruption policy is that we close the link resource. Hence,
					// we could handle the interrupt as any other exception because the close flag
					// should be set already.
					e.printStackTrace();
					System.out.println("interrupted: closed flag = " + closed);
				}
				catch (final KNXException | RuntimeException e) {
					logger().error(e.getMessage());
					scheduleConnect(remainingAttempts - 1);
				}
			};
			f = reconnect.schedule(s, connector.reconnectWait, TimeUnit.MILLISECONDS);
			System.out.println("scheduled future " + f);
		}

		private KNXNetworkLink connect() throws InterruptedException, KNXException
		{
			if (impl != null && impl.isOpen()) {
				System.out.println("got active connection, cancel connect");
				return impl;
			}
			// if currently no connection attempt is active, we create one
			if (connecting.compareAndSet(false, true)) {
				try {
					final KNXNetworkLink link = creator.get();
					if (impl == null) {
						settings = link.getKNXMedium();
						hopCount = link.getHopCount();
					}
					link.setKNXMedium(settings);
					link.setHopCount(hopCount);
					link.addLinkListener(this);
					listeners.forEach(link::addLinkListener);
					impl = link;
				}
				catch (final KNXRemoteException e) {
					final KNXLinkClosedException lce = new KNXLinkClosedException(e.getMessage());
					lce.initCause(e);
					throw lce;
				}
				finally {
					connecting.set(false);
					synchronized (connecting) {
						connecting.notifyAll();
					}
				}
			}
			else {
				// if a connection attempt is active, we use that one
				synchronized (connecting) {
					while (connecting.get())
						connecting.wait();
				}
				if (impl == null || !impl.isOpen())
					throw new KNXLinkClosedException("ongoing connect attempt we waited for failed");
			}
			return impl;
		}

		private Logger logger()
		{
			return LoggerFactory.getLogger(getName());
		}
	}
}
