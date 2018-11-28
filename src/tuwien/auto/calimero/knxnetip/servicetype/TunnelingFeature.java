/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018 B. Malinowsky

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

package tuwien.auto.calimero.knxnetip.servicetype;

import static tuwien.auto.calimero.knxnetip.servicetype.TunnelingFeature.Result.Success;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Provides minimal management services over a KNXnet/IP tunneling connection (similar to the KNX USB Bus Access Server
 * Feature protocol).
 */
class TunnelingFeature extends ServiceType {

	// list of RCs used with tunneling feature services
//	class RC {
//		private static final int Success = 0;
//		private static final int SuccessWithCrc = 1;
//
//		private static final int MemoryError = 0x0;
//		private static final int ImpossibleCommand = 0x0;
//		private static final int InvalidCommand = 0xf2;
//		private static final int LengthExceedsMaxApduLength = 0xf4;
//		private static final int DataOverflow = 0xf5;
//		private static final int DataMin = 0xf6;
//		private static final int DataMax = 0xf7;
//		private static final int DataVoid = 0xf8;
//		private static final int TemporarilyNotAvailable = 0xf9;
//		private static final int AccessWriteOnly = 0xfa;
//		private static final int AccessReadOnly = 0xfb;
//		private static final int AccessDenied = 0xfc;
//		private static final int AddressVoid = 0xfd;
//		private static final int DataTypeConflict = 0xfe;
//		private static final int Error = 0xff;
//	}

	enum Result {
		Success(0), SuccessWithCrc(1), Error(0xff);

		public static Result from(final int code) {
			for (final Result s : values())
				if (code == s.code)
					return s;
			throw new KNXIllegalArgumentException("invalid tunneling feature result code 0x" + Integer.toHexString(code));
		}

		private final int code;

		private Result(final int code) { this.code = code; }

		public String friendly() {
			return name().replaceAll("(\\p{Lower})\\B([A-Z])", "$1 $2").toLowerCase();
		}
	}

	/**
	 * Creates a new tunneling feature-get service.
	 *
	 * @param channelId
	 * @param seq
	 * @param featureId
	 * @return
	 */
	public static TunnelingFeature newGet(final int channelId, final int seq, final int featureId) {
		return new TunnelingFeature(KNXnetIPHeader.TunnelingFeatureGet, featureId, Success);
	}

	/**
	 * Creates a new tunneling feature-response service.
	 *
	 * @param channelId
	 * @param seq
	 * @param featureId
	 * @param featureValue
	 * @return
	 */
	public static TunnelingFeature newResponse(final int channelId, final int seq, final int featureId, final Result result,
		final byte... featureValue) {
		return new TunnelingFeature(KNXnetIPHeader.TunnelingFeatureRes, featureId, result, featureValue);
	}

	/**
	 * Creates a new tunneling feature-set service.
	 *
	 * @param channelId
	 * @param seq
	 * @param featureId
	 * @param featureValue
	 * @return
	 */
	public static TunnelingFeature newSet(final int channelId, final int seq, final int featureId, final byte... featureValue) {
		return new TunnelingFeature(KNXnetIPHeader.TunnelingFeatureSet, featureId, Success, featureValue);
	}

	/**
	 * Creates a new tunneling feature-info service.
	 *
	 * @param channelId
	 * @param seq
	 * @param featureId
	 * @param featureValue
	 * @return
	 */
	public static TunnelingFeature newInfo(final int channelId, final int seq, final int featureId, final byte... featureValue) {
		return new TunnelingFeature(KNXnetIPHeader.TunnelingFeatureInfo, featureId, Success, featureValue);
	}

	/**
	 * Creates a new tunneling feature service by parsing the supplied byte buffer containing a tunneling feature service.
	 *
	 * @param buffer byte buffer positioned at the start of the feature service (within a KNXnet/IP tunneling datagram)
	 * @return a tunneling feature service
	 */
	public static TunnelingFeature from(final ByteBuffer buffer) {
		return new TunnelingFeature(buffer);
	}

	// connection header
	private final int channelId;
	private final int seq;

	// TODO list of features is reused from the USB Bus Access Server Feature protocol
//	HidReport.BusAccessServerFeature featureId;
	private final int featureId;
	private final Result status;
	private final byte[] data;

	private TunnelingFeature(final int serviceType, final int featureId, final Result status, final byte... data) {
		super(serviceType);

		channelId = 0;
		seq = 0;
		this.featureId = featureId;
		this.status = status;
		this.data = data.clone();
	}

	private TunnelingFeature(final ByteBuffer bb) {
		super(bb.getShort() & 0xffff);

		channelId = bb.get() & 0xff;
		seq = bb.get() & 0xff;
		featureId = bb.get() & 0xff;
		status = Result.from(bb.get() & 0xff);
		if (status.code > 0xf0)
			logger.warn("channel {} feature {} responded with '{}'", channelId, featureId, status.friendly());
		data = new byte[bb.remaining()];
		bb.get(data);
	}

	public final int channelId() {
		return channelId;
	}

	public final int sequenceNumber() {
		return seq;
	}

	public final int featureId() {
		return featureId;
	}

	public final Optional<byte[]> featureValue() {
		return data.length > 0 ? Optional.of(data) : Optional.empty();
	}

	Result status() {
		return status;
	}

	@Override
	int getStructLength() {
		return 0;
	}

	@Override
	byte[] toByteArray(final ByteArrayOutputStream os) {
		os.write(svcType >> 8);
		os.write(svcType);
		os.write(channelId);
		os.write(seq);

		os.write(featureId);
		os.write(status.code);
		os.write(data, 0, data.length);
		return os.toByteArray();
	}
}
