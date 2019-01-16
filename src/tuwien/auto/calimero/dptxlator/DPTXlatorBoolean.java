/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2019 B. Malinowsky

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

import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 1, type <b>Boolean</b>.
 * <p>
 * The KNX data type width is the lowest bit of 1 byte.<br>
 * The default return value after creation is <code>0</code>, i.e., <code>false</code>
 * for DPT Boolean for example.
 *
 * @author B. Malinowsky
 */
public class DPTXlatorBoolean extends DPTXlator
{
	/**
	 * DPT ID 1.001, Switch; values <b>off</b>, <b>on</b>.
	 */
	public static final DPT DPT_SWITCH = new DPT("1.001", "Switch", "off", "on");

	/**
	 * DPT ID 1.002, Boolean; values <b>false</b>, <b>true</b>.
	 */
	public static final DPT DPT_BOOL = new DPT("1.002", "Boolean", "false", "true");

	/**
	 * DPT ID 1.003, Enable; values <b>enable</b>, <b>disable</b>.
	 */
	public static final DPT DPT_ENABLE = new DPT("1.003", "Enable", "disable", "enable");

	/**
	 * DPT ID 1.004, Ramp; values <b>no ramp</b>, <b>ramp</b>.
	 */
	public static final DPT DPT_RAMP = new DPT("1.004", "Ramp", "no ramp", "ramp");

	/**
	 * DPT ID 1.005, Alarm; values <b>no alarm</b>, <b>alarm</b>.
	 */
	public static final DPT DPT_ALARM = new DPT("1.005", "Alarm", "no alarm", "alarm");

	/**
	 * DPT ID 1.006, Binary value; values <b>low</b>, <b>high</b>.
	 */
	public static final DPT DPT_BINARYVALUE = new DPT("1.006", "Binary value", "low", "high");

	/**
	 * DPT ID 1.007, Step; values <b>decrease</b>, <b>increase</b>.
	 */
	public static final DPT DPT_STEP = new DPT("1.007", "Step", "decrease", "increase");

	/**
	 * DPT ID 1.008, Up/Down; values <b>up</b>, <b>down</b>.
	 */
	public static final DPT DPT_UPDOWN = new DPT("1.008", "Up/Down", "up", "down");

	/**
	 * DPT ID 1.009, Open/Close; values <b>open</b>, <b>close</b>.
	 */
	public static final DPT DPT_OPENCLOSE = new DPT("1.009", "Open/Close", "open", "close");

	/**
	 * DPT ID 1.010, Start; values <b>stop</b>, <b>start</b>.
	 */
	public static final DPT DPT_START = new DPT("1.010", "Start", "stop", "start");

	/**
	 * DPT ID 1.011, State; values <b>inactive</b>, <b>active</b>.
	 */
	public static final DPT DPT_STATE = new DPT("1.011", "State", "inactive", "active");

	/**
	 * DPT ID 1.012, Invert; values <b>not inverted</b>, <b>inverted</b>.
	 */
	public static final DPT DPT_INVERT = new DPT("1.012", "Invert", "not inverted", "inverted");

	/**
	 * DPT ID 1.013, DimSendStyle; values <b>start/stop</b>, <b>cyclic</b>.
	 */
	public static final DPT DPT_DIMSENDSTYLE = new DPT("1.013", "Dim send-style", "start/stop",
			"cyclic");

	/**
	 * DPT ID 1.014, Input source; values <b>fixed</b>, <b>calculated</b>.
	 */
	public static final DPT DPT_INPUTSOURCE = new DPT("1.014", "Input source", "fixed",
			"calculated");

	/**
	 * DPT ID 1.015, Reset; values <b>no action</b> (dummy), <b>reset</b> (trigger).
	 */
	public static final DPT DPT_RESET = new DPT("1.015", "Reset", "no action", "reset");

	/**
	 * DPT ID 1.016, Acknowledge; values <b>no action</b> (dummy), <b>acknowledge</b>
	 * (trigger).
	 */
	public static final DPT DPT_ACK = new DPT("1.016", "Acknowledge", "no action", "acknowledge");

	/**
	 * DPT ID 1.017, Trigger; values <b>trigger</b>, <b>trigger</b>.
	 */
	public static final DPT DPT_TRIGGER = new DPT("1.017", "Trigger", "trigger", "trigger");

	/**
	 * DPT ID 1.018, Occupancy; values <b>not occupied</b>, <b>occupied</b>.
	 */
	public static final DPT DPT_OCCUPANCY = new DPT("1.018", "Occupancy", "not occupied",
			"occupied");

	/**
	 * DPT ID 1.019, Window/Door; values <b>closed</b>, <b>open</b>.
	 */
	public static final DPT DPT_WINDOW_DOOR = new DPT("1.019", "Window/Door", "closed", "open");

	/**
	 * DPT ID 1.021, Logical function; values <b>OR</b>, <b>AND</b>.
	 */
	public static final DPT DPT_LOGICAL_FUNCTION = new DPT("1.021", "Logical function", "OR", "AND");

	/**
	 * DPT ID 1.022, Scene A/B; values <b>scene A</b>, <b>scene B</b>.
	 * <p>
	 * Note, when displaying scene numbers, scene A is equal to number 1, scene B to
	 * number 2.
	 */
	public static final DPT DPT_SCENE_AB = new DPT("1.022", "Scene A/B", "scene A", "scene B");

	/**
	 * DPT ID 1.023, Shutter/Blinds mode; values <b>only move up/down mode</b> (shutter),
	 * <b>move up/down + step-stop mode</b> (blind).
	 */
	public static final DPT DPT_SHUTTER_BLINDS_MODE = new DPT("1.023", "Shutter/Blinds mode",
			"only move up/down", "move up/down + step-stop");

	/**
	 * DPT ID 1.100, HVAC Heat/Cool; values <b>cooling</b>, <b>heating</b>.
	 */
	public static final DPT DPT_HEAT_COOL = new DPT("1.100", "Heat/Cool", "cooling", "heating");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlatorBoolean.class);

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlatorBoolean(final DPT dpt) throws KNXFormatException
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
	public DPTXlatorBoolean(final String dptID) throws KNXFormatException
	{
		super(0);
		setTypeID(types, dptID);
		data = new short[1];
	}

	/**
	 * Sets the translation value from a boolean.
	 * <p>
	 * Any other items in the translator are discarded.
	 *
	 * @param value the boolean value
	 */
	public final void setValue(final boolean value)
	{
		data = new short[] { (short) (value ? 1 : 0) };
	}

	/**
	 * Returns the first translation item formatted as boolean.
	 * <p>
	 *
	 * @return boolean representation
	 */
	public final boolean getValueBoolean()
	{
		return (data[0] & 0x01) != 0 ? true : false;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	@Override
	public String getValue()
	{
		return fromDPT(0);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] buf = new String[data.length];
		for (int i = 0; i < data.length; ++i)
			buf[i] = fromDPT(i);
		return buf;
	}

	/**
	 * Returns the value of the first translation item, using 0 for boolean <code>false</code> and 1
	 * for boolean <code>true</code>.
	 *
	 * @return 0 for boolean false and 1 for boolean true
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 */
	@Override
	public final double getNumericValue()
	{
		return getValueBoolean() ? 1 : 0;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#setData(byte[], int)
	 */
	@Override
	public void setData(final byte[] data, final int offset)
	{
		if (offset < 0 || offset > data.length)
			throw new KNXIllegalArgumentException("illegal offset " + offset);
		final int size = data.length - offset;
		if (size == 0)
			throw new KNXIllegalArgumentException("data length " + size
				+ " < KNX data type width " + Math.max(1, getTypeSize()));
		this.data = new short[size];
		for (int i = 0; i < size; ++i)
			this.data[i] = (short) (data[offset + i] & 0x01);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getData(byte[], int)
	 */
	@Override
	public byte[] getData(final byte[] dst, final int offset)
	{
		final int end = Math.min(data.length, dst.length - offset);
		for (int i = 0; i < end; ++i)
			if (data[i] != 0)
				dst[offset + i] |= 1;
			else
				dst[offset + i] &= ~1;
		return dst;
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
	 * @return the subtypes of the boolean translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (dpt.getLowerValue().equalsIgnoreCase(value))
			dst[index] = 0;
		else if (dpt.getUpperValue().equalsIgnoreCase(value))
			dst[index] = 1;
		else
			throw newException("translation error, value not recognized", value);
	}

	private String fromDPT(final int index)
	{
		return data[index] != 0 ? dpt.getUpperValue() : dpt.getLowerValue();
	}
}
