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
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.exception.KNXAckTimeoutException;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.serial.FT12Connection;
import tuwien.auto.calimero.serial.KNXPortClosedException;

/**
 * Implementation of the KNX network network link based on the FT1.2 protocol, using a
 * {@link FT12Connection}.
 * <p>
 * Once a link has been closed, it is not available for further link communication, i.e.
 * it can not be reopened.
 * 
 * @author B. Malinowsky
 */
public class KNXNetworkLinkFT12 implements KNXNetworkLink
{
	private static final class LinkNotifier extends EventNotifier
	{
		LinkNotifier(final Object source, final LogService logger)
		{
			super(source, logger);
		}

		public void frameReceived(final FrameEvent e)
		{
			try {
				final CEMILData f =
					(CEMILData) CEMIFactory.createFromEMI(e.getFrameBytes());
				final int mc = f.getMessageCode();
				if (mc == CEMILData.MC_LDATA_IND) {
					addEvent(new Indication(new FrameEvent(source, f)));
					logger.info("indication from " + f.getSource());
				}
				else if (mc == CEMILData.MC_LDATA_CON) {
					addEvent(new Confirmation(new FrameEvent(source, f)));
					logger.info("confirmation of " + f.getDestination());
				}
			}
			catch (final KNXFormatException ex) {
				logger.warn("unspecified frame event - ignored", ex);
			}
		}

		public void connectionClosed(final CloseEvent e)
		{
			((KNXNetworkLinkFT12) source).closed = true;
			super.connectionClosed(e);
			logger.info("link closed");
			LogManager.getManager().removeLogService(logger.getName());
		}
	};

	private static final int PEI_SWITCH = 0xA9;

	private volatile boolean closed;
	private final FT12Connection conn;
	private volatile int hopCount = 6;
	private KNXMediumSettings medium;

	private final String name;
	private final LogService logger;
	// our link connection event notifier
	private final EventNotifier notifier;

	/**
	 * Creates a new network link based on the FT1.2 protocol for accessing the KNX
	 * network.
	 * <p>
	 * The port identifier is used to choose the serial port for communication. These
	 * identifiers are usually device and platform specific.
	 * 
	 * @param portID identifier of the serial communication port to use
	 * @param settings medium settings defining device and medium specifics needed for
	 *        communication
	 * @throws KNXException
	 */
	public KNXNetworkLinkFT12(final String portID, final KNXMediumSettings settings)
		throws KNXException
	{
		conn = new FT12Connection(portID);
		linkLayerMode();
		name = "link " + conn.getPortID();
		logger = LogManager.getManager().getLogService(getName());
		notifier = new LinkNotifier(this, logger);
		conn.addConnectionListener(notifier);
		// configure KNX medium stuff
		setKNXMedium(settings);
	}

	/**
	 * Creates a new network link based on the FT1.2 protocol for accessing the KNX
	 * network.
	 * <p>
	 * The port number is used to choose the serial port for communication. It is mapped
	 * to the default port identifier using that number on the platform.
	 * 
	 * @param portNumber port number of the serial communication port to use
	 * @param settings medium settings defining device and medium specifics needed for
	 *        communication
	 * @throws KNXException
	 */
	public KNXNetworkLinkFT12(final int portNumber, final KNXMediumSettings settings)
		throws KNXException
	{
		conn = new FT12Connection(portNumber);
		linkLayerMode();
		name = "link " + conn.getPortID();
		logger = LogManager.getManager().getLogService(getName());
		notifier = new LinkNotifier(this, logger);
		conn.addConnectionListener(notifier);
		// configure KNX medium stuff
		setKNXMedium(settings);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#setKNXMedium
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
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getKNXMedium()
	 */
	public KNXMediumSettings getKNXMedium()
	{
		return medium;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#addLinkListener
	 * (tuwien.auto.calimero.link.event.NetworkLinkListener)
	 */
	public void addLinkListener(final NetworkLinkListener l)
	{
		notifier.addListener(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#removeLinkListener
	 * (tuwien.auto.calimero.link.event.NetworkLinkListener)
	 */
	public void removeLinkListener(final NetworkLinkListener l)
	{
		notifier.removeListener(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#setHopCount(int)
	 */
	public void setHopCount(final int count)
	{
		if (count < 0 || count > 7)
			throw new KNXIllegalArgumentException("hop count out of range [0..7]");
		hopCount = count;
		logger.info("hop count set to " + count);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getHopCount()
	 */
	public int getHopCount()
	{
		return hopCount;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#sendRequest
	 * (tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])
	 */
	public void sendRequest(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		doSend(createEMI(CEMILData.MC_LDATA_REQ, dst, p, nsdu), false, dst);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#sendRequestWait
	 * (tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])
	 */
	public void sendRequestWait(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		doSend(createEMI(CEMILData.MC_LDATA_REQ, dst, p, nsdu), true, dst);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#send
	 * (tuwien.auto.calimero.cemi.CEMILData, boolean)
	 */
	public void send(final CEMILData msg, final boolean waitForCon) throws KNXTimeoutException,
		KNXLinkClosedException
	{
		doSend(createEMI(msg), waitForCon, msg.getDestination());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getName()
	 */
	public String getName()
	{
		return name;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#isOpen()
	 */
	public boolean isOpen()
	{
		return !closed;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#close()
	 */
	public void close()
	{
		synchronized (this) {
			if (closed)
				return;
			closed = true;
		}
		try {
			normalMode();
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
			+ " hopcount " + hopCount;
	}

	private void linkLayerMode() throws KNXException
	{
		// link layer mode
		final byte[] switchLinkLayer = { (byte) PEI_SWITCH, 0x00, 0x18, 0x34, 0x56, 0x78,
			0x0A, };
		try {
			conn.send(switchLinkLayer, true);
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

	private void normalMode() throws KNXAckTimeoutException, KNXPortClosedException,
		InterruptedException
	{
		final byte[] switchNormal = { (byte) PEI_SWITCH, 0x1E, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, };
		conn.send(switchNormal, true);
	}

	private byte[] createEMI(final CEMILData f)
	{
		final byte[] buf = createEMI(f.getMessageCode(), f.getDestination(), f.getPriority(),
				f.getPayload());
		final int ctrl = (f.isRepetition() ? 0x20 : 0) | (f.isAckRequested() ? 0x02 : 0)
				| (f.isPositiveConfirmation() ? 0 : 0x01);
		buf[1] |= (byte) ctrl;
		if (f.getHopCount() != hopCount)
			buf[6] = (byte) (buf[6] & ~0x70 | f.getHopCount() << 4);
		return buf;
	}

	private byte[] createEMI(final int mc, final KNXAddress dst, final Priority p, final byte[] nsdu)
	{
		if (nsdu.length > 16)
			throw new KNXIllegalArgumentException("maximum TPDU length is 16 in standard frame");
		// TP1, standard frames only
		final byte[] buf = new byte[nsdu.length + 7];
		buf[0] = (byte) mc;
		// ack don't care
		buf[1] = (byte) (p.value << 2);
		// on dst null, default address 0 is used
		// (null indicates system broadcast in link API)
		final int d = dst != null ? dst.getRawAddress() : 0;
		buf[4] = (byte) (d >> 8);
		buf[5] = (byte) d;
		buf[6] = (byte) (hopCount << 4 | (nsdu.length - 1));
		if (dst instanceof GroupAddress)
			buf[6] |= 0x80;
		for (int i = 0; i < nsdu.length; ++i)
			buf[7 + i] = nsdu[i];
		return buf;
	}

	// dst is just for log information
	private void doSend(final byte[] msg, final boolean wait, final KNXAddress dst)
		throws KNXAckTimeoutException, KNXLinkClosedException
	{
		if (closed)
			throw new KNXLinkClosedException("link closed");
		try {
			final boolean trace = logger.isLoggable(LogLevel.TRACE);
			if (trace || logger.isLoggable(LogLevel.INFO))
				logger.info("send message to " + dst + (wait ? ", wait for ack" : ""));
			if (trace)
				logger.trace("EMI " + DataUnitBuilder.toHex(msg, " "));
			conn.send(msg, wait);
			if (trace)
				logger.trace("send to " + dst + " succeeded");
		}
		catch (final KNXPortClosedException e) {
			logger.error("send error, closing link", e);
			close();
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
		catch (final InterruptedException e) {
			logger.error("interrupted, closing link", e);
			close();
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
	}
}
