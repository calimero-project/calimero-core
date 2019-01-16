/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2009, 2019 B. Malinowsky

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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 14, type <b>4-byte float</b>.
 * <p>
 * The KNX data type width is 4 bytes.<br>
 * This type is a four byte floating point format with a maximum usable range as specified in IEEE
 * 754. The largest positive finite float literal is 3.40282347e+38f. The smallest positive finite
 * non-zero literal of type float is 1.40239846e-45f. The negative minimum finite float literal is
 * -3.40282347e+38f. DPTs adjust the usable range to reasonable limits for its values, the translator
 * will check and enforce those DPT specific limits in all methods working with java values (e.g.
 * {@link #setValue(float)}). Data methods for KNX data (e.g. {@link #setData(byte[])} accept all
 * data within the maximum usable range. The DPTs 14.000 to 14.079 do not have any specific range
 * limits set.<br>
 * In value methods expecting a string type, the value is a float type representation.
 * <p>
 * The default return value after creation is <code>0.0</code>.<br>
 *
 * @author B. Malinowsky
 */
public class DPTXlator4ByteFloat extends DPTXlator
{
	// KNX 4 Octet Float is encoded in the IEEE 754 floating point format.
	// It allocates 4 Bytes as follows, MSB to LSB:
	// | 1 bit Sign | 8 bit exponent | 23 bit mantissa |
	// Implementation of this translator is simple, as the Java Float type is specified to
	// use the same encoding.

	/**
	 * DPT ID 14.000, Acceleration; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * ms-2.
	 */
	public static final DPT DPT_ACCELERATION = new DPT("14.000", "Acceleration", "-3.40282347e+38f",
			"3.40282347e+38f", "ms\u207B²");

	/**
	 * DPT ID 14.001, Acceleration, angular; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> rad s-2.
	 */
	public static final DPT DPT_ACCELERATION_ANGULAR = new DPT("14.001", "Acceleration, angular",
			"-3.40282347e+38f", "3.40282347e+38f", "rad s\u207B²");

	/**
	 * DPT ID 14.002, Activation Energy; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> J/mol.
	 */
	public static final DPT DPT_ACTIVATION_ENERGY = new DPT("14.002", "Activation energy",
			"-3.40282347e+38f", "3.40282347e+38f", "J/mol");

	/**
	 * DPT ID 14.003, Activity (radioactive); values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> s\u207B¹.
	 */
	public static final DPT DPT_ACTIVITY = new DPT("14.003", "Activity", "-3.40282347e+38f",
			"3.40282347e+38f", "s\u207B¹");

	/**
	 * DPT ID 14.004, Mol, amount of substance; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> mol.
	 */
	public static final DPT DPT_MOL = new DPT("14.004", "Mol", "-3.40282347e+38f",
			"3.40282347e+38f", "mol");

	/**
	 * DPT ID 14.005, Amplitude; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> (unit
	 * as appropriate).
	 */
	public static final DPT DPT_AMPLITUDE = new DPT("14.005", "Amplitude", "-3.40282347e+38f",
			"3.40282347e+38f");

	/**
	 * DPT ID 14.006, Angle, radiant; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * rad.
	 */
	public static final DPT DPT_ANGLE_RAD = new DPT("14.006", "Angle", "-3.40282347e+38f",
			"3.40282347e+38f", "rad");

	/**
	 * DPT ID 14.007, Angle, degree; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> °.
	 */
	public static final DPT DPT_ANGLE_DEG = new DPT("14.007", "Angle", "-3.40282347e+38f",
			"3.40282347e+38f", "°");

	/**
	 * DPT ID 14.008, Angular momentum; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * Js.
	 */
	public static final DPT DPT_ANGULAR_MOMENTUM = new DPT("14.008", "Momentum", "-3.40282347e+38f",
			"3.40282347e+38f", "Js");

	/**
	 * DPT ID 14.009, Angular velocity; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * rad/s.
	 */
	public static final DPT DPT_ANGULAR_VELOCITY = new DPT("14.009", "Angular velocity",
			"-3.40282347e+38f", "3.40282347e+38f", "rad/s");

	/**
	 * DPT ID 14.010, Area; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> m².
	 */
	public static final DPT DPT_AREA = new DPT("14.010", "Area", "-3.40282347e+38f",
			"3.40282347e+38f", "m²");

	/**
	 * DPT ID 14.011, Capacitance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> F.
	 */
	public static final DPT DPT_CAPACITANCE = new DPT("14.011", "Capacitance", "-3.40282347e+38f",
			"3.40282347e+38f", "F");

	/**
	 * DPT ID 14.012, Charge density (surface); values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> C m-2.
	 */
	public static final DPT DPT_CHARGE_DENSITY_SURFACE = new DPT("14.012",
			"Charge density (surface)", "-3.40282347e+38f", "3.40282347e+38f", "C m\u207B²");

	/**
	 * DPT ID 14.013, Charge density (volume); values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> C m\u207B³.
	 */
	public static final DPT DPT_CHARGE_DENSITY_VOLUME = new DPT("14.013",
			"Charge density (volume)", "-3.40282347e+38f", "3.40282347e+38f", "C m\u207B³");

	/**
	 * DPT ID 14.014, Compressibility; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * m²/N.
	 */
	public static final DPT DPT_COMPRESSIBILITY = new DPT("14.014", "Compressibility",
			"-3.40282347e+38f", "3.40282347e+38f", "m²/N");

	/**
	 * DPT ID 14.015, Conductance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> S =
	 * Ω\u207B¹.
	 */
	public static final DPT DPT_CONDUCTANCE = new DPT("14.015", "Conductance", "-3.40282347e+38f",
			"3.40282347e+38f", "Ω\u207B¹");

	/**
	 * DPT ID 14.016, Electrical conductivity; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> Ω\u207B¹m\u207B¹.
	 */
	public static final DPT DPT_ELECTRICAL_CONDUCTIVITY = new DPT("14.016",
			"Conductivity, electrical", "-3.40282347e+38f", "3.40282347e+38f", "Ω\u207B¹m\u207B¹");

	/**
	 * DPT ID 14.017, Density; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> kg m\u207B³.
	 */
	public static final DPT DPT_DENSITY = new DPT("14.017", "Density", "-3.40282347e+38f",
			"3.40282347e+38f", "kg m\u207B³");

	/**
	 * DPT ID 14.018, Electric charge; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * C.
	 */
	public static final DPT DPT_ELECTRIC_CHARGE = new DPT("14.018", "Electric charge",
			"-3.40282347e+38f", "3.40282347e+38f", "C");

	/**
	 * DPT ID 14.019, Electric current; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * A.
	 */
	public static final DPT DPT_ELECTRIC_CURRENT = new DPT("14.019", "Electric current",
			"-3.40282347e+38f", "3.40282347e+38f", "A");

	/**
	 * DPT ID 14.020, Electric current density; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> A m-2.
	 */
	public static final DPT DPT_ELECTRIC_CURRENTDENSITY = new DPT("14.020",
			"Electric current density", "-3.40282347e+38f", "3.40282347e+38f", "A m\u207B²");

	/**
	 * DPT ID 14.021, Electric dipole moment; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> Cm.
	 */
	public static final DPT DPT_ELECTRIC_DIPOLEMOMENT = new DPT("14.021", "Electric dipole moment",
			"-3.40282347e+38f", "3.40282347e+38f", "Cm");

	/**
	 * DPT ID 14.022, Electric displacement; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> C m-2.
	 */
	public static final DPT DPT_ELECTRIC_DISPLACEMENT = new DPT("14.022", "Electric displacement",
			"-3.40282347e+38f", "3.40282347e+38f", "C m\u207B²");

	/**
	 * DPT ID 14.023, Electric field strength; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> ￼￼￼￼￼￼￼￼￼V/m.
	 */
	public static final DPT DPT_ELECTRIC_FIELDSTRENGTH = new DPT("14.023",
			"Electric field strength", "-3.40282347e+38f", "3.40282347e+38f", "V/m");

	/**
	 * DPT ID 14.024, Electric flux; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * Vm. (In the KNX standard, the physical unit is given as 'c', but I don't think that is
	 * correct. I use the SI units.)
	 */
	public static final DPT DPT_ELECTRIC_FLUX = new DPT("14.024", "Electric flux",
			"-3.40282347e+38f", "3.40282347e+38f", "Vm");

	/**
	 * DPT ID 14.025, Electric flux density; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> C m-2.
	 */
	public static final DPT DPT_ELECTRIC_FLUX_DENSITY = new DPT("14.025", "Electric flux density",
			"-3.40282347e+38f", "3.40282347e+38f", "C m\u207B²");

	/**
	 * DPT ID 14.026, Electric polarization; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> C m-2.
	 */
	public static final DPT DPT_ELECTRIC_POLARIZATION = new DPT("14.026", "Electric polarization",
			"-3.40282347e+38f", "3.40282347e+38f", "C m\u207B²");

	/**
	 * DPT ID 14.027, Electric potential; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> V.
	 */
	public static final DPT DPT_ELECTRIC_POTENTIAL = new DPT("14.027", "Electric potential",
			"-3.40282347e+38f", "3.40282347e+38f", "V");

	/**
	 * DPT ID 14.028, Electric potential difference; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> V.
	 */
	public static final DPT DPT_ELECTRIC_POTENTIAL_DIFFERENCE = new DPT("14.028",
			"Electric potential difference", "-3.40282347e+38f", "3.40282347e+38f", "V");

	/**
	 * DPT ID 14.029, Electromagnetic moment; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> A m².
	 */
	public static final DPT DPT_ELECTROMAGNETIC_MOMENT = new DPT("14.029",
			"Electromagnetic moment", "-3.40282347e+38f", "3.40282347e+38f", "A m²");

	/**
	 * DPT ID 14.030, Electromotive force; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> V.
	 */
	public static final DPT DPT_ELECTROMOTIVE_FORCE = new DPT("14.030", "Electromotive force",
			"-3.40282347e+38f", "3.40282347e+38f", "V");

	/**
	 * DPT ID 14.031, Energy; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> J.
	 */
	public static final DPT DPT_ENERGY = new DPT("14.031", "Energy", "-3.40282347e+38f",
			"3.40282347e+38f", "J");

	/**
	 * DPT ID 14.032, Force; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> N.
	 */
	public static final DPT DPT_FORCE = new DPT("14.032", "Force", "-3.40282347e+38f",
			"3.40282347e+38f", "N");

	/**
	 * DPT ID 14.033, Frequency; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> ￼￼Hz =
	 * 1/s.
	 */
	public static final DPT DPT_FREQUENCY = new DPT("14.033", "Frequency", "-3.40282347e+38f",
			"3.40282347e+38f", "Hz");

	/**
	 * DPT ID 14.034, Frequency, angular; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> rad/s.
	 */
	public static final DPT DPT_ANGULAR_FREQUENCY = new DPT("14.034", "Frequency, angular",
			"-3.40282347e+38f", "3.40282347e+38f", "rad/s");

	/**
	 * DPT ID 14.035, Heat capacity; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * J/K.
	 */
	public static final DPT DPT_HEAT_CAPACITY = new DPT("14.035", "Heat capacity",
			"-3.40282347e+38f", "3.40282347e+38f", "J/K");

	/**
	 * DPT ID 14.036, Heat flow rate; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * W.
	 */
	public static final DPT DPT_HEAT_FLOWRATE = new DPT("14.036", "Heat flow rate",
			"-3.40282347e+38f", "3.40282347e+38f", "W");

	/**
	 * DPT ID 14.037, Quantity of heat; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * J.
	 */
	public static final DPT DPT_HEAT_QUANTITY = new DPT("14.037", "Heat quantity",
			"-3.40282347e+38f", "3.40282347e+38f", "J");

	/**
	 * DPT ID 14.038, Impedance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Ω.
	 */
	public static final DPT DPT_IMPEDANCE = new DPT("14.038", "Impedance", "-3.40282347e+38f",
			"3.40282347e+38f", "Ω");

	/**
	 * DPT ID 14.039, Length; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> m.
	 */
	public static final DPT DPT_LENGTH = new DPT("14.039", "Length", "-3.40282347e+38f",
			"3.40282347e+38f", "m");

	/**
	 * DPT ID 14.040, Quantity of light; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> J or lm s.
	 */
	public static final DPT DPT_LIGHT_QUANTITY = new DPT("14.040", "Quantity of Light",
			"-3.40282347e+38f", "3.40282347e+38f", "J");

	/**
	 * DPT ID 14.041, Luminance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> cd
	 * m-2.
	 */
	public static final DPT DPT_LUMINANCE = new DPT("14.041", "Luminance", "-3.40282347e+38f",
			"3.40282347e+38f", "cd m\u207B²");

	/**
	 * DPT ID 14.042, Luminous flux; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * lm.
	 */
	public static final DPT DPT_LUMINOUS_FLUX = new DPT("14.042", "Luminous flux",
			"-3.40282347e+38f", "3.40282347e+38f", "lm");

	/**
	 * DPT ID 14.043, Luminous intensity; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> cd.
	 */
	public static final DPT DPT_LUMINOUS_INTENSITY = new DPT("14.043", "Luminous intensity",
			"-3.40282347e+38f", "3.40282347e+38f", "cd");

	/**
	 * DPT ID 14.044, Magnetic field strength; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> A/m.
	 */
	public static final DPT DPT_MAGNETIC_FIELDSTRENGTH = new DPT("14.044",
			"Magnetic field strength", "-3.40282347e+38f", "3.40282347e+38f", "A/m");

	/**
	 * DPT ID 14.045, Magnetic flux; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * Wb.
	 */
	public static final DPT DPT_MAGNETIC_FLUX = new DPT("14.045", "Magnetic flux",
			"-3.40282347e+38f", "3.40282347e+38f", "Wb");

	/**
	 * DPT ID 14.046, Magnetic flux density; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> T.
	 */
	public static final DPT DPT_MAGNETIC_FLUX_DENSITY = new DPT("14.046", "Magnetic flux density",
			"-3.40282347e+38f", "3.40282347e+38f", "T");

	/**
	 * DPT ID 14.047, Magnetic moment; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * ￼A m².
	 */
	public static final DPT DPT_MAGNETIC_MOMENT = new DPT("14.047", "Magnetic moment",
			"-3.40282347e+38f", "3.40282347e+38f", "A m²");

	/**
	 * DPT ID 14.048, Magnetic polarization; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> T.
	 */
	public static final DPT DPT_MAGNETIC_POLARIZATION = new DPT("14.048", "Magnetic polarization",
			"-3.40282347e+38f", "3.40282347e+38f", "T");

	/**
	 * DPT ID 14.049, Magnetization; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * A/m.
	 */
	public static final DPT DPT_MAGNETIZATION = new DPT("14.049", "Magnetization",
			"-3.40282347e+38f", "3.40282347e+38f", "A/m");

	/**
	 * DPT ID 14.050, Magneto motive force; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> A.
	 */
	public static final DPT DPT_MAGNETOMOTIVE_FORCE = new DPT("14.050", "Magneto motive force",
			"-3.40282347e+38f", "3.40282347e+38f", "A");

	/**
	 * DPT ID 14.051, Mass; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> kg.
	 */
	public static final DPT DPT_MASS = new DPT("14.051", "Mass", "-3.40282347e+38f",
			"3.40282347e+38f", "kg");

	/**
	 * DPT ID 14.052, Mass flux; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> ￼kg/s.
	 */
	public static final DPT DPT_MASS_FLUX = new DPT("14.052", "Mass flux", "-3.40282347e+38f",
			"3.40282347e+38f", "kg/s");

	/**
	 * DPT ID 14.053, Momentum; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> N/s.
	 */
	public static final DPT DPT_MOMENTUM = new DPT("14.053", "Momentum", "-3.40282347e+38f",
			"3.40282347e+38f", "N/s");

	/**
	 * DPT ID 14.054, Phase angle, radiant; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> rad.
	 */
	public static final DPT DPT_PHASE_ANGLE_RAD = new DPT("14.054", "Phase angle, radiant",
			"-3.40282347e+38f", "3.40282347e+38f", "rad");

	/**
	 * DPT ID 14.055, Phase angle, degree; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> °.
	 */
	public static final DPT DPT_PHASE_ANGLE_DEG = new DPT("14.055", "Phase angle, degree",
			"-3.40282347e+38f", "3.40282347e+38f", "°");

	/**
	 * DPT ID 14.056, Power; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> W.
	 */
	public static final DPT DPT_POWER = new DPT("14.056", "Power", "-3.40282347e+38f",
			"3.40282347e+38f", "W");

	/**
	 * DPT ID 14.057, Power factor; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>.
	 */
	public static final DPT DPT_POWER_FACTOR = new DPT("14.057", "Power factor", "-3.40282347e+38f",
			"3.40282347e+38f");

	/**
	 * DPT ID 14.058, Pressure; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Pa = N
	 * m-2.
	 */
	public static final DPT DPT_PRESSURE = new DPT("14.058", "Pressure", "-3.40282347e+38f",
			"3.40282347e+38f", "Pa");

	/**
	 * DPT ID 14.059, Reactance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Ω.
	 */
	public static final DPT DPT_REACTANCE = new DPT("14.059", "Reactance", "-3.40282347e+38f",
			"3.40282347e+38f", "Ω");

	/**
	 * DPT ID 14.060, Resistance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Ω.
	 */
	public static final DPT DPT_RESISTANCE = new DPT("14.060", "Resistance", "-3.40282347e+38f",
			"3.40282347e+38f", "Ω");

	/**
	 * DPT ID 14.061, Resistivity; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Ωm.
	 */
	public static final DPT DPT_RESISTIVITY = new DPT("14.061", "Resistivity", "-3.40282347e+38f",
			"3.40282347e+38f", "Ωm");

	/**
	 * DPT ID 14.062, Self inductance; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * H.
	 */
	public static final DPT DPT_SELF_INDUCTANCE = new DPT("14.062", "Self inductance",
			"-3.40282347e+38f", "3.40282347e+38f", "H");

	/**
	 * DPT ID 14.063, Solid angle; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> sr.
	 */
	public static final DPT DPT_SOLID_ANGLE = new DPT("14.063", "Solid angle", "-3.40282347e+38f",
			"3.40282347e+38f", "sr");

	/**
	 * DPT ID 14.064, Sound intensity; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * W m-2.
	 */
	public static final DPT DPT_SOUND_INTENSITY = new DPT("14.064", "Sound intensity",
			"-3.40282347e+38f", "3.40282347e+38f", "W m\u207B²");

	/**
	 * DPT ID 14.065, Speed; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> m/s.
	 */
	public static final DPT DPT_SPEED = new DPT("14.065", "Speed", "-3.40282347e+38f",
			"3.40282347e+38f", "m/s");

	/**
	 * DPT ID 14.066, Stress; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Pa = N
	 * m-2.
	 */
	public static final DPT DPT_STRESS = new DPT("14.066", "Stress", "-3.40282347e+38f",
			"3.40282347e+38f", "Pa");

	/**
	 * DPT ID 14.067, Surface tension; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * N/m.
	 */
	public static final DPT DPT_SURFACE_TENSION = new DPT("14.067", "Surface tension",
			"-3.40282347e+38f", "3.40282347e+38f", "N/m");

	/**
	 * DPT ID 14.068, Temperature, common (in Celsius Degree); values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> °C.
	 */
	public static final DPT DPT_COMMON_TEMPERATURE = new DPT("14.068",
			"Temperature in Celsius Degree", "-3.40282347e+38f", "3.40282347e+38f", "°C");

	/**
	 * DPT ID 14.069, Temperature, absolute; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> K.
	 */
	public static final DPT DPT_ABSOLUTE_TEMPERATURE = new DPT("14.069", "Temperature, absolute",
			"-3.40282347e+38f", "3.40282347e+38f", "K");

	/**
	 * DPT ID 14.070, Temperature difference; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> K.
	 */
	public static final DPT DPT_TEMPERATURE_DIFFERENCE = new DPT("14.070",
			"Temperature difference", "-3.40282347e+38f", "3.40282347e+38f", "K");

	/**
	 * DPT ID 14.071, Thermal capacity; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * ￼￼J/K.
	 */
	public static final DPT DPT_THERMAL_CAPACITY = new DPT("14.071", "Thermal capacity",
			"-3.40282347e+38f", "3.40282347e+38f", "J/K");

	/**
	 * DPT ID 14.072, Thermal conductivity; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> W/(m K).
	 */
	public static final DPT DPT_THERMAL_CONDUCTIVITY = new DPT("14.072", "Thermal conductivity",
			"-3.40282347e+38f", "3.40282347e+38f", "W/m K\u207B¹");

	/**
	 * DPT ID 14.073, Thermoelectric power; values from <b>-3.40282347e+38f</b> to
	 * <b>3.40282347e+38f</b> V/K.
	 */
	public static final DPT DPT_THERMOELECTRIC_POWER = new DPT("14.073", "Thermoelectric power",
			"-3.40282347e+38f", "3.40282347e+38f", "V/K");

	/**
	 * DPT ID 14.074, Time; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> s.
	 */
	public static final DPT DPT_TIME = new DPT("14.074", "Time", "-3.40282347e+38f",
			"3.40282347e+38f", "s");

	/**
	 * DPT ID 14.075, Torque; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> Nm.
	 */
	public static final DPT DPT_TORQUE = new DPT("14.075", "Torque", "-3.40282347e+38f",
			"3.40282347e+38f", "Nm");

	/**
	 * DPT ID 14.076, Volume; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> m³.
	 */
	public static final DPT DPT_VOLUME = new DPT("14.076", "Volume", "-3.40282347e+38f",
			"3.40282347e+38f", "m³");

	/**
	 * DPT ID 14.077, Volume flux; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b>
	 * m³/s.
	 */
	public static final DPT DPT_VOLUME_FLUX = new DPT("14.077", "Volume flux", "-3.40282347e+38f",
			"3.40282347e+38f", "m³/s");

	/**
	 * DPT ID 14.078, Weight; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> N.
	 */
	public static final DPT DPT_WEIGHT = new DPT("14.078", "Weight", "-3.40282347e+38f",
			"3.40282347e+38f", "N");

	/**
	 * DPT ID 14.079, Work; values from <b>-3.40282347e+38f</b> to <b>3.40282347e+38f</b> J.
	 */
	public static final DPT DPT_WORK = new DPT("14.079", "Work", "-3.40282347e+38f",
			"3.40282347e+38f", "J");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator4ByteFloat.class);

	private final float min;
	private final float max;

	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator4ByteFloat(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for <code>dptID</code>.
	 *
	 * @param dptId available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) DPT
	 */
	public DPTXlator4ByteFloat(final String dptId) throws KNXFormatException
	{
		super(4);
		setTypeID(types, dptId);
		min = getLimit(dpt.getLowerValue());
		max = getLimit(dpt.getUpperValue());
		data = new short[4];
	}

	/**
	 * Sets the translation value from a float.
	 * <p>
	 * If succeeded, any other items in the translator are discarded.
	 *
	 * @param value the float value
	 * @throws KNXFormatException if <code>value</code> doesn't fit into KNX data type
	 */
	public void setValue(final float value) throws KNXFormatException
	{
		final short[] buf = new short[4];
		toDPT(value, buf, 0);
		data = buf;
	}

	/**
	 * Returns the first translation item formatted as float.
	 * <p>
	 *
	 * @return value as float
	 */
	public final float getValueFloat()
	{
		return fromDPT(0);
	}

	/**
	 * Returns the first translation item formatted as double.
	 *
	 * @return numeric value
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 * @see #getValueFloat()
	 */
	@Override
	public final double getNumericValue()
	{
		return getValueFloat();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	@Override
	public String[] getAllValues()
	{
		final String[] buf = new String[data.length / 4];
		for (int i = 0; i < buf.length; ++i)
			buf[i] = makeString(i);
		return buf;
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
	 * @return the subtypes of the 2-byte float translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String makeString(final int index)
	{
		final float f = fromDPT(index);
		final String s;
		if (Math.abs(f) < 100000) {
			s = String.valueOf(f);
		}
		else {
			final NumberFormat dcf = NumberFormat.getInstance(Locale.US);
			if (dcf instanceof DecimalFormat) {
				((DecimalFormat) dcf).applyPattern("0.#####E0");
			}
			s = dcf.format(f);
		}
		return appendUnit(s);
	}

	private float fromDPT(final int index)
	{
		final int i = 4 * index;
		final int bits = (data[i] << 24) | (data[i + 1] << 16) | (data[i + 2] << 8) | data[i + 3];
		return Float.intBitsToFloat(bits);
	}

	private void toDPT(final float value, final short[] dst, final int index)
		throws KNXFormatException
	{
		if (value < min || value > max)
			throw newException("translation error, value out of range ["
					+ dpt.getLowerValue() + ".." + dpt.getUpperValue() + "]", Float.toString(value));
		final int raw = Float.floatToRawIntBits(value);
		final int i = 4 * index;
		dst[i] = ubyte(raw >>> 24);
		dst[i + 1] = ubyte(raw >>> 16);
		dst[i + 2] = ubyte(raw >>> 8);
		dst[i + 3] = ubyte(raw);
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			toDPT(Float.parseFloat(removeUnit(value)), dst, index);
		}
		catch (final NumberFormatException e) {
			throw newException("wrong value format", value);
		}
	}

	private float getLimit(final String limit) throws KNXFormatException
	{
		try {
			return Float.parseFloat(limit);
		}
		catch (final NumberFormatException e) {}
		throw newException("limit not in valid DPT range", limit);
	}
}
