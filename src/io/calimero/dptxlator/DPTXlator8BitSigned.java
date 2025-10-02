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

package io.calimero.dptxlator;

import java.util.HashMap;
import java.util.Map;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 6, type <b>8 Bit signed value</b>. The KNX data type
 * width is 1 byte.<br>
 * The default return value after creation is 0, except for DPT 6.020. For DPT 6.020, the default
 * value is all status bits set (i.e., 0) and mode 0 is active.<br>
 *
 * @author A. Christian, info@root1.de
 */
public class DPTXlator8BitSigned extends DPTXlator
{
	/**
	 * DPT ID 6.001, Percent 8 Bit; values from <b>-128</b> to <b>127</b> %, resolution 1 %.
	 */
	public static final DPT DPT_PERCENT_V8 = new DPT("6.001", "Percent (8 Bit)", "-128", "127", "%");

	/**
	 * DPT ID 6.010, Value 1 signed count; values from <b>-128</b> to <b>127</b> counter pulses,
	 * resolution 1 counter pulse.
	 */
	public static final DPT DPT_VALUE_1_UCOUNT = new DPT("6.010", "signed count", "-128", "127",
			"counter pulses");

	/**
	 * DPT ID 6.020, Status with Mode; status values <b>0/1</b>, mode <b>0</b> to <b>2</b>.
	 */
	public static final DPT DPT_STATUS_MODE3 = new DPT("6.020", "status with mode", "0/0/0/0/0 0",
			"1/1/1/1/1 2");

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>();
		types.put(DPT_PERCENT_V8.getID(), DPT_PERCENT_V8);
		types.put(DPT_VALUE_1_UCOUNT.getID(), DPT_VALUE_1_UCOUNT);
		types.put(DPT_STATUS_MODE3.getID(), DPT_STATUS_MODE3);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator8BitSigned(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 * <p>
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) {@code dptID}
	 */
	public DPTXlator8BitSigned(final String dptID) throws KNXFormatException
	{
		super(1);
		setTypeID(types, dptID);
		if (dpt.equals(DPT_STATUS_MODE3))
			data[0] = 1;
	}

	@Override
	public String getValue()
	{
		return makeString(0);
	}

	@Override
	public void setValue(final double value) throws KNXFormatException {
		setValue((int) value);
	}

	/**
	 * Sets one new translation item from a signed value, replacing any old items.
	 *
	 * @param value signed value, -128 &le; {@code value} &le; 127
	 * @throws KNXFormatException if {@code value} is out of range
	 */
	public final void setValue(final int value) throws KNXFormatException
	{
		data = new short[] { toDPT(value) };
	}

	@Override
	public double getNumericValue()
	{
		return getValueSigned();
	}

	byte getValueSigned()
	{
		return fromDPT(data[0]);
	}

	/* (non-Javadoc)
	 * @see io.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i) {
			s[i] = makeString(i);
		}
		return s;
	}

	/**
	 * Sets the status and mode of the first translation item for DPT 6.020, Status with Mode, if
	 * the translator is set to that particular datapoint type. This method is only applicable for
	 * DPT 6.020, other DPTs will cause a {@link IllegalStateException}.
	 *
	 * @param a status bit 0 (corresponding to the MSB, bit 7, in the encoding)
	 * @param b status bit 1
	 * @param c status bit 2
	 * @param d status bit 3
	 * @param e status bit 4
	 * @param mode the active mode, {@code 0} &le; mode &le; {@code 2}
	 */
	public final void setStatusMode(final boolean a, final boolean b, final boolean c,
		final boolean d, final boolean e, final int mode)
	{
		if (!dpt.equals(DPT_STATUS_MODE3))
			throw new IllegalStateException("translator not set to DPT 6.020 (Status with Mode)");
		if (mode < 0 || mode > 2)
			throw new KNXIllegalArgumentException("mode out of range [0..2]");
		int status = a ? 1 << 7 : 0;
		status |= b ? (1 << 6) : 0;
		status |= c ? (1 << 5) : 0;
		status |= d ? (1 << 4) : 0;
		status |= e ? (1 << 3) : 0;
		final int enc = mode == 0 ? 1 : mode == 1 ? 2 : 4;
		data = new short[] { (short) (status | enc) };
	}

	/**
	 * Returns a status bit; this method is only applicable for DPT 6.020, other DPTs will cause a
	 * {@link IllegalStateException}.
	 *
	 * @param statusBit to check, {@code 0 ≤ statusBit ≤ 4}
	 * @return {@code true} if status bit is set, {@code false} otherwise
	 * @see #setStatusMode
	 */
	boolean statusBit(final int statusBit)
	{
		if (!dpt.equals(DPT_STATUS_MODE3))
			throw new IllegalStateException("translator not set to DPT 6.020 (Status with Mode)");
		if (statusBit < 0 || statusBit > 4)
			throw new KNXIllegalArgumentException("status bit " + statusBit + " out of range [0..4]");
		return (data[0] & (0x80 >> statusBit)) != 0;
	}

	/**
	 * Returns the mode of the first translation item for DPT 6.020, Status with Mode, if the
	 * translator is set to that particular datapoint type. This method is only applicable for DPT
	 * 6.020, other DPTs will cause a {@link IllegalStateException}.
	 *
	 * @return the active mode, with a value of either 0, 1, or 2
	 */
	public final int getMode()
	{
		if (!dpt.equals(DPT_STATUS_MODE3))
			throw new IllegalStateException("translator not set to DPT 6.020 (Status with Mode)");
		final int enc = data[0] & 0x07;
		if (enc != 1 && enc != 2 && enc != 4)
			throw new IllegalStateException("invalid mode encoding " + enc + " out of {1, 2, 4}");
		return enc == 1 ? 0 : enc == 2 ? 1 : 2;
	}

	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * {@return the subtypes of the 8 Bit signed translator type}
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private static byte fromDPT(final short data)
	{
		return (byte) data;
	}

	private String makeString(final int index)
	{
		if (dpt.equals(DPT_STATUS_MODE3)) {
			final short d = data[index];
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 4; i++)
				sb.append((d >> (7 - i)) & 0x01).append("/");
			sb.append((d >> 3) & 0x01);
			sb.append(' ').append(getMode());
			return sb.toString();
		}
		return appendUnit(Short.toString(fromDPT(data[index])));
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (dpt.equals(DPT_STATUS_MODE3)) {
			if (value.length() != 11)
				throw new KNXFormatException("status mode requires 11 characters", value);
			short d = 0;
			for (int i = 0; i < 5; i++) {
				final char c = value.charAt(2 * i);
				if (c == '1')
					d |=  1 << (7 - i);
				else if (c != '0')
					throw new KNXFormatException("invalid status", c);
			}
			final char c = value.charAt(10);
			d += (short) switch (c) {
				case '0' -> 1;
				case '1' -> 2;
				case '2' -> 4;
				default -> throw new KNXFormatException("invalid mode", c);
			};
			dst[index] = d;
			return;
		}
		try {
			dst[index] = toDPT(Short.decode(removeUnit(value)));
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}

	private static short toDPT(final int value) throws KNXFormatException
	{
		if (value < -128 || value > 127)
			throw new KNXFormatException("value out of range [-128 .. 127]", value);
		return (short) (value & 0xff);
	}
}
