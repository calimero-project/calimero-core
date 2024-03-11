/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2020, 2022 B. Malinowsky

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

package tuwien.auto.calimero.serial;

import static tuwien.auto.calimero.serial.FT12Connection.ACK;
import static tuwien.auto.calimero.serial.FT12Connection.START;
import static tuwien.auto.calimero.serial.FT12Connection.START_FIXED;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.serial.spi.SerialCom;

class BauFt12Emulator implements SerialCom {
	private final ArrayBlockingQueue<Byte> queue = new ArrayBlockingQueue<>(100);

	boolean replyWithAck = true;
	boolean replyWithCon = true;

	private final OutputStream os = new OutputStream() {
		private int receivingFrameState;
		private byte[] frame;
		private int offset;

		@Override
		@SuppressWarnings("fallthrough")
		public void write(final int b) {
			if (receivingFrameState == 5) {
				frame[offset++] = (byte) b;
				if (offset >= frame.length) {
					receivingFrameState = 0;
					queue.add((byte) ACK);
					frame[4] = (byte) (FT12Connection.DIR_FROM_BAU | FT12Connection.INITIATOR);
					frame[5] = CEMILData.MC_LDATA_CON;
					frame[frame.length - 2] = checksum(frame, 4, frame[1] & 0xff);
					for (final byte e : frame)
						queue.add(e);
					frame = null;
					offset = 0;
				}
			}
			else if (receivingFrameState == 0) {
				if (b == START_FIXED)
					queue.add((byte) ACK);
			}

			switch (receivingFrameState) {
			case 0:
				if (b == START)
					receivingFrameState++;
				break;

			case 1:
				frame = new byte[6 + b];
				frame[offset++] = START;
			case 2:
			case 3:
			case 4:
				frame[offset++] = (byte) b;
				receivingFrameState++;
			}
		}
	};

	private final InputStream is = new InputStream() {
		@Override
		public int read() throws InterruptedIOException {
			try {
				final int b = queue.take() & 0xff;
				if (b == ACK && replyWithAck)
					return b;

				if (!replyWithAck)
					return -1;
				if (!replyWithCon)
					return -1;

				return b;
			}
			catch (final InterruptedException e) {
				throw new InterruptedIOException();
			}
		}
	};

	private static byte checksum(final byte[] data, final int offset, final int length) {
		byte chk = 0;
		for (int i = 0; i < length; ++i)
			chk += data[offset + i];
		return chk;
	}

	BauFt12Emulator() {}

	@Override
	public OutputStream outputStream() {
		return os;
	}

	@Override
	public InputStream inputStream() {
		return is;
	}

	@Override
	public int baudRate() {
		return 19200;
	}

	@Override
	public void close() {}
}
