/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2024, 2024 B. Malinowsky

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

package io.calimero.mgmt;

import java.net.UnixDomainSocketAddress;
import java.util.function.Consumer;

import io.calimero.CloseEvent;
import io.calimero.Connection.BlockingMode;
import io.calimero.KNXException;
import io.calimero.KNXTimeoutException;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIDevMgmt;
import io.calimero.knxnetip.ClientConnection;
import io.calimero.knxnetip.KNXConnectionClosedException;
import io.calimero.knxnetip.KNXnetIPConnection;
import io.calimero.knxnetip.KNXnetIPDevMgmt;
import io.calimero.knxnetip.SecureConnection;
import io.calimero.knxnetip.StreamConnection.SecureSession;
import io.calimero.knxnetip.UnixDomainSocketConnection;

/**
 * Adapter for KNX property services using KNXnet/IP local device management over a Unix domain socket.
 *
 * @see KNXnetIPDevMgmt
 */
public final class LocalDeviceManagementUds extends LocalDeviceManagement<CEMI> {

	private final UnixDomainSocketAddress remote;

	/**
	 * Creates a new property service adapter for local device management over a Unix domain socket connection.
	 *
	 * @param connection the Unix domain socket connection to the server
	 * @param adapterClosed receives the notification if the adapter got closed
	 * @return a new local device management connection
	 * @throws KNXException on failure establishing local device management connection or failure while initializing the
	 *         property adapter
	 * @throws InterruptedException on interrupted thread while initializing the adapter
	 */
	public static LocalDeviceManagementUds newAdapter(final UnixDomainSocketConnection connection,
			final Consumer<CloseEvent> adapterClosed) throws KNXException, InterruptedException {
		final var mgmt = new KNXnetIPDevMgmt(connection);
		return new LocalDeviceManagementUds(mgmt, adapterClosed, false);
	}

	/**
	 * Creates a new secure property service adapter for local device management of a KNXnet/IP server using the
	 * supplied secure session.
	 *
	 * @param session secure session with a KNXnet/IP server
	 * @param adapterClosed receives the notification if the adapter got closed
	 * @return a new local device management connection
	 * @throws KNXException if establishing the local device management connection or initializing the property adapter
	 *         fails
	 * @throws InterruptedException on interrupted thread while initializing the adapter
	 */
	public static LocalDeviceManagementUds newSecureAdapter(final SecureSession session,
			final Consumer<CloseEvent> adapterClosed) throws KNXException, InterruptedException {
		final var mgmt = SecureConnection.newDeviceManagement(session);
		return new LocalDeviceManagementUds(mgmt, adapterClosed, false);
	}

	LocalDeviceManagementUds(final KNXnetIPConnection mgmt, final Consumer<CloseEvent> adapterClosed,
		final boolean queryWriteEnable) throws KNXException, InterruptedException {
		super(mgmt, adapterClosed, queryWriteEnable);
		remote = (UnixDomainSocketAddress) ((ClientConnection) mgmt).remoteAddress();
		c.addConnectionListener(new KNXListenerImpl());
		init();
	}

	/**
	 * Sends a reset request to the KNXnet/IP server. A successful reset request causes the KNXnet/IP server to close
	 * the KNXnet/IP device management connection.
	 *
	 * @throws KNXConnectionClosedException on closed connection
	 * @throws KNXTimeoutException if a timeout regarding a response message was encountered
	 * @throws InterruptedException on thread interrupt
	 */
	public void reset() throws KNXException, InterruptedException {
		send(new CEMIDevMgmt(CEMIDevMgmt.MC_RESET_REQ), BlockingMode.Ack);
	}

	/**
	 * The name for this adapter starts with "Local-DM " + KNXnet/IP server control endpoint, allowing
	 * easier distinction of adapter types.
	 */
	@Override
	public String getName() { return "Local-DM " + remote; }

	@Override
	protected void send(final CEMIDevMgmt frame, final BlockingMode mode)
			throws KNXException, InterruptedException {
		c.send(frame, mode);
	}
}
