/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2021 B. Malinowsky

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

package tuwien.auto.calimero.baos.ip;

import java.net.InetSocketAddress;

import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.baos.BaosLink;
import tuwien.auto.calimero.baos.BaosService;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.knxnetip.TcpConnection;
import tuwien.auto.calimero.knxnetip.KNXConnectionClosedException;
import tuwien.auto.calimero.knxnetip.KNXnetIPConnection.BlockingMode;
import tuwien.auto.calimero.link.AbstractLink;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.medium.TPSettings;

public final class BaosLinkIp extends AbstractLink<ObjectServerConnection> implements BaosLink {
	private final ObjectServerConnection c;

	public static BaosLink newUdpLink(final InetSocketAddress localEp, final InetSocketAddress serverCtrlEp)
			throws KNXException, InterruptedException {
		final var c = new ObjectServerConnection(localEp, serverCtrlEp);
		return new BaosLinkIp(c);
	}

	public static BaosLink newTcpLink(final TcpConnection connection) throws KNXException {
		final var c = new ObjectServerConnection(connection);
		return new BaosLinkIp(c);
	}


	BaosLinkIp(final ObjectServerConnection c) {
		super(c, c.name(), new TPSettings());
		this.c = c;

		notifier.registerEventType(BaosService.class);
		c.addConnectionListener(notifier);
		c.addConnectionListener((ObjectServerConnection.ObjectServerListener) this::dispatchCustomEvent);
	}

	@Override
	public void send(final BaosService service) throws KNXTimeoutException, KNXLinkClosedException {
		try {
			c.send(service, BlockingMode.WaitForAck);
		}
		catch (final KNXConnectionClosedException e) {
			throw new KNXLinkClosedException("link closed, " + e.getMessage());
		}
	}

	@Override
	protected void onSend(final KNXAddress dst, final byte[] msg, final boolean waitForCon) {
		throw new UnsupportedOperationException("object server protocol does not support cEMI");
	}

	@Override
	protected void onSend(final CEMILData msg, final boolean waitForCon) {
		throw new UnsupportedOperationException("object server protocol does not support cEMI");
	}

	private void dispatchCustomEvent(final BaosService event) {
		notifier.dispatchCustomEvent(event);
	}
}
