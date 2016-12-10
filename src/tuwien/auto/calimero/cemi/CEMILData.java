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

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Priority;

/**
 * A cEMI link layer data message (L-Data).
 * <p>
 * Only standard frame formats are supported, with a transport layer protocol data unit of
 * 16 bytes maximum. Additional information in the message structure is not supported.
 * <p>
 * Objects of this L-Data type are immutable.
 *
 * @author B. Malinowsky
 */
public class CEMILData implements CEMI
{
	// Note: RF and PL frame type is not supported here at all, since additional info
	// fields are needed and RF is extended frame only, anyway

	/**
	 * Message code for L-Data request, code = {@value #MC_LDATA_REQ}.
	 */
	public static final int MC_LDATA_REQ = 0x11;

	/**
	 * Message code for L-Data confirmation, code = {@value #MC_LDATA_CON}.
	 */
	public static final int MC_LDATA_CON = 0x2E;

	/**
	 * Message code for L-Data indication, code = {@value #MC_LDATA_IND}.
	 */
	public static final int MC_LDATA_IND = 0x29;

	static final int BASIC_LENGTH = 9;

	/**
	 * Message code of this message.
	 */
	protected int mc;

	// all externally configurable ctrl parameters:
	// repeat priority ack confirm hop count (and broadcast in subtype)

	/**
	 * Control field 1, the lower 8 bits contain control information.
	 */
	protected int ctrl1;

	/**
	 * Control field 2, the lower 8 bits contain control information.
	 */
	protected int ctrl2;

	byte[] data;

	private volatile Priority p;
	private IndividualAddress source;
	private KNXAddress dst;

	/**
	 * Creates a new L-Data message from a byte stream.
	 *
	 * @param data byte stream containing a cEMI L-Data message
	 * @param offset start offset of cEMI frame in <code>data</code>
	 * @param length length in bytes of the whole cEMI message in <code>data</code>
	 * @throws KNXFormatException if no (valid) frame was found or the provided frame is
	 *         not a standard frame
	 */
	public CEMILData(final byte[] data, final int offset, final int length) throws KNXFormatException
	{
		if (data.length - offset < length || length < BASIC_LENGTH + 1)
			throw new KNXFormatException("buffer too short for frame");
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
		readMC(is);
		readAddInfo(is);
		readCtrlAndAddr(is);
		if ((ctrl1 & 0x80) == 0)
			throw new KNXFormatException("only cEMI standard frame supported");
		readPayload(is);
	}

	/**
	 * Creates a L-Data message with most control information set to default values.
	 * <p>
	 * The initialized message has send repetitions according to default medium behavior
	 * (for indication message this equals "not repeated frame"), broadcast is "don't
	 * care", acknowledge of request is default medium behavior, hop count is 6 and
	 * confirmation request is "don't care" in the control field.<br>
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application
	 *        layer protocol control information), i.e., the NPDU without the length field,
	 *        tpdu.length &lt;= 16
	 * @param p message priority, priority set in the control field
	 */
	public CEMILData(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p)
	{
		this(msgCode, src, dst, tpdu, p, true, true, false, 6);
	}

	/**
	 * Creates a L-Data message, mainly for confirmation.
	 * <p>
	 * The message hop count is set to 6, send repetitions according to default medium
	 * behavior, broadcast and request acknowledge are set to "don't care" in the control
	 * field.<br>
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application
	 *        layer protocol control information); i.e., the NPDU without the length field,
	 *        tpdu.length &lt;= 16
	 * @param p message priority, priority set in the control field
	 * @param confirm confirm flag in the control field, <code>true</code> to set error,
	 *        <code>false</code> for no error
	 */
	public CEMILData(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean confirm)
	{
		this(msgCode, src, dst, tpdu, p, true, true, false, 6);
		setConfirmation(confirm);
	}

	/**
	 * Creates a L-Data message with full customization for control information.
	 * <p>
	 * The confirmation flag of the control field is left out, since it is mutual
	 * exclusive with the rest of the control information and set to "don't care" (refer
	 * to
	 * {@link #CEMILData(int, IndividualAddress, KNXAddress, byte[], Priority, boolean)}).
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application
	 *        layer protocol control information), i.e., the NPDU without the length field,
	 *        tpdu.length &lt;= 16
	 * @param p message priority, priority set in the control field
	 * @param repeat for request messages, send repetitions on the medium -
	 *        <code>false</code> for do not repeat if error, <code>true</code> for
	 *        default repeat behavior;<br>
	 *        meaning of default behavior on media:<br>
	 *        <ul>
	 *        <li>RF: no repetitions</li>
	 *        <li>TP1, PL110: repetitions allowed</li>
	 *        </ul>
	 *        for indication message - <code>true</code> if is repeated frame,
	 *        <code>false</code> otherwise
	 * @param broadcast system / domain broadcast behavior, applicable on open media only:
	 *        <code>false</code> for system broadcast, <code>true</code> for
	 *        broadcast; on closed media set <code>true</code> for "don't care"
	 * @param ack acknowledge request, <code>true</code> if acknowledge is requested,
	 *        <code>false</code> for default behavior;<br>
	 *        meaning of default behavior on media:<br>
	 *        <ul>
	 *        <li>TP1, PL110: acknowledge requested</li>
	 *        </ul>
	 * @param hopCount hop count starting value set in control field, in the range 0 &lt;=
	 *        value &lt;= 7
	 */
	protected CEMILData(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean repeat, final boolean broadcast,
		final boolean ack, final int hopCount)
	{
		// ctor used for these kinds with relevant ctrl flags:
		// .ind on PL110: repeat broadcast priority hop count
		// .req on PL110: repeat broadcast priority hop count

		if (msgCode != MC_LDATA_REQ && msgCode != MC_LDATA_CON && msgCode != MC_LDATA_IND)
			throw new KNXIllegalArgumentException("unknown L-Data message code " + Integer.toHexString(msgCode));
		mc = msgCode;
		source = src;
		this.dst = dst;

		// set standard frame
		ctrl1 |= 0x80;
		// set address type
		if (dst instanceof GroupAddress)
			ctrl2 |= 0x80;
		if (!isValidTPDULength(tpdu))
			throw new KNXIllegalArgumentException("maximum TPDU length is 16 in standard frame");
		data = tpdu.clone();
		setPriority(p);
		setRepeat(repeat);
		setBroadcast(broadcast);
		setAcknowledgeRequest(ack);
		setHopCount(hopCount);
	}

	/**
	 * Creates a L-Data message, mainly for TP1 media.
	 *
	 * @param msgCode a message code value specified in the L-Data type
	 * @param src individual address of source
	 * @param dst destination address
	 * @param tpdu data array, starting with the TPCI / APCI (transport / application
	 *        layer protocol control information), i.e., the NPDU without the length field,
	 *        tpdu.length &lt;= 16
	 * @param p message priority, priority set in the control field
	 * @param repeat for request message, send repetitions on the medium -
	 *        <code>false</code> for do not repeat if error, <code>true</code> for
	 *        default repeat behavior;<br>
	 *        meaning of default behavior on media:<br>
	 *        <ul>
	 *        <li>RF: no repetitions</li>
	 *        <li>TP1, PL110: repetitions allowed</li>
	 *        </ul>
	 *        for indication message - <code>true</code> if is repeated frame,
	 *        <code>false</code> otherwise
	 * @param hopCount hop count starting value set in control field, in the range 0 &lt;=
	 *        value &lt;= 7
	 */
	public CEMILData(final int msgCode, final IndividualAddress src, final KNXAddress dst,
		final byte[] tpdu, final Priority p, final boolean repeat, final int hopCount)
	{
		// ctor used for these kinds with relevant ctrl flags:
		// .req on TP1: repeat priority hop count
		// .ind on TP1: repeat priority hop count

		this(msgCode, src, dst, tpdu, p, repeat, true, false, hopCount);
	}

	CEMILData()
	{}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.cemi.CEMI#getMessageCode()
	 */
	@Override
	public final int getMessageCode()
	{
		return mc;
	}

	/**
	 * Returns the L-Data TPDU.
	 * <p>
	 * The returned array is the NPDU without the length field of the message structure,
	 * starting with the TPCI / APCI field.
	 *
	 * @return a copy of the TPDU as byte array
	 */
	@Override
	public final byte[] getPayload()
	{
		return data.clone();
	}

	/**
	 * Returns the KNX individual source address.
	 * <p>
	 *
	 * @return address as IndividualAddress
	 */
	public final IndividualAddress getSource()
	{
		return source;
	}

	/**
	 * Returns the KNX destination address.
	 * <p>
	 *
	 * @return destination address as KNXAddress
	 */
	public final KNXAddress getDestination()
	{
		return dst;
	}

	/**
	 * Returns the hop count set in the control information.
	 * <p>
	 * The hop count value is in the range 0 &lt;= value &lt;= 7.
	 *
	 * @return hop count as 3 bit value
	 */
	public final int getHopCount()
	{
		return (ctrl2 & 0x70) >> 4;
	}

	/**
	 * Returns the message priority.
	 * <p>
	 *
	 * @return used {@link Priority}
	 */
	public final Priority getPriority()
	{
		return p;
	}

	/**
	 * Returns the broadcast type of this message, applicable for open transmission media. By default, returns
	 * <code>false</code>, indicating domain-only broadcast on open media or "don't care" on closed media.
	 *
	 * @return <code>true</code> if system broadcast (open media), <code>false</code> for broadcast within domain or
	 *         "don't care"
	 */
	public final boolean isSystemBroadcast() {
		return (ctrl1 & 0x10) == 0;
	}

	/**
	 * Returns whether L2 acknowledge was requested.
	 * <p>
	 * This information is valid in L-Data requests and partially in L-Data indications;
	 * for L-Data confirmations the value behavior is undefined (it might have the same
	 * value like the corresponding request).
	 * <p>
	 * For requests the following returns apply:<br>
	 * If <code>true</code>, acknowledge was requested explicitly, <code>false</code>
	 * for "don't care" (default medium behavior).<br>
	 * Default behavior on media for L2 ack:
	 * <ul>
	 * <li>TP1, PL110: acknowledge requested</li>
	 * </ul>
	 * <p>
	 * For indication messages following media behavior applies:
	 * <ul>
	 * <li>TP1, PL110: unused, undefined value behavior</li>
	 * </ul>
	 *
	 * @return acknowledge request as boolean
	 */
	public final boolean isAckRequested()
	{
		return (ctrl1 & 0x02) != 0;
	}

	/**
	 * Returns whether frame repetition is requested, or this is a repeated frame.
	 * <p>
	 * For request messages, returns <code>false</code> for do not repeat if error,
	 * <code>true</code> for default repeat behavior.<br>
	 * Meaning of default behavior on media:
	 * <ul>
	 * <li>TP1, PL110: repetitions allowed</li>
	 * </ul>
	 * <p>
	 * For indication messages, returns <code>false</code> if this is not a repeated
	 * frame, <code>true</code> if repeated frame.
	 * <p>
	 * For L-Data confirmations the value behavior is undefined (it might have the same
	 * value like the corresponding request).
	 *
	 * @return repeat state as boolean
	 */
	public final boolean isRepetition()
	{
		// ind: flag 0 = repeated frame, 1 = not repeated
		if (mc == MC_LDATA_IND)
			return (ctrl1 & 0x20) == 0;
		// req, (con): flag 0 = do not repeat, 1 = default behavior
		return (ctrl1 & 0x20) == 0x20;
	}

	/**
	 * Returns if confirmation indicates success or error in a confirmation message.
	 * <p>
	 * If return is <code>true</code> (confirmation bit in control field is 0 for no
	 * error), the associated request message to this confirmation was transmitted
	 * successfully, <code>false</code> otherwise (confirmation bit in control field is
	 * 1 for error).<br>
	 * On messages types other than confirmation, this information is "don't care" and
	 * always returns <code>true</code>.
	 *
	 * @return the confirmation state as boolean
	 */
	public final boolean isPositiveConfirmation()
	{
		return (ctrl1 & 0x01) == 0;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.cemi.CEMI#getStructLength()
	 */
	@Override
	public int getStructLength()
	{
		return BASIC_LENGTH + data.length;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.cemi.CEMI#toByteArray()
	 */
	@Override
	public byte[] toByteArray()
	{
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write(mc);
		writeAddInfo(os);
		setCtrlPriority();
		os.write(ctrl1);
		os.write(ctrl2);
		byte[] buf = source.toByteArray();
		os.write(buf, 0, buf.length);
		buf = dst.toByteArray();
		os.write(buf, 0, buf.length);
		writePayload(os);
		return os.toByteArray();
	}

	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder();
		buf.append(source).append("->").append(dst);
		buf.append(" L_Data");
		buf.append(mc == MC_LDATA_IND ? ".ind" : mc == MC_LDATA_REQ ? ".req" : ".con");
		if (mc == MC_LDATA_CON)
			buf.append(isPositiveConfirmation() ? " (pos)" : " (neg)");
		buf.append(", ").append(p).append(" priority");
		buf.append(" hop count ").append(getHopCount());
		if (mc != MC_LDATA_CON) {
			if (isAckRequested())
				buf.append(" ack-request");
			if (isRepetition())
				buf.append(" repeat");
		}
		buf.append(", tpdu ").append(DataUnitBuilder.toHex(data, " "));
		return buf.toString();
	}

	void readAddInfo(final ByteArrayInputStream is) throws KNXFormatException
	{
		if (is.read() != 0)
			throw new KNXFormatException("cEMI frames with additional info not supported");
	}

	void readPayload(final ByteArrayInputStream is) throws KNXFormatException
	{
		final int len = is.read() + 1;
		if (len > is.available())
			throw new KNXFormatException("length of tpdu exceeds available data", len);
		data = new byte[len];
		is.read(data, 0, len);
	}

	/**
	 * Writes additional information to <code>os</code>.
	 * <p>
	 * This type does not support additional information; the additional info length is
	 * set to 0, indicating no additional information.
	 *
	 * @param os the output stream
	 */
	void writeAddInfo(final ByteArrayOutputStream os)
	{
		os.write(0);
	}

	void writePayload(final ByteArrayOutputStream os)
	{
		os.write(data.length - 1);
		os.write(data, 0, data.length);
	}

	boolean isValidTPDULength(final byte[] tpdu)
	{
		return tpdu.length <= 16;
	}

	void readCtrlAndAddr(final ByteArrayInputStream is)
	{
		ctrl1 = is.read();
		getCtrlPriority();
		ctrl2 = is.read();
		final byte[] addr = new byte[2];
		is.read(addr, 0, 2);
		source = new IndividualAddress(addr);
		is.read(addr, 0, 2);
		if ((ctrl2 & 0x80) != 0)
			dst = new GroupAddress(addr);
		else
			dst = new IndividualAddress(addr);
	}

	void readMC(final ByteArrayInputStream is) throws KNXFormatException
	{
		mc = is.read();
		if (mc != MC_LDATA_REQ && mc != MC_LDATA_CON && mc != MC_LDATA_IND)
			throw new KNXFormatException("msg code indicates no L-data frame", mc);
	}

	void setHopCount(final int hobbes)
	{
		if (hobbes < 0 || hobbes > 7)
			throw new KNXIllegalArgumentException("hop count out of range [0..7]");
		ctrl2 &= 0x8F;
		ctrl2 |= hobbes << 4;
	}

	void setPriority(final Priority priority)
	{
		p = priority;
	}

	void setBroadcast(final boolean domain)
	{
		if (domain)
			ctrl1 |= 0x10;
		else
			ctrl1 &= 0xEF;
	}

	private void getCtrlPriority()
	{
		final int bits = ctrl1 >> 2 & 0x03;
		p = bits == Priority.LOW.value ? Priority.LOW : bits == Priority.NORMAL.value
				? Priority.NORMAL : bits == Priority.SYSTEM.value ? Priority.SYSTEM
						: Priority.URGENT;
		// clear priority info in control field
		ctrl1 &= ~0xC;
	}

	private void setAcknowledgeRequest(final boolean ack)
	{
		if (ack)
			ctrl1 |= 0x02;
		else
			ctrl1 &= 0xFD;
	}

	private void setConfirmation(final boolean error)
	{
		if (error)
			ctrl1 |= 0x01;
		else
			ctrl1 &= 0xFE;
	}

	private void setCtrlPriority()
	{
		ctrl1 &= ~0xC;
		ctrl1 |= p.value << 2;
	}

	/**
	 * Sets the repeat flag in control field, using the message code type for decision.
	 *
	 * @param repeat <code>true</code> for a repeat request or repeated frame,
	 *        <code>false</code> otherwise
	 */
	private void setRepeat(final boolean repeat)
	{
		final boolean flag = mc == MC_LDATA_IND ? !repeat : repeat;
		if (flag)
			ctrl1 |= 0x20;
		else
			ctrl1 &= 0xDF;
	}
}
