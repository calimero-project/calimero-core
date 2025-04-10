/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2018, 2024 B. Malinowsky

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

package io.calimero.knxnetip;

import static io.calimero.knxnetip.Net.hostPort;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.calimero.IndividualAddress;
import io.calimero.KNXException;
import io.calimero.knxnetip.StreamConnection.SecureSession;
import io.calimero.knxnetip.util.CRI;
import io.calimero.knxnetip.util.TunnelCRI;
import io.calimero.link.medium.KNXMediumSettings;

final class SecureTunnel extends KNXnetIPTunnel {
	private final SecureSession session;

	SecureTunnel(final SecureSession session, final TunnelingLayer knxLayer,
			final IndividualAddress tunnelingAddress) throws KNXException, InterruptedException {
		super(knxLayer, session.connection(), tunnelingAddress);
		this.session = session;

		final var cri = tunnelingAddress.equals(KNXMediumSettings.BackboneRouter) ? new TunnelCRI(knxLayer)
				: new TunnelCRI(knxLayer, tunnelingAddress);
		session.registerConnectRequest(this);
		try {
			doConnect(session.connection(), cri);
		}
		finally {
			session.unregisterConnectRequest(this);
		}
	}

	@Override
	public String name() {
		final String remote = ctrlEndpt != null ? hostPort(ctrlEndpt) : controlEndpoint.toString();
		return "KNX IP " + SecureConnection.secureSymbol + " Tunneling " + remote;
	}

	@Override
	protected void connect(final StreamConnection c, final CRI cri) {
		// we don't have session assigned yet, connect in ctor
	}

	@Override
	protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
		final byte[] wrapped = SecureConnection.newSecurePacket(session.id(), session.nextSendSeq(),
				session.serialNumber(), 0, packet, session.secretKey);
		super.send(wrapped, dst);
	}
}
