/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2024 B. Malinowsky

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

package io.calimero.link.medium;

import java.io.ByteArrayInputStream;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXFormatException;
import io.calimero.LteHeeTag;
import io.calimero.Priority;

/**
 * Implementation for raw frames with common used functionality in L-data and L-polldata
 * frames.
 * <p>
 * For now, implementation is only done to accommodate reception of raw frame (contained
 * in byte arrays), not to build raw frames out of information parts required to assemble
 * a new raw frame.
 * <p>
 * Objects of this type are considered immutable.
 *
 * @author B. Malinowsky
 */
public abstract class RawFrameBase implements RawFrame
{
	/**
	 * Frame type identifier.
	 */
	protected int type;

	/** Domain address for PL. */
	protected byte[] doa;

	/**
	 * Source address.
	 */
	protected IndividualAddress src;

	/**
	 * Destination address.
	 */
	protected KNXAddress dst;

	/**
	 * Is this an extended ({@code true}) or a standard frame ({@code false}).
	 */
	protected boolean ext;

	/**
	 * Frame repetition flag.
	 */
	protected boolean repetition;

	/**
	 * Frame priority.
	 */
	protected Priority p;

	/**
	 * Frame hop count.
	 */
	protected int hopcount;

	/**
	 * Frame checksum.
	 */
	protected int fcs;

	/**
	 * Transport layer protocol data unit.
	 * <p>
	 * In L-polldata frame tpdu is not used.
	 */
	protected byte[] tpdu;

	private int eff; // extended frame format in extended ctrl field,


	public RawFrameBase() {}

	@Override
	public final int getFrameType()
	{
		return type;
	}

	/** {@return {@code true} if extended frame, {@code false} if standard frame} */
	public final boolean extended() { return ext; }

	/**
	 * {@return the KNX individual source address}
	 */
	public final IndividualAddress getSource()
	{
		return src;
	}

	/**
	 * {@return the KNX destination address}
	 */
	public final KNXAddress getDestination()
	{
		return dst;
	}

	/**
	 * {@return the message priority used for this frame}
	 */
	public final Priority getPriority()
	{
		return p;
	}

	/**
	 * {@return the hop count of this frame, in the range 0 &lt;= count &lt;= 7}
	 */
	public final int getHopcount()
	{
		return hopcount;
	}

	/**
	 * Returns whether frame repetition is requested, or this is a repeated frame.
	 * <p>
	 * A request for repetition or repeated frame is indicated with {@code true},
	 * otherwise {@code false} is returned.
	 *
	 * @return repeat state as boolean
	 */
	public final boolean isRepetition()
	{
		return repetition;
	}

	/**
	 * {@return a copy of the TPDU, if available, or <code>null</code> for L-polldata frames}
	 */
	public final byte[] getTPDU()
	{
		return tpdu == null ? null : tpdu.clone();
	}

	/**
	 * Returns the frame checksum as contained in the frame.
	 * <p>
	 * The returned checksum is taken from the frame "as is", it is not recalculated for
	 * validation nor checked for correctness.<br>
	 * The length and structure of the returned checksum depends on the communication
	 * medium.
	 *
	 * @return frame checksum
	 */
	public final int getChecksum()
	{
		return fcs;
	}

	@Override
	public String toString()
	{
		final int lteHeeExtAddrType = 0x04;
		final boolean lte = ext && (eff & lteHeeExtAddrType) == lteHeeExtAddrType;
		final var dstAddr = lte ? LteHeeTag.from(eff, (GroupAddress) dst) : dst;

		final StringBuilder sb = new StringBuilder();
		sb.append(src).append("->").append(dstAddr);
		sb.append(type == LDATA_FRAME ? " L-Data" : " L-Polldata").append(".req");
		if (ext)
			sb.append(" (ext)");
		sb.append(", ").append(p).append(" priority");
		if (repetition)
			sb.append(" repeat");
		sb.append(" FCS 0x").append(Integer.toHexString(fcs));
		return sb.toString();
	}

	ByteArrayInputStream asStream(final byte[] data, final int offset,
		final int minLength, final String type) throws KNXFormatException
	{
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, data.length - offset);
		final int avail = is.available();
		if (avail < minLength)
			throw new KNXFormatException("data too short for " + type + " frame", avail);
		return is;
	}

	/**
	 * Inits the basic fields for TP1 and PL110 L-Data format reading the input stream.
	 *
	 * @param is
	 * @param parseDoA parse domain address after control field
	 * @return length field value of the raw frame
	 * @throws KNXFormatException
	 */
	int init(final ByteArrayInputStream is, final boolean parseDoA) throws KNXFormatException
	{
		final int ctrl = is.read();
		// parse control field and check if valid
		if ((ctrl & 0x53) != 0x10)
			throw new KNXFormatException("invalid control field", ctrl);

		type = LDATA_FRAME;
		ext = (ctrl & 0x80) == 0;
		repetition = (ctrl & 0x20) == 0;
		p = Priority.get((ctrl >> 2) & 0x3);
		final int ctrle = ext ? readCtrlEx(is) : 0;
		eff = ctrle & 0x0f;

		if (parseDoA) {
			doa = new byte[2];
			is.read(doa, 0, 2);
		}

		src = new IndividualAddress((is.read() << 8) | is.read());
		final int addr = (is.read() << 8) | is.read();
		final int npci = is.read();
		final int len;
		if (ext) {
			hopcount = (ctrle & 0x70) >> 4;
			setDestination(addr, (ctrle & 0x80) != 0);
			len = npci;
		}
		else {
			hopcount = (npci & 0x70) >> 4;
			setDestination(addr, (npci & 0x80) != 0);
			len = npci & 0x0f;
		}
		return len;
	}

	void setDestination(final int addr, final boolean group)
	{
		dst = group ? new GroupAddress(addr) : new IndividualAddress(addr);
	}

	int readCtrlEx(final ByteArrayInputStream is)
	{
		return is.read();
	}
}
