/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.DeviceDescriptor;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.KNXConnectionClosedException;
import tuwien.auto.calimero.link.BcuSwitcher.BcuMode;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.serial.KNXPortClosedException;
import tuwien.auto.calimero.serial.usb.HidReport;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.KnxTunnelEmi;
import tuwien.auto.calimero.serial.usb.UsbConnection;
import tuwien.auto.calimero.serial.usb.UsbConnection.EmiType;

/**
 * Implementation of the KNX network network link over USB, using a {@link UsbConnection}. Once a link has been closed,
 * it is not available for further link communication, i.e., it cannot be reopened.
 *
 * @author B. Malinowsky
 */
public class KNXNetworkLinkUsb extends AbstractLink
{
	// EMI1/2 switch command
	private static final int PEI_SWITCH = 0xA9;

	private final UsbConnection conn;
	private final EnumSet<EmiType> emiTypes;
	private EmiType activeEmi;

	/**
	 * Creates a new network link for accessing the KNX network over a USB connection, using a USB
	 * vendor and product ID for USB interface identification.
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
	 * Creates a new network link for accessing the KNX network over a USB connection. Because
	 * arguments for parameter <code>device</code> are not necessarily unique identifiers, the first
	 * matching USB interface is selected.
	 *
	 * @param device an identifier to lookup the USB device, e.g., based on (part of) a device
	 *        string like the product or manufacturer name, or USB vendor and product ID in the
	 *        format <code>vendorId:productId</code>
	 * @param settings KNX medium settings, with device and medium-specific communication settings
	 * @throws KNXException on error creating USB link
	 * @throws InterruptedException on interrupt
	 */
	public KNXNetworkLinkUsb(final String device, final KNXMediumSettings settings)
		throws KNXException, InterruptedException
	{
		this(new UsbConnection(device), settings);
	}

	/**
	 * Creates a new network link for accessing the KNX network over the supplied USB connection.
	 *
	 * @param c USB connection in open state, connected to a KNX network; the link takes ownership
	 * @param settings KNX medium settings, with device and medium-specific communication settings
	 * @throws KNXException on error creating USB link
	 * @throws InterruptedException on interrupt
	 */
	protected KNXNetworkLinkUsb(final UsbConnection c, final KNXMediumSettings settings)
		throws KNXException, InterruptedException
	{
		super(c, c.getName(), settings);
		conn = c;
		try {
			if (!conn.isKnxConnectionActive())
				throw new KNXConnectionClosedException("USB interface is not connected to KNX network");

			emiTypes = conn.getSupportedEmiTypes();
			// Responding to active EMI type is optional (and might fail) if only one EMI type is
			// supported. Or, we get a positive response, but with no emi type set (void)
//			EmiType active = conn.getActiveEmiType();
//			if (emiTypes.size() == 1)
//				activeEmi = emiTypes.iterator().next();
//			else
			if (!trySetActiveEmi(EmiType.CEmi) && !trySetActiveEmi(EmiType.Emi2) && !trySetActiveEmi(EmiType.Emi1)) {
				throw new KNXConnectionClosedException("failed to set active any supported EMI type");
			}
			try {
				// report device descriptor before switching to link layer mode
				// not all devices provide a device descriptor 0
				final int dd0 = conn.getDeviceDescriptorType0();
				logger.info("Device Descriptor (Mask Version) {}", DeviceDescriptor.DD0.fromType0(dd0));
			}
			catch (final KNXTimeoutException expected) {}

			linkLayerMode();
		}
		catch (final KNXException e) {
			notifier.quit();
			conn.close();
			throw e;
		}
		conn.addConnectionListener(notifier);
		cEMI = emiTypes.contains(EmiType.CEmi);
		sendCEmiAsByteArray = true;
	}

	@Override
	protected void onSend(final KNXAddress dst, final byte[] msg, final boolean waitForCon)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		try {
			final boolean trace = logger.isTraceEnabled();
			logger.debug("send message to {}, {}blocking", dst, (waitForCon ? "" : "non-"));
			if (trace)
				logger.trace("EMI {}", DataUnitBuilder.toHex(msg, " "));
			final List<HidReport> reports = HidReport.create(activeEmi.emi, msg);
			for (final HidReport r : reports)
				conn.send(r, waitForCon);
			logger.trace("send to {} succeeded", dst);
		}
		catch (final KNXPortClosedException e) {
			logger.error("send error, closing link", e);
			close();
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
	}

	@Override
	protected void onSend(final CEMILData msg, final boolean waitForCon)
	{}

	@Override
	protected void onClose()
	{
		try {
			normalMode();
		}
		catch (final Exception e) {
			logger.error("could not switch BCU back to normal mode", e);
		}
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
		}
		else {
			final byte[] switchLinkLayer = { (byte) PEI_SWITCH, 0x00, 0x18, 0x34, 0x56, 0x78, 0x0A, };
			conn.send(HidReport.create(activeEmi.emi, switchLinkLayer).get(0), true);
		}
	}

	private void normalMode() throws KNXPortClosedException, KNXTimeoutException,
		InterruptedException
	{
		if (activeEmi == EmiType.CEmi) {
			final CEMI frame = new CEMIDevMgmt(CEMIDevMgmt.MC_RESET_REQ);
			conn.send(HidReport.create(KnxTunnelEmi.CEmi, frame.toByteArray()).get(0), true);
		}
		else if (activeEmi == EmiType.Emi1) {
			new BcuSwitcher(conn, logger).reset();
		}
		else {
			final byte[] switchNormal = { (byte) PEI_SWITCH, 0x1E, 0x12, 0x34, 0x56, 0x78,
				(byte) 0x9A, };
			conn.send(HidReport.create(activeEmi.emi, switchNormal).get(0), true);
		}
	}
}
