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

package io.calimero.knxnetip;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

/**
 * Support for Unix Domain Socket connections to KNXnet/IP servers.
 */
public final class UnixDomainSocketConnection extends StreamConnection {
	private final SocketChannel channel;


	public static UnixDomainSocketConnection newConnection(final Path path) throws IOException {
		return new UnixDomainSocketConnection(path);
	}


	private UnixDomainSocketConnection(final Path path) throws IOException {
		super(UnixDomainSocketAddress.of(path));
		channel = SocketChannel.open(StandardProtocolFamily.UNIX);
	}

	@Override
	public synchronized void connect() throws IOException {
		if (isConnected())
			return;
		channel.connect(server());
		startReceiver();
	}

	@Override
	public boolean isConnected() { return channel.isConnected(); }

	@Override
	SocketAddress localEndpoint() { return UnixDomainSocketAddress.of(""); }

	@Override
	public UnixDomainSocketAddress server() { return (UnixDomainSocketAddress) super.server(); }

	@Override
	void close(final int initiator, final String reason) {
		super.close(initiator, reason);
		try {
			channel.close();
		}
		catch (final IOException ignore) {}
	}

	@Override
	public String toString() {
		final var state = channel.isOpen() ? channel.isConnected() ? "connected" : "open" : "closed";
		return socketName(server()) + " (" + state +")";
	}

	@Override
	void send(final byte[] data) throws IOException { channel.write(ByteBuffer.wrap(data)); }

	@Override
	String socketName(final SocketAddress addr) { return addr.toString(); }

	@Override
	boolean streamClosed() { return !channel.isOpen(); }

	@Override
	int read(final byte[] data, final int offset) throws IOException {
		return channel.read(ByteBuffer.wrap(data, offset, data.length - offset));
	}
}
