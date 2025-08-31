/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.calimero.CloseEvent;
import io.calimero.DataUnitBuilder;
import io.calimero.FrameEvent;
import io.calimero.KNXAddress;
import io.calimero.KNXTimeoutException;
import io.calimero.Priority;
import io.calimero.buffer.cache.Cache;
import io.calimero.buffer.cache.CacheObject;
import io.calimero.buffer.cache.LFUCache;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMILData;
import io.calimero.datapoint.DatapointModel;
import io.calimero.internal.EventListeners;
import io.calimero.link.KNXLinkClosedException;
import io.calimero.link.KNXNetworkLink;
import io.calimero.link.NetworkLinkListener;
import io.calimero.link.medium.KNXMediumSettings;

/**
 * A network buffer temporarily stores KNX network messages for improving response and/or KNX network performance. The
 * main purpose for using a network buffer is to lower response time as well as bus traffic when answering frequently
 * occurring application queries, leading to better runtime performance. Another use-case is to enable user-side polling
 * applications. Interfaces for KNX access as well as the KNX transmission medium differ in processing delays and
 * maximum message rates. This can have adverse effects -- caused by queue overflows or dropped messages -- for client
 * applications, which don't take those limitations into account. Overall, a network buffer helps in keeping the KNX
 * network message load lower for client usage patterns that are not optimal for a KNX network.
 * <p>
 * A network buffer provides the most up-to-date state (or command sequence) of buffered KNX datapoints. It is not
 * designed as general-purpose message store. KNX messages are buffered at link layer (L_Data).
 * <p>
 * A network buffer contains one or more {@link Configuration}s, each maintaining rules to filter and buffer messages. A
 * configuration works with filters set by the user for that particular configuration. A filter can be specified at two
 * points in the messages flow:
 * <ol>
 * <li>when messages arrive from the KNX network at the buffer, and</li>
 * <li>when the buffer shall answer a message request from a user.</li>
 * </ol>
 * Two types of filters exist for this:
 * <ol>
 * <li>a {@link Configuration.NetworkFilter} applies its filter rules on incoming messages,
 * <li>a {@link Configuration.RequestFilter} filters message requests from user-side and answers them using the network
 * buffer.</li>
 * </ol>
 * Any active configuration without network filter uses a default filter which simply accepts all cEMI L-Data.<br>
 * If no request filter is set, no buffer lookup is done on requests, instead the request is forwarded directly to the
 * KNX network.
 *
 * @author B. Malinowsky
 */
public final class NetworkBuffer implements AutoCloseable
{
	// this is for network link only, for now
	private static final class ConfigImpl implements Configuration
	{
		private final SquirrelLink lnk;
		private final NetworkLinkListener ll;
		private Cache cache;
		private Configuration.NetworkFilter nwFilter;
		private Configuration.RequestFilter reqFilter;
		private volatile boolean active;
		private volatile boolean queryBufferOnly;
		private DatapointModel<?> model;

		// listen on the link and update our buffers
		private final class SquirrelListener implements NetworkLinkListener
		{
			@Override
			public void confirmation(final FrameEvent e)
			{
				if (active && ((CEMILData) e.getFrame()).isPositiveConfirmation())
					updateBuffer(e.getFrame());
			}

			@Override
			public void indication(final FrameEvent e)
			{
				if (active)
					updateBuffer(e.getFrame());
			}

			@Override
			public void linkClosed(final CloseEvent e)
			{
				activate(false);
			}

			private void updateBuffer(final CEMI frame)
			{
				if (nwFilter != null)
					nwFilter.accept(frame, ConfigImpl.this);
			}
		}

		private final class SquirrelLink implements KNXNetworkLink
		{
			private final KNXNetworkLink base;
			private final EventListeners<NetworkLinkListener> listeners = new EventListeners<>();

			SquirrelLink(final KNXNetworkLink baseLink)
			{
				base = baseLink;
			}

			@Override
			public void addLinkListener(final NetworkLinkListener l)
			{
				base.addLinkListener(l);
				listeners.add(l);
			}

			@Override
			public void removeLinkListener(final NetworkLinkListener l)
			{
				base.removeLinkListener(l);
				listeners.remove(l);
			}

			@Override
			public int getHopCount()
			{
				return base.getHopCount();
			}

			@Override
			public void setHopCount(final int count)
			{
				base.setHopCount(count);
			}

			@Override
			public KNXMediumSettings getKNXMedium()
			{
				return base.getKNXMedium();
			}

			@Override
			public void sendRequest(final KNXAddress dst, final Priority p, final byte... nsdu)
					throws KNXLinkClosedException, KNXTimeoutException
			{
				if (!doBufferedResponse(dst, nsdu))
					base.sendRequest(dst, p, nsdu);
			}

			@Override
			public void sendRequestWait(final KNXAddress dst, final Priority p, final byte... nsdu)
					throws KNXTimeoutException, KNXLinkClosedException
			{
				if (!doBufferedResponse(dst, nsdu))
					base.sendRequestWait(dst, p, nsdu);
			}

			@Override
			public void send(final CEMILData msg, final boolean waitForCon)
				throws KNXTimeoutException, KNXLinkClosedException
			{
				if (!doBufferedResponse(msg.getDestination(), msg.getPayload()))
					base.send(msg, waitForCon);
			}

			@Override
			public String getName()
			{
				return "buffered " + base.getName();
			}

			@Override
			public boolean isOpen()
			{
				return base.isOpen();
			}

			@Override
			public void close()
			{
				activate(false);
				base.close();
				listeners.removeAll();
			}

			@Override
			public String toString()
			{
				return "network buffer " + base;
			}

			private boolean doBufferedResponse(final KNXAddress dst, final byte[] nsdu)
				throws KNXTimeoutException
			{
				final RequestFilter rf = reqFilter;
				// check valid access and A-group.read
				if (rf == null || !isOpen() || nsdu.length < 2 || DataUnitBuilder.getAPDUService(nsdu) != 0)
					return false;
				final CEMILData cemi = rf.request(dst, ConfigImpl.this);
				if (cemi != null) {
					fireIndication(cemi);
					return true;
				}
				if (queryBufferOnly)
					throw new KNXTimeoutException("query limited to network buffer");
				return false;
			}

			private void fireIndication(final CEMILData frame)
			{
				final FrameEvent e = new FrameEvent(this, frame);
				listeners.fire(l -> l.indication(e));
			}
		}

		ConfigImpl(final KNXNetworkLink link)
		{
			lnk = new SquirrelLink(link);
			ll = new SquirrelListener();
			link.addLinkListener(ll);
		}

		@Override
		public void activate(final boolean activate)
		{
			active = activate;
			if (active && getCache() == null)
				setCache(new LFUCache(0, 0));
			// supply a really simple "all you can buffer"-filter
			if (active && nwFilter == null) {
				nwFilter = new NetworkFilter()
				{
					@Override
					public void accept(final CEMI frame, final Configuration c)
					{
						final Cache localCache = c.getCache();
						if (localCache == null || !(frame instanceof final CEMILData f))
							return;
						// put into cache object
						final CacheObject co = localCache.get(f.getDestination());
						if (co != null) {
							((LDataObject) co).setFrame(f);
							c.getCache().put(co);
						}
						else
							localCache.put(new LDataObject(f));
					}

					@Override
					public void init(final Configuration c)
					{}
				};
			}
			// we might not have any filter if we get deactivated on first call
			if (nwFilter != null)
				nwFilter.init(this);
		}

		@Override
		public boolean isActive()
		{
			return active;
		}

		@Override
		public KNXNetworkLink getBaseLink()
		{
			return lnk.base;
		}

		@Override
		public KNXNetworkLink getBufferedLink()
		{
			return lnk;
		}

		@Override
		public void setQueryBufferOnly(final boolean bufferOnly)
		{
			queryBufferOnly = bufferOnly;
		}

		@Override
		public synchronized void setCache(final Cache c)
		{
			if (c == null && active)
				activate(false);
			cache = c;
		}

		@Override
		public synchronized Cache getCache()
		{
			return cache;
		}

		@Override
		public synchronized void setDatapointModel(final DatapointModel<?> m)
		{
			model = m;
			if (nwFilter != null)
				nwFilter.init(this);
		}

		@Override
		public synchronized DatapointModel<?> getDatapointModel()
		{
			return model;
		}

		@Override
		public void setFilter(final NetworkFilter nf, final RequestFilter rf)
		{
			nwFilter = nf;
			reqFilter = rf;
			if (nwFilter != null)
				nwFilter.init(this);
		}

		@Override
		public NetworkFilter getNetworkFilter()
		{
			return nwFilter;
		}

		@Override
		public RequestFilter getRequestFilter()
		{
			return reqFilter;
		}

		void unregister()
		{
			activate(false);
			getBaseLink().removeLinkListener(ll);
		}
	}

	private final List<ConfigImpl> configs = Collections.synchronizedList(new ArrayList<>());

	private NetworkBuffer() {}

	/**
	 * Creates a new network buffer for a KNX installation.
	 *
	 * @param installationId not used
	 * @return the new network buffer
	 */
	public static NetworkBuffer createBuffer(final String installationId)
	{
		return new NetworkBuffer();
	}

	/**
	 * Creates and adds a new configuration for the supplied network link to this network
	 * buffer.
	 * <p>
	 * By default, the added configuration is not activated, and no filters are set by the
	 * network buffer.<br>
	 * If the supplied {@code link} gets closed, the created configuration will get
	 * deactivated (see {@link Configuration#activate(boolean)}), and the buffered link of
	 * the configuration, obtained with {@link Configuration#getBufferedLink()}, will get
	 * closed as well.
	 *
	 * @param link KNX network link communicating with the KNX network
	 * @return the new configuration
	 */
	public Configuration addConfiguration(final KNXNetworkLink link)
	{
		final ConfigImpl c = new ConfigImpl(link);
		configs.add(c);
		return c;
	}

	/**
	 * Removes a configuration from this network buffer.
	 * <p>
	 * The configuration is deactivated and will not receive any further events or
	 * incoming messages from the base network link supplied at creation of that
	 * configuration.
	 *
	 * @param c the configuration to remove
	 */
	public void removeConfiguration(final Configuration c)
	{
		if (configs.remove(c)) {
			((ConfigImpl) c).unregister();
		}
	}

	/**
	 * Gets the configuration which provides the buffered link.
	 * <p>
	 * If the network link is not a buffered link or not found in the current
	 * configurations of this network buffer, {@code null} is returned.
	 *
	 * @param bufferedLink the buffered link to get the configuration for
	 * @return the owning configuration of that link or {@code null}
	 */
	public Configuration getConfiguration(final KNXNetworkLink bufferedLink)
	{
		synchronized (configs) {
			for (final ConfigImpl lc : configs) {
				if (lc.getBufferedLink() == bufferedLink)
					return lc;
			}
		}
		return null;
	}

	/**
	 * Returns all configurations of this network buffer.
	 *
	 * @return a new Configuration array holding the configurations, with the array
	 *         length equal to the number of network buffer configurations
	 */
	public Configuration[] getAllConfigurations()
	{
		return configs.toArray(new Configuration[0]);
	}

	/**
	 * Closes this network buffer, all configurations hold by this buffer are removed (see
	 * {@link #removeConfiguration(Configuration)}).
	 */
	@Override
	public void close()
	{
		synchronized (configs) {
			while (!configs.isEmpty())
				removeConfiguration(configs.getLast());
		}
	}
}
