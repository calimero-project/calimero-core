/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2023 B. Malinowsky

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

import static io.calimero.serial.usb.UsbConnection.EmiType.Cemi;
import static io.calimero.serial.usb.UsbConnection.EmiType.Emi1;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.TRACE;

import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.calimero.Connection.BlockingMode;
import io.calimero.DeviceDescriptor.DD0;
import io.calimero.FrameEvent;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXException;
import io.calimero.KNXListener;
import io.calimero.KNXTimeoutException;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIDevMgmt;
import io.calimero.cemi.CEMILData;
import io.calimero.link.BcuSwitcher.BcuMode;
import io.calimero.link.medium.KNXMediumSettings;
import io.calimero.serial.ConnectionEvent;
import io.calimero.serial.ConnectionStatus;
import io.calimero.serial.KNXPortClosedException;
import io.calimero.serial.usb.UsbConnection;
import io.calimero.serial.usb.UsbConnectionFactory;

/**
 * Implementation of the KNX network network link over USB, using a {@link UsbConnection}. Once a link has been closed,
 * it is not available for further link communication, i.e., it cannot be reopened.
 *
 * @author B. Malinowsky
 */
public class KNXNetworkLinkUsb extends AbstractLink<UsbConnection>
{
	// EMI1/2 switch command
	private static final int PEI_SWITCH = 0xA9;

	private final EnumSet<UsbConnection.EmiType> emiTypes;
	private UsbConnection.EmiType activeEmi;

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
		this(UsbConnectionFactory.open(vendorId, productId), settings);
	}

	/**
	 * Creates a new network link for accessing the KNX network over a USB connection. Because
	 * arguments for parameter {@code device} are not necessarily unique identifiers, the first
	 * matching USB interface is selected.
	 *
	 * @param device an identifier to lookup the USB device, e.g., based on (part of) a device
	 *        string like the product or manufacturer name, or USB vendor and product ID in the
	 *        format {@code vendorId:productId}
	 * @param settings KNX medium settings, with device and medium-specific communication settings
	 * @throws KNXException on error creating USB link
	 * @throws InterruptedException on interrupt
	 */
	public KNXNetworkLinkUsb(final String device, final KNXMediumSettings settings)
		throws KNXException, InterruptedException
	{
		this(UsbConnectionFactory.open(device), settings);
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
		super(c, c.name(), settings);
		try {
			if (!conn.isKnxConnectionActive())
				throw new KNXLinkClosedException("USB interface is not connected to KNX network");

			emiTypes = conn.supportedEmiTypes();
			if (!trySetActiveEmi(Cemi) && !trySetActiveEmi(UsbConnection.EmiType.Emi2) && !trySetActiveEmi(Emi1)) {
				throw new KNXLinkClosedException("failed to set active any supported EMI type");
			}
			try {
				// report device descriptor before switching to link layer mode
				// not all devices provide a device descriptor 0
				final DD0 dd0 = conn.deviceDescriptor();
				logger.log(DEBUG, "KNX device descriptor 0 (Mask Version): {0}", dd0);
			}
			catch (final KNXTimeoutException expected) {}

			conn.addConnectionListener(notifier);
			conn.addConnectionListener(new KNXListener() {
				@Override
				public void frameReceived(final FrameEvent e) {}

				@ConnectionEvent
				void connectionStatus(final ConnectionStatus status) { notifyConnectionStatus(status); }
			});
			notifier.registerEventType(ConnectionStatus.class);

			linkLayerMode();
		}
		catch (final KNXException e) {
			notifier.quit();
			conn.close();
			throw e;
		}
		cEMI = emiTypes.contains(Cemi);
		sendCEmiAsByteArray = true;

		supportedCommModes();
		deviceAddr();
		mediumType();
		setMaxApduLength();
		disableFilters();
	}

	@Override
	protected void onSend(final KNXAddress dst, final byte[] msg, final boolean waitForCon)
		throws KNXTimeoutException, KNXLinkClosedException
	{
		try {
			logger.log(TRACE, () -> "EMI " + HexFormat.ofDelimiter(" ").formatHex(msg));
			conn.send(msg, waitForCon ? BlockingMode.Confirmation : BlockingMode.NonBlocking);
			logger.log(TRACE, "send to {0} succeeded", dst);
		}
		catch (final KNXPortClosedException e) {
			logger.log(ERROR, "send error, closing link", e);
			close();
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
	}

	@Override
	protected void onSend(final CEMILData msg, final boolean waitForCon) {}

	@Override
	protected void onClose()
	{
		try {
			normalMode();
		}
		catch (final Exception e) {
			logger.log(ERROR, "could not switch BCU back to normal mode", e);
		}
	}

	@Override
	void onSend(final CEMIDevMgmt frame) throws KNXPortClosedException, KNXTimeoutException {
		if (activeEmi != Cemi)
			throw new IllegalStateException("cEMI mode not active in KNX USB interface");
		conn.send(frame.toByteArray(), BlockingMode.Confirmation);
	}

	private boolean trySetActiveEmi(final UsbConnection.EmiType active) throws KNXPortClosedException,
		KNXTimeoutException, InterruptedException
	{
		if (emiTypes.contains(active)) {
			conn.setActiveEmiType(active);
			activeEmi = conn.activeEmiType();
			return activeEmi == active;
		}
		return false;
	}

	private void linkLayerMode() throws KNXException, InterruptedException
	{
		if (activeEmi == Cemi) {
			final var frame = BcuSwitcher.commModeRequest(BcuSwitcher.DataLinkLayer);
			conn.send(frame, BlockingMode.Confirmation);
			// wait for .con
			responseFor(CEMIDevMgmt.MC_PROPWRITE_CON, BcuSwitcher.pidCommMode);
		}
		else if (activeEmi == Emi1) {
			new BcuSwitcher<>(conn, logger).enter(BcuMode.LinkLayer);
		}
		else {
			final byte[] switchLinkLayer = { (byte) PEI_SWITCH, 0x00, 0x18, 0x34, 0x56, 0x78, 0x0A, };
			conn.send(switchLinkLayer, BlockingMode.Confirmation);
		}
	}

	private void normalMode() throws KNXPortClosedException, KNXTimeoutException, InterruptedException
	{
		if (activeEmi == Cemi) {
			final CEMI frame = new CEMIDevMgmt(CEMIDevMgmt.MC_RESET_REQ);
			conn.send(frame.toByteArray(), BlockingMode.Confirmation);
		}
		else if (activeEmi == Emi1) {
			new BcuSwitcher<>(conn, logger).reset();
		}
		else {
			final byte[] switchNormal = { (byte) PEI_SWITCH, 0x1E, 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, };
			conn.send(switchNormal, BlockingMode.Confirmation);
		}
	}

	private void supportedCommModes() throws KNXException, InterruptedException {
		// supported comm modes: b3 = TLL | b2 = raw | b1 = BusMon | b0 = DLL
		final int pidSupportedCommModes = 64;
		read(cemiServerObject, pidSupportedCommModes).map(AbstractLink::unsigned).ifPresent(this::logCommModes);
	}

	private void logCommModes(final int modes) {
		logger.log(DEBUG, "KNX interface supports {0}",
				Stream.of(bool(modes & 0b1000, "transport link layer"), bool(modes & 0b100, "raw mode"),
						bool(modes & 0b10, "busmonitor"), bool(modes & 0b1, "data link layer"))
						.filter(s -> !s.isEmpty()).collect(Collectors.joining(", ")));
	}

	private static String bool(final int condition, final String ifTrue) {
		return condition != 0 ? ifTrue : "";
	}

	private void deviceAddr() throws KNXException, InterruptedException {
		final int pidSubnet = 57;
		final int pidDeviceAddr = 58;
		final Optional<byte[]> subnet = read(0, pidSubnet);
		if (subnet.isPresent()) {
			final int addr = read(0, pidDeviceAddr).map(data -> unsigned(subnet.get()[0], data[0])).orElse(0);
			logger.log(DEBUG, "KNX interface address {0}", new IndividualAddress(addr));
		}
	}

	private void disableFilters() throws KNXException {
		if (getKNXMedium().getMedium() != KNXMediumSettings.MEDIUM_RF)
			return;
		final int pidFilteringModeSelect = 66;
		write(cemiServerObject, pidFilteringModeSelect, new byte[] { 0, 0xf });
	}

	private void write(final int objectType, final int pid, final byte[] data) throws KNXException {
		if (!cEMI)
			return;
		final int objectInstance = 1;
		final CEMI frame = new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, objectType, objectInstance, pid, 1, 1, data);
		logger.log(TRACE, "write mgmt OT {0} PID {1} data 0x{2}", objectType, pid, HexFormat.of().formatHex(data));
		conn.send(frame.toByteArray(), BlockingMode.Confirmation);
	}

	private void notifyConnectionStatus(final ConnectionStatus status) {
		notifier.dispatchCustomEvent(status);
	}
}
