/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2018, 2019 B. Malinowsky

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

import java.util.EnumSet;
import java.util.Map;
import java.util.StringJoiner;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 22, type <b>Bit Array of Length 16 (B16)</b>. It provides DPTs for the
 * following application domains:
 * <ul>
 * <li>HVAC</li>
 * <li>System</li>
 * </ul>
 * The KNX data type width is 2 bytes. The default return value after creation is 0 for the defined bit array.
 * <p>
 * In value methods expecting string items, an item is either formatted as sequence of enum constant names, a
 * bit/boolean sequence (e.g., "0 1 1 0", "false true true false"), or a single value (e.g., "0x15", "3"). For a single
 * value, the following representations are supported, distinguished by its prefix:
 * <dl>
 * <dt>no prefix</dt>
 * <dd>for decimal numeral</dd>
 * <dt><code>0x</code>, <code>0X</code>, <code>#</code>
 * <dd>for hexadecimal numeral</dd>
 * <dt><code>0</code>
 * <dd>for octal numeral</dd>
 * </dl>
 */
@SuppressWarnings("checkstyle:javadocvariable")
public class DptXlator16BitSet extends DPTXlator {
	private interface EnumBase<E extends Enum<E> & EnumBase<E>> {
		@SuppressWarnings("unchecked")
		default int value() {
			return 1 << ((Enum<E>) this).ordinal();
		}
	}

	public static class EnumDpt<T extends Enum<T> & EnumBase<T>> extends DPT {
		private final Class<T> elements;

		public EnumDpt(final String typeId, final Class<T> elements) {
			this(typeId, elements.getSimpleName().replaceAll("\\B([A-Z])", " $1"), elements);
		}

		private EnumDpt(final String typeId, final String description, final Class<T> elements) {
			super(typeId, description, "0", Integer.toString(maxValue(elements)));
			this.elements = elements;
		}

		private T find(final int element) {
			for (final T e : EnumSet.allOf(elements))
				if (e.value() == element)
					return e;
			return null;
		}

		private T find(final String description) {
			for (final T e : EnumSet.allOf(elements))
				if (e.name().equals(description) || friendly(e.value()).equals(description))
					return e;
			return null;
		}

		private String textOf(final int element) {
			final T e = find(element);
			if (e != null)
				return e.name();
			throw new KNXIllegalArgumentException(getID() + " " + elements.getSimpleName() + " has no element " + element + " specified");
		}

		private String friendly(final int element) {
			return textOf(element).replaceAll("(\\p{Lower})\\B([A-Z])", "$1 $2");
		}
	}

	// HVAC domain

	public enum RhccStatus implements EnumBase<RhccStatus> {
		Fault, HeatingEcoMode, LimitFlowTemperature, LimitReturnTemperature, HeatingMorningBoost, EarlyMorningStart, EarlyEveningShutdown,
		HeatingDisabled, HeatingMode, CoolingEcoMode, PreCoolingMode, CoolingDisabled, DewPointAlarm, FrostAlarm, OverheatAlarm;
	}
	public static final EnumDpt<RhccStatus> DptRhccStatus = new EnumDpt<>("22.101", RhccStatus.class);

	// System domain

	public enum Medium implements EnumBase<Medium> {
		_0, TP1, PL110, _3, RF, Knxip;
	}
	public static final EnumDpt<Medium> DptMedia = new EnumDpt<>("22.1000", Medium.class);

	private static final Map<String, DPT> types = loadDatapointTypes(DptXlator16BitSet.class);

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlator16BitSet(final DPT dpt) throws KNXFormatException {
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId datapoint type ID
	 * @throws KNXFormatException on wrong formatted or unsupported <code>dptId</code>
	 */
	public DptXlator16BitSet(final String dptId) throws KNXFormatException {
		super(2);
		setTypeID(types, dptId);
	}

	@Override
	public String getValue() {
		return textOf(0);
	}

	/**
	 * Sets one new translation item from an unsigned value, replacing any old items.
	 *
	 * @param value the bit array as unsigned value, the dimension is determined by the set DPT, 0 &le; value &le;
	 *        maximum value of DPT
	 * @throws KNXFormatException if value is out of range specified by the DPT
	 * @see #getType()
	 */
	public final void setValue(final int value) throws KNXFormatException {
		data = toDpt(value);
	}

	/**
	 * Sets one new translation item from a set of enumeration elements, replacing any old items.
	 *
	 * @param elements the elements to set, the enumeration has to match the set DPT
	 * @see #getType()
	 */
	public final void setValue(final EnumSet<?> elements) {
		data = toDpt(elements);
	}

	@Override
	public final double getNumericValue() throws KNXFormatException {
		return fromDpt(0);
	}

	@Override
	public String[] getAllValues() {
		final int items = getItems();
		final String[] s = new String[items];
		for (int i = 0; i < items; i++)
			s[i] = textOf(i);
		return s;
	}

	@Override
	public final Map<String, DPT> getSubTypes() {
		return types;
	}

	/**
	 * @return all available subtypes of this translator
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic() {
		return types;
	}

	private String textOf(final int index) {
		final int offset = 2 * index;
		final int d = (data[offset] << 8) | data[offset + 1];
		final StringJoiner joiner = new StringJoiner(", ");
		for (int i = 0x4000; i > 0; i >>= 1)
			if ((d & i) == i)
				joiner.add(((EnumDpt<?>) dpt).friendly(i));
		// RHCC status special case: controller is cooling if HeatingMode = false and CoolingDisabled = false
		// show cooling mode in case no other bit is set
		if (dpt.equals(DptRhccStatus) && joiner.length() == 0)
			return "Cooling Mode";
		return joiner.toString();
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException {
		int result = 0;
		try {
			// try single dec/hex/octal number
			result = (Integer.decode(value));
			validate(result);
		}
		catch (final NumberFormatException nfe) {
			final EnumDpt<?> enumDpt = (EnumDpt<?>) dpt;

			// try as sequence: if sequence contains "," we use that as delimiter
			String regex = value.contains(",") ? "," : " ";
			// special case: a single pretty-printed constant, where we cannot use space delimiter
			if ("Cooling Mode".equals(value) || enumDpt.find(value) != null)
				regex = ",";

			final String[] split = value.split(regex, 0);
			if (split.length == 1 && "".equals(split[0]))
				return;

			for (int i = 0; i < split.length; i++) {
				final String s = split[split.length - 1 - i].trim(); // trim in case of ',' delimiter

				// try bit flag
				final int bit = "1".equals(s) || "true".equalsIgnoreCase(s) ? 1 : "0".equals(s) || "false".equalsIgnoreCase(s) ? 0 : -1;
				if (bit == 1)
					result |= bit << i;
				else if (bit == -1) {
					// RHCC status special case: controller in 'Cooling Mode' means HeatingMode = false and CoolingDisabled = false
					if (dpt.equals(DptRhccStatus) && "Cooling Mode".equals(s))
						continue;

					// try enum element name
					final Enum<?> e = enumDpt.find(s);
					if (e == null)
						throw newException("value is no element of " + enumDpt.elements.getSimpleName(), s);
					result |= 1 << e.ordinal();
				}
			}
		}
		final int offset = 2 * index;
		dst[offset] = (short) (result >> 8);
		dst[offset + 1] = ubyte(result);
	}

	private static short[] toDpt(final EnumSet<?> elements) {
		int v = 0;
		for (final Enum<?> e : elements)
			v |= 1 << e.ordinal();
		return new short[] { (short) (v >> 8), ubyte(v) };
	}

	private short[] toDpt(final int value) throws KNXFormatException {
		validate(value);
		return new short[] { (short) (value >> 8), ubyte(value) };
	}

	private int fromDpt(final int index) throws KNXFormatException {
		final int offset = 2 * index;
		final int v = (data[offset] << 8) | data[offset + 1];
		validate(v);
		return v;
	}

	// checks whether value is within the allowed DPT value range
	private void validate(final int value) throws KNXFormatException {
		final EnumDpt<?> enumDpt = (EnumDpt<?>) dpt;
		if (value < 0 || value > maxValue(enumDpt.elements))
			throw newException("value is out of range [" + enumDpt.getLowerValue() + ".." + enumDpt.getUpperValue() + "]",
					Integer.toString(value));
	}

	private static <E extends Enum<E>> int maxValue(final Class<E> elements) {
		final int bits = EnumSet.allOf(elements).size();
		return (1 << bits) - 1;
	}
}
