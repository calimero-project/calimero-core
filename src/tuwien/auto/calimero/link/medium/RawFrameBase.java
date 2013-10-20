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

package tuwien.auto.calimero.link.medium;

import java.io.ByteArrayInputStream;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.exception.KNXFormatException;

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
	 * <p>
	 */
	protected int type;

	/**
	 * Source address.
	 * <p>
	 */
	protected IndividualAddress src;

	/**
	 * Destination address.
	 */
	protected KNXAddress dst;

	/**
	 * Is this an extended (<code>true</code>) or a standard frame (<code>false</code>).
	 * <p>
	 */
	protected boolean ext;

	/**
	 * Frame repetition flag.
	 * <p>
	 */
	protected boolean repetition;

	/**
	 * Frame priority.
	 * <p>
	 */
	protected Priority p;

	/**
	 * Frame hop count.
	 * <p>
	 */
	protected int hopcount;

	/**
	 * Frame checksum.
	 * <p>
	 */
	protected int fcs;

	/**
	 * Transport layer protocol data unit.
	 * <p>
	 * In L-polldata frame tpdu is not used.
	 */
	protected byte[] tpdu;

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.link.medium.RawFrame#getFrameType()
	 */
	public final int getFrameType()
	{
		return type;
	}

	/**
	 * Returns the KNX individual source address.
	 * <p>
	 * 
	 * @return address of type IndividualAddress
	 */
	public final IndividualAddress getSource()
	{
		return src;
	}

	/**
	 * Returns the KNX destination address.
	 * <p>
	 * 
	 * @return destination address of type KNXAddress
	 */
	public final KNXAddress getDestination()
	{
		return dst;
	}

	/**
	 * Returns the message priority used for this frame.
	 * <p>
	 * 
	 * @return the used Priority
	 */
	public final Priority getPriority()
	{
		return p;
	}

	/**
	 * Returns the hop count of this frame.
	 * <p>
	 * 
	 * @return hop count in the range 0 &lt;= count &lt;= 7
	 */
	public final int getHopcount()
	{
		return hopcount;
	}

	/**
	 * Returns whether frame repetition is requested, or this is a repeated frame.
	 * <p>
	 * A request for repetition or repeated frame is indicated with <code>true</code>,
	 * otherwise <code>false</code> is returned.
	 * 
	 * @return repeat state as boolean
	 */
	public final boolean isRepetition()
	{
		return repetition;
	}

	/**
	 * Returns a copy of the TPDU, if available.
	 * <p>
	 * 
	 * @return tpdu as byte array or <code>null</code> for L-polldata frames
	 */
	public final byte[] getTPDU()
	{
		return tpdu == null ? null : (byte[]) tpdu.clone();
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		final StringBuffer sb = new StringBuffer();
		sb.append(type == LDATA_FRAME ? "L-Data" : "L-Polldata").append(".req ");
		sb.append(ext ? "(ext)" : "(std)");
		sb.append(" from ").append(src).append(" to ").append(dst);
		sb.append(", ").append(p).append(" priority");
		if (repetition)
			sb.append(" repeat");
		sb.append(" fcs 0x").append(Integer.toHexString(fcs));
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
	 * <p>
	 * 
	 * @throws KNXFormatException
	 */
	int init(final ByteArrayInputStream is) throws KNXFormatException
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
		dst = group ? (KNXAddress) new GroupAddress(addr) : new IndividualAddress(addr);
	}

	int readCtrlEx(final ByteArrayInputStream is) throws KNXFormatException
	{
		final int ctrle = is.read();
		if ((ctrle & 0xf) != 0)
			throw new KNXFormatException("LTE-HEE frame not supported");
		return ctrle;
	}
}
