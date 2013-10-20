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

package tuwien.auto.calimero.link;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.RawFrameFactory;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * Implementation of the KNX network monitor link based on the KNXnet/IP protocol, using a
 * {@link KNXnetIPConnection}.
 * <p>
 * Once a monitor has been closed, it is not available for further link communication,
 * i.e., it can't be reopened.
 * <p>
 * Pay attention to the IP address consideration stated in the documentation comments of
 * class {@link KNXNetworkLinkIP}.
 * 
 * @author B. Malinowsky
 */
public class KNXNetworkMonitorIP implements KNXNetworkMonitor
{
	private static final class MonitorNotifier extends EventNotifier
	{
		volatile boolean decode;

		MonitorNotifier(final Object source, final LogService logger)
		{
			super(source, logger);
		}

		public void frameReceived(final FrameEvent e)
		{
			final int mc = e.getFrame().getMessageCode();
			if (mc == CEMIBusMon.MC_BUSMON_IND) {
				logger.trace("received monitor indication");
				final KNXNetworkMonitorIP netmon = (KNXNetworkMonitorIP) source;
				MonitorFrameEvent mfe = new MonitorFrameEvent(netmon, e.getFrame());
				if (decode) {
					try {
						final int m = netmon.medium.getMedium();
						mfe = new MonitorFrameEvent(netmon, e.getFrame(), RawFrameFactory.create(m,
								e.getFrame().getPayload(), 0));
					}
					catch (final KNXFormatException ex) {
						logger.error("decoding raw frame", ex);
						mfe = new MonitorFrameEvent(netmon, e.getFrame(), ex);
					}
				}
				addEvent(new Indication(mfe));
			}
			else
				logger.warn("unspecified frame event - ignored, msg code = 0x"
						+ Integer.toHexString(mc));
		}

		public void connectionClosed(final CloseEvent e)
		{
			((KNXNetworkMonitorIP) source).closed = true;
			super.connectionClosed(e);
			logger.info("monitor closed");
			LogManager.getManager().removeLogService(logger.getName());
		}
	};

	private volatile boolean closed;
	private final KNXnetIPConnection conn;
	private KNXMediumSettings medium;

	private final String name;
	private final LogService logger;
	// our link connection event notifier
	private final MonitorNotifier notifier;
	
	/**
	 * Creates a new network monitor based on the KNXnet/IP protocol for accessing the KNX
	 * network.
	 * <p>
	 * 
	 * @param localEP the local endpoint to use for the link, this is the client control
	 *        endpoint, use <code>null</code> for the default local host and an
	 *        ephemeral port number
	 * @param remoteEP the remote endpoint of the link; this is the server control
	 *        endpoint
	 * @param useNAT <code>true</code> to use network address translation in the
	 *        KNXnet/IP protocol, <code>false</code> to use the default (non aware) mode
	 * @param settings medium settings defining the specific KNX medium needed for
	 *        decoding raw frames received from the KNX network
	 * @throws KNXException on failure establishing the link
	 * @throws InterruptedException on interrupted thread while establishing link
	 */
	public KNXNetworkMonitorIP(final InetSocketAddress localEP,
		final InetSocketAddress remoteEP, final boolean useNAT,
		final KNXMediumSettings settings) throws KNXException, InterruptedException
	{
		InetSocketAddress ep = localEP;
		if (ep == null)
			try {
				ep = new InetSocketAddress(InetAddress.getLocalHost(), 0);
			}
			catch (final UnknownHostException e) {
				throw new KNXException("no local host available");
			}
		conn = new KNXnetIPTunnel(KNXnetIPTunnel.BUSMONITOR_LAYER, ep, remoteEP, useNAT);
		
		// do our own IP:port string, since InetAddress.toString() always prepends a '/'
		final InetSocketAddress a = conn.getRemoteAddress();
		name = "monitor " + a.getAddress().getHostAddress() + ":" + a.getPort();

		logger = LogManager.getManager().getLogService(getName());
		logger.info("in busmonitor mode - ready to receive");
		notifier = new MonitorNotifier(this, logger);
		conn.addConnectionListener(notifier);
		// configure KNX medium stuff
		setKNXMedium(settings);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#setKNXMedium
	 * (tuwien.auto.calimero.link.medium.KNXMediumSettings)
	 */
	public void setKNXMedium(final KNXMediumSettings settings)
	{
		if (settings == null)
			throw new KNXIllegalArgumentException("medium settings are mandatory");
		if (medium != null && !settings.getClass().isAssignableFrom(medium.getClass())
			&& !medium.getClass().isAssignableFrom(settings.getClass()))
			throw new KNXIllegalArgumentException("medium differs");
		medium = settings;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#getKNXMedium()
	 */
	public KNXMediumSettings getKNXMedium()
	{
		return medium;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#addMonitorListener
	 * (tuwien.auto.calimero.link.event.LinkListener)
	 */
	public void addMonitorListener(final LinkListener l)
	{
		notifier.addListener(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#removeMonitorListener
	 * (tuwien.auto.calimero.link.event.LinkListener)
	 */
	public void removeMonitorListener(final LinkListener l)
	{
		notifier.removeListener(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#setDecodeRawFrames(boolean)
	 */
	public void setDecodeRawFrames(final boolean decode)
	{
		notifier.decode = decode;
		logger.info((decode ? "enable" : "disable") + " decoding of raw frames");
	}

	/**
	 * {@inheritDoc}<br>
	 * The returned name is "monitor " + remote IP address of the control endpoint + ":" +
	 * remote port used by the monitor.
	 */
	public String getName()
	{
		return name;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#isOpen()
	 */
	public boolean isOpen()
	{
		return !closed;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkMonitor#close()
	 */
	public void close()
	{
		synchronized (this) {
			if (closed)
				return;
			closed = true;
		}
		conn.close();
		notifier.quit();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getName() + (closed ? " (closed), " : ", ") + medium.getMediumString()
			+ " medium" + (notifier.decode ? ", decode raw frames" : "");
	}
}
