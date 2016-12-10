/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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
import java.util.List;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;

/**
 * A cEMI link layer data message (L-Data) supporting extended frame formats and cEMI additional information, with a
 * transport layer protocol data unit maximum of 255 bytes.
 * <p>
 * In contrast to CEMILData, objects of this L-Data type are mutable.
 *
 * @author B. Malinowsky
 */
public class CEMILDataEx extends CEMILData implements Cloneable
{
	/**
	 * Holds an additional info type with corresponding information data.
	 */
	public static class AddInfo
	{
		private final int type;
		private final byte[] data;

		/**
		 * Creates new wrapper for additional information.
		 *
		 * @param infoType additional information type ID
		 * @param info information data
		 */
		public AddInfo(final int infoType, final byte[] info)
		{
			if (info.length > 255)
				throw new KNXIllegalArgumentException("cEMI additional info exceeds maximum length of 255 bytes");
			if (infoType < ADDINFO_LENGTHS.length && info.length != ADDINFO_LENGTHS[infoType])
				throw new KNXIllegalArgumentException(
						"invalid length " + info.length + " for cEMI additional info type " + infoType);
			type = infoType;
			data = info;
		}

		/**
		 * Returns the additional information data wrapped by this type.
		 * <p>
		 *
		 * @return the data as byte array
		 */
		public final byte[] getInfo()
		{
			return data;
		}

		/**
		 * Returns the type of additional information (see ADDINFO_* constants in class
		 * CEMILDataEx).
		 * <p>
		 *
		 * @return type ID
		 */
		public final int getType()
		{
			return type;
		}
	}

	// public static final int ADDINFO_RESERVED = 0x00;

	/**
	 * Additional information type for PL medium information.
	 */
	public static final int ADDINFO_PLMEDIUM = 0x01;

	/**
	 * Additional information type for RF medium information.
	 */
	public static final int ADDINFO_RFMEDIUM = 0x02;
	// public static final int ADDINFO_BUSMON = 0x03;

	/**
	 * Additional information type for relative timestamp information.
	 */
	public static final int ADDINFO_TIMESTAMP = 0x04;

	/**
	 * Additional information type for time delay until sending information.
	 */
	public static final int ADDINFO_TIMEDELAY = 0x05;

	/**
	 * Additional information type for extended relative timestamp information.
	 */
	public static final int ADDINFO_TIMESTAMP_EXT = 0x06;

	/**
	 * Additional information type for BiBat information.
	 */
	public static final int ADDINFO_BIBAT = 0x07;

	private static final int ADDINFO_ESC = 0xFF;

	private static final int[] ADDINFO_LENGTHS = { 0, 2, 8, 1, 2, 4, 4, 2, };

	private byte[][] addInfo = new byte[10][];

	/**
	 * Creates a new L-Data message from a byte stream.
	 *
	 * @param data byte stream containing a cEMI L-Data message
	 * @param offset start offset of cEMI frame in <code>data</code>
	 * @param length length in bytes of the whole cEMI message in <code>data</code>
	 * @throws KNXFormatException if no (valid) frame was found
	 */
	public CEMILDataEx(final byte[] data, final int offset, final int length)
		throws KNXFormatException
	{
		if (data.length - offset < length || length < BASIC_LENGTH + 1)
			throw new KNXFormatException("buffer too short for frame");
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
		readMC(is);
		readAddInfo(is);
		readCtrlAndAddr(is);
		readPayload(is);
	}

	/**
	 * Creates a L-Data message with most control information set to default values.
	 * <p>
	 * The initialized message has send repetitions according to default medium behavior (for
	 * indication message this equals "not repeated frame"), broadcast is "don't care", acknowledge
	 * of request is default medium behavior, hop count is 6 and confirmation request is
	 * "don't care" in the control field.<br>
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application layer protocol
	 *        control information), i.e., the NPDU without the length field, tpdu.length &le; 255
	 * @param p message priority, priority set in the control field
	 */
	public CEMILDataEx(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p)
	{
		this(msgCode, src, dst, tpdu, p, true, true, false, 6);
	}

	/**
	 * Creates a L-Data message, mainly for confirmation.
	 * <p>
	 * The message hop count is set to 6, send repetitions according to default medium behavior,
	 * broadcast and acknowledge request are set to "don't care" in the control field.<br>
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application layer protocol
	 *        control information); i.e., the NPDU without the length field, tpdu.length &le; 255
	 * @param p message priority, priority set in the control field
	 * @param confirm confirm flag in the control field, <code>true</code> to set error,
	 *        <code>false</code> for no error
	 */
	public CEMILDataEx(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean confirm)
	{
		super(msgCode, src, dst, tpdu, p, confirm);
		// check extended frame
		if (tpdu.length > 16)
			ctrl1 &= ~0x80;
	}

	/**
	 * Creates a L-Data message with full customization for control information.
	 * <p>
	 * The confirmation flag of the control field is left out, since it is mutual exclusive with the
	 * rest of the control information and set to "don't care" (refer to
	 * {@link #CEMILDataEx(int, IndividualAddress, KNXAddress, byte[], Priority, boolean)}).
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application layer protocol
	 *        control information), i.e., the NPDU without the length field, tpdu.length &le; 255
	 * @param p message priority, priority set in the control field
	 * @param repeat for request messages send repetitions on the medium - <code>false</code> for do
	 *        not repeat if error, <code>true</code> for default repeat behavior; meaning of default
	 *        behavior on media:
	 *        <ul>
	 *        <li>RF: no repetitions</li>
	 *        <li>TP1, PL110: repetitions allowed</li>
	 *        </ul>
	 *        for indication message - <code>true</code> if is repeated frame, <code>false</code>
	 *        otherwise
	 * @param broadcast system / domain broadcast behavior, applicable on open media only:
	 *        <code>false</code> for system broadcast, <code>true</code> for broadcast; on closed
	 *        media set <code>true</code> for "don't care"
	 * @param ack acknowledge request, <code>true</code> if acknowledge is requested,
	 *        <code>false</code> for default behavior; meaning of default behavior on media:
	 *        <ul>
	 *        <li>TP1, PL110: acknowledge requested</li>
	 *        </ul>
	 * @param hopCount hop count starting value set in control field, in the range 0 &le; value &le; 7
	 */
	public CEMILDataEx(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean repeat, final boolean broadcast,
		final boolean ack, final int hopCount)
	{
		super(msgCode, src, dst, tpdu, p, repeat, broadcast, ack, hopCount);
		// check extended frame
		if (tpdu.length > 16)
			ctrl1 &= ~0x80;
	}

	/**
	 * Creates a L-Data message, mainly for TP1 media.
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application layer protocol
	 *        control information), i.e., the NPDU without the length field, tpdu.length &le; 255
	 * @param p message priority, priority set in the control field
	 * @param repeat for request message, send repetitions on the medium - <code>false</code> for do
	 *        not repeat if error, <code>true</code> for default repeat behavior; meaning of default
	 *        behavior on media:
	 *        <ul>
	 *        <li>RF: no repetitions</li>
	 *        <li>TP1, PL110: repetitions allowed</li>
	 *        </ul>
	 *        for indication message - <code>true</code> if is repeated frame, <code>false</code>
	 *        otherwise
	 * @param hopCount hop count starting value set in control field, in the range 0 &le; value
	 *        &le; 7
	 */
	public CEMILDataEx(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean repeat, final int hopCount)
	{
		this(msgCode, src, dst, tpdu, p, repeat, true, false, hopCount);
	}

	/**
	 * Adds additional information to the message.
	 * <p>
	 * It replaces additional information of the same type, if any was previously added.
	 *
	 * @param infoType type ID of additional information
	 * @param info additional information data
	 */
	public synchronized void addAdditionalInfo(final int infoType, final byte[] info)
	{
		if (infoType < 0 || infoType >= ADDINFO_ESC)
			throw new KNXIllegalArgumentException("info type out of range [0..254]");
		if (!checkAddInfoLength(infoType, info.length))
			throw new KNXIllegalArgumentException("wrong info data length, expected "
					+ ADDINFO_LENGTHS[infoType] + " bytes");
		putAddInfo(infoType, info);
	}

	/**
	 * Returns all additional information currently set.
	 * <p>
	 *
	 * @return a List with {@link AddInfo} objects
	 */
	public synchronized List<AddInfo> getAdditionalInfo()
	{
		final List<AddInfo> l = new ArrayList<>();
		for (int i = 0; i < addInfo.length; ++i)
			if (addInfo[i] != null)
				l.add(new AddInfo(i, addInfo[i].clone()));
		return l;
	}

	/**
	 * Returns additional information data corresponding to the supplied type ID, if it is contained
	 * in the message.
	 *
	 * @param infoType type ID of the request additional information
	 * @return additional information data or <code>null</code> if no such information is available
	 */
	public synchronized byte[] getAdditionalInfo(final int infoType)
	{
		if (infoType < addInfo.length && addInfo[infoType] != null)
			return addInfo[infoType].clone();
		return null;
	}

	@Override
	public int getStructLength()
	{
		return super.getStructLength() + getAddInfoLength();
	}

	/**
	 * Returns whether the message assembles an extended frame format.
	 * <p>
	 *
	 * @return <code>true</code> if this is an extended frame, <code>false</code> otherwise
	 */
	public synchronized boolean isExtendedFrame()
	{
		return (ctrl1 & 0x80) == 0;
	}

	/**
	 * Specifies the kind of broadcast to use for sending.
	 *
	 * @param domainOnly <code>true</code> for doing a broadcast only within the domain,
	 *        <code>false</code> for a system broadcast
	 */
	@Override
	public synchronized void setBroadcast(final boolean domainOnly)
	{
		super.setBroadcast(domainOnly);
	}

	/**
	 * Returns the kind of broadcast set for this message.
	 * <p>
	 * By default, <code>true</code> is returned, indicating "domain-only" broadcast on open media
	 * or "don't care" on closed media.
	 *
	 * @return <code>true</code> if broadcast only within domain or "don't care" mode,
	 *         <code>false</code> for system broadcast
	 */
	public synchronized boolean isDomainBroadcast()
	{
		return (ctrl1 & 0x10) != 0;
	}

	/**
	 * Removes the additional information with the supplied type ID.
	 *
	 * @param infoType type ID of additional information to remove
	 */
	public synchronized void removeAdditionalInfo(final int infoType)
	{
		if (infoType < addInfo.length)
			addInfo[infoType] = null;
	}

	@Override
	public final synchronized void setHopCount(final int hobbes)
	{
		super.setHopCount(hobbes);
	}

	@Override
	public final void setPriority(final Priority p)
	{
		super.setPriority(p);
	}

	@Override
	public synchronized byte[] toByteArray()
	{
		return super.toByteArray();
	}

	@Override
	public String toString()
	{
		final String s = super.toString();
		final StringBuffer buf = new StringBuffer(s.length() + 30);
		final int split = s.indexOf(',');
		buf.append(s.substring(0, split + 1));
		for (int i = 0; i < addInfo.length; ++i) {
			final byte[] info = addInfo[i];
			if (info != null)
				if (i == ADDINFO_PLMEDIUM) {
					buf.append(" domain ");
					buf.append(Integer.toHexString((info[0] & 0xff) << 8 | info[1] & 0xff));
				}
				else if (i == ADDINFO_RFMEDIUM)
					buf.append(" ").append(new RFMediumInfo(info, !isDomainBroadcast())).append(",");
				else if (i == ADDINFO_TIMESTAMP) {
					buf.append(" timestamp ");
					buf.append((info[0] & 0xff) << 8 | info[1] & 0xff);
				}
				else if (i == ADDINFO_TIMEDELAY)
					buf.append(" timedelay ").append(toLong(info));
				else if (i == ADDINFO_TIMESTAMP_EXT)
					buf.append(" ext.timestamp ").append(toLong(info));
				else if (i == ADDINFO_BIBAT)
					buf.append(" bibat 0x").append(DataUnitBuilder.toHex(info, " "));
				else
					buf.append(" type ").append(i).append(" 0x").append(DataUnitBuilder.toHex(info, ""));
		}
		buf.append(s.substring(split + 1));
		return buf.toString();
	}

	@Override
	public Object clone()
	{
		try {
			final CEMILDataEx clone = (CEMILDataEx) super.clone();
			clone.data = getPayload();
			// the byte arrays with additional info content are used internal only
			// and don't need to be cloned
			clone.addInfo = clone.addInfo.clone();
			return clone;
		}
		catch (final CloneNotSupportedException ignore) {
			throw new UnsupportedOperationException("cloning cEMI L-Data", ignore);
		}
	}

	@Override
	void readAddInfo(final ByteArrayInputStream is) throws KNXFormatException
	{
		final int ail = is.read();
		if (ail == 0)
			return;
		if (ail > is.available())
			throw new KNXFormatException("additional info length exceeds frame length", ail);
		int remaining = ail;
		for (; remaining > 0; remaining -= 2) {
			if (remaining < 1)
				throw new KNXFormatException("lack of space for additional info");
			final int type = is.read();
			final int len = is.read();
			if (len > remaining || !checkAddInfoLength(type, len))
				throw new KNXFormatException("additional info type 0x" + Integer.toHexString(type)
						+ " with invalid length", len);
			final byte[] info = new byte[len];
			is.read(info, 0, len);
			putAddInfo(type, info);
			remaining -= len;
		}
	}

	@Override
	void readPayload(final ByteArrayInputStream is) throws KNXFormatException
	{
		int len = is.read();
		// length field is 0 in RF frames
		if (len == 0)
			len = is.available();
		else {
			++len;
			if (len > is.available())
				throw new KNXFormatException("length of tpdu exceeds available data", len);
		}
		data = new byte[len];
		is.read(data, 0, len);
	}

	/**
	 * Writes all additional information to <code>os</code>.
	 *
	 * @param os the output stream
	 */
	@Override
	synchronized void writeAddInfo(final ByteArrayOutputStream os)
	{
		os.write(getAddInfoLength());
		for (int i = 0; i < addInfo.length; i++)
			if (addInfo[i] != null) {
				os.write(i);
				os.write(addInfo[i].length);
				os.write(addInfo[i], 0, addInfo[i].length);
			}
	}

	@Override
	void writePayload(final ByteArrayOutputStream os)
	{
		// RF frames don't use NPDU length field
		os.write(addInfo[ADDINFO_RFMEDIUM] != null ? 0 : data.length - 1);
		os.write(data, 0, data.length);
	}

	@Override
	boolean isValidTPDULength(final byte[] tpdu)
	{
		// value of length field is limited to 254, 255 is reserved as ESC code
		return tpdu.length <= 255;
	}

	private static boolean checkAddInfoLength(final int infoType, final int len)
	{
		if (len > 255)
			throw new KNXIllegalArgumentException(
					"additional info exceeds maximum length of 255 bytes");
		if (infoType < ADDINFO_LENGTHS.length && len != ADDINFO_LENGTHS[infoType])
			return false;
		return true;
	}

	private synchronized int getAddInfoLength()
	{
		int len = 0;
		for (int i = 0; i < addInfo.length; i++)
			if (addInfo[i] != null)
				len += 2 + addInfo[i].length;
		return len;
	}

	private void putAddInfo(final int infoType, final byte[] info)
	{
		if (addInfo.length <= infoType) {
			final byte[][] newInfo = new byte[Math.max(2 * addInfo.length, infoType + 1)][];
			System.arraycopy(addInfo, 0, newInfo, 0, addInfo.length);
			addInfo = newInfo;
		}
		addInfo[infoType] = info.clone();
	}

	private long toLong(final byte[] data)
	{
		final long l = (data[0] & 0xff) << 8 | data[1] & 0xff;
		return l << 16 | (data[2] & 0xff) << 8 | data[3] & 0xff;
	}
}
