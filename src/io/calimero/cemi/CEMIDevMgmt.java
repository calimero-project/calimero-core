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
import java.util.BitSet;
import java.util.HexFormat;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.ReturnCode;

/**
 * A cEMI device management message.
 * Objects of this type are immutable.
 *
 * @author B. Malinowsky
 */
public class CEMIDevMgmt implements CEMI
{
	/**
	 * Set of error codes for device management error handling.
	 */
	public static final class ErrorCodes
	{
		/**
		 * Error code: unknown error, used in negative read/write message confirmations.
		 */
		public static final int UNSPECIFIED_ERROR = 0x00;

		/**
		 * Error code: write value not allowed (used if {@link #OUT_OF_MAXRANGE} and
		 * {@link #OUT_OF_MINRANGE} are not appropriate), used in negative write message
		 * confirmations.
		 */
		public static final int OUT_OF_RANGE = 0x01;

		/**
		 * Error code: write value too high, used in negative write message confirmations.
		 */
		public static final int OUT_OF_MAXRANGE = 0x02;

		/**
		 * Error code: write value too low, used in negative write message confirmations.
		 */
		public static final int OUT_OF_MINRANGE = 0x03;

		/**
		 * Error code: memory can not be written or only with fault(s), used in negative
		 * write messages confirmations.
		 */
		public static final int MEMORY_ERROR = 0x04;

		/**
		 * Error code: write access to a 'read only' or a write protected property, used
		 * in negative write request message confirmations.
		 */
		public static final int READ_ONLY = 0x05;

		/**
		 * Error code: command not valid or not supported, used in negative write message
		 * confirmations.
		 */
		public static final int ILLEGAL_COMMAND = 0x06;

		/**
		 * Error code: read or write access to an non-existing property, used in negative
		 * read/write message confirmations.
		 */
		public static final int VOID_DP = 0x07;

		/**
		 * Error code: write access with a wrong data type (datapoint length), used in
		 * negative write message confirmations.
		 */
		public static final int TYPE_CONFLICT = 0x08;

		/**
		 * Error code: read or write access to a non-existing property array index, used
		 * in negative read/write message confirmations.
		 */
		public static final int PROP_INDEX_RANGE_ERROR = 0x09;

		/**
		 * Error code: the property exists but can at this moment not be written with a new value,
		 * used in negative write message confirmations.
		 */
		public static final int VALUE_NOT_WRITEABLE_NOW = 0x0A;

		// enforce non-instantiability
		private ErrorCodes() {}
	}

	/**
	 * Message code for property read request, code = {@value #MC_PROPREAD_REQ}.
	 */
	public static final int MC_PROPREAD_REQ = 0xFC;
	/**
	 * Message code for property read confirmation, code = {@value #MC_PROPREAD_CON}.
	 */
	public static final int MC_PROPREAD_CON = 0xFB;
	/**
	 * Message code for property write request, code = {@value #MC_PROPWRITE_REQ}.
	 */
	public static final int MC_PROPWRITE_REQ = 0xF6;
	/**
	 * Message code for property write confirmation, code = {@value #MC_PROPWRITE_CON}.
	 */
	public static final int MC_PROPWRITE_CON = 0xF5;
	/**
	 * Message code for property info indication, code = {@value #MC_PROPINFO_IND}.
	 */
	public static final int MC_PROPINFO_IND = 0xF7;
	/**
	 * Message code for function property command request, code = {@value #MC_FUNCPROP_CMD_REQ}.
	 */
	public static final int MC_FUNCPROP_CMD_REQ = 0xF8;
	/**
	 * Message code for function property status read request, code = {@value #MC_FUNCPROP_READ_REQ}.
	 */
	public static final int MC_FUNCPROP_READ_REQ = 0xF9;
	/**
	 * Message code for function property command or status read confirmation, code = {@value #MC_FUNCPROP_CON}.
	 */
	public static final int MC_FUNCPROP_CON = 0xFA;
	/**
	 * Message code for property reset request, code = {@value #MC_RESET_REQ}.
	 */
	public static final int MC_RESET_REQ = 0xF1;
	/**
	 * Message code for property reset indication, code = {@value #MC_RESET_IND}.
	 */
	public static final int MC_RESET_IND = 0xF0;


	private static final BitSet msgCodes;
	private static final int MC_OFFSET = 0xF0;

	private static final byte[] empty = new byte[0];

	private static final String[] errors = new String[] {
		"unspecified Error (unknown error)",
		"out of range (write value not allowed)",
		"out of max. range (write value too high)",
		"out of min. range (write value too low)",
		"memory error (memory can not be written or only with faults)",
		"read-only (write access to a read-only or write-protected property)",
		"illegal command (command not valid or not supported)",
		"void DP (read/write access to nonexistent property)",
		"type conflict (write access with a wrong data type (datapoint length))",
		"property index/range error (read/write access to nonexistent property index)",
		"the property exists but can at this moment not be written with a new value"
	};

	private final int mc;
	private int iot;
	private int oi;
	private int pid;
	private int elems;
	private int start;
	private byte[] data = empty;
	private int header = 1;

	static {
		msgCodes = new BitSet(20);
		msgCodes.set(MC_PROPREAD_REQ - MC_OFFSET);
		msgCodes.set(MC_PROPREAD_CON - MC_OFFSET);
		msgCodes.set(MC_PROPWRITE_REQ - MC_OFFSET);
		msgCodes.set(MC_PROPWRITE_CON - MC_OFFSET);
		msgCodes.set(MC_PROPINFO_IND - MC_OFFSET);
		msgCodes.set(MC_FUNCPROP_CMD_REQ - MC_OFFSET);
		msgCodes.set(MC_FUNCPROP_READ_REQ - MC_OFFSET);
		msgCodes.set(MC_FUNCPROP_CON - MC_OFFSET);
		msgCodes.set(MC_RESET_REQ - MC_OFFSET);
		msgCodes.set(MC_RESET_IND - MC_OFFSET);
	}


	/**
	 * Creates a new device management message for a function property service.
	 *
	 * @param msgCode a message code constant for a function property service declared by this class
	 * @param objType interface object type, value in the range 0 &lt;= value &lt;= 0xFFFF
	 * @param objInstance object instance, value in the range 1 &lt;= value &lt;= 0xFF
	 * @param propId property identifier (PID), in the range 0 &lt;= PID &lt;= 0xFF
	 * @param data contains the data for the function property service
	 * @return new CEMIDevMgmt message containing the function property service
	 */
	public static CEMIDevMgmt newFunctionPropertyService(final int msgCode, final int objType, final int objInstance,
			final int propId, final byte... data) {
		return new CEMIDevMgmt(msgCode, objType, objInstance, propId, data);
	}

	/**
	 * Creates a new device management message for a function property service.
	 *
	 * @param msgCode a message code constant for a function property service declared by this class
	 * @param objType interface object type, value in the range 0 &lt;= value &lt;= 0xFFFF
	 * @param objInstance object instance, value in the range 1 &lt;= value &lt;= 0xFF
	 * @param propId property identifier (PID), in the range 0 &lt;= PID &lt;= 0xFF
	 * @param returnCode return code, use {@link ReturnCode#Success} for requests
	 * @param serviceId function property service identifier
	 * @param serviceInfo contains the service data for the function property service
	 * @return new CEMIDevMgmt message containing the function property service
	 */
	public static CEMIDevMgmt newFunctionPropertyService(final int msgCode, final int objType, final int objInstance,
		final int propId, final ReturnCode returnCode, final int serviceId, final byte... serviceInfo) {
		final byte[] data = new byte[2 + serviceInfo.length];
		data[0] = (byte) returnCode.code();
		data[1] = (byte) serviceId;
		System.arraycopy(serviceInfo, 0, data, 2, serviceInfo.length);
		return new CEMIDevMgmt(msgCode, objType, objInstance, propId, data);
	}

	/**
	 * Creates a new device management message from a byte stream.
	 *
	 * @param data byte stream containing a cEMI device management message
	 * @param offset start offset of cEMI frame in {@code data}
	 * @param length length in bytes of the whole device management message
	 * @throws KNXFormatException if no device management frame found or invalid frame structure
	 */
	public CEMIDevMgmt(final byte[] data, final int offset, final int length) throws KNXFormatException
	{
		final ByteArrayInputStream is = new ByteArrayInputStream(data, offset, length);
		checkLength(is, 1);
		mc = is.read();
		if (mc < MC_OFFSET || !msgCodes.get(mc - MC_OFFSET))
			throw new KNXFormatException("unknown message code 0x" + Integer.toHexString(mc));
		if (mc == MC_RESET_REQ || mc == MC_RESET_IND)
			initReset(is);
		else {
			initHeader(is);
			initBody(is);
		}
	}

	/**
	 * Creates a new device management message with the given message code, only used for reset messages.
	 * <p>
	 * The message structure (and resulting frame) will only consist of the message code
	 * field. All other device management methods are not used (and will consequently
	 * return 0 or empty fields by default).
	 *
	 * @param msgCode a reset message code value specified by this class
	 */
	public CEMIDevMgmt(final int msgCode)
	{
		if (msgCode != MC_RESET_REQ && msgCode != MC_RESET_IND)
			throw new KNXIllegalArgumentException("not a reset message code");
		mc = msgCode;
	}

	/**
	 * Creates a new device management message.
	 * <p>
	 * Used for messages without a data (or error information) part in the message
	 * structure (like read request).
	 *
	 * @param msgCode a message code constant declared by this class
	 * @param objType interface object type, value in the range 0 &lt;= value &lt;= 0xFFFF
	 * @param objInstance object instance, value in the range 1 &lt;= value &lt;= 0xFF
	 * @param propID property identifier (PID), in the range 0 &lt;= PID &lt;= 0xFF
	 * @param startIndex start index into the property array, first element value has
	 *        index 1, index 0 in the property contains the current number of valid
	 *        elements (read only)
	 * @param elements number of elements in the array of the property, in the range 1 &lt;= elements &lt;= 0xF;
	 */
	public CEMIDevMgmt(final int msgCode, final int objType, final int objInstance,
		final int propID, final int startIndex, final int elements)
	{
		if (msgCode < MC_OFFSET || !msgCodes.get(msgCode - MC_OFFSET))
			throw new KNXIllegalArgumentException("unknown cEMI DevMgmt msg code 0x" + Integer.toHexString(msgCode));
		mc = msgCode;
		header = 7;
		checkSetHeaderInfo(objType, objInstance, propID, startIndex, elements);
	}

	/**
	 * Creates a new device management message.
	 * <p>
	 * Used for messages containing a data (or error information) part in the message
	 * structure (like read confirmation).
	 *
	 * @param msgCode a message code constant declared by this class
	 * @param objType interface object type, value in the range 0 &lt;= value &lt;= 0xFFFF
	 * @param objInstance object instance, value in the range 1 &lt;= value &lt;= 0xFF
	 * @param propID property identifier (PID), in the range 0 &lt;= PID &lt;= 0xFF
	 * @param startIndex start index in the property, first element has index 1, index 0
	 *        in the property contains the current number of valid elements (read only)
	 * @param elements number of elements in the array of the property, in the range 0
	 *        &lt;= elements &lt;= 0xF; the number 0 is used to indicate a negative
	 *        response
	 * @param data contains the data (or the error information, if numElems = 0) as byte
	 *        array
	 */
	public CEMIDevMgmt(final int msgCode, final int objType, final int objInstance,
		final int propID, final int startIndex, final int elements, final byte[] data)
	{
		this(msgCode, objType, objInstance, propID, startIndex, elements);
		this.data = data.clone();
	}

	protected CEMIDevMgmt(final int msgCode, final int objType, final int objInstance, final int propID,
			final byte[] data) {
		this(msgCode, objType, objInstance, propID, 0, 0);
		header = 5;
		this.data = data.clone();
	}

	/**
	 * Returns a descriptive error message for the supplied error code parameter.
	 * <p>
	 * If the error code is not known, the string "unknown error code" is returned.
	 *
	 * @param errorCode error code to get message for, {@code 0 ≤ errorCode}
	 * @return error status message as string
	 */
	public static String getErrorMessage(final int errorCode)
	{
		if (errorCode < 0)
			throw new KNXIllegalArgumentException("error code has to be >= 0");
		if (errorCode > errors.length - 1)
			return "unknown error code";
		return errors[errorCode];
	}

	@Override
	public final int getMessageCode()
	{
		return mc;
	}

	/**
	 * Returns the data part, i.e., the property data or error information following the
	 * start index in the message structure.
	 * <p>
	 * The property content depends on the property data type, and in case of an array
	 * structured property value also on the accessed number of array elements.
	 * <p>
	 * In case of a message carrying a negative response, the payload holds error
	 * information. To determine whether the returned array contains error information,
	 * use {@link #isNegativeResponse()}.<br>
	 * If the message does not contain any data (or error information), a byte array with
	 * length 0 is returned.
	 *
	 * @return a copy of the data part in the message structure as byte array
	 */
	@Override
	public final byte[] getPayload()
	{
		return data.clone();
	}

	/**
	 * Returns the interface object type.
	 * <p>
	 * For example,<br>
	 * <ul>
	 * <li>Standardized system interface object types
	 * <ol start="0">
	 * <li>Device object</li>
	 * <li>Addresstable object</li>
	 * <li>Associationtable object</li>
	 * <li>...</li>
	 * </ol>
	 * </li>
	 * <li>Standardized application interface object types
	 * <ul>
	 * <li>[100..399] HVAC</li>
	 * <li>[400..599] Lighting</li>
	 * <li>[600..799] HVAC (sensors &amp; actuators)</li>
	 * <li>[800..999] Shutters and blinds</li>
	 * <li>...</li>
	 * </ul>
	 * </li>
	 * <li>Non-standardized interface object types
	 * <ul>
	 * <li>...</li>
	 * </ul>
	 * </li>
	 * </ul>
	 *
	 * @return the object type as 16 bit identifier
	 */
	public final int getObjectType()
	{
		return iot;
	}

	/**
	 * Returns the object instance field from the message.
	 * <p>
	 * The instance value is in the range 1 to 0xFF.<br>
	 * If the message structure does not contain this field, 0 is returned.
	 *
	 * @return the object instance
	 */
	public final int getObjectInstance()
	{
		return oi;
	}

	/**
	 * Returns the number of elements field from the message.
	 * <p>
	 * An element count in the range 1 to 15 indicates the presence of element data. A
	 * value of 0 indicates a negative response.<br>
	 * If the message structure does not contain this field, 0 is returned.
	 *
	 * @return number of elements as unsigned 4 bit value
	 */
	public final int getElementCount()
	{
		return elems;
	}

	/**
	 * Returns the property identifier used in the message.
	 * <p>
	 * If the message structure does not contain this field, 0 is returned.
	 *
	 * @return the PID
	 */
	public final int getPID()
	{
		return pid;
	}

	/**
	 * Returns the start index in the data array of the property.
	 * <p>
	 * If the message structure does not contain this field, 0 is returned.
	 *
	 * @return start index as 12 bit value
	 */
	public final int getStartIndex()
	{
		return start;
	}

	/**
	 * Returns if the message contains a negative response.
	 * <p>
	 * A message contains a negative response, iff the message code equals
	 * {@link #MC_PROPREAD_CON} or {@link #MC_PROPWRITE_CON} and number of elements is 0.
	 *
	 * @return response state as boolean
	 */
	public final boolean isNegativeResponse()
	{
		return (mc == MC_PROPREAD_CON || mc == MC_PROPWRITE_CON) && elems == 0;
	}

	/**
	 * Returns a descriptive error message on a negative response, as determined by
	 * {@link #isNegativeResponse()}.
	 * <p>
	 * A negative response contains an error information code, which is used to find the
	 * associated message.<br>
	 * If invoked on positive response, "no error" will be returned.
	 *
	 * @return error status message as string
	 */
	public String getErrorMessage()
	{
		if (!isNegativeResponse())
			return "no error";
		return getErrorMessage(data[0] & 0xff);
	}

	@Override
	public final int getStructLength()
	{
		return header + data.length;
	}

	@Override
	public byte[] toByteArray()
	{
		final byte[] buf = new byte[header + data.length];
		int i = 0;
		buf[i++] = (byte) mc;
		if (header > 1) {
			buf[i++] = (byte) (iot >>> 8);
			buf[i++] = (byte) iot;
			buf[i++] = (byte) oi;
			buf[i++] = (byte) pid;
			if (header == 7) {
				buf[i++] = (byte) (elems << 4 | start >>> 8);
				buf[i++] = (byte) start;
			}
			for (final byte b : data)
				buf[i++] = b;
		}
		return buf;
	}

	@Override
	public String toString()
	{
		final StringBuilder buf = new StringBuilder(100);
		buf.append("DM ");
		buf.append(switch (mc) {
			case MC_PROPREAD_REQ      -> "prop-read.req";
			case MC_PROPREAD_CON      -> "prop-read.con";
			case MC_PROPWRITE_REQ     -> "prop-write.req";
			case MC_PROPWRITE_CON     -> "prop-write.con";
			case MC_PROPINFO_IND      -> "prop-info.ind";
			case MC_FUNCPROP_CMD_REQ  -> "funcprop-cmd.req";
			case MC_FUNCPROP_READ_REQ -> "funcprop-read.req";
			case MC_FUNCPROP_CON      -> "funcprop.con";
			case MC_RESET_REQ         -> "reset.req";
			case MC_RESET_IND         -> "reset.ind";
			default -> throw new IllegalStateException("invalid message code 0x%02x".formatted(mc));
		});
		if (mc == MC_RESET_REQ || mc == MC_RESET_IND)
			return buf.toString();

		buf.append(" objtype ").append(iot);
		buf.append(" instance ").append(oi);
		buf.append(" pid ").append(pid);
		final boolean funcprop = isFuncProp();
		if (!funcprop)
			buf.append(" start ").append(start);
		if (isNegativeResponse())
			buf.append(" ").append(getErrorMessage());
		else {
			if (!funcprop)
				buf.append(" elements ").append(elems);
			if (data.length > 0) {
				buf.append(" data ");
				HexFormat.ofDelimiter(" ").formatHex(buf, data);
			}
		}
		return buf.toString();
	}

	private boolean isFuncProp() {
		return mc == MC_FUNCPROP_CMD_REQ || mc == MC_FUNCPROP_READ_REQ || mc == MC_FUNCPROP_CON;
	}

	private static void checkLength(final ByteArrayInputStream is, final int len) throws KNXFormatException
	{
		if (is.available() < len)
			throw new KNXFormatException("insufficient frame length", len);
	}

	private static void initReset(final ByteArrayInputStream is) throws KNXFormatException
	{
		if (is.available() != 0)
			throw new KNXFormatException("invalid length for a reset frame", is.available());
	}

	private void initHeader(final ByteArrayInputStream is) throws KNXFormatException
	{
		checkLength(is, isFuncProp() ? 4 : 6);
		header = isFuncProp() ? 5 : 7;
		try {
			final int objType = is.read() << 8 | is.read();
			final int objInstance = is.read();
			final int propID = is.read();
			int startIndex = 0;
			int elements = 0;
			if (!isFuncProp()) {
				final int tmp = is.read();
				startIndex = (tmp & 0x0F) << 8 | is.read();
				elements = tmp >> 4;
			}
			checkSetHeaderInfo(objType, objInstance, propID, startIndex, elements);
		}
		catch (final KNXIllegalArgumentException e) {
			throw new KNXFormatException(e.getMessage());
		}
	}

	private void initBody(final ByteArrayInputStream is) throws KNXFormatException
	{
		// read error information on negative response
		if (isNegativeResponse())
			data = new byte[] { (byte) is.read() };
		else if (mc == MC_PROPREAD_CON || mc == MC_PROPWRITE_REQ || mc == MC_PROPINFO_IND || isFuncProp()) {
			// if a function property call was not to a property of type PDT_Function, .con does not contain return code and data
			checkLength(is, mc == MC_FUNCPROP_CON ? 0 : 1);
			data = new byte[is.available()];
			is.read(data, 0, data.length);
		}
	}

	private void checkSetHeaderInfo(final int objType, final int objInstance, final int propID,
		final int startIndex, final int elements)
	{
		if (objType < 0 || objType > 0xFFFF)
			throw new KNXIllegalArgumentException("interface object type " + objType + " out of range [0..0xFFFF]");
		if (objInstance < 1 || objInstance > 0xFF)
			throw new KNXIllegalArgumentException("object instance " + objInstance + " out of range [1..0xFF]");
		if (propID < 0 || propID > 0xFF)
			throw new KNXIllegalArgumentException("property ID " + propID + " out of range [0..0xFF]");
		if (startIndex < 0 || startIndex > 0xFFF)
			throw new KNXIllegalArgumentException("start index " + startIndex + " out of range [0..0xFFF]");
		if (elements < 0 || elements > 0xF)
			throw new KNXIllegalArgumentException("elements " + elements + " out of range [0..0xF]");
		if (mc == MC_PROPREAD_REQ || mc == MC_PROPWRITE_REQ || mc == MC_PROPINFO_IND)
			if (elements < 1)
				throw new KNXIllegalArgumentException("elements may not be 0");
		// NOTE: according to cEMI spec. start index should be > 0, but since we do
		// the check for write-enabled property on index 0, allow it
		// if (mc == MC_PROPWRITE_REQ || mc == MC_PROPWRITE_CON || mc == MC_PROPINFO_IND) {
		// 	if (startIndex < 1)
		// 		throw new KNXIllegalArgumentException("start index may not be 0");
		// }
		iot = objType;
		oi = objInstance;
		pid = propID;
		start = startIndex;
		elems = elements;
	}
}
