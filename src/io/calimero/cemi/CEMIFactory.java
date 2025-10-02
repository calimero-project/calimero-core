/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.cemi;

import static java.lang.System.Logger.Level.INFO;
import static java.lang.System.Logger.Level.WARNING;

import java.util.Arrays;

import io.calimero.DataUnitBuilder;
import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.Priority;
import io.calimero.log.LogService;

/**
 * Factory helper for creating and copying cEMI messages.
 *
 * @author B. Malinowsky
 */
public final class CEMIFactory
{
	// Workaround for some non spec-conform USB sticks, which sets some control bits when sending EMI frames.
	// Those bits are reserved and shall be 0, but the USB stick requires them to not reject the frame.
	private static final boolean setReservedEmiCtrlBits;
	static {
		final String pkg = "io.calimero.cemi";
		final String key = pkg + ".setReservedEmiCtrlBits";
		boolean value = false;
		try {
			final String prop = System.getProperty(key);
			value = (prop != null && prop.isEmpty()) || Boolean.parseBoolean(prop);
			if (value)
				LogService.getLogger(pkg).log(INFO, "using {0}", key);
		}
		catch (final RuntimeException e) {
			LogService.getLogger(pkg).log(WARNING, "on checking property " + key, e);
		}
		setReservedEmiCtrlBits = value;
	}


	private CEMIFactory() {}

	/**
	 * Creates a new cEMI message out of the given {@code data} byte stream.
	 *
	 * @param data byte stream containing a cEMI message frame structure
	 * @param offset start offset of cEMI message in {@code data}
	 * @param length length in bytes of the whole cEMI message in {@code data}
	 * @return the new created cEMI message
	 * @throws KNXFormatException if no (valid) cEMI structure was found or unsupported cEMI message
	 *         code
	 */
	public static CEMI create(final byte[] data, final int offset, final int length) throws KNXFormatException
	{
		if (length < 1)
			throw new KNXFormatException("buffer too short for cEMI frame", length);
		final int mc = data[offset] & 0xff;
		return switch (mc) {
			case CEMILData.MC_LDATA_REQ, CEMILData.MC_LDATA_CON, CEMILData.MC_LDATA_IND -> {
				if (length < 26 && !isLteFrame(data, offset, length)) {
					try {
						yield new CEMILData(data, offset, length);
					} catch (final KNXFormatException e) {
						// fall-through and try if the extended cEMI works
					}
				}
				yield new CEMILDataEx(data, offset, length);
			}
			case CEMIDevMgmt.MC_PROPREAD_REQ, CEMIDevMgmt.MC_PROPREAD_CON, CEMIDevMgmt.MC_PROPWRITE_REQ,
					CEMIDevMgmt.MC_PROPWRITE_CON, CEMIDevMgmt.MC_FUNCPROP_CMD_REQ, CEMIDevMgmt.MC_FUNCPROP_READ_REQ,
					CEMIDevMgmt.MC_FUNCPROP_CON, CEMIDevMgmt.MC_PROPINFO_IND, CEMIDevMgmt.MC_RESET_REQ,
					CEMIDevMgmt.MC_RESET_IND ->      new CEMIDevMgmt(data, offset, length);
			case CEMIBusMon.MC_BUSMON_IND ->         new CEMIBusMon(data, offset, length);
			case CemiTData.IndividualRequest, CemiTData.IndividualIndication, CemiTData.ConnectedRequest,
					CemiTData.ConnectedIndication -> new CemiTData(data, offset, length);
			default -> throw new KNXFormatException("unsupported cEMI msg code", mc);
		};
	}

	private static boolean isLteFrame(final byte[] data, final int offset, final int length) {
		if (length < 4)
			return false;
		final int addInfoLen = data[offset + 1] & 0xff;
		if (length < 4 + addInfoLen)
			return false;
		final int ctrl2 = data[offset + 3 + addInfoLen] & 0xff;
		return (ctrl2 & 0x04) != 0;
	}

	/**
	 * Creates a new cEMI message with information provided by {@code original}, and adjusts it
	 * to match the supplied {@code msgCode} and {@code data}.
	 * <p>
	 * The message code has to correspond to the type of cEMI frame supplied with
	 * {@code original}. The byte length of data has to fit the cEMI frame type supplied with
	 * {@code original}.<br>
	 * The {@code data} argument varies according to the supplied message code. For L-Data
	 * frames, this is the tpdu, for busmonitor frames, this is the raw frame, for device management
	 * frames, this is the data part or error information.
	 *
	 * @param msgCode the message code for the new cEMI frame
	 * @param data the data for the frame
	 * @param original the original frame providing all necessary information for the new frame
	 * @return the new cEMI frame adjusted with message code and data
	 * @throws KNXFormatException if cEMI type is unsupported or frame creation failed
	 */
	@SuppressWarnings("unchecked")
	public static <T extends CEMI> T create(final int msgCode, final byte[] data, final T original)
			throws KNXFormatException {
		return (T) switch (original) {
			case CEMILData ldata -> create(msgCode, null, null, data, ldata, false, ldata.isRepetition());
			case CEMIDevMgmt dm -> new CEMIDevMgmt(msgCode, dm.getObjectType(), dm.getObjectInstance(), dm.getPID(),
					dm.getStartIndex(), dm.getElementCount(), data);
			case CEMIBusMon mon -> newCemiBusMon(msgCode, data, mon);
			default -> throw new KNXFormatException("unsupported cEMI type " + original.getClass());
		};
	}

	/**
	 * Creates a new cEMI L-Data message with information provided by {@code original}, and
	 * adjusts source and destination address to match the supplied addresses.
	 *
	 * @param src the new KNX source address for the message, use {@code null} to use original
	 *        address
	 * @param dst the new KNX destination address for the message, use {@code null} to use
	 *        original address
	 * @param original the original frame providing all missing information for the adjusted message
	 * @param extended {@code true} to always created an extended frame, {@code false} to
	 *        create type according to {@code original}
	 * @return the new cEMI L-Data message adjusted with KNX addresses
	 */
	public static CEMILData create(final IndividualAddress src, final KNXAddress dst,
		final CEMILData original, final boolean extended)
	{
		return create(0, src, dst, null, original, extended, original.isRepetition());
	}

	/**
	 * Creates a new cEMI L-Data message with information provided by {@code original}, and
	 * adjusts source and destination address to match the supplied addresses, and sets the
	 * repeat/is repeated frame indication.
	 *
	 * @param src the new KNX source address for the message, use {@code null} to use original
	 *        address
	 * @param dst the new KNX destination address for the message, use {@code null} to use
	 *        original address
	 * @param original the original frame providing all missing information for the adjusted message
	 * @param extended {@code true} to always created an extended frame, {@code false} to
	 *        create type according to {@code original}
	 * @param repeat request frame repetition, or indicate a repeated frame; see
	 *        {@link CEMILData#isRepetition()}
	 * @return the new cEMI L-Data message adjusted with KNX addresses
	 */
	public static CEMILData create(final IndividualAddress src, final KNXAddress dst,
		final CEMILData original, final boolean extended, final boolean repeat)
	{
		return create(0, src, dst, null, original, extended, repeat);
	}

	private static final int Emi1_LData_ind = 0x49;
	private static final int Emi1_LBusmon_ind = 0x49;
	private static final int Emi1_LData_con = 0x4e;
	private static final int Emi1_LSysBcast_req = 0x15;
	private static final int Emi1_LSysBcast_con = 0x4c;
	private static final int Emi1_LSysBcast_ind = 0x4d;

	/**
	 * Creates a new cEMI message out of the supplied EMI 1/2 busmonitor frame. This specialization
	 * for busmonitor frames only is necessary, because EMI 1 has assigned the same message code for
	 * L-Data.ind and L-Busmon.ind.
	 *
	 * @param frame EMI 1/2 frame
	 * @return the cEMI message
	 * @throws KNXFormatException if no (valid) EMI structure was found or unsupported EMI message
	 *         code
	 */
	public static CEMI fromEmiBusmon(final byte[] frame) throws KNXFormatException
	{
		// check for minimum busmon frame length
		if (frame.length < 4)
			throw new KNXFormatException("EMI frame too short");
		final int mc = frame[0] & 0xff;
		if (mc != CEMIBusMon.MC_BUSMON_IND && mc != Emi1_LBusmon_ind)
			throw new KNXFormatException("not a busmonitor frame with msg code 0x" + Integer.toHexString(mc));
		return CEMIBusMon.newWithStatus(frame[1] & 0xff, (frame[2] & 0xff) << 8 | (frame[3] & 0xff),
				false, Arrays.copyOfRange(frame, 4, frame.length));
	}

	/**
	 * Creates a new cEMI message out of the supplied EMI frame.
	 *
	 * @param frame EMI frame
	 * @return the new cEMI message
	 * @throws KNXFormatException if no (valid) EMI structure was found or unsupported EMI message
	 *         code
	 */
	public static CEMI fromEmi(final byte[] frame) throws KNXFormatException
	{
		// check for minimum frame length (i.e., a busmonitor frame)
		if (frame.length < 4)
			throw new KNXFormatException("EMI frame too short");
		int mc = frame[0] & 0xff;

		// check for system broadcast on open media
		boolean domainBcast = true;
		if (mc == Emi1_LSysBcast_req || mc == Emi1_LSysBcast_con || mc == Emi1_LSysBcast_ind)
			domainBcast = false;

		// For EMI 1, L-Data.ind is given preference over L-Busmon.ind
		if (mc == Emi1_LData_ind)
			mc = CEMILData.MC_LDATA_IND;
		if (mc == Emi1_LData_con)
			mc = CEMILData.MC_LDATA_CON;
		if (mc == Emi1_LSysBcast_req)
			mc = CEMILData.MC_LDATA_REQ;
		if (mc == Emi1_LSysBcast_con)
			mc = CEMILData.MC_LDATA_CON;
		if (mc == Emi1_LSysBcast_ind)
			mc = CEMILData.MC_LDATA_IND;

		if (mc == CEMIBusMon.MC_BUSMON_IND) {
			return CEMIBusMon.newWithStatus(frame[1] & 0xff, (frame[2] & 0xff) << 8 | (frame[3] & 0xff), false,
					Arrays.copyOfRange(frame, 4, frame.length));
		}
		final Priority p = Priority.get(frame[1] >> 2 & 0x3);
		final boolean ack = (frame[1] & 0x02) != 0;
		final boolean c = (frame[1] & 0x01) != 0;
		final int dst = (frame[4] & 0xff) << 8 | (frame[5] & 0xff);
		final KNXAddress a = (frame[6] & 0x80) != 0 ? new GroupAddress(dst)
				: new IndividualAddress(dst);
		final int hops = frame[6] >> 4 & 0x07;
		final int len = (frame[6] & 0x0f) + 1;
		final byte[] tpdu = Arrays.copyOfRange(frame, 7, len + 7);
		final int src = ((frame[2] & 0xff) << 8) | (frame[3] & 0xff);

		if (c) return new CEMILData(mc, new IndividualAddress(src), a, tpdu, p, c);
		// for .ind always create a not repeated frame, otherwise default repetition behavior
		final boolean repeat = mc != CEMILData.MC_LDATA_IND;
		return new CEMILData(mc, new IndividualAddress(src), a, tpdu, p, repeat, domainBcast, ack, hops);
	}

	/**
	 * Returns an EMI1/EMI2-formatted message of the supplied cEMI L-Data message.
	 *
	 * @param msg a cEMI L-Data message
	 * @return byte array containing the link layer message in EMI format
	 * @throws KNXIllegalArgumentException on unsupported ASDU length
	 */
	public static byte[] toEmi(final CEMILData msg)
	{
		int mc = msg.getMessageCode();
		final KNXAddress dst = msg.getDestination();
		// find out if we need L-Data system broadcast
		if (msg.isSystemBroadcast())
			mc = Emi1_LSysBcast_req;
		if (dst.getRawAddress() == 0) {
			// APCI domain address.read is always system broadcast
			if (DataUnitBuilder.getAPDUService(msg.getPayload()) == 0x03e1)
				mc = Emi1_LSysBcast_req;
		}

		final Priority p = msg.getPriority();
		final boolean repeat = msg.isRepetition();
		final boolean ackRequest = msg.isAckRequested();
		final boolean posCon = msg.isPositiveConfirmation();
		final int hopCount = msg.getHopCount();
		final byte[] nsdu = msg.getPayload();
		return toEmi(mc, dst, p, repeat, ackRequest, posCon, hopCount, nsdu);
	}

	/**
	 * Returns an EMI1/EMI2-formatted message using the specified L-Data message parameters. This
	 * method performs the same formatting as {@link #toEmi(CEMILData)}, with the flexibility of
	 * individual message parameters from an already dissected cEMI L-Data message.
	 *
	 * @param mc message code
	 * @param dst destination address
	 * @param p message priority
	 * @param repeat see
	 *        {@link CEMILData#CEMILData(int, IndividualAddress, KNXAddress, byte[], Priority, boolean, boolean, boolean, int)}
	 * @param ackRequest see
	 *        {@link CEMILData#CEMILData(int, IndividualAddress, KNXAddress, byte[], Priority, boolean, boolean, boolean, int)}
	 * @param positiveCon positive confirmation, {@code true} if not applicable
	 * @param hopCount the hop count starting value set in control field, in the range 0 &lt;= value
	 *        &lt;= 7
	 * @param nsdu the NSDU
	 * @return byte array containing the link layer message in EMI1/EMI2 format
	 * @throws KNXIllegalArgumentException on unsupported ASDU length
	 */
	public static byte[] toEmi(final int mc, final KNXAddress dst, final Priority p,
		final boolean repeat, final boolean ackRequest, final boolean positiveCon,
		final int hopCount, final byte[] nsdu)
	{
		// TP1, standard frames only
		if (nsdu.length > 16)
			throw new KNXIllegalArgumentException("maximum TPDU length is 16 in standard frame");
		final byte[] buf = new byte[nsdu.length + 7];
		buf[0] = (byte) mc;

		buf[1] = (byte) (p.value << 2);
		if (setReservedEmiCtrlBits)
			buf[1] |= (byte) 0xb0;
		// repeat flag is only relevant for .con
		final boolean rep = mc == Emi1_LData_con && repeat;
		final int ctrl = (rep ? 0x20 : 0) | (ackRequest ? 0x02 : 0) | (positiveCon ? 0 : 0x01);
		buf[1] |= (byte) ctrl;

		// on dst null, default address 0 is used (null indicates system broadcast in link API)
		final int d = dst != null ? dst.getRawAddress() : 0;
		buf[4] = (byte) (d >> 8);
		buf[5] = (byte) d;

		buf[6] = (byte) (hopCount << 4 | (nsdu.length - 1));
		if (dst instanceof GroupAddress)
			buf[6] |= 0x80;
		System.arraycopy(nsdu, 0, buf, 7, nsdu.length);
		return buf;
	}

	/**
	 * Does a lazy copy of the supplied cEMI frame.
	 * <p>
	 * Only for cEMI frames which are <b>not</b> immutable a copy is created, for all other frames
	 * {@code original} is returned.
	 *
	 * @param original the frame to copy
	 * @return the {@code original} frame if immutable, a copy of it otherwise
	 */
	public static CEMI copy(final CEMI original)
	{
		// on original == null we just return null, too
		if (original instanceof final CEMILDataEx ex)
			return ex.clone();
		// all others are immutable
		return original;
	}

	// leave msgcode/src/dst/data null/0 to use from original
	private static CEMILData create(final int msgCode, final IndividualAddress src, final KNXAddress dst,
			final byte[] data, final CEMILData original, final boolean ext, final boolean repeat) {
		if (msgCode != 0 && msgCode != CEMILData.MC_LDATA_REQ && msgCode != CEMILData.MC_LDATA_CON && msgCode != CEMILData.MC_LDATA_IND)
			throw new KNXIllegalArgumentException("unsupported cEMI L-Data msg code 0x" + Integer.toHexString(msgCode));

		final int mc = msgCode != 0 ? msgCode : original.getMessageCode();
		final IndividualAddress s = src != null ? src : original.getSource();
		final KNXAddress d = dst != null ? dst : original.getDestination();
		final byte[] content = data != null ? data : original.getPayload();
		if (original instanceof final CEMILDataEx f) {
			final CEMILDataEx copy = new CEMILDataEx(mc, s, d, content, f.getPriority(), repeat,
					f.isDomainBroadcast(), f.isAckRequested(), f.getHopCount());
			if ((original.ctrl1 & 0x80) == 0)
				copy.ctrl1 &= ~0x80;
			copy.ctrl2 |= original.ctrl2 & 0x0f;
			// copy additional info
			copy.additionalInfo().addAll(f.additionalInfo());
			return copy;
		}
		if (ext)
			return new CEMILDataEx(mc, s, d, content, original.getPriority(), repeat, original.getHopCount());
		return new CEMILData(mc, s, d, content, original.getPriority(), repeat, original.getHopCount());
	}

	private static CEMIBusMon newCemiBusMon(final int msgCode, final byte[] data, final CEMIBusMon mon) {
		if (msgCode != CEMIBusMon.MC_BUSMON_IND)
			throw new KNXIllegalArgumentException("unsupported cEMI BusMon msg code 0x" + Integer.toHexString(msgCode));
		return CEMIBusMon.newWithStatus(mon.getStatus(), mon.getTimestamp(),
				mon.getTimestampType() == CEMIBusMon.TYPEID_TIMESTAMP_EXT, data);
	}
}
