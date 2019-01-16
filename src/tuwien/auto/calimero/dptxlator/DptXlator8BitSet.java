/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2017, 2019 B. Malinowsky

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

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

/**
 * Translator for KNX DPTs with main number 21, type <b>Bit Array of Length 8 (B8)</b>. It provides DPTs for the
 * following application domains:
 * <ul>
 * <li>Common use</li>
 * <li>HVAC</li>
 * <li>Lighting</li>
 * <li>System</li>
 * </ul>
 * The KNX data type width is 1 byte. The default return value after creation is 0 for the defined bit array.
 * <p>
 * In value methods expecting string items, an item is either formatted as single value (e.g., "0x15", "3") or as
 * bit/boolean sequence (e.g., "0 1 1 0", "false true true false"). For a single value, the following representations
 * are supported, distinguished by its prefix:
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
public class DptXlator8BitSet extends DPTXlator
{
	private interface EnumBase<E extends Enum<E> & EnumBase<E>>
	{
		@SuppressWarnings("unchecked")
		default int value()
		{
			return 1 << ((Enum<E>) this).ordinal();
		}
	}

	public static class EnumDpt<T extends Enum<T> & EnumBase<T>> extends DPT
	{
		private final Class<T> elements;

		public EnumDpt(final String typeId, final Class<T> elements)
		{
			this(typeId, elements.getSimpleName().replaceAll("\\B([A-Z])", " $1"), elements);
		}

		private EnumDpt(final String typeId, final String description, final Class<T> elements)
		{
			super(typeId, description, "0", Integer.toString(maxValue(elements)));
			this.elements = elements;
		}

		private T find(final int element)
		{
			for (final T e : EnumSet.allOf(elements))
				if (e.value() == element)
					return e;
			return null;
		}

		private T find(final String description)
		{
			for (final T e : EnumSet.allOf(elements))
				if (e.name().equals(description))
					return e;
			return null;
		}

		private String textOf(final int element)
		{
			final T e = find(element);
			if (e != null)
				return e.name();
			throw new KNXIllegalArgumentException(
					getID() + " " + elements.getSimpleName() + " has no element " + element + " specified");
		}
	}

	// Common use domain

	public enum GeneralStatus implements EnumBase<GeneralStatus> {
		OutOfService, Fault, Overridden, InAlarm, AlarmUnAck;
	}
	public static final EnumDpt<GeneralStatus> DptGeneralStatus = new EnumDpt<>("21.001", GeneralStatus.class);

	public enum DeviceControl implements EnumBase<DeviceControl> {
		UserStopped, OwnIndAddress, VerifyMode;
	}
	public static final EnumDpt<DeviceControl> DptDeviceControl = new EnumDpt<>("21.002", DeviceControl.class);

	// HVAC domain

	public enum ForcingSignal implements EnumBase<ForcingSignal> {
		ForceRequest, Protection, Oversupply, Overrun, DhwNorm, DhwLegio, RoomHeatingComfort, RoomHeatingMax;
	}
	public static final EnumDpt<ForcingSignal> DptForcingSignal = new EnumDpt<>("21.100", ForcingSignal.class);

	public enum ForcingSignalCool implements EnumBase<ForcingSignalCool> {
		ForceRequest;
	}
	public static final EnumDpt<ForcingSignalCool> DptForcingSignalCool = new EnumDpt<>("21.101",
			ForcingSignalCool.class);

	public enum RoomHeatingControllerStatus implements EnumBase<RoomHeatingControllerStatus> {
		Fault, EcoMode, FlowTempLimit, ReturnTempLimit, MorningBoost, StartOptimization, StopOptimization, SummerMode;
	}
	public static final EnumDpt<RoomHeatingControllerStatus> DptRoomHeatingControllerStatus = new EnumDpt<>("21.102",
			RoomHeatingControllerStatus.class);

	public enum SolarDhwControllerStatus implements EnumBase<SolarDhwControllerStatus> {
		Fault, SolarDhwLoadActive, SolarLoadSufficient;
	}
	public static final EnumDpt<SolarDhwControllerStatus> DptSolarDhwControllerStatus = new EnumDpt<>("21.103",
			SolarDhwControllerStatus.class);

	public enum FuelTypeSet implements EnumBase<FuelTypeSet> {
		Oil, Gas, SolidState;
	}
	public static final EnumDpt<FuelTypeSet> DptFuelTypeSet = new EnumDpt<>("21.104", FuelTypeSet.class);

	public enum RoomCoolingControllerStatus implements EnumBase<RoomCoolingControllerStatus> {
		Fault;
	}
	public static final EnumDpt<RoomCoolingControllerStatus> DptRoomCoolingControllerStatus = new EnumDpt<>("21.105",
			RoomCoolingControllerStatus.class);

	public enum VentilationControllerStatus implements EnumBase<VentilationControllerStatus> {
		Fault, FanActive, Heat, Cool;
	}
	public static final EnumDpt<VentilationControllerStatus> DptVentilationControllerStatus = new EnumDpt<>("21.106",
			VentilationControllerStatus.class);

	// Lighting domain

	public enum LightActuatorErrorInfo implements EnumBase<LightActuatorErrorInfo> {
		LoadDetectionFailed, Undervoltage, Overcurrent, Underload, DefectiveLoad, LampFailure, Overheat;
	}
	public static final EnumDpt<LightActuatorErrorInfo> DptLightActuatorErrorInfo = new EnumDpt<>("21.601",
			LightActuatorErrorInfo.class);

	// System domain

	public enum RFCommModeInfo implements EnumBase<RFCommModeInfo> {
		Asynchronous, BiBatMaster, BiBatSlave;
	}
	public static final EnumDpt<RFCommModeInfo> DptRFCommModeInfo = new EnumDpt<>("21.1000", RFCommModeInfo.class);

	public enum RFFilterModes implements EnumBase<RFFilterModes> {
		DomainAddress, SerialNumber, DoAAndSN;
	}
	public static final EnumDpt<RFFilterModes> DptRFFilterInfo = new EnumDpt<>("21.1001", RFFilterModes.class);

	public enum ChannelActivationState implements EnumBase<ChannelActivationState> {
		Channel1, Channel2, Channel3, Channel4, Channel5, Channel6, Channel7, Channel8;
	}
	public static final EnumDpt<ChannelActivationState> DptChannelActivation = new EnumDpt<>("21.1010",
			ChannelActivationState.class);

	private static final Map<String, DPT> types = loadDatapointTypes(DptXlator8BitSet.class);

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DptXlator8BitSet(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptId datapoint type ID
	 * @throws KNXFormatException on wrong formatted or unsupported <code>dptId</code>
	 */
	public DptXlator8BitSet(final String dptId) throws KNXFormatException
	{
		super(1);
		setTypeID(types, dptId);
	}

	@Override
	public String getValue()
	{
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
	public final void setValue(final int value) throws KNXFormatException
	{
		data = new short[] { toDpt(value) };
	}

	/**
	 * Sets one new translation item from a set of enumeration elements, replacing any old items.
	 *
	 * @param elements the elements to set, the enumeration has to match the set DPT
	 * @see #getType()
	 */
	public final void setValue(final EnumSet<?> elements)
	{
		data = new short[] { toDpt(elements) };
	}

	@Override
	public final double getNumericValue() throws KNXFormatException
	{
		return fromDpt(0);
	}

	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i)
			s[i] = textOf(i);
		return s;
	}

	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return all available subtypes of this translator
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String textOf(final int index)
	{
		final StringBuilder s = new StringBuilder();
		final int d = data[index];
		for (int i = 0x80; i > 0; i >>= 1)
			if ((d & i) == i)
				s.append(((EnumDpt<?>) dpt).textOf(i) + " ");
		return s.toString().trim();
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index) throws KNXFormatException
	{
		int result = 0;
		try {
			// try single dec/hex/octal number
			result = toDpt(Integer.decode(value));
		}
		catch (final NumberFormatException nfe) {
			// try as sequence
			final String[] split = value.split(" ");
			for (int i = 0; i < split.length; i++) {
				final String s = split[split.length - 1 - i];

				// try bit flag
				final int bit = "1".equals(s) || "true".equalsIgnoreCase(s) ? 1
						: "0".equals(s) || "false".equalsIgnoreCase(s) ? 0 : -1;
				if (bit == 1)
					result |= bit << i;
				else if (bit == -1) {
					// try enum element name
					final EnumDpt<?> enumDpt = (EnumDpt<?>) dpt;
					final Enum<?> e = enumDpt.find(s);
					if (e == null)
						throw newException("value is no element of " + enumDpt.elements.getSimpleName(), value);
					result |= 1 << e.ordinal();
				}
			}
		}
		dst[index] = (short) result;
	}

	private static short toDpt(final EnumSet<?> elements)
	{
		int v = 0;
		for (final Enum<?> e : elements)
			v |= 1 << e.ordinal();
		return (short) v;
	}

	private short toDpt(final int value) throws KNXFormatException
	{
		validate(value);
		return (short) value;
	}

	private short fromDpt(final int index) throws KNXFormatException
	{
		final short v = data[index];
		validate(v);
		return v;
	}

	// checks whether value is within the allowed DPT value range
	private void validate(final int value) throws KNXFormatException
	{
		final EnumDpt<?> enumDpt = (EnumDpt<?>) dpt;
		if (value < 0 || value > maxValue(enumDpt.elements))
			throw newException(
					"value is out of range [" + enumDpt.getLowerValue() + ".." + enumDpt.getUpperValue() + "]",
					Integer.toString(value));
	}

	private static <E extends Enum<E>> int maxValue(final Class<E> elements)
	{
		final int bits = EnumSet.allOf(elements).size();
		return (1 << bits) - 1;
	}
}
