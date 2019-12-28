/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2019 B. Malinowsky

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

package tuwien.auto.calimero.cemi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * A cEMI transport layer data message (T-Data) supporting cEMI additional information, with a maximum transport layer
 * protocol data unit of 255 bytes. Objects of this T-Data type are mutable.
 */
public class CemiTData implements CEMI {
	public static final int IndividualRequest = 0x4a;
	public static final int IndividualIndication = 0x94;
	public static final int ConnectedRequest = 0x41;
	public static final int ConnectedIndication = 0x89;

	private static final int basicLength = 9;

	private final int mc;
	private final byte[] tpdu;

	private final List<AdditionalInfo> additionalInfo = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Creates a new T-Data message from a byte stream.
	 *
	 * @param data byte stream containing a cEMI T-Data message
	 * @param offset start offset of cEMI frame in <code>data</code>
	 * @param length length in bytes of the whole cEMI message in <code>data</code>
	 * @throws KNXFormatException if no (valid) frame was found
	 */
	public CemiTData(final byte[] data, final int offset, final int length) throws KNXFormatException {
		if (data.length - offset < length || length < basicLength + 1)
			throw new KNXFormatException("buffer too short for frame");
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
		mc = is.read();
		if (mc != IndividualRequest && mc != IndividualIndication && mc != ConnectedRequest && mc != ConnectedIndication)
			throw new KNXFormatException("invalid msg code for T-data frame", mc);
		readAddInfo(is);
		is.skip(6); // reserved

		int len = is.read();
		// length field is 0 in RF frames
		if (len == 0)
			len = is.available();
		else {
			++len;
			if (len > is.available())
				throw new KNXFormatException("length of tpdu exceeds available data", len);
		}
		this.tpdu = new byte[len];
		if (!isValidTpduLength(data))
			throw new KNXFormatException("length of tpdu exceeds maximum length of 255", len);
		is.read(data, 0, len);
	}

	/**
	 * Creates a new T-Data message with message code and TPDU.
	 *
	 * @param msgCode a message code value specified in the T-Data type
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application layer protocol
	 *        control information), i.e., the NPDU without the length field, tpdu.length &le; 255
	 */
	public CemiTData(final int msgCode, final byte[] tpdu) {
		mc = msgCode;
		if (mc != IndividualRequest && mc != IndividualIndication && mc != ConnectedRequest && mc != ConnectedIndication)
			throw new KNXIllegalArgumentException("invalid msg code for T-data frame");

		this.tpdu = tpdu.clone();
		if (!isValidTpduLength(tpdu))
			throw new KNXIllegalArgumentException("tpdu length " + tpdu.length + " out of range [0..255]");
	}

	@Override
	public int getMessageCode() { return mc; }

	/**
	 * @return mutable list with cEMI additional information, empty list if no additional information
	 */
	public List<AdditionalInfo> additionalInfo() { return additionalInfo; }

	@Override
	public byte[] getPayload() { return tpdu.clone(); }

	@Override
	public int getStructLength() { return basicLength + tpdu.length; }

	@Override
	public synchronized byte[] toByteArray() {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(mc);
		writeAddInfo(os);
		os.writeBytes(new byte[6]);
		writePayload(os);
		return os.toByteArray();
	}

	@Override
	public String toString() {
		final String svc = mc == IndividualRequest ? "Individual.req" : mc == IndividualIndication ? "Individual.ind"
				: mc == ConnectedIndication ? "Connected.ind" : mc == ConnectedRequest ? "Connected.req" : "Unknown";

		final StringBuilder buf = new StringBuilder();
		buf.append("T-Data");
		buf.append(svc);

		for (final AdditionalInfo additionalInfo : additionalInfo) {
			buf.append(" ");
			buf.append(additionalInfo);
			buf.append(",");
		}
		buf.append(" tpdu ").append(DataUnitBuilder.toHex(tpdu, " "));
		return buf.toString();
	}

	private void readAddInfo(final ByteArrayInputStream is) throws KNXFormatException {
		final int ail = is.read();
		if (ail == 0)
			return;
		if (ail > is.available())
			throw new KNXFormatException("additional info length exceeds frame length", ail);
		for (int remaining = ail; remaining > 0; remaining -= 2) {
			final var info = AdditionalInfo.from(is);
			additionalInfo.add(info);
			remaining -= info.info().length;
		}
	}

	private void writeAddInfo(final ByteArrayOutputStream os) {
		synchronized (additionalInfo) {
			os.write(additionalInfoLength());
			additionalInfo.sort((lhs, rhs) -> lhs.type() - rhs.type());
			for (final AdditionalInfo info : additionalInfo) {
				os.write(info.type());
				os.write(info.info().length);
				os.writeBytes(info.info());
			}
		}
	}

	private int additionalInfoLength() {
		int len = 0;
		synchronized (additionalInfo) {
			for (final AdditionalInfo info : additionalInfo)
				len += 2 + info.info().length;
		}
		return len;
	}

	private void writePayload(final ByteArrayOutputStream os) {
		// RF frames don't use NPDU length field
		final boolean rf = additionalInfo.stream().anyMatch(info -> info.type() == AdditionalInfo.RfMedium);
		os.write(rf ? 0 : tpdu.length - 1);
		os.write(tpdu, 0, tpdu.length);
	}

	private static boolean isValidTpduLength(final byte[] tpdu) {
		// value of length field is limited to 254, 255 is reserved as ESC code
		return tpdu.length > 0 && tpdu.length <= 255;
	}
}
