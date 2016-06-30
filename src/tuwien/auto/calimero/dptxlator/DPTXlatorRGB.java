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
import java.util.StringTokenizer;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 232, type <b>rgb</b>.
 * <p>
 * The KNX data type width is 3 bytes.<br>
 * The type contains the color information red, green and blue, with a resolution of 1.<br>
 * <p>
 * The default return value after creation is <code>0 0 0</code>.
 * <p>
 * Note, that the RGB color model is device dependent, and RGB values are based on a
 * device-dependent interpretation of RGB. Therefore, do not expect consistent behavior with respect
 * to, e.g., linearity or brightness, across different applications or function block (FB)
 * specifications.
 *
 * @author T. Wegner
 */
public class DPTXlatorRGB extends DPTXlator {
	public static final String Description = "RGB Color";

	/**
	 * DPT ID 232.600, RGB Color; values from <b>0 0 0</b> to <b>255 255 255</b>.
	 */
	public static final DPT DPT_RGB = new DPT("232.600", "RGB", "0 0 0", "255 255 255", "r g b");

	private static final int RED = 0;
	private static final int GREEN = 1;
	private static final int BLUE = 2;

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>(3);
		types.put(DPT_RGB.getID(), DPT_RGB);
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlatorRGB(final DPT dpt) throws KNXFormatException
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
	public DPTXlatorRGB(final String dptID) throws KNXFormatException
	{
		super(3);
		setTypeID(types, dptID);
		data = new short[] { 0, 0, 0 };
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] buf = new String[data.length / 3];
		for (int i = 0; i < buf.length; ++i)
			buf[i] = fromDPT(i);
		return buf;
	}

	/**
	 * Sets the translation value from a 3-tuple (R,G,B), specifying the red, green, and blue
	 * color values.
	 * <p>
	 * If succeeded, any other items in the translator are discarded.
	 *
	 * @param red the red color component, <code>0 &le; red &le; 255</code>
	 * @param green the green color component, <code>0 &le; red &le; 255</code>
	 * @param blue the blue color component, <code>0 &le; red &le; 255</code>
	 */
	public final void setValue(final int red, final int green, final int blue)
	{
		data = set(red, green, blue, new short[3], 0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
	@Override
	public Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the RGB translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String fromDPT(final int index)
	{
		final int i = index * 3;

		// return red green blue
		return "r:" + Short.toString(data[i + RED]) + " g:" + Short.toString(data[i + GREEN])
				+ " b:" + Short.toString(data[i + BLUE]);
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		final StringTokenizer t = new StringTokenizer(value, "- ");
		final int maxTokens = 3;
		int r=-1, g=-1, b=-1;
		try {
			int count = 0;
			for (; count < maxTokens && t.hasMoreTokens(); ++count) {
				final String colorComponent = t.nextToken();
				final StringTokenizer t2 = new StringTokenizer(colorComponent, ":");

				if (t2.hasMoreTokens()) {
					final String componentID = t2.nextToken();

					if (t2.hasMoreTokens()) {
						final String componentValue = t2.nextToken();

						if (componentID.equals("r")) {
							r = Short.parseShort(componentValue);
						}
						else if (componentID.equals("g")) {
							g = Short.parseShort(componentValue);
						}
						else if (componentID.equals("b")) {
							b = Short.parseShort(componentValue);
						}
						else {
							throw newException("invalid color component " + componentID
									+ " in", value);
						}
					}
					else
						throw new KNXIllegalArgumentException("expected number after "
								+ componentID + ":");
				}
				else
					throw new KNXIllegalArgumentException("expected component identifier "
							+ "e.g. 'r:', 'g:', 'b:' in " + colorComponent);
			}
			if ((r == -1) || (g == -1) || (b == -1))
				throw newException("invalid color", value);

			set(r, g, b, dst, index);
		}
		catch (final KNXIllegalArgumentException e) {
			throw newException("invalid color", value, e);
		}
		catch (final NumberFormatException e) {
			throw newException("invalid number", value);
		}
	}

	private short[] set(final int red, final int green, final int blue, final short[] dst,
		final int index)
	{
		if (red < 0 || red > 255)
			throw new KNXIllegalArgumentException("red out of range [0..255]");
		if (green < 0 || green > 255)
			throw new KNXIllegalArgumentException("green out of range [0..255]");
		if (blue < 0 || blue > 255)
			throw new KNXIllegalArgumentException("blue out of range [0..255]");
		final int i = 3 * index;
		dst[i + RED] = (short) red;
		dst[i + GREEN] = (short) green;
		dst[i + BLUE] = (short) blue;
		return dst;
	}
}
