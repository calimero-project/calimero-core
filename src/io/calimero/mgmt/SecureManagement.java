/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2019, 2023 B. Malinowsky

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

import java.util.Map;
import java.util.Optional;

import io.calimero.CloseEvent;
import io.calimero.DetachEvent;
import io.calimero.FrameEvent;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXTimeoutException;
import io.calimero.Priority;
import io.calimero.SerialNumber;
import io.calimero.internal.EventListeners;
import io.calimero.link.KNXLinkClosedException;
import io.calimero.secure.SecureApplicationLayer;
import io.calimero.secure.Security;

public class SecureManagement extends SecureApplicationLayer {

	private static final int DataConnected = 0x40;

	private final TransportLayer transportLayer;
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


	protected SecureManagement(final TransportLayerImpl transportLayer, final SerialNumber serialNumber,
			final long sequenceNumber, final Map<IndividualAddress, byte[]> deviceToolKeys) {
		super(transportLayer.link(), serialNumber, sequenceNumber, deviceToolKeys);
		this.transportLayer = transportLayer;
		listeners = new EventListeners<>();
		transportLayer.addTransportListener(transportListener);
	}

	SecureManagement(final TransportLayer transportLayer, final Map<IndividualAddress, byte[]> deviceToolKeys) {
		this((TransportLayerImpl) transportLayer, SerialNumber.Zero, 0, deviceToolKeys);
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
		final var destination = destination(remote);
		if (destination.isPresent() && destination.get().isConnectionOriented()) {
			try {
				transportLayer.sendData(destination.get(), Priority.SYSTEM, secureApdu);
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
		int seqSend = 0;
		int tlMode = 0;

		final var proxy = ((TransportLayerImpl) transportLayer).proxies().get(dst);
		final var destinationOpt = destination(dst);
		if (destinationOpt.isPresent()) {
			final var destination = destinationOpt.get();
			tlMode = destination.isConnectionOriented() ? DataConnected : 0;
			seqSend = proxy.getSeqSend();
		}

		final int tpci = tlMode | seqSend << 2;
		return tpci;
	}

	private Optional<Destination> destination(final KNXAddress remote) {
		return remote instanceof IndividualAddress ia ? transportLayer.destination(ia)
				: Optional.empty();
	}

	@Override
	public Security security() { return super.security(); }

    protected TransportLayer transportLayer() { return transportLayer; }
}
