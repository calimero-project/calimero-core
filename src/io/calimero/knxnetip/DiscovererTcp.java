/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2024 B. Malinowsky

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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.calimero.IndividualAddress;
import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.KNXTimeoutException;
import io.calimero.knxnetip.Discoverer.Result;
import io.calimero.knxnetip.KNXnetIPTunnel.TunnelingLayer;
import io.calimero.knxnetip.TcpConnection.SecureSession;
import io.calimero.knxnetip.servicetype.DescriptionRequest;
import io.calimero.knxnetip.servicetype.DescriptionResponse;
import io.calimero.knxnetip.servicetype.KNXnetIPHeader;
import io.calimero.knxnetip.servicetype.PacketHelper;
import io.calimero.knxnetip.servicetype.SearchRequest;
import io.calimero.knxnetip.servicetype.SearchResponse;
import io.calimero.knxnetip.util.CRI;
import io.calimero.knxnetip.util.Srp;
import io.calimero.link.medium.KNXMediumSettings;

/**
 * KNXnet/IP discovery and retrieval of self-description for TCP capable KNX devices, supporting both plain and
 * KNX IP Secure connections.
 */
public class DiscovererTcp {
	private final TcpConnection connection;
	private final SecureSession session;

	private volatile Duration timeout = Duration.ofSeconds(10);


	DiscovererTcp(final TcpConnection c) {
		this.connection = c;
		this.session = null;
	}

	DiscovererTcp(final SecureSession s) {
		this.connection = s.connection();
		this.session = s;
	}

	/**
	 * Sets the timeout used for subsequent KNXnet/IP discovery &amp; description; the default timeout is 10 seconds.
	 *
	 * @param timeout timeout &gt; 0
	 * @return this discoverer
	 */
	public DiscovererTcp timeout(final Duration timeout) {
		if (timeout.isNegative() || timeout.isZero())
			throw new KNXIllegalArgumentException("timeout <= 0");
		this.timeout = timeout;
		return this;
	}

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
			if (cause instanceof KNXException exception)
				throw exception;
			if (cause instanceof TimeoutException)
				throw new KNXTimeoutException(cause.getMessage());
			throw new KNXException("waiting for description response", cause);
		}
	}

	private <T> CompletableFuture<Result<T>> send(final byte[] packet) {
		try {
			final var cf = new CompletableFuture<Result<T>>();
			final var tunnel = new Tunnel<>(TunnelingLayer.LinkLayer, connection, KNXMediumSettings.BackboneRouter, cf);
			tunnel.send(packet);
			cf.whenCompleteAsync((_1, _2) -> tunnel.close());
			return cf.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
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
