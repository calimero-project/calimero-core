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

package tuwien.auto.calimero.dptxlator;

import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 16, type <b>string</b>.
 * <p>
 * The KNX data type width is 14 bytes.<br>
 * The default return value after creation is the empty string "" or 14 bytes filled with
 * <i>NULL</i> (ASCII code 0).<br>
 * Characters not supported by the selected character set are silently replaced with the
 * question mark character, '?'.<br>
 * On supplying string items for translation, strings are limited to a maximum length of
 * 14 characters. Exceeding this maximum string length will result in a
 * {@link KNXFormatException}.
 */
public class DPTXlatorString extends DPTXlator
{
	/**
	 * DPT ID 16.000, ASCII string; 7 Bit character set encoding.
	 */
	public static final DPT DPT_STRING_ASCII = new DPT("16.000", "ASCII string", "", "");

	/**
	 * DPT ID 16.001, ISO-8859-1 string (Latin 1); 8 Bit character set encoding.
	 */
	public static final DPT DPT_STRING_8859_1 =
		new DPT("16.001", "ISO-8859-1 string (Latin 1)", "", "");

	private static final Map<String, DPT> types;

	// default replacement for unsupported characters
	private static final short replacement = '?';

	// standard string length of this type
	private static final int stringLength = 14;

	static {
		types = new HashMap<>(5);
		types.put(DPT_STRING_ASCII.getID(), DPT_STRING_ASCII);
		types.put(DPT_STRING_8859_1.getID(), DPT_STRING_8859_1);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlatorString(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 * <p>
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available)
	 *         <code>dptID</code>
	 */
	public DPTXlatorString(final String dptID) throws KNXFormatException
	{
		super(stringLength);
		setTypeID(types, dptID);
		data = new short[stringLength];
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] buf = new String[getItems()];
		for (int i = 0; i < buf.length; ++i)
			buf[i] = fromDPT(i);
		return buf;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#setData(byte[], int)
	 */
	@Override
	public void setData(final byte[] data, final int offset)
	{
		if (offset < 0 || offset > data.length)
			throw new KNXIllegalArgumentException("illegal offset " + offset);
		toDPT(data, offset);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the string translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String fromDPT(final int index)
	{
		final int offset = index * stringLength;
		final char[] output = new char[stringLength];
		int strlen = 0;
		for (int i = offset; i < (offset + stringLength) && data[i] != 0; ++i) {
			output[strlen] = (char) data[i];
			++strlen;
		}
		return new String(output, 0, strlen);
	}

	private void toDPT(final byte[] buf, final int offset)
	{
		// check character set, default to ASCII 7 bit encoding
		int rangeMax = 0x7f;
		if (dpt.equals(DPT_STRING_8859_1))
			rangeMax = 0xff;
		final int len = buf.length - offset;
		if (len % stringLength != 0)
			throw new KNXIllegalArgumentException(
					"data length " + len + " does not exactly fit KNX strings of length 14 bytes");
		final int items = len / stringLength;
		if (items == 0) {
			throw new KNXIllegalArgumentException("data length " + len + " < required 14 bytes for KNX string");
		}
		data = new short[items * stringLength];
		// 7 bit encoded characters of ISO-8859-1 are equal to ASCII
		// the lower 256 characters of UCS correspond to ISO-8859-1
		for (int i = 0; i < data.length; ++i) {
			final short c = ubyte(buf[offset + i]);
			data[i] = c <= rangeMax ? c : replacement;
		}
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (value.length() > stringLength)
			throw newException("maximum KNX string length is 14 characters", value);
		// check character set, default to ASCII 7 bit encoding
		char rangeMax = '\u007f';
		if (dpt.equals(DPT_STRING_8859_1))
			rangeMax = '\u00ff';
		// 7 bit encoded characters of ISO-8859-1 are equal to ASCII
		// the lower 256 characters of UCS correspond to ISO-8859-1
		final int offset = index * stringLength;
		for (int i = 0; i < value.length(); ++i) {
			final char c = value.charAt(i);
			dst[offset + i] = c <= rangeMax ? (short) c : replacement;
		}
	}
}
