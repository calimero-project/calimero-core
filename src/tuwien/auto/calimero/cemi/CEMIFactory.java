/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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

package tuwien.auto.calimero.cemi;

import java.util.Iterator;
import java.util.List;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * Factory helper for creating and copying cEMI messages.
 * <p>
 * 
 * @author B. Malinowsky
 */
public final class CEMIFactory
{
	private CEMIFactory()
	{}

	/**
	 * Creates a new cEMI message out of the given <code>data</code> byte stream.
	 * <p>
	 * 
	 * @param data byte stream containing a cEMI message frame structure
	 * @param offset start offset of cEMI message in <code>data</code>
	 * @param length length in bytes of the whole cEMI message in <code>data</code>
	 * @return the new created cEMI message
	 * @throws KNXFormatException if no (valid) cEMI structure was found or unsupported
	 *         cEMI message code
	 */
	public static CEMI create(final byte[] data, final int offset, final int length)
		throws KNXFormatException
	{
		if (length < 1)
			throw new KNXFormatException("buffer too short for cEMI frame", length);
		final int mc = data[offset] & 0xff;
		switch (mc) {
		case CEMILData.MC_LDATA_REQ:
		case CEMILData.MC_LDATA_CON:
		case CEMILData.MC_LDATA_IND:
			if (length < 26) {
				try {
					return new CEMILData(data, offset);
				}
				catch (final KNXFormatException e) {
					// fall-through and try if the extended cEMI works
				}
			}
			return new CEMILDataEx(data, offset);
		case CEMIDevMgmt.MC_PROPREAD_REQ:
		case CEMIDevMgmt.MC_PROPREAD_CON:
		case CEMIDevMgmt.MC_PROPWRITE_REQ:
		case CEMIDevMgmt.MC_PROPWRITE_CON:
		case CEMIDevMgmt.MC_PROPINFO_IND:
		case CEMIDevMgmt.MC_RESET_REQ:
		case CEMIDevMgmt.MC_RESET_IND:
			return new CEMIDevMgmt(data, offset, length);
		case CEMIBusMon.MC_BUSMON_IND:
			return new CEMIBusMon(data, offset, length);
		default:
			throw new KNXFormatException("cEMI msg code not supported", mc);
		}
	}

	/**
	 * Creates a new cEMI message with information provided by <code>original</code>,
	 * and adjusts it to match the supplied <code>msgCode</code> and <code>data</code>.
	 * <p>
	 * The message code has to correspond to the type of cEMI frame supplied with
	 * <code>original</code>. The byte length of data has to fit the cEMI frame type
	 * supplied with <code>original</code>.<br>
	 * The <code>data</code> argument varies according to the supplied message code. For
	 * L-Data frames, this is the tpdu, for busmonitor frames, this is the raw frame, for
	 * device management frames, this is the data part or error information.
	 * 
	 * @param msgCode the message code for the new cEMI frame
	 * @param data the data for the frame
	 * @param original the original frame providing all necessary information for the new
	 *        frame
	 * @return the new cEMI frame adjusted with message code and data
	 * @throws KNXFormatException if cEMI message code is unsupported or frame creation
	 *         failed
	 */
	public static CEMI create(final int msgCode, final byte[] data, final CEMI original)
		throws KNXFormatException
	{
		switch (msgCode) {
		case CEMILData.MC_LDATA_REQ:
		case CEMILData.MC_LDATA_CON:
		case CEMILData.MC_LDATA_IND:
			return create(msgCode, null, null, data, (CEMILData) original, false,
				((CEMILData) original).isRepetition());
		case CEMIDevMgmt.MC_PROPREAD_REQ:
		case CEMIDevMgmt.MC_PROPREAD_CON:
		case CEMIDevMgmt.MC_PROPWRITE_REQ:
		case CEMIDevMgmt.MC_PROPWRITE_CON:
		case CEMIDevMgmt.MC_PROPINFO_IND:
		case CEMIDevMgmt.MC_RESET_REQ:
		case CEMIDevMgmt.MC_RESET_IND: {
			final CEMIDevMgmt f = (CEMIDevMgmt) original;
			return new CEMIDevMgmt(msgCode, f.getObjectType(), f.getObjectInstance(), f
				.getPID(), f.getStartIndex(), f.getElementCount(), data);
		}
		case CEMIBusMon.MC_BUSMON_IND:
			final CEMIBusMon f = (CEMIBusMon) original;
			return CEMIBusMon.newWithStatus(f.getStatus(), f.getTimestamp(),
				f.getTimestampType() == CEMIBusMon.TYPEID_TIMESTAMP_EXT, data);
		default:
			throw new KNXFormatException("not supported cEMI msg code", msgCode);
		}
	}

	/**
	 * Creates a new cEMI L-Data message with information provided by
	 * <code>original</code>, and adjusts source and destination address to match the
	 * supplied addresses.
	 * <p>
	 * 
	 * @param src the new KNX source address for the message, use <code>null</code> to
	 *        use original address
	 * @param dst the new KNX destination address for the message, use <code>null</code>
	 *        to use original address
	 * @param original the original frame providing all missing information for the
	 *        adjusted message
	 * @param extended <code>true</code> to always created an extended frame,
	 *        <code>false</code> to create type according to <code>original</code>
	 * @return the new cEMI L-Data message adjusted with KNX addresses
	 */
	public static CEMILData create(final IndividualAddress src, final KNXAddress dst,
		final CEMILData original, final boolean extended)
	{
		return (CEMILData) create(0, src, dst, null, original, extended,
			original.isRepetition());
	}
	
	/**
	 * Creates a new cEMI L-Data message with information provided by
	 * <code>original</code>, and adjusts source and destination address to match the
	 * supplied addresses, and sets the repeat/is repeated frame indication.
	 * <p>
	 * 
	 * @param src the new KNX source address for the message, use <code>null</code> to
	 *        use original address
	 * @param dst the new KNX destination address for the message, use <code>null</code>
	 *        to use original address
	 * @param original the original frame providing all missing information for the
	 *        adjusted message
	 * @param extended <code>true</code> to always created an extended frame,
	 *        <code>false</code> to create type according to <code>original</code>
	 * @param repeat @see {@link CEMILData#isRepetition()}
	 * @return the new cEMI L-Data message adjusted with KNX addresses
	 */
	public static CEMILData create(final IndividualAddress src, final KNXAddress dst,
		final CEMILData original, final boolean extended, final boolean repeat)
	{
		return (CEMILData) create(0, src, dst, null, original, extended, repeat);
	}
	
	/**
	 * Creates a new cEMI message out of the supplied EMI frame.
	 * <p>
	 * 
	 * @param frame EMI frame
	 * @return the new cEMI message
	 * @throws KNXFormatException if no (valid) EMI structure was found or unsupported EMI
	 *         message code
	 */
	public static CEMI createFromEMI(final byte[] frame) throws KNXFormatException
	{
		// check for minimum frame length (i.e., a busmonitor frame)
		if (frame.length < 4)
			throw new KNXFormatException("EMI frame too short");
		final int mc = frame[0] & 0xff;
		if (mc == CEMIBusMon.MC_BUSMON_IND) {
			return CEMIBusMon.newWithStatus(frame[1] & 0xff, (frame[2] & 0xff) << 8
				| frame[3] & 0xff, false,
				DataUnitBuilder.copyOfRange(frame, 4, frame.length));
		}
		final Priority p = Priority.get(frame[1] >> 2 & 0x3);
		final boolean ack = (frame[1] & 0x02) != 0;
		final boolean c = (frame[1] & 0x01) != 0;
		final int dst = (frame[4] & 0xff) << 8 | frame[5] & 0xff;
		final KNXAddress a = (frame[6] & 0x80) != 0 ?
			(KNXAddress) new GroupAddress(dst) : new IndividualAddress(dst);
		final int hops = frame[6] >> 4 & 0x07;
		final int len = (frame[6] & 0x0f) + 1;
		final byte[] tpdu = DataUnitBuilder.copyOfRange(frame, 7, len + 7);
		// no long frames in EMI2
		return c ? new CEMILData(mc, new IndividualAddress(0), a, tpdu, p, c)
			: new CEMILData(mc, new IndividualAddress(0), a, tpdu, p, true, true, ack,
				hops);
	}
	
	/**
	 * Does a lazy copy of the supplied cEMI frame.
	 * <p>
	 * Only for cEMI frames which are <b>not</b> immutable a copy is created, for all
	 * other frames <code>original</code> is returned.
	 * 
	 * @param original the frame to copy
	 * @return the <code>original</code> frame if immutable, a copy of it otherwise
	 */
	public static CEMI copy(final CEMI original)
	{
		// on original == null we just return null, too
		if (original instanceof CEMILDataEx)
			return (CEMI) ((CEMILDataEx) original).clone();
		// all other are immutable
		return original;
	}
	
	// leave msgcode/src/dst/data null/0 to use from original
	private static CEMI create(final int msgCode, final IndividualAddress src,
		final KNXAddress dst, final byte[] data, final CEMILData original, final boolean ext,
		final boolean repeat)
	{
		final int mc = msgCode != 0 ? msgCode : original.getMessageCode();
		final IndividualAddress s = src != null ? src : original.getSource();
		final KNXAddress d = dst != null ? dst : original.getDestination();
		final byte[] content = data != null ? data : original.getPayload();
		if (original instanceof CEMILDataEx) {
			final CEMILDataEx f = (CEMILDataEx) original;
			final CEMILDataEx copy =
				new CEMILDataEx(mc, s, d, content, f.getPriority(), repeat, f
					.isDomainBroadcast(), f.isAckRequested(), f.getHopCount());
			// copy additional info
			final List l = f.getAdditionalInfo();
			for (final Iterator i = l.iterator(); i.hasNext();) {
				final CEMILDataEx.AddInfo info = (CEMILDataEx.AddInfo) i.next();
				copy.addAdditionalInfo(info.getType(), info.getInfo());
			}
			return copy;
		}
		if (ext)
			return new CEMILDataEx(mc, s, d, content, original.getPriority(), repeat,
				original.getHopCount());
		return new CEMILData(mc, s, d, content, original.getPriority(), repeat,
			original.getHopCount());
	}
}
