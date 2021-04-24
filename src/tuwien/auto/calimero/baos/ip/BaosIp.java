/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019, 2021 B. Malinowsky

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

import java.nio.ByteBuffer;

import tuwien.auto.calimero.knxnetip.Discoverer;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.knxnetip.util.ManufacturerDIB;

public final class BaosIp {
	private static final int ObjectServerProtocol = 0xf0;
	private static final int WeinzierlMfrId = 0x00c5;
	private static final int Version = 0x20;


	public static boolean supportsBaos(final Discoverer.Result<SearchResponse> result) {
		final var mfrDib = result.getResponse().description().stream().filter(ManufacturerDIB.class::isInstance)
				.map(ManufacturerDIB.class::cast).filter(dib -> dib.getStructLength() == 8).findFirst();
		if (mfrDib.isEmpty())
			return false;

		final var buf = ByteBuffer.wrap(mfrDib.get().getData());
		return buf.getShort() == WeinzierlMfrId && buf.get() == 0x01 && buf.get() == 0x04
				&& (buf.get() & 0xff) == ObjectServerProtocol && buf.get() == Version;
	}

	private BaosIp() {}
}
