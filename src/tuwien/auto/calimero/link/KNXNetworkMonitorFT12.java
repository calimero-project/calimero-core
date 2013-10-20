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

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.cemi.CEMIBusMon;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.exception.KNXAckTimeoutException;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.RawFrameFactory;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.serial.FT12Connection;
import tuwien.auto.calimero.serial.KNXPortClosedException;

/**
 * Implementation of the KNX network monitor link based on the FT1.2 protocol, using a
 * {@link FT12Connection}.
 * <p>
 * Once a monitor has been closed, it is not available for further link communication,
 * i.e., it can't be reopened.
 * 
 * @author B. Malinowsky
 */
public class KNXNetworkMonitorFT12 implements KNXNetworkMonitor
{
	private static final class MonitorNotifier extends EventNotifier
	{
		volatile boolean decode;

		MonitorNotifier(final Object source, final LogService logger)
		{
			super(source, logger);
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.link.EventNotifier#frameReceived
		 * (tuwien.auto.calimero.FrameEvent)
		 */
		public void frameReceived(final FrameEvent e)
		{
			try {
				final CEMIBusMon mon = (CEMIBusMon) CEMIFactory.createFromEMI(e.getFrameBytes());
				logger.trace("received monitor indication");
				final KNXNetworkMonitorFT12 netmon = (KNXNetworkMonitorFT12) source;
				MonitorFrameEvent mfe = new MonitorFrameEvent(netmon, mon);
				if (decode) {
					try {
						mfe = new MonitorFrameEvent(netmon, mon, RawFrameFactory.create(
								netmon.medium.getMedium(), mon.getPayload(), 0));
					}
					catch (final KNXFormatException ex) {
						logger.error("decoding raw frame", ex);
						mfe = new MonitorFrameEvent(netmon, mon, ex);
					}
				}
				addEvent(new Indication(mfe));
			}
			catch (final KNXFormatException ex) {
				logger.warn("unspecified frame event - ignored", ex);
			}
		}

		public void connectionClosed(final CloseEvent e)
		{
			((KNXNetworkMonitorFT12) source).closed = true;
			super.connectionClosed(e);
			logger.info("monitor closed");
			LogManager.getManager().removeLogService(logger.getName());
		}
	}

	private static final int PEI_SWITCH = 0xA9;

	private volatile boolean closed;
	private final FT12Connection conn;
	private KNXMediumSettings medium;

	private final String name;
	private final LogService logger;
	// our link connection event notifier
	private final MonitorNotifier notifier;

	/**
	 * Creates a new network monitor based on the FT1.2 protocol for accessing the KNX
	 * network.
	 * <p>
	 * The port identifier is used to choose the serial port for communication. These
	 * identifiers are usually device and platform specific.
	 * 
	 * @param portID identifier of the serial communication port to use
	 * @param settings medium settings defining the specific KNX medium needed for
	 *        decoding raw frames received from the KNX network
	 * @throws KNXException
	 */
	public KNXNetworkMonitorFT12(final String portID, final KNXMediumSettings settings)
		throws KNXException
	{
		conn = new FT12Connection(portID);
		enterBusmonitor();
		name = "monitor " + conn.getPortID();
		logger = LogManager.getManager().getLogService(getName());
		logger.info("in busmonitor mode - ready to receive");
		notifier = new MonitorNotifier(this, logger);
		conn.addConnectionListener(notifier);
		// configure KNX medium stuff
		setKNXMedium(settings);
	}

	/**
	 * Creates a new network monitor based on the FT1.2 protocol for accessing the KNX
	 * network.
	 * <p>
	 * The port number is used to choose the serial port for communication. It is mapped
	 * to the default port identifier using that number on the platform.
	 * 
	 * @param portNumber port number of the serial communication port to use
	 * @param settings medium settings defining the specific KNX medium needed for
	 *        decoding raw frames received from the KNX network
	 * @throws KNXException
	 */
	public KNXNetworkMonitorFT12(final int portNumber, final KNXMediumSettings settings)
		throws KNXException
	{
		conn = new FT12Connection(portNumber);
		enterBusmonitor();
		name = "monitor " + conn.getPortID();
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
	 * The returned name is "monitor " + port identifier.
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
		try {
			leaveBusmonitor();
		}
		catch (final Exception e) {
			logger.error("could not switch BCU back to normal mode", e);
		}
		conn.close();
		notifier.quit();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return getName() + (closed ? "(closed), " : ", ") + medium.getMediumString()
			+ " medium" + (notifier.decode ? ", decode raw frames" : "");
	}

	private void enterBusmonitor() throws KNXAckTimeoutException, KNXPortClosedException,
		KNXLinkClosedException
	{
		try {
			final byte[] switchBusmon = { (byte) PEI_SWITCH, (byte) 0x90, 0x18, 0x34, 0x56, 0x78,
				0x0A, };
			conn.send(switchBusmon, true);
		}
		catch (final InterruptedException e) {
			conn.close();
			throw new KNXLinkClosedException(e.getMessage());
		}
		catch (final KNXAckTimeoutException e) {
			conn.close();
			throw e;
		}
	}

	private void leaveBusmonitor() throws KNXAckTimeoutException, KNXPortClosedException,
		InterruptedException
	{
		normalMode();
	}

	private void normalMode() throws KNXAckTimeoutException, KNXPortClosedException,
		InterruptedException
	{
		final byte[] switchNormal = { (byte) PEI_SWITCH, 0x1E, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, };
		conn.send(switchNormal, true);
	}
}
