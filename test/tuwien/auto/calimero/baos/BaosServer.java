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

package tuwien.auto.calimero.baos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.Function;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KnxRuntimeException;
import tuwien.auto.calimero.baos.BaosService.Item;
import tuwien.auto.calimero.baos.BaosService.Property;
import tuwien.auto.calimero.knxnetip.servicetype.KNXnetIPHeader;
import tuwien.auto.calimero.knxnetip.servicetype.ServiceRequest;

class BaosServer implements Runnable, AutoCloseable {
	private static final int port = 12004;

	private volatile Socket socket;

	@Override
	public void run() {
		try (var serverSocket = new ServerSocket(port)) {
			while (true) {
				socket = serverSocket.accept();
				final var in = socket.getInputStream();
				final var baos = new ByteArrayOutputStream();

				for (int b = in.read(); b != -1; b = in.read()) {
					baos.write(b);
					if (baos.size() > 10) {
						try {
							final byte[] data = baos.toByteArray();
							System.out.println(DataUnitBuilder.toHex(data, " "));
							final var h = new KNXnetIPHeader(data, 0);

							final Function<ByteBuffer, BaosService> objectServerParser = buf -> {
								try {
									return BaosService.from(buf);
								}
								catch (final KNXFormatException e) {
									throw new KnxRuntimeException("parsing BAOS service", e);
								}
							};

							final var req = ServiceRequest.from(h, data, h.getStructLength(), objectServerParser);
							final var svc = req.service();
							System.out.println(svc);
							baos.reset();

							final var res = BaosService.response(BaosService.GetServerItem, 9,
									Item.property(Property.TimeSinceReset, new byte[4]));
							System.out.println(res);

							final var out = socket.getOutputStream();

							final var resbuf = new ByteArrayOutputStream();
							resbuf.write(new KNXnetIPHeader(0xf080, 4 + res.toByteArray().length).toByteArray());
							resbuf.write(4);
							resbuf.write(0);
							resbuf.write(0);
							resbuf.write(0);
							resbuf.write(res.toByteArray());
							out.write(resbuf.toByteArray());
							out.flush();
						}
						catch (final IndexOutOfBoundsException e) {}
						catch (final KNXFormatException | KnxRuntimeException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void close() {
		try {
			if (socket != null)
				socket.close();
		}
		catch (final IOException e) {}
	}
}
