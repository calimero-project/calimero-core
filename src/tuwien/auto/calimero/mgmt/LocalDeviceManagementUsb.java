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

package tuwien.auto.calimero.mgmt;

import java.util.EnumSet;
import java.util.List;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.serial.KNXPortClosedException;
import tuwien.auto.calimero.serial.usb.HidReport;
import tuwien.auto.calimero.serial.usb.TransferProtocolHeader.KnxTunnelEmi;
import tuwien.auto.calimero.serial.usb.UsbConnection;
import tuwien.auto.calimero.serial.usb.UsbConnection.EmiType;

/**
 * USB adapter for local device management. This adapter is based on a {@link UsbConnection}.
 *
 * @author B. Malinowsky
 */
public class LocalDeviceManagementUsb extends LocalDeviceManagement
{
	private static final int cEmiServerObject = 8;
	private static final int pidCommMode = 52;

	private static final int responseTimeout = 1000;

	private final UsbConnection conn;

	/**
	 * Creates a new property adapter for local device management. The cEMI server is specified by
	 * the supplied USB connection.
	 * <p>
	 * A note on write enabled / read only properties:<br>
	 * The check whether a property is read only or write enabled, is done by issuing a write
	 * request for that property. Due to the memory layout, write cycles of a memory location and
	 * similar, this might not always be desired. To enable or skip this check, the
	 * <code>queryWriteEnable</code> option has to be set appropriately. The write-enabled check is
	 * only of interest when getting a property description {@link #getDescription(int, int, int)}.
	 *
	 * @param c the USB connection
	 * @param l property adapter listener to get notified about adapter events, use
	 *        <code>null</code> for no listener
	 * @param queryWriteEnable <code>true</code> to check whether a property is write enabled or
	 *        read only, <code>false</code> to skip the check
	 * @throws KNXException on failure establishing local device management connection or failure
	 *         while initializing the property adapter
	 * @throws InterruptedException on interrupt during initialization
	 */
	public LocalDeviceManagementUsb(final UsbConnection c, final PropertyAdapterListener l,
		final boolean queryWriteEnable) throws KNXException, InterruptedException
	{
		super(c, l, queryWriteEnable);

		conn = c;
		final EnumSet<EmiType> emiTypes = conn.getSupportedEmiTypes();
		conn.addConnectionListener(new KNXListenerImpl());
		if (emiTypes.contains(EmiType.CEmi)) {
			conn.setActiveEmiType(EmiType.CEmi);
			final int objectInstance = 1;
			setProperty(cEmiServerObject, objectInstance, pidCommMode, 1, 1, new byte[] { 0x00 });
		}
		init();
	}

	//
	// Property methods with direct mapping to cEMI Device Management
	//

	/**
	 * Sets property value elements of an interface object property.
	 *
	 * @param objectType the interface object type
	 * @param objectInstance the interface object instance (usually 1)
	 * @param propertyId the property identifier (PID)
	 * @param start start index in the property value to start writing to
	 * @param elements number of elements to set
	 * @param data byte array containing the property value data
	 * @throws KNXTimeoutException on timeout setting the property elements
	 * @throws KNXRemoteException on remote error or invalid response
	 * @throws KNXPortClosedException if adapter is closed
	 */
	public void setProperty(final int objectType, final int objectInstance, final int propertyId,
		final int start, final int elements, final byte[] data) throws KNXTimeoutException,
		KNXRemoteException, KNXPortClosedException
	{
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPWRITE_REQ, objectType, objectInstance, propertyId,
				start, elements, data), null);
		findFrame(CEMIDevMgmt.MC_PROPWRITE_CON);
	}

	/**
	 * Gets property value elements of an interface object property.
	 *
	 * @param objectType the interface object type
	 * @param objectInstance the interface object instance (usually 1)
	 * @param propertyId the property identifier (PID)
	 * @param start start index in the property value to start writing to
	 * @param elements number of elements to set
	 * @return byte array containing the property element data
	 * @throws KNXTimeoutException on timeout setting the property elements
	 * @throws KNXRemoteException on remote error or invalid response
	 * @throws KNXPortClosedException if adapter is closed
	 */
	public byte[] getProperty(final int objectType, final int objectInstance, final int propertyId,
		final int start, final int elements) throws KNXTimeoutException, KNXRemoteException,
		KNXPortClosedException
	{
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_PROPREAD_REQ, objectType, objectInstance, propertyId,
				start, elements), null);
		return findFrame(CEMIDevMgmt.MC_PROPREAD_CON);
	}

	/**
	 * {@inheritDoc} The name for this adapter starts with "Local-DM " + USB connection name.
	 */
	@Override
	public String getName()
	{
		return "Local-DM " + conn.getName();
	}

	@Override
	protected void send(final CEMIDevMgmt frame, final Object mode) throws KNXTimeoutException,
		KNXPortClosedException
	{
		final List<HidReport> reports = HidReport.create(KnxTunnelEmi.CEmi, frame.toByteArray());
		for (final HidReport r : reports)
			conn.send(r, true);
	}

	@Override
	protected byte[] findFrame(final int messageCode) throws KNXRemoteException
	{
		long remaining = responseTimeout;
		final long end = System.currentTimeMillis() + remaining;

		while (remaining > 0) {
			try {
				return super.findFrame(messageCode);
			}
			catch (final KNXInvalidResponseException e) {}
			remaining = end - System.currentTimeMillis();
			// TODO replace busy waiting
			try {
				Thread.sleep(50);
			}
			catch (final InterruptedException e) {}
		}
		throw new KNXInvalidResponseException("expected service confirmation msg code 0x"
				+ Integer.toHexString(messageCode));
	}
}
