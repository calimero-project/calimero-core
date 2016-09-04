/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2013, 2016 B. Malinowsky
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
import java.util.StringTokenizer;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 18, representing <b>Scene Control</b>.
 * <p>
 * The KNX data type width is 1 byte.<br>
 * The default return value after creation is "activate 0".<br>
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal, hexadecimal,
 * and octal numbers, distinguished by using these prefixes:
 * <ul>
 * <li>no prefix for decimal numeral</li>
 * <li><code>0x</code>, <code>0X</code> or <code>#</code> for hexadecimal numeral</li>
 * <li><code>0</code> for octal numeral</li>
 * </ul>
 *
 * @author Juan Ruzafa Millán
 * @author B. Malinowsky
 */
public class DPTXlatorSceneControl extends DPTXlator
{
	/**
	 * DPT ID 18.001, Scene Control; activate or learn a scene, with scene numbers from <b>0</b> to <b>63</b>.
	 */
	public static final DPT DPT_SCENE_CONTROL = new DPT("18.001", "Scene Control", "activate 0", "learn 63");

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>(5);
		types.put(DPT_SCENE_CONTROL.getID(), DPT_SCENE_CONTROL);
	}

	/**
	 * Creates a translator for the given datapoint type.
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

	@Override
	public String getValue()
	{
		return fromDPT(0);
	}

	/**
	 * Sets one new translation item, replacing any old items.
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
	 *
	 * @return unsigned 6 Bit using type short
	 */
	public final short getSceneNumber()
	{
		return (short) (data[0] & 0x3F);
	}

	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i)
			s[i] = fromDPT(i);
		return s;
	}

	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 8 Bit unsigned translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
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

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		final StringTokenizer token = new StringTokenizer(value, " \t");
		if (token.countTokens() < 2)
			throw newException("wrong value format", value);
		boolean learn = false;
		String s = token.nextToken();
		if (s.equals("learn"))
			learn = true;
		else if (!s.equals("activate"))
			throw newException("wrong value format", value);
		try {
			s = token.nextToken();
			dst[index] = toDPT(learn, Integer.decode(s).intValue());
		}
		catch (final NumberFormatException e) {
			throw newException("parsing", value);
		}
	}

	private short toDPT(final boolean ctrl, final int scene)
	{
		if (scene < 0 || scene > 63)
			throw new KNXIllegalArgumentException("input scene number out of range [0..63]");
		return (short) (ctrl ? scene | 0x80 : scene);
	}
}
