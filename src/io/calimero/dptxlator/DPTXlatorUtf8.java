/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2015, 2023 B. Malinowsky

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 28, type <b>Unicode UTF-8 string</b>.
 * <p>
 * The KNX data type width is variable, strings are not upper-bounded in length. A string length is
 * implicitly known by the used container format, e.g., the frame length of the frame containing the
 * string in its ASDU.<br>
 * Note that implementations still do have limits on string lengths:
 * <ul>
 * <li>KNX frames with non-supported string lengths shall be neglected</li>
 * <li>application layer implementations cut the character sequence to [0..n-1], with <i>n</i> the
 * maximum supported length</li>
 * <li>interface object servers shall send a negative response for requests that exceed the maximum
 * supported length</li>
 * </ul>
 * Although KNX does not specify a maximum string length, this translator currently does not allow
 * UTF-8 strings with a byte array encoding exceeding 1024 Kilobytes (2**20 Bytes).
 * <p>
 * The default return value after creation is the empty string "" or 1 byte containing the
 * <i>NULL</i> (0x00) string termination character.<br>
 * Each character is encoded according the Unicode Transformation Format
 * <a href="http://www.ietf.org/rfc/rfc3629.txt">UTF-8</a>.
 */
public class DPTXlatorUtf8 extends DPTXlator
{
	/**
	 * DPT ID 28.001, UTF-8 encoding.
	 */
	public static final DPT DPT_UTF8 = new DPT("28.001", "UTF-8", "", "");

	private static final Map<String, DPT> types;

	// enforce a maximum string length for sanity checks; this length is still insanely high
	// compared to the strings used in practice in KNX networks
	private static final int maxLength = 1024 * 1024;

	static {
		types = new HashMap<>(5);
		types.put(DPT_UTF8.getID(), DPT_UTF8);
	}

	private int items = 1;

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlatorUtf8(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) {@code dptID}
	 */
	public DPTXlatorUtf8(final String dptID) throws KNXFormatException
	{
		super(1);
		setTypeID(types, dptID);
		data = new short[1];
	}

	@Override
	public void setValues(final String... values) throws KNXFormatException
	{
		if (values.length == 0)
			return;
		int length = 0;
		for (final String value : values)
			length += toUtf8(value).length + 1;
		final short[] buf = new short[length];
		for (int i = 0; i < values.length; ++i)
			toDPT(values[i], buf, i);
		data = buf;
		items = values.length;
	}

	@Override
	public String[] getAllValues()
	{
		final String[] buf = new String[getItems()];
		for (int i = 0; i < buf.length; ++i)
			buf[i] = fromDPT(i);
		return buf;
	}

	@Override
	public void setValue(final String value) throws KNXFormatException
	{
		setValues(value);
	}

	@Override
	public void setData(final byte[] data, final int offset)
	{
		if (offset < 0 || offset > data.length)
			throw new KNXIllegalArgumentException("illegal offset " + offset);
		toDPT(data, offset);
	}

	@Override
	public int getItems()
	{
		return items;
	}

	@Override
	public final int getTypeSize()
	{
		return findOffsetFor(1, data);
	}

	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	@Override
	public List<DPTXlator> split()
	{
		if (getItems() == 1)
			return List.of(this);

		final List<DPTXlator> l = new ArrayList<>();
		int from = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == 0) {
				try {
					l.add(new DPTXlatorUtf8(getType()));
				}
				catch (final KNXFormatException ignore) {}
				final int to = i + 1;
				l.get(l.size() - 1).data = Arrays.copyOfRange(data, from, to);
				from = to;
			}
		}
		return l;
	}

	/**
	 * {@return the subtypes of the string translator type}
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String fromDPT(final int index)
	{
		final int offset = findOffsetFor(index, data);
		int length = 0;
		for (int i = offset; i < data.length && data[i] != 0; i++)
			length++;
		final byte[] utfdata = new byte[length];
		for (int i = 0; i < utfdata.length; i++)
			utfdata[i] = (byte) data[offset + i];
		return new String(utfdata, 0, length, StandardCharsets.UTF_8);
	}

	private void toDPT(final byte[] buf, final int offset)
	{
		final int length = buf.length - offset;
		if (length <= 0)
			throw new KNXIllegalArgumentException("data length " + length
					+ " < minimum of 1 byte for empty string");
		if (length > maxLength)
			throw new KNXIllegalArgumentException("data length " + length
					+ " exceeds translator limit of " + maxLength + " bytes");
		if (buf[buf.length - 1] != 0)
			throw new KNXIllegalArgumentException("UTF-8 string not NULL terminated");
		data = new short[length];
		// TODO check here for valid encoding?
		for (int i = 0; i < length; ++i)
			data[i] = ubyte(buf[offset + i]);
		countItems();
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException
	{
		final byte[] utfdata = toUtf8(value);
		final int offset = findOffsetFor(index, dst);
		for (int i = 0; i < utfdata.length; ++i)
			dst[offset + i] = ubyte(utfdata[i]);
		// ensure we are NULL terminated
		if (utfdata.length == 0 || utfdata[utfdata.length - 1] != 0)
			dst[offset + utfdata.length] = 0;
	}

	private byte[] toUtf8(final String value) throws KNXFormatException
	{
		final byte[] utfdata = value.getBytes(StandardCharsets.UTF_8);
		if (utfdata.length > maxLength - 1)
			throw newException("UTF-8 string exceeds translator limit of " + maxLength + " bytes", value);
		return utfdata;
	}

	private static int findOffsetFor(final int index, final short[] dst)
	{
		int offset = 0;
		for (int idx = index; idx > 0;) {
			if (offset >= dst.length)
				throw new KNXIllegalArgumentException("index " + index + " past last string");
			if (dst[offset++] == 0)
				idx--;
		}
		return offset;
	}

	private void countItems()
	{
		int found = 0;
		for (final short s : data)
			if (s == 0)
				found++;
		items = found;
	}
}
