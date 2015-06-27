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

import java.util.EnumSet;
import java.util.List;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.DeviceDescriptor;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.cemi.CEMILDataEx;
import tuwien.auto.calimero.knxnetip.KNXConnectionClosedException;
import tuwien.auto.calimero.link.BcuSwitcher.BcuMode;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.serial.KNXPortClosedException;
import tuwien.auto.calimero.serial.usb.HidReport;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.KnxTunnelEmi;
import tuwien.auto.calimero.serial.usb.UsbConnection;
import tuwien.auto.calimero.serial.usb.UsbConnection.EmiType;

/**
 * Implementation of the KNX network network link over USB, using a {@link UsbConnection}. Once a
 * link has been closed, it is not available for further link communication, i.e. it can not be
 * reopened.
 *
 * @author B. Malinowsky
 */
public class KNXNetworkLinkUsb implements KNXNetworkLink
{
	private static final class LinkNotifier extends EventNotifier
	{
		LinkNotifier(final Object source, final Logger logger)
		{
			super(source, logger);
		}

		@Override
		public void frameReceived(final FrameEvent e)
		{
			try {
				final CEMI frame = e.getFrame();
				if (frame instanceof CEMIDevMgmt) {
					// XXX check .con correctly (required for setting cEMI link layer mode)
					final int mc = frame.getMessageCode();
					if (mc == CEMIDevMgmt.MC_PROPWRITE_CON) {
						final CEMIDevMgmt f = (CEMIDevMgmt) frame;
						if (f.isNegativeResponse())
							logger.error("L-DM negative response, " + f.getErrorMessage());
					}
				}
				if (frame != null && !(frame instanceof CEMILData))
					return;
				final CEMILData f = frame != null ? (CEMILData) frame : (CEMILData) CEMIFactory
						.createFromEMI(e.getFrameBytes());
				final int mc = f.getMessageCode();
				if (mc == CEMILData.MC_LDATA_IND) {
					addEvent(new Indication(new FrameEvent(source, f)));
					logger.debug("indication from " + f.getSource());
				}
				else if (mc == CEMILData.MC_LDATA_CON) {
					addEvent(new Confirmation(new FrameEvent(source, f)));
					logger.debug("confirmation of " + f.getDestination());
				}
			}
			catch (final KNXFormatException ex) {
				logger.warn("unspecified frame event - ignored", ex);
			}
		}

		@Override
		public void connectionClosed(final CloseEvent e)
		{
			((KNXNetworkLinkUsb) source).closed = true;
			super.connectionClosed(e);
			logger.info("link closed");
			LogService.removeLogger(logger);
		}
	};

	// EMI1/2 switch command
	private static final int PEI_SWITCH = 0xA9;

	private volatile boolean closed;
	private final UsbConnection conn;
	private final EnumSet<EmiType> emiTypes;
	private EmiType activeEmi;
	private volatile int hopCount = 6;
	private KNXMediumSettings medium;

	private final String name;
	private final Logger logger;
	private final EventNotifier notifier;

	/**
	 * Creates a new network link for accessing the KNX network over a USB connection.
	 * <p>
	 * The port identifier is used to choose the serial port for communication. These identifiers
	 * are usually device and platform specific.
	 *
	 * @param vendorId the USB vendor ID of the KNX USB device interface
	 * @param productId the USB product ID of the KNX USB device interface
	 * @param settings medium settings defining device and medium specifics needed for communication
	 * @throws KNXException on error creating USB link
	 * @throws InterruptedException on interrupt
	 */
	public KNXNetworkLinkUsb(final int vendorId, final int productId,
		final KNXMediumSettings settings) throws KNXException, InterruptedException
	{
		this(new UsbConnection(vendorId, productId), settings);
	}

	/**
	 * Creates a new network link for accessing the KNX network over a USB connection.
	 * <p>
	 *
	 * @param settings medium settings defining device and medium specifics needed for communication
	 * @throws KNXException on error creating USB link
	 * @throws InterruptedException on interrupt
	 */
	public KNXNetworkLinkUsb(final String device, final KNXMediumSettings settings)
		throws KNXException, InterruptedException
	{
		this(new UsbConnection(device), settings);
	}

	private KNXNetworkLinkUsb(final UsbConnection c, final KNXMediumSettings settings)
		throws KNXException, InterruptedException
	{
		conn = c;
		try {
			if (!conn.isKnxConnectionActive())
				throw new KNXConnectionClosedException("interface is not connected to KNX network");

			emiTypes = conn.getSupportedEmiTypes();
			// Responding to active EMI type is optional (and might fail) if only one EMI type is
			// supported. Or, we get a positive response, but with no emi type set (void)
//			EmiType active = conn.getActiveEmiType();
//			if (emiTypes.size() == 1)
//				activeEmi = emiTypes.iterator().next();
//			else
			if (!trySetActiveEmi(EmiType.CEmi) && !trySetActiveEmi(EmiType.Emi2)
					&& !trySetActiveEmi(EmiType.Emi1)) {
				throw new KNXConnectionClosedException(
						"failed to set active any supported EMI type");
			}
		}
		catch (final KNXException e) {
			conn.close();
			throw e;
		}
		name = conn.getName();
		logger = LogService.getLogger("calimero.link." + getName());

		try {
			final int dd0 = conn.getDeviceDescriptorType0();
			logger.info("Device Mask Version {}", DeviceDescriptor.DD0.fromType0(dd0));
		} catch (final KNXTimeoutException e) {
			// device does not provide a device descriptor 0
		}
		linkLayerMode();
		notifier = new LinkNotifier(this, logger);
		conn.addConnectionListener(notifier);
		// configure KNX medium stuff
		setKNXMedium(settings);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#setKNXMedium
	 * (tuwien.auto.calimero.link.medium.KNXMediumSettings)
	 */
	@Override
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
	@Override
	public KNXMediumSettings getKNXMedium()
	{
		return medium;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#addLinkListener
	 * (tuwien.auto.calimero.link.event.NetworkLinkListener)
	 */
	@Override
	public void addLinkListener(final NetworkLinkListener l)
	{
		notifier.addListener(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#removeLinkListener
	 * (tuwien.auto.calimero.link.event.NetworkLinkListener)
	 */
	@Override
	public void removeLinkListener(final NetworkLinkListener l)
	{
		notifier.removeListener(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#setHopCount(int)
	 */
	@Override
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
	@Override
	public int getHopCount()
	{
		return hopCount;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#sendRequest
	 * (tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void sendRequest(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		doSend(createEMI(CEMILData.MC_LDATA_REQ, dst, p, nsdu), false, dst);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#sendRequestWait
	 * (tuwien.auto.calimero.KNXAddress, tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void sendRequestWait(final KNXAddress dst, final Priority p, final byte[] nsdu)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		doSend(createEMI(CEMILData.MC_LDATA_REQ, dst, p, nsdu), true, dst);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#send
	 * (tuwien.auto.calimero.cemi.CEMILData, boolean)
	 */
	@Override
	public void send(final CEMILData msg, final boolean waitForCon) throws KNXTimeoutException,
		KNXLinkClosedException
	{
		doSend(createEMI(msg), waitForCon, msg.getDestination());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#isOpen()
	 */
	@Override
	public boolean isOpen()
	{
		return !closed;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#close()
	 */
	@Override
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
	@Override
	public String toString()
	{
		return getName() + (closed ? "(closed), " : ", ") + medium.getMediumString() + " hopcount "
				+ hopCount;
	}

	private boolean trySetActiveEmi(final EmiType active) throws KNXPortClosedException,
		KNXTimeoutException, InterruptedException
	{
		if (emiTypes.contains(active)) {
			conn.setActiveEmiType(active);
			activeEmi = conn.getActiveEmiType();
			return activeEmi == active;
		}
		return false;
	}

	// TODO should close and cleanup on error switching to link layer mode
	private void linkLayerMode() throws KNXException, InterruptedException
	{
		if (activeEmi == EmiType.CEmi) {
			final int CEMI_SERVER_OBJECT = 8;
			final int PID_COMM_MODE = 52;
			final int objectInstance = 1;
			final CEMI frame = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, CEMI_SERVER_OBJECT,
					objectInstance, PID_COMM_MODE, 1, 1, new byte[] { 0 });
			conn.send(HidReport.create(KnxTunnelEmi.CEmi, frame.toByteArray()).get(0), true);
			// check for .con
			//findFrame(CEMIDevMgmt.MC_PROPWRITE_CON);
		}
		else if (activeEmi == EmiType.Emi1) {
			new BcuSwitcher(conn, logger).enter(BcuMode.LinkLayer);
			return;
		}
		else {
			final byte[] switchLinkLayer = { (byte) PEI_SWITCH, 0x00, 0x18, 0x34, 0x56, 0x78, 0x0A, };
			conn.send(HidReport.create(activeEmi.emi, switchLinkLayer).get(0), true);
		}
	}

	private void normalMode() throws KNXPortClosedException, KNXTimeoutException, InterruptedException
	{
		if (activeEmi == EmiType.CEmi) {
			final CEMI frame = new CEMIDevMgmt(CEMIDevMgmt.MC_RESET_REQ);
			conn.send(HidReport.create(KnxTunnelEmi.CEmi, frame.toByteArray()).get(0), true);
		}
		else if (activeEmi == EmiType.Emi1) {
			new BcuSwitcher(conn, logger).reset();
		}
		else {
			final byte[] switchNormal = { (byte) PEI_SWITCH, 0x1E, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, };
			conn.send(HidReport.create(activeEmi.emi, switchNormal).get(0), true);
		}
	}

	private byte[] createEMI(final CEMILData f)
	{
		if (emiTypes.contains(EmiType.CEmi))
			return f.toByteArray();
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
		if (emiTypes.contains(EmiType.CEmi))
			return cEMI(mc, dst, p, nsdu).toByteArray();

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

	private CEMI cEMI(final int mc, final KNXAddress dst, final Priority p, final byte[] nsdu)
	{
		final IndividualAddress src = medium.getDeviceAddress();
		// use default address 0 in system broadcast
		final KNXAddress d = dst == null ? new GroupAddress(0) : dst;
		final boolean tp = medium.getMedium() == KNXMediumSettings.MEDIUM_TP1;
		if (nsdu.length <= 16 && tp)
			return new CEMILData(mc, src, d, nsdu, p, true, hopCount);
//		CEMILDataEx f = new CEMILDataEx(mc, src, d, nsdu, p, true, dst != null, false, hopCount);
		// TODO allow domain bcast, currently we send everything as system broadcast
		final CEMILDataEx f = new CEMILDataEx(mc, src, d, nsdu, p, true, false, false, hopCount);
		if (medium.getMedium() == KNXMediumSettings.MEDIUM_RF) {
			final RFSettings rf = (RFSettings) medium;
			if (f.getAdditionalInfo(CEMILDataEx.ADDINFO_RFMEDIUM) == null) {
				final byte[] sn = f.isDomainBroadcast() ? rf.getDomainAddress() : rf
						.getSerialNumber();
				// add-info: rf-info (0 ignores a lot), sn (6 bytes), lfn (=255:void)
				f.addAdditionalInfo(CEMILDataEx.ADDINFO_RFMEDIUM, new byte[] { 0, sn[0], sn[1],
					sn[2], sn[3], sn[4], sn[5], (byte) 0xff });
				logger.trace("send - added RF additional info to message "
						+ (f.isDomainBroadcast() ? "(domain address)" : "(S/N)"));
			}
		}
		return f;
	}

	// dst is just for log information
	private void doSend(final byte[] msg, final boolean blocking, final KNXAddress dst)
		throws KNXLinkClosedException, KNXTimeoutException
	{
		if (closed)
			throw new KNXLinkClosedException("link closed");
		try {
			final boolean trace = logger.isTraceEnabled();
			if (trace || logger.isInfoEnabled())
				logger.info("send message to " + dst + (blocking ? ", blocking" : ""));
			if (trace)
				logger.trace("EMI " + DataUnitBuilder.toHex(msg, " "));
			final List<HidReport> reports = HidReport.create(activeEmi.emi, msg);
			for (final HidReport r : reports)
				conn.send(r, blocking);
			if (trace)
				logger.trace("send to " + dst + " succeeded");
		}
		catch (final KNXPortClosedException e) {
			logger.error("send error, closing link", e);
			close();
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
	}
}
