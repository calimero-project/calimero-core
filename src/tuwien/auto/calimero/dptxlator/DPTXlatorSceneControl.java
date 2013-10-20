/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2013 B. Malinowsky
    Copyright (c) 2013 Juan Ruzafa Millán

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
import java.util.StringTokenizer;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.log.LogLevel;

/**
 * Translator for KNX DPTs with main number 18, representing <b>Scene Control</b>.
 * <p>
 * The KNX data type width is 1 byte.<br>
 * The default return value after creation is "activate 0".<br>
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal, hexadecimal,
 * and octal numbers, distinguished by using these prefixes:
 * <li>no prefix for decimal numeral</li>
 * <li><code>0x</code>, <code>0X</code> or <code>#</code> for hexadecimal</li>
 * <li><code>0</code> for octal numeral</li>
 * 
 * @author Juan Ruzafa Millán
 * @author B. Malinowsky
 */
public class DPTXlatorSceneControl extends DPTXlator
{
	/**
	 * DPT ID 18.001, Scene Control; activate or learn a scene, with scene numbers from <b>0</b> to
	 * <b>63</b>.
	 * <p>
	 */
	public static final DPT DPT_SCENE_CONTROL = new DPT("18.001", "Scene Control", "activate 0",
			"learn 63");

	private static final Map types;

	static {
		types = new HashMap(5);
		types.put(DPT_SCENE_CONTROL.getID(), DPT_SCENE_CONTROL);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 * 
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlatorSceneControl(final DPT dpt) throws KNXFormatException
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
	public DPTXlatorSceneControl(final String dptID) throws KNXFormatException
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
	 * @param control control bit to activate or learn a scene, <code>false</code> = activate,
	 *        <code>true</code> = learn
	 * @param scene number, 0 &lt;= scene number &lt;= 63
	 */
	public final void setValue(final boolean control, final int scene)
	{
		data = new short[] { toDPT(control, scene) };
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
		final boolean c = (data[index] & 0x80) == 128;
		final int scene = data[index] & 0x3F;
		final String value = c ? dpt.getUpperValue() : dpt.getLowerValue();
		return new StringTokenizer(value).nextToken() + " " + scene;
	}

	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		final StringTokenizer token = new StringTokenizer(value, " \t");
		if (token.countTokens() < 2)
			logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
		boolean learn = false;
		String s = token.nextToken();
		if (s.equals("learn"))
			learn = true;
		else if (!s.equals("activate"))
			logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
		try {
			s = token.nextToken();
			dst[index] = toDPT(learn, Integer.decode(s).intValue());
		}
		catch (final NumberFormatException e) {
			logThrow(LogLevel.WARN, "parsing " + value, null, value);
		}
	}

	private short toDPT(final boolean ctrl, final int scene)
	{
		if (scene < 0 || scene > 63)
			throw new KNXIllegalArgumentException("input scene number out of range [0..63]");
		return (short) (ctrl ? scene | 0x80 : scene);
	}
}
