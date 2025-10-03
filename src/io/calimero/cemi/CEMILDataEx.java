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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.LteHeeTag;
import io.calimero.Priority;

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
	private final List<AdditionalInfo> addInfo = Collections.synchronizedList(new ArrayList<>());


	public static CEMILDataEx newLte(final int msgCode, final IndividualAddress src, final LteHeeTag tag,
			final byte[] tpdu, final Priority p, final boolean repeat, final boolean domainBroadcast, final boolean ack,
			final int hopCount) {
		final var ldata = new CEMILDataEx(msgCode, src, tag.toGroupAddress(), tpdu, p, repeat, domainBroadcast, ack,
				hopCount);
		// LTE is always extended frame
		ldata.ctrl1 &= ~0x80;
		// adjust cEMI Ext Ctrl Field with frame format parameters for LTE
		final int lteExtAddrType = 0x04; // LTE-HEE extended address type
		ldata.ctrl2 |= lteExtAddrType;
		ldata.ctrl2 |= tag.type().ordinal();
		return ldata;
	}

	/**
	 * Creates a new L-Data message from a byte stream.
	 *
	 * @param data byte stream containing a cEMI L-Data message
	 * @param offset start offset of cEMI frame in {@code data}
	 * @param length length in bytes of the whole cEMI message in {@code data}
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
	 * Creates an L-Data message with most control information set to default values.
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
	 * Creates an L-Data message, mainly for confirmation.
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
	 * @param confirm confirm flag in the control field, {@code true} to set error,
	 *        {@code false} for no error
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
	 * Creates an L-Data message with full customization for control information.
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
	 * @param repeat for request messages send repetitions on the medium - {@code false} for do
	 *        not repeat if error, {@code true} for default repeat behavior; meaning of default
	 *        behavior on media:
	 *        <ul>
	 *        <li>RF: no repetitions</li>
	 *        <li>TP1, PL110: repetitions allowed</li>
	 *        </ul>
	 *        for indication message - {@code true} if is repeated frame, {@code false}
	 *        otherwise
	 * @param domainBroadcast system / domain broadcast behavior:
	 *        {@code true} for domain broadcast, {@code false} for system broadcast (on TP1 medium,
	 *        set {@code true} for "don't care")
	 * @param ack acknowledge request, {@code true} if acknowledge is requested,
	 *        {@code false} for default behavior; meaning of default behavior on media:
	 *        <ul>
	 *        <li>TP1, PL110: acknowledge requested</li>
	 *        </ul>
	 * @param hopCount hop count starting value set in control field, in the range 0 &le; value &le; 7
	 */
	public CEMILDataEx(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean repeat, final boolean domainBroadcast,
		final boolean ack, final int hopCount)
	{
		super(msgCode, src, dst, tpdu, p, repeat, domainBroadcast, ack, hopCount);
		// check extended frame
		if (tpdu.length > 16)
			ctrl1 &= ~0x80;
	}

	/**
	 * Creates an L-Data message, mainly for TP1 media.
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application layer protocol
	 *        control information), i.e., the NPDU without the length field, tpdu.length &le; 255
	 * @param p message priority, priority set in the control field
	 * @param repeat for request message, send repetitions on the medium - {@code false} for do
	 *        not repeat if error, {@code true} for default repeat behavior; meaning of default
	 *        behavior on media:
	 *        <ul>
	 *        <li>RF: no repetitions</li>
	 *        <li>TP1, PL110: repetitions allowed</li>
	 *        </ul>
	 *        for indication message - {@code true} if is repeated frame, {@code false}
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
		rhs.additionalInfo().forEach(info -> addInfo.add(AdditionalInfo.of(info.type(), info.info())));
	}

	/**
	 * {@return the mutable list with cEMI additional info, empty list if no additional info}
	 */
	public List<AdditionalInfo> additionalInfo() {
		return addInfo;
	}

	/**
	 * Returns additional information data corresponding to the supplied type ID, if it is contained
	 * in the message.
	 *
	 * @param infoType type ID of the request additional information
	 * @return additional information data or {@code null} if no such information is available
	 */
	public synchronized byte[] getAdditionalInfo(final int infoType)
	{
		for (final var info : addInfo) {
			if (info.type() == infoType)
				return info.info();
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
	 * @return {@code true} if this is an extended frame, {@code false} otherwise
	 */
	public synchronized boolean isExtendedFrame()
	{
		return (ctrl1 & 0x80) == 0;
	}

	/**
	 * Specifies the kind of broadcast to use for sending.
	 *
	 * @param domainOnly {@code true} for doing a broadcast only within the domain,
	 *        {@code false} for a system broadcast
	 */
	@Override
	public synchronized void setBroadcast(final boolean domainOnly)
	{
		super.setBroadcast(domainOnly);
	}

	/**
	 * Returns the kind of broadcast set for this message.
	 * <p>
	 * By default, {@code true} is returned, indicating "domain-only" broadcast on open media
	 * or "don't care" on closed media.
	 *
	 * @return {@code true} if broadcast only within domain or "don't care" mode,
	 *         {@code false} for system broadcast
	 */
	public synchronized boolean isDomainBroadcast()
	{
		return (ctrl1 & 0x10) != 0;
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
	public boolean equals(final Object o) {
		return o instanceof final CEMILDataEx ldataEx && super.equals(o) && Objects.equals(addInfo, ldataEx.addInfo);
	}

	@Override
	public int hashCode() { return Objects.hash(super.hashCode(), addInfo); }

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
			buf.append(LteHeeTag.from(ctrl2, (GroupAddress) getDestination())).append(" LTE");
		else
			buf.append(getDestination());

		final int svcStart = s.indexOf(' ');
		final int split = s.indexOf(',');
		buf.append(s, svcStart, split + 1);

		for (final var infoField : addInfo) {
			buf.append(" ");
			buf.append(infoField);
			buf.append(",");
		}
		buf.append(s.substring(split + 1));
		return buf.toString();
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
				addInfo.add(AdditionalInfo.of(type, info));
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
	 * Writes all additional information to {@code os}.
	 *
	 * @param os the output stream
	 */
	@Override
	void writeAddInfo(final ByteArrayOutputStream os)
	{
		synchronized (addInfo) {
			os.write(getAddInfoLength());
			addInfo.sort(Comparator.comparingInt(AdditionalInfo::type));
			for (final var infoField : addInfo) {
				os.write(infoField.type());
				final byte[] info = infoField.info();
				os.write(info.length);
				os.write(info, 0, info.length);
			}
		}
	}

	@Override
	void writePayload(final ByteArrayOutputStream os)
	{
		// RF frames don't use NPDU length field
//		final boolean rf = addInfo.stream().anyMatch(info -> info.type() == AdditionalInfo.RfMedium);
//		os.write(rf ? 0 : data.length - 1);
		os.write(data.length - 1);
		os.write(data, 0, data.length);
	}

	@Override
	boolean isValidTPDULength(final byte[] tpdu)
	{
		// value of length field is limited to 254, 255 is reserved as ESC code
		return tpdu.length <= 255;
	}

	private int getAddInfoLength()
	{
		int len = 0;
		synchronized (addInfo) {
			for (final var field : addInfo)
				len += 2 + field.info().length;
		}
		return len;
	}
}
