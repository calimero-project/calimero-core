/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2022 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip;

import static tuwien.auto.calimero.knxnetip.Net.hostPort;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import tuwien.auto.calimero.knxnetip.TcpConnection.SecureSession;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionRequest;
import tuwien.auto.calimero.knxnetip.servicetype.DescriptionResponse;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.PacketHelper;
import tuwien.auto.calimero.knxnetip.servicetype.SearchRequest;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.knxnetip.util.CRI;
import tuwien.auto.calimero.knxnetip.util.Srp;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;

/**
 * KNXnet/IP discovery and retrieval of self description for TCP capable KNX devices, supporting both plain and
 * KNX IP Secure connections.
 */
public class DiscovererTcp extends Discoverer {
	private final TcpConnection connection;
	private final SecureSession session;


	DiscovererTcp(final TcpConnection c) {
		this.connection = c;
		this.session = null;
	}

	DiscovererTcp(final SecureSession s) {
		this.connection = s.connection();
		this.session = s;
	}

	@Override
	public DiscovererTcp timeout(final Duration timeout) {
		return (DiscovererTcp) super.timeout(timeout);
	}

	@Override
	public CompletableFuture<List<Result<SearchResponse>>> search(final Srp... searchParameters) {
		final SearchRequest req = SearchRequest.newTcpRequest(searchParameters);
		final CompletableFuture<Result<SearchResponse>> result = send(PacketHelper.toPacket(req));
		return result.thenApply(List::of);
	}

	public Result<DescriptionResponse> description() throws KNXException, InterruptedException {
		try {
			final byte[] request = PacketHelper.toPacket(DescriptionRequest.tcpRequest());
			final CompletableFuture<Result<DescriptionResponse>> cf = send(request);
			return cf.get();
		}
		catch (final ExecutionException e) {
			final var cause = e.getCause();
			if (cause instanceof KNXException)
				throw (KNXException) cause;
			if (cause instanceof TimeoutException)
				throw new KNXTimeoutException(cause.getMessage());
			throw new KNXException("waiting for description response", cause);
		}
	}

	/**
	 * @deprecated
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	@Deprecated(forRemoval = true)
	public CompletableFuture<Result<SearchResponse>> search(final InetSocketAddress serverControlEndpoint,
			final Srp... searchParameters) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	@Deprecated(forRemoval = true)
	public void startSearch(final NetworkInterface ni, final int timeout, final boolean wait) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	@Deprecated(forRemoval = true)
	public Result<DescriptionResponse> getDescription(final InetSocketAddress server, final int timeout) {
		throw new UnsupportedOperationException();
	}

	private <T> CompletableFuture<Result<T>> send(final byte[] packet) {
		try {
			final var cf = new CompletableFuture<Result<T>>();
			final var tunnel = new Tunnel<>(TunnelingLayer.LinkLayer, connection, KNXMediumSettings.BackboneRouter, cf);
			tunnel.send(packet);
			cf.whenCompleteAsync((_1, _2) -> tunnel.close());
			return cf.orTimeout(timeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (KNXException | IOException | InterruptedException e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	private final class Tunnel<T> extends KNXnetIPTunnel {
		private final CompletableFuture<Result<T>> cf;

		Tunnel(final TunnelingLayer knxLayer, final TcpConnection connection,
				final IndividualAddress tunnelingAddress, final CompletableFuture<Result<T>> cf) throws KNXException,
				InterruptedException {
			super(knxLayer, connection, tunnelingAddress);
			this.cf = cf;
		}

		@Override
		protected boolean handleServiceType(final KNXnetIPHeader h, final byte[] data, final int offset,
				final InetAddress src, final int port) throws KNXFormatException, IOException {

			final int svc = h.getServiceType();
			if (svc == KNXnetIPHeader.SearchResponse || svc == KNXnetIPHeader.DESCRIPTION_RES) {
				final var sr = svc == KNXnetIPHeader.SearchResponse ? SearchResponse.from(h, data, offset)
						: new DescriptionResponse(data, offset, h.getTotalLength() - h.getStructLength());
				final var result = new Result<>(sr,
						NetworkInterface.getByInetAddress(connection.localEndpoint().getAddress()),
						connection.localEndpoint(), connection.server());
				complete(result);
				return true;
			}
			return super.handleServiceType(h, data, offset, src, port);
		}

		@SuppressWarnings("unchecked")
		private void complete(final Result<?> result) { cf.complete((Result<T>) result); }

		@Override
		public String name() {
			final String lock = new String(Character.toChars(0x1F512));
			final String secure = session != null ? (" " + lock) : "";
			return "KNX IP" + secure + " Tunneling " + hostPort(ctrlEndpt);
		}

		@Override
		protected void connect(final TcpConnection c, final CRI cri) throws KNXException, InterruptedException {
			if (session == null) {
				super.connect(c, cri);
				return;
			}

			session.ensureOpen();
			session.registerConnectRequest(this);
			try {
				super.connect(c.localEndpoint(), c.server(), cri, false);
			}
			finally {
				session.unregisterConnectRequest(this);
			}
		}

		@Override
		protected void send(final byte[] packet, final InetSocketAddress dst) throws IOException {
			var send = packet;
			if (session != null)
				send = SecureConnection.newSecurePacket(session.id(), session.nextSendSeq(), session.serialNumber(), 0,
						packet, session.secretKey);
			super.send(send, dst);
		}

		void send(final byte[] packet) throws IOException { send(packet, new InetSocketAddress(0)); }
	}
}
