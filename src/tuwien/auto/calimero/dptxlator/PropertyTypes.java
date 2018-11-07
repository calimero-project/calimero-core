/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2018 B. Malinowsky

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.TranslatorTypes.MainType;

/**
 * Maintains all available KNX property data types (PDT).
 * <p>
 * It stores all available, registered PDTs and the associated datapoint type (DPT) for
 * such a property type.<br>
 * It offers methods to work with and alter these PDT to DPT mappings, to look up DPT
 * translators or to do complete translation of data.<br>
 *
 * @author B. Malinowsky
 */
public final class PropertyTypes
{
	/**
	 * Type for a value entry in the map of property types, returned by
	 * {@link PropertyTypes#getAllPropertyTypes()}.
	 * <p>
	 * Objects of this type are immutable.<br>
	 * For a more detailed description of main numbers and DPTs, see
	 * {@link TranslatorTypes}.
	 */
	public static class DPTID
	{
		private final int main;
		private final String dpt;

		/**
		 * Creates a new DPTID used to identify a DPT translator.
		 *
		 * @param mainNumber DPT main number identifying a data type matching the property
		 *        data type
		 * @param dpt appropriate datapoint type for the property type
		 */
		public DPTID(final int mainNumber, final String dpt)
		{
			main = mainNumber;
			this.dpt = dpt;
		}

		/**
		 * Returns the main number of the translator data type.
		 * <p>
		 * If the datapoint type returned by {@link #getDPT()} is formatted the preferred
		 * way as described in {@link TranslatorTypes}, the main number might be 0.
		 *
		 * @return main number (or 0) as int
		 */
		public final int getMainNumber()
		{
			return main;
		}

		/**
		 * Returns the datapoint type ID to be used in the translator.
		 * <p>
		 *
		 * @return datapoint type as string
		 */
		public final String getDPT()
		{
			return dpt;
		}
	}

	/**
	 * PDT_CONTROL, PDT = 0x00, usage dependent.
	 */
	public static final int PDT_CONTROL = 0x00;
	/**
	 * PDT_CHAR, PDT = 0x01, format V8.
	 */
	public static final int PDT_CHAR = 0x01;
	/**
	 * PDT_UNSIGNED_CHAR, PDT = 0x02, format U8.
	 */
	public static final int PDT_UNSIGNED_CHAR = 0x02;
	/**
	 * PDT_INT, PDT = 0x03, format V16.
	 */
	public static final int PDT_INT = 0x03;
	/**
	 * PDT_UNSIGNED_INT, PDT = 0x04, format U16.
	 */
	public static final int PDT_UNSIGNED_INT = 0x04;
	/**
	 * PDT_KNX_FLOAT, PDT = 0x05, format F16.
	 */
	public static final int PDT_KNX_FLOAT = 0x05;
	/**
	 * PDT_DATE, PDT = 0x06, format DPT_Date.
	 */
	public static final int PDT_DATE = 0x06;
	/**
	 * PDT_TIME, PDT = 0x07, format DPT_Time.
	 */
	public static final int PDT_TIME = 0x07;
	/**
	 * PDT_LONG, PDT = 0x08, format V32.
	 */
	public static final int PDT_LONG = 0x08;
	/**
	 * PDT_UNSIGNED_LONG, PDT = 0x09, format U32.
	 */
	public static final int PDT_UNSIGNED_LONG = 0x09;
	/**
	 * PDT_FLOAT, PDT = 0x0A, format F32.
	 */
	public static final int PDT_FLOAT = 0x0A;
	/**
	 * PDT_DOUBLE, PDT = 0x0B, format F64.
	 */
	public static final int PDT_DOUBLE = 0x0B;
	/**
	 * PDT_CHAR_BLOCK, PDT = 0x0C, format A[10].
	 */
	public static final int PDT_CHAR_BLOCK = 0x0C;
	/**
	 * PDT_POLL_GROUP_SETTINGS, PDT = 0x0D, format U16U8.
	 */
	public static final int PDT_POLL_GROUP_SETTINGS = 0x0D;
	/**
	 * PDT_SHORT_CHAR_BLOCK, PDT = 0x0E, format A[5].
	 */
	public static final int PDT_SHORT_CHAR_BLOCK = 0x0E;
	/**
	 * PDT_DATE_TIME, PDT = 0x0F, format DPT_DateTime.
	 */
	public static final int PDT_DATE_TIME = 0x0F;
	/**
	 * PDT_VARIABLE_LENGTH, PDT = 0x10, format DPT_VarString_8859_1.
	 */
	public static final int PDT_VARIABLE_LENGTH = 0x10;
	/**
	 * PDT_GENERIC_01, PDT = 0x11, format undefined, length = 1 octet.
	 */
	public static final int PDT_GENERIC_01 = 0x11;
	/**
	 * PDT_GENERIC_02, PDT = 0x12, format undefined, length = 2 octet.
	 */
	public static final int PDT_GENERIC_02 = 0x12;
	/**
	 * PDT_GENERIC_03, PDT = 0x13, format undefined, length = 3 octet.
	 */
	public static final int PDT_GENERIC_03 = 0x13;
	/**
	 * PDT_GENERIC_04, PDT = 0x14, format undefined, length = 4 octet.
	 */
	public static final int PDT_GENERIC_04 = 0x14;
	/**
	 * PDT_GENERIC_05, PDT = 0x15, format undefined, length = 5 octet.
	 */
	public static final int PDT_GENERIC_05 = 0x15;
	/**
	 * PDT_GENERIC_06, PDT = 0x16, format undefined, length = 6 octet.
	 */
	public static final int PDT_GENERIC_06 = 0x16;
	/**
	 * PDT_GENERIC_07, PDT = 0x17, format undefined, length = 7 octet.
	 */
	public static final int PDT_GENERIC_07 = 0x17;
	/**
	 * PDT_GENERIC_08, PDT = 0x18, format undefined, length = 8 octet.
	 */
	public static final int PDT_GENERIC_08 = 0x18;
	/**
	 * PDT_GENERIC_09, PDT = 0x19, format undefined, length = 9 octet.
	 */
	public static final int PDT_GENERIC_09 = 0x19;
	/**
	 * PDT_GENERIC_10, PDT = 0x1A, format undefined, length = 10 octet.
	 */
	public static final int PDT_GENERIC_10 = 0x1A;
	/**
	 * PDT_GENERIC_11, PDT = 0x1B, format undefined, length = 11 octet.
	 */
	public static final int PDT_GENERIC_11 = 0x1B;
	/**
	 * PDT_GENERIC_12, PDT = 0x1C, format undefined, length = 12 octet.
	 */
	public static final int PDT_GENERIC_12 = 0x1C;
	/**
	 * PDT_GENERIC_13, PDT = 0x1D, format undefined, length = 13 octet.
	 */
	public static final int PDT_GENERIC_13 = 0x1D;
	/**
	 * PDT_GENERIC_14, PDT = 0x1E, format undefined, length = 14 octet.
	 */
	public static final int PDT_GENERIC_14 = 0x1E;
	/**
	 * PDT_GENERIC_15, PDT = 0x1F, format undefined, length = 15 octet.
	 */
	public static final int PDT_GENERIC_15 = 0x1F;
	/**
	 * PDT_GENERIC_16, PDT = 0x20, format undefined, length = 16 octet.
	 */
	public static final int PDT_GENERIC_16 = 0x20;
	/**
	 * PDT_GENERIC_17, PDT = 0x21, format undefined, length = 17 octet.
	 */
	public static final int PDT_GENERIC_17 = 0x21;
	/**
	 * PDT_GENERIC_18, PDT = 0x22, format undefined, length = 18 octet.
	 */
	public static final int PDT_GENERIC_18 = 0x22;
	/**
	 * PDT_GENERIC_19, PDT = 0x23, format undefined, length = 19 octet.
	 */
	public static final int PDT_GENERIC_19 = 0x23;
	/**
	 * PDT_GENERIC_20, PDT = 0x24, format undefined, length = 20 octet.
	 */
	public static final int PDT_GENERIC_20 = 0x24;

	// Reserved 0x25 - 0x2F

	/**
	 * PDT_VERSION, PDT = 0x30, format DPT_Version.
	 */
	public static final int PDT_VERSION = 0x30;
	/**
	 * PDT_ALARM_INFO, PDT = 0x31, format DPT_AlarmInfo.
	 */
	public static final int PDT_ALARM_INFO = 0x31;
	/**
	 * PDT_BINARY_INFORMATION, PDT = 0x32, format B1.
	 */
	public static final int PDT_BINARY_INFORMATION = 0x32;
	/**
	 * PDT_BITSET8, PDT = 0x33, format B8.
	 */
	public static final int PDT_BITSET8 = 0x33;
	/**
	 * PDT_BITSET16, PDT = 0x34, format B16.
	 */
	public static final int PDT_BITSET16 = 0x34;
	/**
	 * PDT_ENUM8, PDT = 0x35, format N8.
	 */
	public static final int PDT_ENUM8 = 0x35;
	/**
	 * PDT_SCALING, PDT = 0x36, format U8.
	 */
	public static final int PDT_SCALING = 0x36;

	// Reserved 0x37 - 0x3B

	/**
	 * PDT_NE_VL, PDT = 0x3C, format undefined.
	 */
	public static final int PDT_NE_VL = 0x3C;
	/**
	 * PDT_NE_FL, PDT = 0x3D, format undefined.
	 */
	public static final int PDT_NE_FL = 0x3D;
	/**
	 * PDT_FUNCTION, PDT = 0x3E, format usage dependent.
	 */
	public static final int PDT_FUNCTION = 0x3E;

	// escape not used at all for now..
	/**
	 * PDT_ESCAPE, PDT = 0x3F, format defined or undefined.
	 */
	// private static final int PDT_ESCAPE = 0x3F;

	private static final Map<Integer, DPTID> pt = Collections.synchronizedMap(new HashMap<>());

	static {
		pt.put(PDT_CHAR, new DPTID(TranslatorTypes.TYPE_8BIT_SIGNED, "6.010"));
		pt.put(PDT_UNSIGNED_CHAR, new DPTID(TranslatorTypes.TYPE_8BIT_UNSIGNED, "5.010"));
		pt.put(PDT_INT, new DPTID(TranslatorTypes.TYPE_2OCTET_SIGNED, "8.001"));
		pt.put(PDT_UNSIGNED_INT, new DPTID(TranslatorTypes.TYPE_2OCTET_UNSIGNED, "7.001"));
		pt.put(PDT_KNX_FLOAT, new DPTID(TranslatorTypes.TYPE_2OCTET_FLOAT, "9.002"));
		pt.put(PDT_DATE, new DPTID(TranslatorTypes.TYPE_DATE, "11.001"));
		pt.put(PDT_TIME, new DPTID(TranslatorTypes.TYPE_TIME, "10.001"));
		pt.put(PDT_LONG, new DPTID(TranslatorTypes.TYPE_4OCTET_SIGNED, "13.001"));
		pt.put(PDT_UNSIGNED_LONG, new DPTID(TranslatorTypes.TYPE_4OCTET_UNSIGNED, "12.001"));
		pt.put(PDT_FLOAT, new DPTID(TranslatorTypes.TYPE_4OCTET_FLOAT, "14.005"));
		// p.put(PDT_DOUBLE), );
		pt.put(PDT_CHAR_BLOCK, new DPTID(24, "24.001"));
		// p.put(PDT_POLL_GROUP_SETTINGS), );
		pt.put(PDT_SHORT_CHAR_BLOCK, new DPTID(24, "24.001"));
		pt.put(PDT_DATE_TIME, new DPTID(TranslatorTypes.TYPE_DATE_TIME, "19.001"));
		pt.put(PDT_VARIABLE_LENGTH, new DPTID(24, "24.001"));
		pt.put(PDT_VERSION, new DPTID(217, "217.001"));
		pt.put(PDT_ALARM_INFO, new DPTID(219, "219.001"));
		pt.put(PDT_BINARY_INFORMATION, new DPTID(TranslatorTypes.TYPE_BOOLEAN, "1.002"));
		pt.put(PDT_BITSET8, new DPTID(21, "21.001"));
		pt.put(PDT_BITSET16, new DPTID(22, "22.100"));
		pt.put(PDT_ENUM8, new DPTID(20, "20.1000"));
		pt.put(PDT_SCALING, new DPTID(TranslatorTypes.TYPE_8BIT_UNSIGNED, "5.001"));
	}

	private PropertyTypes()
	{}

	/**
	 * Returns all property types which have an associated (but not necessarily
	 * implemented/available) DPT translator.
	 * <p>
	 * A map key is of type Integer, holding the PDT, a map value is of type {@link DPTID}.
	 *
	 * @return property type map
	 */
	public static Map<Integer, DPTID> getAllPropertyTypes()
	{
		return pt;
	}

	/**
	 * Does a lookup if the given property data type (PDT) has an associated translator
	 * available.
	 * <p>
	 * The translator looked for is specified in the property map. An available translator
	 * is implemented and can be used for translation.
	 *
	 * @param dataType property data type (PDT) to lookup
	 * @return <code>true</code> iff translator and its subtype was found,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasTranslator(final int dataType)
	{
		final DPTID dpt = pt.get(dataType);
		if (dpt != null)
			try {
				final MainType t = TranslatorTypes.getMainType(dpt.getMainNumber());
				if (t != null)
					return t.getSubTypes().get(dpt.getDPT()) != null;
			}
			catch (final NumberFormatException e) {}
			catch (final KNXException e) {}
		return false;
	}

	/**
	 * Creates a new DPT translator for the specified property type.
	 * <p>
	 * The translator is initialized with a subtype as specified by the property map.
	 * Also, appending of units is disabled in the returned translator.
	 *
	 * @param dataType property data type to get the associated translator for
	 * @return the created DPT translator
	 * @throws KNXException on PDT not found or translator could not be created
	 * @see TranslatorTypes#createTranslator(int, String)
	 */
	public static DPTXlator createTranslator(final int dataType) throws KNXException
	{
		final DPTID dpt = pt.get(dataType);
		if (dpt == null)
			throw new KNXException("PDT not found");
		final DPTXlator t = TranslatorTypes.createTranslator(dpt.getMainNumber(),
			dpt.getDPT());
		t.setAppendUnit(false);
		return t;
	}

	/**
	 * Utility method, like {@link #createTranslator(int)}, with the additional capability
	 * to set the data to be used by the DPT translator.
	 *
	 * @param dataType property data type to get the associated translator for
	 * @param data array with KNX DPT formatted data, the number of contained items is
	 *        determined by the used DPT
	 * @return the created DPT translator with the set data
	 * @throws KNXException on PDT not found or translator could not be created
	 * @see #createTranslator(int)
	 */
	public static DPTXlator createTranslator(final int dataType, final byte[] data)
		throws KNXException
	{
		final DPTXlator t = createTranslator(dataType);
		t.setData(data);
		return t;
	}

	/**
	 * Utility method for retrieving the string representations of the KNX DPT data of the
	 * specified property data type.
	 *
	 * @param dataType property data type of the <code>data</code> items
	 * @param data array with KNX DPT formatted data, the number of contained items is
	 *        determined by the used DPT
	 * @return string array with representation of the data items according to the used
	 *         DPT translator as returned by {@link DPTXlator#getAllValues()}, length of
	 *         array equals translated items in <code>data</code>
	 * @throws KNXException if translator could not be created
	 */
	public static String[] getValues(final int dataType, final byte[] data) throws KNXException
	{
		return createTranslator(dataType, data).getAllValues();
	}
}
