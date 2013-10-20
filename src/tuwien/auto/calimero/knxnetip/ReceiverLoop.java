/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2011 B. Malinowsky

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
*/

package tuwien.auto.calimero.knxnetip;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.internal.UdpSocketLooper;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogService;

final class ReceiverLoop extends UdpSocketLooper implements Runnable
{
	private final ConnectionBase conn;
	private final LogService logger;
	
	// precondition: an initialized logger instance in ConnectionBase
	ReceiverLoop(final ConnectionBase connection, final DatagramSocket socket,
		final int receiveBufferSize)
	{
		super(socket, true, receiveBufferSize, 0, 0);
		conn = connection;
		logger = connection.logger;
	}

	public void run()
	{
		try {
			loop();
		}
		catch (final IOException e) {
			conn.close(CloseEvent.INTERNAL, "receiver communication failure", LogLevel.ERROR, e);
		}
	}

	protected void onReceive(final InetSocketAddress source, final byte[] data,
		final int offset, final int length) throws IOException
	{
		try {
			final KNXnetIPHeader h = new KNXnetIPHeader(data, offset);
			if (h.getTotalLength() > length)
				logger.warn("received frame length " + length + " for " + h + " - ignored");
			else if (h.getServiceType() == 0)
				// check service type for 0 (invalid type),
				// so unused service types of us can stay 0 by default
				logger.warn("received frame with service type 0 - ignored");
			else if (!conn.handleServiceType(h, data, offset + h.getStructLength(),
					source.getAddress(), source.getPort()))
				logger.warn("received unknown frame, service type 0x"
						+ Integer.toHexString(h.getServiceType()) + " - ignored");
		}
		catch (final KNXFormatException e) {
			// if available log bad item, too
			if (e.getItem() != null)
				logger.warn("received invalid frame, item " + e.getItem(), e);
			else
				logger.warn("received invalid frame", e);
		}
	}
}
