/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.cemi.CEMIDevMgmt;
import tuwien.auto.calimero.knxnetip.KNXConnectionClosedException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode;
import tuwien.auto.calimero.knxnetip.KNXnetIPDevMgmt;

/**
 * Property adapter for KNXnet/IP local device management.
 * <p>
 * This adapter is based on a {@link KNXnetIPDevMgmt} connection.<br>
 * The object instance used is always the first one, i.e., object instance 1.
 *
 * @author B. Malinowsky
 */
public class LocalDeviceMgmtAdapter extends LocalDeviceManagement
{
	private final KNXnetIPConnection conn;

	/**
	 * Creates a new property adapter for local device management.
	 * <p>
	 * The server to do management for is specified with the server control endpoint.
	 * <p>
	 * A note on write enabled / read only properties:<br>
	 * The check whether a property is read only or write enabled, is done by issuing a write
	 * request for that property. Due to the memory layout, write cycles of a memory location and
	 * similar, this might not always be desired. To enable or skip this check, the
	 * <code>queryWriteEnable</code> option has to be set appropriately. Currently, the write
	 * enabled check is only of interest when getting a property description
	 * {@link #getDescription(int, int, int)}.
	 *
	 * @param localEP the local endpoint of the connection, use <code>null</code> for assigning the
	 *        default local host and an unused (ephemeral) port
	 * @param serverCtrlEP the remote server control endpoint used for connect request
	 * @param useNat <code>true</code> to use a network address translation aware communication
	 *        mechanism, <code>false</code> to use the default way
	 * @param l property adapter listener to get notified about adapter events, use
	 *        <code>null</code> for no listener
	 * @param queryWriteEnable <code>true</code> to check whether a property is write enabled or
	 *        read only, <code>false</code> to skip the check
	 * @throws KNXException on failure establishing local device management connection or failure
	 *         while initializing the property adapter
	 * @throws InterruptedException on interrupted thread while initializing the adapter
	 */
	public LocalDeviceMgmtAdapter(final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat,
		final PropertyAdapterListener l, final boolean queryWriteEnable) throws KNXException,
		InterruptedException
	{
		super(create(localEP, serverCtrlEP, useNat), l, queryWriteEnable);
		conn = (KNXnetIPConnection) c;
		conn.addConnectionListener(new KNXListenerImpl());
		init();
	}

	/**
	 * {@inheritDoc} The name for this adapter starts with "Local-DM " + KNXnet/IP server control
	 * endpoint, allowing easier distinction of adapter types.
	 */
	@Override
	public String getName()
	{
		return "Local-DM " + conn.getRemoteAddress();
	}

	@Override
	protected void send(final CEMIDevMgmt frame, final Object mode)
		throws KNXTimeoutException, KNXConnectionClosedException, InterruptedException
	{
		conn.send(frame, (BlockingMode) mode);
	}

	private static KNXnetIPDevMgmt create(final InetSocketAddress localEP,
		final InetSocketAddress serverCtrlEP, final boolean useNat) throws KNXException,
		InterruptedException
	{
		InetSocketAddress local = localEP;
		if (local == null)
			try {
				local = new InetSocketAddress(InetAddress.getLocalHost(), 0);
			}
			catch (final UnknownHostException e) {
				throw new KNXException("no local host available");
			}
		return new KNXnetIPDevMgmt(local, serverCtrlEP, useNat);
	}
}
