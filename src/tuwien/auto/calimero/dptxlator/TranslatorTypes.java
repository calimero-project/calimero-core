/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;

/**
 * Maintains available KNX datapoint main numbers and its associated DPT translators.
 * <p>
 * It stores all available, registered DP main numbers with the corresponding translator
 * and an optional description of the type.<br>
 * For more common used data types, the main types are declared as constants, although
 * this doesn't necessarily indicate a translator is actually available.<br>
 * All DPT translator implementations in this package are registered here and available by
 * default. Translators might be added or removed by the user.
 * <p>
 * A datapoint type consists of a data type and a dimension. The data type is referred to
 * through a main number, the existing dimensions of a data type are listed through sub
 * numbers. The data type specifies format and encoding, while dimension specifies the
 * range and unit.<br>
 * A datapoint type identifier (dptID for short), stands for one particular datapoint
 * type. The preferred - but not enforced - way of naming a dptID is using the expression
 * "<i>main number</i>.<i>sub number</i>".<br>
 * In short, a datapoint type has a dptID and standardizes one combination of format,
 * encoding, range and unit.
 * <p>
 * Note: main and sub refer to the former used terms major / minor.
 * 
 * @author B. Malinowsky
 * @see DPTXlator
 */
public final class TranslatorTypes
{
	/**
	 * DPT main number for <b>B1 (boolean)</b>, number = {@value #TYPE_BOOLEAN}.
	 * <p>
	 */
	public static final int TYPE_BOOLEAN = 1;

	/**
	 * DPT main number for <b>B2 (1 Bit controlled)</b>, number =
	 * {@value #TYPE_1BIT_CONTROLLED}.
	 * <p>
	 */
	public static final int TYPE_1BIT_CONTROLLED = 2;

	/**
	 * DPT main number for <b>B1U3 (3 Bit controlled)</b>, number =
	 * {@value #TYPE_3BIT_CONTROLLED}.
	 * <p>
	 */
	public static final int TYPE_3BIT_CONTROLLED = 3;

	/**
	 * DPT main number for <b>character set</b>, number = {@value #TYPE_CHARACTER_SET}.
	 * <p>
	 */
	public static final int TYPE_CHARACTER_SET = 4;

	/**
	 * DPT main number for <b>8 Bit unsigned value</b>, number =
	 * {@value #TYPE_8BIT_UNSIGNED}.
	 * <p>
	 */
	public static final int TYPE_8BIT_UNSIGNED = 5;

	/**
	 * DPT main number for <b>V8 (8 Bit signed value)</b>, shares main number with
	 * "Status with mode", number = {@value #TYPE_8BIT_SIGNED}.
	 * <p>
	 */
	public static final int TYPE_8BIT_SIGNED = 6;

	/**
	 * DPT main number for <b>2-octet unsigned value</b>, number =
	 * {@value #TYPE_2OCTET_UNSIGNED}.
	 * <p>
	 */
	public static final int TYPE_2OCTET_UNSIGNED = 7;

	/**
	 * DPT main number for <b>2-octet signed value</b>, number =
	 * {@value #TYPE_2OCTET_SIGNED}.
	 * <p>
	 */
	public static final int TYPE_2OCTET_SIGNED = 8;

	/**
	 * DPT main number for <b>2-octet float value</b>, number =
	 * {@value #TYPE_2OCTET_FLOAT}.
	 * <p>
	 */
	public static final int TYPE_2OCTET_FLOAT = 9;

	/**
	 * DPT main number for <b>time</b>, number = {@value #TYPE_TIME}.
	 * <p>
	 */
	public static final int TYPE_TIME = 10;

	/**
	 * DPT main number for <b>date</b>, number = {@value #TYPE_DATE}.
	 * <p>
	 */
	public static final int TYPE_DATE = 11;

	/**
	 * DPT main number for <b>4-octet unsigned value</b>, number =
	 * {@value #TYPE_4OCTET_UNSIGNED}.
	 * <p>
	 */
	public static final int TYPE_4OCTET_UNSIGNED = 12;

	/**
	 * DPT main number for <b>4-octet signed value</b>, number =
	 * {@value #TYPE_4OCTET_SIGNED}.
	 * <p>
	 */
	public static final int TYPE_4OCTET_SIGNED = 13;

	/**
	 * DPT main number for <b>4-octet float value</b>, number =
	 * {@value #TYPE_4OCTET_FLOAT}.
	 * <p>
	 */
	public static final int TYPE_4OCTET_FLOAT = 14;

	/**
	 * DPT main number for <b>access data</b>, number = {@value #TYPE_ACCESS}.
	 * <p>
	 */
	public static final int TYPE_ACCESS = 15;

	/**
	 * DPT main number for <b>string</b>, number = {@value #TYPE_STRING}.
	 * <p>
	 */
	public static final int TYPE_STRING = 16;

	/**
	 * DPT main number for <b>scene number</b>, number = {@value #TYPE_SCENE_NUMBER}.
	 * <p>
	 */
	public static final int TYPE_SCENE_NUMBER = 17;
	
	/**
	 * DPT main number for <b>scene control</b>, number = {@value #TYPE_SCENE_CONTROL}.
	 * <p>
	 */
	public static final int TYPE_SCENE_CONTROL = 18;
	
	/**
	 * DPT main number for <b>date/time</b>, number = {@value #TYPE_DATE_TIME}.
	 * <p>
	 */
	public static final int TYPE_DATE_TIME = 19;

	/**
	 * DPT main number for <b>8 Bit enumeration</b>, number = {@value #TYPE_ENUM8}.
	 * <p>
	 */
	public static final int TYPE_ENUM8 = 20;
	
	/**
	 * Maps a data type main number to a corresponding translator class doing the DPT
	 * translations.
	 * <p>
	 * Objects of this type are immutable.<br>
	 */
	public static class MainType
	{
		private final Class xlator;
		private final String desc;
		private final int main;

		/**
		 * Creates a new main number to translator mapping.
		 * <p>
		 * 
		 * @param mainNumber main number assigned to the data type
		 * @param translator represents a translator class of type {@link DPTXlator}
		 * @param description textual information describing this data type to a user, use
		 *        <code>null</code> for no description
		 */
		public MainType(final int mainNumber, final Class translator, final String description)
		{
			if (mainNumber <= 0)
				throw new KNXIllegalArgumentException("invalid main number");
			if (!DPTXlator.class.isAssignableFrom(translator) || DPTXlator.class.equals(translator))
				throw new KNXIllegalArgumentException(translator.getName()
						+ " is not a valid DPT translator type");
			main = mainNumber;
			xlator = translator;
			desc = description == null ? "" : description;
		}

		/**
		 * Returns the data type main number.
		 * <p>
		 * 
		 * @return main number as int
		 */
		public final int getMainNumber()
		{
			return main;
		}

		/**
		 * Creates a new translator for the given datapoint type.
		 * <p>
		 * 
		 * @param dpt datapoint type specifying the particular translation behavior; if
		 *        the datapoint type is not part of the translator of this main type, a
		 *        {@link KNXFormatException} is thrown
		 * @return the new {@link DPTXlator} instance
		 * @throws KNXFormatException to forward all target exceptions thrown in the
		 *         constructor of the translator
		 * @throws KNXException thrown on translator class creation errors (e.g. security /
		 *         access problems)
		 */
		public final DPTXlator createTranslator(final DPT dpt) throws KNXException
		{
			return createTranslator(dpt.getID());
		}

		/**
		 * Creates a new instance of the translator for the given datapoint type ID.
		 * <p>
		 * 
		 * @param dptID datapoint type ID for selecting a particular kind of value
		 *        translation; if the datapoint type ID is not part of the translator of
		 *        this main type, a {@link KNXFormatException} is thrown
		 * @return the new {@link DPTXlator} instance
		 * @throws KNXFormatException to forward all target exceptions thrown in the
		 *         constructor of the translator
		 * @throws KNXException thrown on translator class creation errors (e.g. security /
		 *         access problems)
		 */
		public DPTXlator createTranslator(final String dptID) throws KNXException
		{
			try {
				return (DPTXlator) xlator.getConstructor(new Class[] { String.class }).newInstance(
						new String[] { dptID });
			}
			catch (final InvocationTargetException e) {
				// try to forward encapsulated target exception
				if (e.getTargetException() instanceof KNXFormatException)
					throw (KNXFormatException) e.getTargetException();
				// throw generic message
				throw new KNXFormatException("failed to init translator", dptID);
			}
			catch (final NoSuchMethodException e) {
				DPTXlator.logger.fatal("DPT translator is required to "
					+ "have a public constructor(String dptID)");
				throw new KNXException("interface specification error at translator");
			}
			catch (final Exception e) {
				// for SecurityException, InstantiationException, IllegalAccessException
				throw new KNXException("failed to create translator, " + e.getMessage());
			}
		}

		/**
		 * Returns the translator class used for this main type.
		 * <p>
		 * 
		 * @return the translator class as {@link Class} object
		 */
		public Class getTranslator()
		{
			return xlator;
		}

		/**
		 * Returns the description to this main type, if any.
		 * <p>
		 * 
		 * @return description as String, or the empty string if no description set
		 */
		public final String getDescription()
		{
			return desc;
		}

		/**
		 * Returns a map containing all implemented subtypes.
		 * <p>
		 * 
		 * @return available subtypes as {@link Map}
		 * @throws KNXException thrown on problems accessing the translator while
		 *         retrieving sub types (e.g. security / access problem) for external,
		 *         user supplied translators
		 * @see DPTXlator#getSubTypes()
		 */
		public Map getSubTypes() throws KNXException
		{
			try {
				return (Map) xlator.getDeclaredMethod("getSubTypesStatic", null).invoke(null, null);
			}
			catch (final NoSuchMethodException e) {
				throw new KNXException("no method to get subtypes, " + e.getMessage());
			}
			catch (final Exception e) {
				// for SecurityException and IllegalAccessException
				// no reason for InvocationTargetException
				throw new KNXException("security / access problem, " + e.getMessage());
			}
		}
	}

	private static final Map map;

	static {
		map = Collections.synchronizedMap(new HashMap(20));
		addTranslator(TYPE_BOOLEAN, "DPTXlatorBoolean", "Boolean (main type 1)");
		addTranslator(TYPE_1BIT_CONTROLLED, "DPTXlator1BitControlled",
				"Boolean controlled (main type 2");
		addTranslator(TYPE_3BIT_CONTROLLED, "DPTXlator3BitControlled",
				"3 Bit controlled (main type 3)");
		addTranslator(TYPE_8BIT_UNSIGNED, "DPTXlator8BitUnsigned",
				"8 Bit unsigned value (main type 5)");
		addTranslator(TYPE_2OCTET_UNSIGNED, "DPTXlator2ByteUnsigned",
				"2 octet unsigned value (main type 7)");
		addTranslator(TYPE_2OCTET_FLOAT, "DPTXlator2ByteFloat", "2 octet float value (main type 9)");
		addTranslator(TYPE_TIME, "DPTXlatorTime", "Time (main type 10)");
		addTranslator(TYPE_DATE, "DPTXlatorDate", "Date (main type 11)");
		addTranslator(TYPE_4OCTET_UNSIGNED, "DPTXlator4ByteUnsigned",
				"4 octet unsigned value (main type 12)");
		addTranslator(TYPE_4OCTET_SIGNED, "DPTXlator4ByteSigned",
				"4 octet signed value (main type 13)");
		addTranslator(TYPE_4OCTET_FLOAT, "DPTXlator4ByteFloat",
				"4 octet float value (main type 14)");
		addTranslator(TYPE_STRING, "DPTXlatorString", "String (main type 16)");
		addTranslator(TYPE_SCENE_NUMBER, "DPTXlatorSceneNumber", "Scene number (main type 17)");
		addTranslator(TYPE_SCENE_CONTROL, "DPTXlatorSceneControl", "Scene control (main type 18)");
		addTranslator(TYPE_DATE_TIME, "DPTXlatorDateTime", "Date with time (main type 19)");
	}

	private TranslatorTypes()
	{}
	
	private static void addTranslator(final int main, final String className, final String desc)
	{
		try {
			map.put(new Integer(main), new MainType(main,
					Class.forName("tuwien.auto.calimero.dptxlator." + className), desc));
			DPTXlator.logger.trace(desc + " loaded");
		}
		catch (final ClassNotFoundException e) {
			DPTXlator.logger.warn(className + " not found, " + desc + " not added");
		}
	}
	
	/**
	 * Returns the {@link MainType} object assigned the given data type main number.
	 * <p>
	 * 
	 * @param mainNumber main type to lookup
	 * @return the main type information found, or <code>null</code> if main number not
	 *         listed
	 */
	public static MainType getMainType(final int mainNumber)
	{
		return (MainType) map.get(new Integer(mainNumber));
	}

	/**
	 * Returns all available data types which have a DPT translator implementation
	 * assigned.
	 * <p>
	 * The map returned is the same used by this class for type lookup. Map entries can be
	 * added, likewise entries might be removed to change future lookup results.
	 * 
	 * @return a {@link Map} containing all data types as {@link MainType} objects
	 */
	public static Map getAllMainTypes()
	{
		return map;
	}

	/**
	 * Does a lookup if the specified DPT is supported by a DPT translator.
	 * <p>
	 * 
	 * @param mainNumber data type main number, number >= 0; use 0 to infer translator
	 *        type from <code>dptID</code> argument only
	 * @param dptID datapoint type ID to lookup this particular kind of value translation
	 * @return <code>true</code> iff translator was found, <code>false</code>
	 *         otherwise
	 */
	public static boolean hasTranslator(final int mainNumber, final String dptID)
	{
		try {
			final MainType t = getMainType(getMainNumber(mainNumber, dptID));
			if (t != null)
				return t.getSubTypes().get(dptID) != null;
		}
		catch (final NumberFormatException e) {}
		catch (final KNXException e) {}
		return false;
	}

	/**
	 * Creates a DPT translator for the given datapoint type ID.
	 * <p>
	 * The translation behavior of a DPT translator instance is uniquely defined by the
	 * supplied datapoint type ID.
	 * <p>
	 * If the <code>dptID</code> argument is built up the recommended way, that is "<i>main
	 * number</i>.<i>sub number</i>", the <code>mainNumber</code> argument might be
	 * left 0 to use the datapoint type ID only.<br>
	 * Note, that we don't enforce any particular or standardized format on the dptID
	 * structure, so using a different formatted dptID solely without main number argument
	 * results in undefined behavior.
	 * 
	 * @param mainNumber data type main number, number >= 0; use 0 to infer translator
	 *        type from <code>dptID</code> argument only
	 * @param dptID datapoint type ID for selecting a particular kind of value translation
	 * @return the new {@link DPTXlator} object
	 * @throws KNXException on main type not found or creation failed (refer to
	 *         {@link MainType#createTranslator(String)})
	 */
	public static DPTXlator createTranslator(final int mainNumber, final String dptID)
		throws KNXException
	{
		try {
			final int main = getMainNumber(mainNumber, dptID);
			final MainType type = (MainType) map.get(new Integer(main));
			if (type != null)
				return type.createTranslator(dptID);
		}
		catch (final NumberFormatException e) {}
		throw new KNXException("main number not found for " + dptID);
	}

	/**
	 * Creates a DPT translator for the given datapoint type.
	 * <p>
	 * The translation behavior of a DPT translator instance is uniquely defined by the
	 * supplied datapoint type.
	 * <p>
	 * If translator creation according {@link #createTranslator(int, String)} fails, all
	 * available main types are enumerated to find an appropriate translator.
	 * 
	 * @param dpt datapoint type selecting a particular kind of value translation
	 * @return the new {@link DPTXlator} object
	 * @throws KNXException if no translator could be found or creation failed
	 */
	public static DPTXlator createTranslator(final DPT dpt) throws KNXException
	{
		try {
			return createTranslator(0, dpt.getID());
		}
		catch (final KNXException e) {
			for (final Iterator i = map.values().iterator(); i.hasNext(); )
				try {
					return ((MainType) i.next()).createTranslator(dpt);
				}
				catch (final KNXException ignore) {}
		}
		throw new KNXException("failed to create translator for DPT " + dpt.getID());
	}
	
	// throws NumberFormatException
	private static int getMainNumber(final int mainNumber, final String dptID)
	{
		return mainNumber != 0 ? mainNumber : Integer.parseInt(dptID.substring(0,
				dptID.indexOf('.')));
	}
}
