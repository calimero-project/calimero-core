/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2018 B. Malinowsky

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
		 * Creates new cEMI additional information using the type ID and a copy of the supplied info data.
		 *
		 * @param infoType additional information type ID
		 * @param info information data
		 */
		public AddInfo(final int infoType, final byte[] info)
		{
			if (infoType < 0 || infoType >= ADDINFO_ESC)
				throw new KNXIllegalArgumentException("cEMI additional info type " + infoType + " out of range [0..254]");
			if (info.length > 255)
				throw new KNXIllegalArgumentException("cEMI additional info of type " + infoType + " exceeds maximum length of 255 bytes");
			if (infoType < ADDINFO_LENGTHS.length && info.length != ADDINFO_LENGTHS[infoType])
				throw new KNXIllegalArgumentException(
						"invalid length " + info.length + " for cEMI additional info type " + infoType);
			type = infoType;
			data = info.clone();
		}

		/**
		 * Returns the additional information associated with this type.
		 *
		 * @return the data as byte array
		 */
		public final byte[] getInfo()
		{
			return data.clone();
		}

		/**
		 * Returns the type of additional information (see ADDINFO_* constants in class CEMILDataEx).
		 *
		 * @return type ID
		 */
		public final int getType()
		{
			return type;
		}

		@Override
		public String toString() {
			switch (type) {
			case ADDINFO_PLMEDIUM:
				return "PL DoA " + (Integer.toHexString((data[0] & 0xff) << 8 | (data[1] & 0xff)));
			case ADDINFO_RFMEDIUM:
				return new RFMediumInfo(data, false).toString(); // we default to domain broadcast
			case ADDINFO_TIMESTAMP:
				return "timestamp " + ((data[0] & 0xff) << 8 | (data[1] & 0xff));
			case ADDINFO_TIMEDELAY:
				return "timedelay " + toLong(data);
			case ADDINFO_TIMESTAMP_EXT:
				return "ext.timestamp " + toLong(data);
			case ADDINFO_BIBAT:
				return "BiBat 0x" + DataUnitBuilder.toHex(data, " ");
			default:
				return "type " + type + " = 0x" + DataUnitBuilder.toHex(data, "");
			}
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

	private static final int[] ADDINFO_LENGTHS = { 0, 2, 8, 1, 2, 4, 4, 2, 4, 3 };

	private final List<AddInfo> addInfo = Collections.synchronizedList(new ArrayList<>());

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

	private CEMILDataEx(final CEMILDataEx rhs) {
		super(rhs.getMessageCode(), rhs.getSource(), rhs.getDestination(), rhs.getPayload(), rhs.getPriority(), rhs.isRepetition(),
				!rhs.isSystemBroadcast(), rhs.isAckRequested(), rhs.getHopCount());
		ctrl1 = rhs.ctrl1;
		ctrl2 |= rhs.ctrl2 & 0x0f;
		rhs.additionalInfo().forEach(info -> addInfo.add(new AddInfo(info.type, info.data)));
	}

	/**
	 * @return mutable list with cEMI additional info, empty list if no additional info
	 */
	public List<AddInfo> additionalInfo() {
		return addInfo;
	}

	/**
	 * @deprecated Use {@link #additionalInfo()}.
	 * @param infoType type ID of additional information
	 * @param info additional information data
	 */
	@Deprecated
	public synchronized void addAdditionalInfo(final int infoType, final byte[] info)
	{
		if (infoType < 0 || infoType >= ADDINFO_ESC)
			throw new KNXIllegalArgumentException("info type out of range [0..254]");
		if (!checkAddInfoLength(infoType, info.length))
			throw new KNXIllegalArgumentException("wrong info data length, expected " + ADDINFO_LENGTHS[infoType] + " bytes");
		addInfo.add(new AddInfo(infoType, info));
	}

	/**
	 * @deprecated Use {@link #additionalInfo()}.
	 * @return a List with {@link AddInfo} objects
	 */
	@Deprecated
	public synchronized List<AddInfo> getAdditionalInfo()
	{
		return new ArrayList<>(addInfo);
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
		for (final AddInfo info : addInfo) {
			if (info.type == infoType)
				return info.getInfo();
		}
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
	 * @deprecated Use {@link #additionalInfo()}.removeIf(info -&gt; info.type == infoType).
	 * @param infoType type ID of additional information to remove
	 */
	@Deprecated
	public synchronized void removeAdditionalInfo(final int infoType)
	{
		addInfo.removeIf(info -> info.type == infoType);
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
		final StringBuilder buf = new StringBuilder(s.length() + 50);
		buf.append(getSource()).append("->");
		// check LTE for destination
		if ((ctrl2 & 0x04) == 0x04)
			buf.append(lteTag(ctrl2, getDestination())).append(" LTE");
		else
			buf.append(getDestination());

		final int svcStart = s.indexOf(' ');
		final int split = s.indexOf(',');
		buf.append(s.substring(svcStart, split + 1));

		for (final AddInfo addInfo : addInfo) {
			buf.append(" ");
			if (addInfo.type == ADDINFO_RFMEDIUM)
				buf.append(new RFMediumInfo(addInfo.data, !isDomainBroadcast()));
			else
				buf.append(addInfo);
			buf.append(",");
		}
		buf.append(s.substring(split + 1));
		return buf.toString();
	}

	private static String lteTag(final int extFormat, final KNXAddress dst) {
		// LTE-HEE bits 1 and 0 contain the extension of the group address
		final int ext = extFormat & 0b11;
		final int rawAddress = dst.getRawAddress();
		if (rawAddress == 0)
			return "broadcast";

		// geographical tags: Apartment/Room/...
		if (ext <= 1) {
			final int aptFloor = (ext << 6) | ((rawAddress & 0b1111110000000000) >> 10);
			final int room = (rawAddress & 0b1111110000) >> 4;
			final int subzone = rawAddress & 0b1111;
			return (aptFloor == 0 ? "*" : aptFloor) + "/" + (room == 0 ? "*" : room) + "/"
					+ (subzone == 0 ? "*" : subzone);
		}
		// application specific tags
		if (ext == 2) {
			final int domain = rawAddress & 0xf000;
			if (domain == 0) {
				// TODO improve output format for domain 0
				final int mapping = (rawAddress >> 5);
				final int producer = (rawAddress >> 5) & 0xf;
				final int zone = rawAddress & 0x1f;
				if (mapping < 7) {
					// distribution (segments or zones)
					final String[] zones = { "", "D HotWater", "D ColdWater", "D Vent", "DHW", "Outside", "Calendar" };
					return zone + " (Z HVAC " + zones[mapping] + ")";
				}
				// producers and their zones
				if ((mapping & 0x70) == 0x10)
					return producer + "/" + zone + " (P/Z HVAC HotWater)";
				if ((mapping & 0x70) == 0x20)
					return producer + "/" + zone + " (P/Z HVAC ColdWater)";

				final String s = String.format("%8s", Integer.toBinaryString(rawAddress & 0xfff)).replace(' ', '0');
				return "0b" + s + " (HVAC)";
			}
			return domain + "/0x" + Integer.toHexString(rawAddress & 0xfff) + " (app)";
		}
		// ext = 3, unassigned (peripheral) tags & broadcast
		return "0x" + Integer.toHexString(rawAddress & 0xfff) + " (?)";
	}

	@Override
	public CEMILDataEx clone()
	{
		return new CEMILDataEx(this);
	}

	@Override
	void readAddInfo(final ByteArrayInputStream is) throws KNXFormatException
	{
		final int ail = is.read();
		if (ail == 0)
			return;
		if (ail > is.available())
			throw new KNXFormatException("additional info length exceeds frame length", ail);
		for (int remaining = ail; remaining > 0; remaining -= 2) {
			if (remaining < 3)
				throw new KNXFormatException("invalid additional info, remaining length " + remaining + " < 3 bytes");
			final int type = is.read();
			final int len = is.read();
			if (len > remaining)
				throw new KNXFormatException("additional info length of type " + type + " exceeds info block", len);
			final byte[] info = new byte[len];
			is.read(info, 0, len);
			try {
				addInfo.add(new AddInfo(type, info));
			}
			catch (final KNXIllegalArgumentException e) {
				throw new KNXFormatException(e.getMessage());
			}
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
	void writeAddInfo(final ByteArrayOutputStream os)
	{
		synchronized (addInfo) {
			os.write(getAddInfoLength());
			addInfo.sort((lhs, rhs) -> lhs.type - rhs.type);
			for (final AddInfo info : addInfo) {
				os.write(info.type);
				os.write(info.data.length);
				os.write(info.data, 0, info.data.length);
			}
		}
	}

	@Override
	void writePayload(final ByteArrayOutputStream os)
	{
		// RF frames don't use NPDU length field
		final boolean rf = addInfo.stream().anyMatch(info -> info.type == ADDINFO_RFMEDIUM);
		os.write(rf ? 0 : data.length - 1);
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

	private int getAddInfoLength()
	{
		synchronized (addInfo) {
			return addInfo.stream().mapToInt(info -> 2 + info.data.length).sum();
		}
	}

	private static long toLong(final byte[] data)
	{
		final long l = (long) (data[0] & 0xff) << 8 | (data[1] & 0xff);
		return l << 16 | (data[2] & 0xff) << 8 | (data[3] & 0xff);
	}
}
