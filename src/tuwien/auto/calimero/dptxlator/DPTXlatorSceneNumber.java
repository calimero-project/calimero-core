/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2013 B. Malinowsky

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

package tuwien.auto.calimero.dptxlator;

import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.log.LogLevel;

/**
 * Translator for KNX DPTs with main number 17, representing <b>Scene Number</b>.
 * <p>
 * The KNX data type width is 1 byte.<br>
 * The default return value after creation is scene 0.<br>
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal, hexadecimal,
 * and octal numbers, distinguished by using these prefixes:
 * <li>no prefix for decimal numeral</li>
 * <li><code>0x</code>, <code>0X</code> or <code>#</code> for hexadecimal</li>
 * <li><code>0</code> for octal numeral</li>
 */
public class DPTXlatorSceneNumber extends DPTXlator
{
	/**
	 * DPT ID 17.001, Scene Number; scenes are numbered with values from <b>0</b> to <b>63</b>.
	 * <p>
	 */
	public static final DPT DPT_SCENE_NUMBER = new DPT("17.001", "Scene Number", "0",
			 "63");

	private static final Map types;

	static {
		types = new HashMap();
		types.put(DPT_SCENE_NUMBER.getID(), DPT_SCENE_NUMBER);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 * 
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlatorSceneNumber(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 * <p>
	 * 
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptID</code>
	 */
	public DPTXlatorSceneNumber(final String dptID) throws KNXFormatException
	{
		super(1);
		setTypeID(types, dptID);
		data = new short[1];
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	public String getValue()
	{
		return fromDPT(0);
	}

	/**
	 * Sets one new translation item, replacing any old items.
	 * <p>
	 * 
	 * @param scene number, 0 &lt;= scene number &lt;= 63
	 */
	public final void setValue(final int scene)
	{
		data = new short[] { toDPT(scene) };
	}

	/**
	 * Returns the scene number of the first translation item.
	 * <p>
	 * 
	 * @return unsigned 6 Bit using type short
	 */
	public final short getSceneNumber()
	{
		return (short) (data[0] & 0x3F);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i)
			s[i] = fromDPT(i);
		return s;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
	public final Map getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 8 Bit unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map getSubTypesStatic()
	{
		return types;
	}

	private String fromDPT(final int index)
	{
		final int scene = data[index] & 0x3F;
		return Integer.toString(scene);
	}

	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			dst[index] = toDPT(Short.decode(value.trim()).shortValue());
		}
		catch (final NumberFormatException e) {
			logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
		}
	}

	private short toDPT(final int value)
	{
		if (value < 0 || value > 63)
			throw new KNXIllegalArgumentException("input scene number out of range [0..63]");
		return (short) value;
	}
}
