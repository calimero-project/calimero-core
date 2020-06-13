/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019, 2020 B. Malinowsky

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

import java.util.Map;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.internal.SecureApplicationLayer;
import tuwien.auto.calimero.link.KNXLinkClosedException;

public class SecureManagement extends SecureApplicationLayer {

	private static final int DataConnected = 0x40;

	private final TransportLayerImpl transportLayer;
	private final EventListeners<TransportListener> listeners;

	private final TransportListener transportListener = new TransportListener() {
		@Override
		public void group(final FrameEvent e) {
			extract(e).ifPresent(e2 -> {
				listeners.fire(tl -> tl.group(e2));
				dispatchLinkEvent(e2);
			});
		}

		@Override
		public void broadcast(final FrameEvent e) { extract(e).ifPresent(e2 -> listeners.fire(tl -> tl.broadcast(e2))); }

		@Override
		public void dataIndividual(final FrameEvent e) { extract(e).ifPresent(e2 -> listeners.fire(tl -> tl.dataIndividual(e2))); }

		@Override
		public void dataConnected(final FrameEvent e) { extract(e).ifPresent(e2 -> listeners.fire(tl -> tl.dataConnected(e2))); }

		@Override
		public void disconnected(final Destination d) { listeners.fire(tl -> tl.disconnected(d)); }

		@Override
		public void detached(final DetachEvent e) { listeners.fire(tl -> tl.detached(e)); }

		@Override
		public void linkClosed(final CloseEvent e) { listeners.fire(tl -> tl.linkClosed(e)); }
	};


	protected SecureManagement(final TransportLayerImpl transportLayer, final byte[] serialNumber,
			final long sequenceNumber, final Map<IndividualAddress, byte[]> deviceToolKeys) {
		super(transportLayer.link(), serialNumber, sequenceNumber, deviceToolKeys);
		this.transportLayer = transportLayer;
		listeners = new EventListeners<>();
		transportLayer.addTransportListener(transportListener);
	}

	SecureManagement(final TransportLayer transportLayer, final Map<IndividualAddress, byte[]> deviceToolKeys) {
		this((TransportLayerImpl) transportLayer, new byte[6], 0, deviceToolKeys);
	}

	public void addListener(final TransportListener l) { listeners.add(l); }

	public void removeListener(final TransportListener l) { listeners.remove(l); }

	@Override
	public void close() {
		transportLayer.removeTransportListener(transportListener);
		super.close();
	}

	@Override
	protected void send(final KNXAddress remote, final byte[] secureApdu)
			throws KNXTimeoutException, KNXLinkClosedException {
		final var destination = remote instanceof IndividualAddress
				? transportLayer.getDestination((IndividualAddress) remote) : null;
		if (destination != null && destination.isConnectionOriented()) {
			try {
				transportLayer.sendData(destination, Priority.SYSTEM, secureApdu);
			}
			catch (final KNXDisconnectException e) {
				throw new KNXTimeoutException("timeout caused by disconnect from " + remote, e);
			}
		}
		else
			transportLayer.sendData(remote, Priority.SYSTEM, secureApdu);
	}

	@Override
	protected int tpci(final KNXAddress dst) {
		final var proxy = transportLayer.proxies().get(dst);

		int seqSend = 0;
		int tlMode = 0;
		if (proxy != null) {
			final var destination = proxy.getDestination();
			tlMode = destination.isConnectionOriented() ? DataConnected : 0;
			seqSend = proxy.getSeqSend();
		}

		final int tpci = tlMode | seqSend << 2;
		return tpci;
	}
}
