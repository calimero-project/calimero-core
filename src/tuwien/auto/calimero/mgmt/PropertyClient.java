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

package tuwien.auto.calimero.mgmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tuwien.auto.calimero.Settings;
import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteUnsigned;
import tuwien.auto.calimero.dptxlator.PropertyTypes;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXIllegalStateException;
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.xml.Attribute;
import tuwien.auto.calimero.xml.Element;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLFactory;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

/**
 * A client to access properties in interface objects of a device.
 * <p>
 * This can be done in different ways, to specify the kind of access a property adapter is
 * supplied on creation of the property client. The implementation of the
 * {@link PropertyAdapter} interface methods don't need to be synchronized for use by this
 * property client.
 * <p>
 * Properties can be retrieved or set in the device, property descriptions can be read and
 * scans of properties can be done.<br>
 * If desired, the property data type of a property element is used to return an
 * appropriate DPT translator or the ready formatted string representation.<br>
 * The DPT translators used are requested from {@link PropertyTypes} by default, or, if
 * property definitions were loaded, there will be a lookup in these data at first (i.e.
 * loaded property definitions take priority over the default PDT to DPT mapping).
 * <p>
 * It is possible to load property definitions with information about KNX properties from
 * a resource to be used by the property client for lookup requests and property type
 * translation. Also, definitions can be saved to a resource. A global resource handler
 * takes care of working with the resources where those definitions are stored. Loaded
 * definitions can added to a property client for subsequent lookup.<br>
 * Nevertheless, adding any definitions is not required for a client to work properly,
 * i.e., this functionality is optional.
 * <p>
 * By default, the resource handler uses a xml property file structure.<br>
 * XML file layout: <br>
 * &lt;propertyDefinitions&gt;<br>
 * &lt;object type=(object-type:number | "global")&gt;<br>
 * &lt;property pid=PID:number pidName=PID-name:string name=friendly-name:string
 * pdt=PDT:number [dpt=DPT-ID:string] rw=("R"|"W"|"R/W"|"R(/W"):string
 * writeEnabled=("0"|"1"|"0/1"):string&gt;<br>
 * &lt;usage&gt;<br>
 * usage description and additional information<br>
 * &lt;/usage&gt;<br>
 * &lt;/property&gt;<br>
 * ...next property<br>
 * &lt;/object&gt;<br>
 * ...next object<br>
 * &lt;/propertyDefinitions&gt;<br>
 * <br>
 * Attribute values of type number might be written in hexadecimal form by prepending
 * "0x". The optional attribute "dpt" is to specify a DPT to use for the property, it will
 * be used in preference before the default DPT assigned to the PDT.<br>
 * The attribute "pdt" might have a value of "&lt;tbd&gt;", standing for "to be defined",
 * in this case the PDT value used by the property client is -1.
 * <p>
 * Reduced Property Interfaces:<br>
 * When working with reduced property interfaces, the user has to be aware of the
 * limitations and act accordingly.
 * <p>
 * Some KNX devices only support a 5 Bit field for storing the Property Data Type (PDT),
 * i.e., they only use a PDT identifier up to value 0x1F. When accessing property
 * descriptions in the interface object (for example, using
 * {@link PropertyClient#getDescription(int, int)}), not all existing PDTs can be
 * transmitted. Consequently, for properties that are formatted according any of the
 * higher PDTs, "alternative PDTs" get used. Following implementations are known to
 * support only a 5 Bit PDT in the description:
 * <ul>
 * <li>mask 0x0020</li>
 * <li>mask 0x0021</li>
 * <li>mask 0x0701</li>
 * </ul>
 * In general, it is not possible for the property client to deduce the actual type to use
 * for encoding/decoding values from such an "alternative PDT". For these cases, the
 * property client has to rely on property definitions supplied by the user through
 * {@link PropertyClient#loadDefinitions(String, PropertyClient.ResourceHandler)}.
 * <p>
 * A note on property descriptions:<br>
 * With a local device management adapter, not all information is supported when reading a
 * description, or is not supported by the protocol at all. In particular, no access
 * levels for read/write (i.e., access is always done with maximum rights) and no property
 * data type (PDT) are available. Also, the maximum number of elements allowed in the
 * property is not available (only the current number of elements).<br>
 * All methods for property access invoked after a close of the property client will throw
 * a {@link KNXIllegalStateException}.
 * 
 * @author B. Malinowsky
 * @see PropertyAdapter
 * @see PropertyTypes
 */
public class PropertyClient implements PropertyAccess
{
	/**
	 * Provides an interface to load property definitions from a resource, and store
	 * property definitions into a resource.
	 * <p>
	 * It is used by the property client when loading or saving of property definitions is
	 * requested.
	 * <p>
	 * To allow the property client handling a user defined property resource, create a
	 * new property resource implementing this interface and supply it to the load/save
	 * definition methods.<br>
	 * It is not necessary for subtypes implementing this interface to synchronize the
	 * methods for concurrent access. All library access to a resource handler instance
	 * will occur within the execution thread of the invoking method, or the library will
	 * ensure its own appropriate synchronization on the handler.
	 * 
	 * @author B. Malinowsky
	 */
	public static interface ResourceHandler
	{
		/**
		 * Loads the properties from the resource.
		 * <p>
		 * 
		 * @param resource the identifier of the resource used for loading the properties
		 * @return a collection containing the property definitions of type
		 *         {@link PropertyClient.Property}
		 * @throws KNXException on error reading from the resource
		 */
		Collection load(String resource) throws KNXException;

		/**
		 * Saves the properties to the resource.
		 * 
		 * @param resource the identifier of the resource used for saving the properties
		 * @param definitions the property definitions in a collection holding
		 *        {@link PropertyClient.Property}-type values
		 * @throws KNXException on error writing to the resource
		 */
		void save(String resource, Collection definitions) throws KNXException;
	}

	/**
	 * Key value in the map returned by {@link PropertyClient#getDefinitions()}.
	 * <p>
	 * A key consists of the interface object type and the property identifier of the
	 * associated property. If the property is defined globally, the global object type
	 * {@link PropertyClient.PropertyKey#GLOBAL_OBJTYPE} is used.
	 * 
	 * @author B. Malinowsky
	 */
	public static final class PropertyKey implements Comparable
	{
		/** Identifier for a property defined with global object type. */
		public static final int GLOBAL_OBJTYPE = -1;

		private final int ot;
		private final int id;

		/**
		 * Creates a new key for a global defined property.
		 * <p>
		 * 
		 * @param pid property identifier
		 */
		public PropertyKey(final int pid)
		{
			ot = GLOBAL_OBJTYPE;
			id = pid;
		}

		/**
		 * Creates a new key for a property.
		 * <p>
		 * 
		 * @param objType object type of the property
		 * @param pid property identifier
		 */
		public PropertyKey(final int objType, final int pid)
		{
			ot = objType;
			id = pid;
		}

		/**
		 * Returns the property identifier part of this key.
		 * <p>
		 * 
		 * @return the PID as unsigned number
		 */
		public int getPID()
		{
			return id;
		}

		/**
		 * Returns whether the property is defined with global object type.
		 * <p>
		 * 
		 * @return <code>true</code> if property has global object type,
		 *         <code>false</code> if property has a specific object type
		 */
		public boolean isGlobal()
		{
			return ot == GLOBAL_OBJTYPE;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode()
		{
			return ot << 16 | id;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(final Object obj)
		{
			if (obj instanceof PropertyKey) {
				final PropertyKey key = (PropertyKey) obj;
				return ot == key.ot && id == key.id;
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(final Object o)
		{
			final int rhs = o.hashCode();
			return hashCode() < rhs ? -1 : hashCode() > rhs ? 1 : 0;
		}
	}

	/**
	 * Stores property definition information of one property, used for type translation
	 * and property lookup by a property client.
	 * <p>
	 * 
	 * @author B. Malinowsky
	 */
	public static class Property
	{
		final int id;
		final String name;
		final int objType;
		final int pdt;
		final String dpt;
		final String propName;
		final int read;
		final int write;

		/**
		 * Creates a new property object of the supplied information.
		 * <p>
		 * 
		 * @param pid property identifier
		 * @param pidName name of the property ID
		 * @param propertyName property name, a friendly readable name for the property
		 * @param objectType object type the property belongs to
		 * @param pdt property data type
		 * @param dpt datapoint type, use <code>null</code> if no DPT specified or to
		 *        indicate default DPT usage
		 */
		public Property(final int pid, final String pidName, final String propertyName,
			final int objectType, final int pdt, final String dpt)
		{
			id = pid;
			name = pidName;
			propName = propertyName;
			objType = objectType;
			this.pdt = pdt;
			this.dpt = dpt;
			read = 0;
			write = 0;
		}

		Property(final int pid, final String pidName, final String propertyName,
			final int objectType, final int pdt, final String dpt, final int readLevel,
			final int writeLevel)
		{
			id = pid;
			name = pidName;
			propName = propertyName;
			objType = objectType;
			this.pdt = pdt;
			this.dpt = dpt;
			read = readLevel;
			write = writeLevel;
		}

		/**
		 * Returns the property identifier.
		 * <p>
		 * 
		 * @return the PID
		 */
		public final int getPID()
		{
			return id;
		}

		/**
		 * Returns the PID name as string representation.
		 * 
		 * @return the PID name as string
		 */
		public final String getPIDName()
		{
			return name;
		}

		/**
		 * Returns the interface object type the property belongs to.
		 * <p>
		 * 
		 * @return the interface object type
		 */
		public final int getObjectType()
		{
			return objType;
		}

		/**
		 * Returns the property data type used for the property elements.
		 * <p>
		 * 
		 * @return the PDT
		 */
		public final int getPDT()
		{
			return pdt;
		}

		/**
		 * Returns the datapoint type ID used for the property elements.
		 * <p>
		 * 
		 * @return the DPT ID as string, or <code>null</code> if no DPT was set
		 */
		public final String getDPT()
		{
			return dpt;
		}

		/**
		 * Returns the property friendly name, a more readable name of the property.
		 * <p>
		 * 
		 * @return the property name
		 */
		public final String getName()
		{
			return propName;
		}
	}

	// mapping of object type numbers to the associated object type names
	// the offset of a name in the array corresponds to the object type number
	private static final String[] OBJECT_TYPE_NAMES = { "Device Object",
		"Addresstable Object", "Associationtable Object", "Applicationprogram Object",
		"Interfaceprogram Object", "EIB-Object Associationtable Object", "Router Object",
		"LTE Address Filter Table Object", "cEMI Server Object",
		"Group Object Table Object", "Polling Master", "KNXnet/IP Parameter Object",
		"Application Controller", "File Server Object", };

	//private static ResourceHandler rh;

	private final Map properties = Collections.synchronizedMap(new HashMap());

	private final PropertyAdapter pa;
	// helper flag to determine local DM mode, mainly for detecting absence of PDT
	// detection is currently done by querying PropertyAdapter.getName()
	private final boolean local;
	private final LogService logger;

	// maps object index to object type
	private final List objectTypes = new ArrayList();
	private final DPTXlator2ByteUnsigned tObjType;

	/**
	 * Creates a new property client using the specified adapter for accessing device
	 * properties.
	 * <p>
	 * The property client obtains ownership of the adapter.<br>
	 * The log service used by this property client is named "PC " + adapter.getName().
	 * 
	 * @param adapter property adapter object
	 * @throws KNXFormatException on missing DPT for translating interface object types (
	 *         DPTXlator2ByteUnsigned.DPT_PROP_DATATYPE)
	 */
	public PropertyClient(final PropertyAdapter adapter) throws KNXFormatException
	{
		pa = adapter;
		local = pa.getName().startsWith("local");
		try {
			tObjType = new DPTXlator2ByteUnsigned(DPTXlator2ByteUnsigned.DPT_PROP_DATATYPE);
		}
		catch (final KNXFormatException e) {
			pa.close();
			throw e;
		}
		logger = LogManager.getManager().getLogService("PC " + pa.getName());
	}

	/**
	 * Returns the object type name associated to the requested object type.
	 * <p>
	 * 
	 * @param objType object type to get name for
	 * @return object type name as string
	 */
	public static String getObjectTypeName(final int objType)
	{
		if (objType < OBJECT_TYPE_NAMES.length)
			return OBJECT_TYPE_NAMES[objType];
		return "";
	}

	/**
	 * Loads property definitions from a resource using the supplied
	 * {@link ResourceHandler} or a default handler.
	 * <p>
	 * 
	 * @param resource the resource location identifier of a resource to load
	 * @param handler the resource handler used for loading the property definitions, if
	 *        <code>null</code>, a default handler is used
	 * @return collection with loaded property definitions of type {@link Property}
	 * @throws KNXException on errors in the property resource handler
	 */
	public static Collection loadDefinitions(final String resource, final ResourceHandler handler)
		throws KNXException
	{
		final ResourceHandler rh = handler == null ? new XmlPropertyHandler() : handler;
		return rh.load(resource);
	}

	/**
	 * Saves the supplied property definitions to a resource using the supplied resource
	 * handler.
	 * <p>
	 * To save definitions of a property client <code>client</code>, invoke the method
	 * with the argument <code>client.getDefinitions().values()</code>.
	 * 
	 * @param resource the resource location identifier to a resource for saving the
	 *        definitions
	 * @param definitions the property definitions to save, the collection holds entries
	 *        of type {@link Property}
	 * @param handler the resource handler used for saving the property definitions, if
	 *        <code>null</code>, a default handler is used
	 * @throws KNXException on errors in the property resource handler
	 */
	public static void saveDefinitions(final String resource, final Collection definitions,
		final ResourceHandler handler) throws KNXException
	{
		// for saving an ordered collection based on property key order,
		// the saving procedure has to be called with the Collection argument
		// <code>new TreeMap(getDefinitions()).values()</code>, since our property map
		// is not ordered for the time being

		final ResourceHandler rh = handler == null ? new XmlPropertyHandler() : handler;
		rh.save(resource, definitions);
	}

	/**
	 * Adds the property definitions contained in the collection argument to the property
	 * definitions of the property client.
	 * <p>
	 * Any definitions already existing in the client are not removed before adding new
	 * ones. To remove definitions, use {@link #getDefinitions()} and remove entries
	 * manually.<br>
	 * An added property definition will replace an existing definition with its property
	 * key being equal to the one of the added definition.
	 * 
	 * @param definitions collection of property definitions, containing entries of type
	 *        {@link Property}
	 */
	public void addDefinitions(final Collection definitions)
	{
		for (final Iterator i = definitions.iterator(); i.hasNext();) {
			final Property p = (Property) i.next();
			properties.put(new PropertyKey(p.objType, p.id), p);
		}
	}

	/**
	 * Returns the property definitions used by property clients, if definitions were
	 * loaded.
	 * <p>
	 * The returned map is synchronized and references the one used by the property
	 * client. Property definitions might be added or removed as required. Modifications
	 * will influence subsequent lookup behavior of the property client.<br>
	 * A map key is of type {@link PropertyKey}, a map value is of type {@link Property}.
	 * 
	 * @return property map, or <code>null</code> if no definitions loaded
	 */
	public Map getDefinitions()
	{
		return properties;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAccess
	 * #setProperty(int, int, int, java.lang.String)
	 */
	public void setProperty(final int objIndex, final int pid, final int position,
		final String value) throws KNXException
	{
		try {
			final DPTXlator t = createTranslator(objIndex, pid);
			t.setValue(value);
			setProperty(objIndex, pid, position, t.getItems(), t.getData());
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
	}

	/**
	 * Gets the first property element using the associated property data type of the
	 * requested property.
	 * <p>
	 * 
	 * @param objIndex interface object index in the device
	 * @param pid property identifier
	 * @return property element value represented as string
	 * @throws KNXException on adapter errors while querying the property element or data
	 *         type translation problems
	 */
	public String getProperty(final int objIndex, final int pid) throws KNXException
	{
		return getPropertyTranslated(objIndex, pid, 1, 1).getValue();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAccess
	 * #setProperty(int, int, int, int, byte[])
	 */
	public void setProperty(final int objIndex, final int pid, final int start,
		final int elements, final byte[] data) throws KNXException
	{
		try {
			pa.setProperty(objIndex, pid, start, elements, data);
		}
		catch (final KNXException e) {
			logger.error("set property failed", e);
			throw e;
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAccess#getProperty(int, int, int, int)
	 */
	public byte[] getProperty(final int objIndex, final int pid, final int start,
		final int elements) throws KNXException
	{
		try {
			return pa.getProperty(objIndex, pid, start, elements);
		}
		catch (final KNXException e) {
			logger.error("get property failed", e);
			throw e;
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAccess
	 * #getPropertyTranslated(int, int, int, int)
	 */
	public DPTXlator getPropertyTranslated(final int objIndex, final int pid,
		final int start, final int elements) throws KNXException
	{
		try {
			final DPTXlator t = createTranslator(objIndex, pid);
			t.setData(getProperty(objIndex, pid, start, elements));
			return t;
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.mgmt.PropertyAccess#getDescription(int, int)
	 */
	public Description getDescription(final int objIndex, final int pid)
		throws KNXException
	{
		if (pid == 0)
			throw new KNXIllegalArgumentException("pid has to be > 0");
		try {
			return createDesc(objIndex, pa.getDescription(objIndex, pid, 0));
		}
		catch (final KNXException e) {
			logger.error("get description failed", e);
			throw e;
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
	}

	/**
	 * Gets the property description based on the property index.
	 * <p>
	 * 
	 * @param objIndex interface object index in the device
	 * @param propIndex property index in the object
	 * @return a property description object
	 * @throws KNXException on adapter errors while querying the description
	 */
	public Description getDescriptionByIndex(final int objIndex, final int propIndex)
		throws KNXException
	{
		try {
			return createDesc(objIndex, pa.getDescription(objIndex, 0, propIndex));
		}
		catch (final KNXException e) {
			logger.error("get description failed", e);
			throw e;
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
	}

	/**
	 * Does a property description scan of the properties in all interface objects.
	 * <p>
	 * 
	 * @param allProperties <code>true</code> to scan all property descriptions in the
	 *        interface objects, <code>false</code> to only scan the object type
	 *        descriptions, i.e., ({@link PropertyAccess.PID#OBJECT_TYPE})
	 * @return a list containing the property descriptions of type {@link Description}
	 * @throws KNXException on adapter errors while querying the descriptions
	 */
	public List scanProperties(final boolean allProperties) throws KNXException
	{
		final List scan = new ArrayList();
		for (int index = 0;; ++index) {
			final List l = scanProperties(index, allProperties);
			if (l.size() == 0)
				break;
			scan.addAll(l);
		}
		return scan;
	}

	/**
	 * Does a property description scan of the properties of one interface object.
	 * <p>
	 * 
	 * @param objIndex interface object index in the device
	 * @param allProperties <code>true</code> to scan all property descriptions in that
	 *        interface object, <code>false</code> to only scan the object type
	 *        description of the interface object specified by <code>objIndex</code>,
	 *        i.e., ({@link PropertyAccess.PID#OBJECT_TYPE})
	 * @return a list containing the property descriptions of type {@link Description}
	 * @throws KNXException on adapter errors while querying the descriptions
	 */
	public List scanProperties(final int objIndex, final boolean allProperties)
		throws KNXException
	{
		final List scan = new ArrayList();
		// property with index 0 is description of object type
		// rest are ordinary properties of the object
		try {
			scan.add(createDesc(objIndex, pa.getDescription(objIndex, 0, 0)));
			if (allProperties)
				for (int i = 1;; ++i)
					scan.add(createDesc(objIndex, pa.getDescription(objIndex, 0, i)));
		}
		catch (final KNXException e) {
			if (!KNXRemoteException.class.equals(e.getClass())) {
				logger.error("scan properties failed", e);
				throw e;
			}
		}
		catch (final InterruptedException e) {
			throw new KNXTimeoutException("interrupted", e);
		}
		return scan;
	}

	/**
	 * Returns whether the adapter used for property access is opened.
	 * <p>
	 * 
	 * @return <code>true</code> on open adapter, <code>false</code> on closed adapter
	 */
	public final boolean isOpen()
	{
		return pa.isOpen();
	}

	/**
	 * Closes the property client and the used adapter.
	 * <p>
	 */
	public void close()
	{
		if (pa.isOpen()) {
			pa.close();
			logger.info("closed property client");
			LogManager.getManager().removeLogService(logger.getName());
		}
	}

	private Description createDesc(final int oi, final byte[] desc) throws KNXException,
		InterruptedException
	{
		final Description d = new Description(getObjectType(oi, true), desc);
		d.setCurrentElements(pa.getProperty(oi, d.getPID(), 0, 1));
		// workaround for PDT on local DM
		if (local)
			d.setPDT(-1);
		return d;
	}

	private int getObjectType(final int objIndex, final boolean queryObject)
		throws KNXException, InterruptedException
	{
		for (final Iterator i = objectTypes.iterator(); i.hasNext();) {
			final Pair p = (Pair) i.next();
			if (p.oindex == objIndex)
				return p.otype;
		}
		if (queryObject)
			return queryObjectType(objIndex);
		throw new KNXException("couldn't deduce object type");
	}

	private int queryObjectType(final int objIndex) throws KNXException,
		InterruptedException
	{
		tObjType.setData(pa.getProperty(objIndex, 1, 1, 1));
		objectTypes.add(new Pair(objIndex, tObjType.getValueUnsigned()));
		return tObjType.getValueUnsigned();
	}

	private DPTXlator createTranslator(final int objIndex, final int pid)
		throws KNXException, InterruptedException
	{
		final int ot = getObjectType(objIndex, true);
		int pdt = -1;
		Property p = (Property) properties.get(new PropertyKey(ot, pid));
		// if no property found, lookup global pid
		if (p == null && pid < 50)
			p = (Property) properties.get(new PropertyKey(pid));
		if (p != null) {
			if (p.dpt != null)
				try {
					return TranslatorTypes.createTranslator(0, p.dpt);
				}
				catch (final KNXException e) {
					logger.warn("fallback to default translator", e);
				}
			pdt = p.pdt;
		}
		// if we didn't get pdt from definitions, query property description,
		// in local dev.mgmt, no pdt description is available
		if (pdt == -1 && !local)
			pdt = pa.getDescription(objIndex, pid, 0)[3] & 0x3f;
		if (PropertyTypes.hasTranslator(pdt))
			return PropertyTypes.createTranslator(pdt);
		final KNXException e = new KNXException("no translator available for PID 0x"
			+ Integer.toHexString(pid) + ", " + getObjectTypeName(ot));
		logger.warn("translator missing", e);
		throw e;
	}

	private static class XmlPropertyHandler implements ResourceHandler
	{
		private static final String PROPDEFS_TAG = "propertyDefinitions";
		private static final String OBJECT_TAG = "object";
		private static final String OBJECTTYPE_ATTR = "type";
		private static final String PROPERTY_TAG = "property";
		private static final String PID_ATTR = "pid";
		private static final String PIDNAME_ATTR = "pidName";
		private static final String NAME_ATTR = "name";
		private static final String PDT_ATTR = "pdt";
		private static final String DPT_ATTR = "dpt";
		private static final String RW_ATTR = "rw";
		private static final String WRITE_ATTR = "writeEnabled";
		private static final String USAGE_TAG = "usage";

		XmlPropertyHandler()
		{}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.mgmt.PropertyClient.ResourceHandler#load
		 * (java.lang.String)
		 */
		public Collection load(final String resource) throws KNXException
		{
			final XMLReader r = XMLFactory.getInstance().createXMLReader(resource);
			final List list = new ArrayList(30);
			int objType = -1;
			try {
				if (r.read() != XMLReader.START_TAG
					|| !r.getCurrent().getName().equals(PROPDEFS_TAG))
					throw new KNXMLException("no property defintions");
				while (r.read() != XMLReader.END_DOC) {
					final Element e = r.getCurrent();
					if (r.getPosition() == XMLReader.START_TAG) {
						if (e.getName().equals(OBJECT_TAG)) {
							// on no type attribute, toInt() throws, that's ok
							final String type = e.getAttribute(OBJECTTYPE_ATTR);
							objType = "global".equals(type) ? -1 : toInt(type);
						}
						else if (e.getName().equals(PROPERTY_TAG)) {
							r.complete(e);
							parseRW(e.getAttribute(RW_ATTR));
							list.add(new Property(toInt(e.getAttribute(PID_ATTR)),
									e.getAttribute(PIDNAME_ATTR), e.getAttribute(NAME_ATTR),
									objType, toInt(e.getAttribute(PDT_ATTR)),
									e.getAttribute(DPT_ATTR)));
						}
					}
					else if (r.getPosition() == XMLReader.END_TAG
							&& e.getName().equals(PROPDEFS_TAG))
						break;
				}
				return list;
			}
			catch (final KNXFormatException e) {
				throw new KNXException("loading property definitions, " + e.getMessage());
			}
			finally {
				r.close();
			}
		}

		/* (non-Javadoc)
		 * @see tuwien.auto.calimero.mgmt.PropertyClient.ResourceHandler#save
		 * (java.lang.String, java.util.Collection)
		 */
		public void save(final String resource, final Collection properties) throws KNXException
		{
			final XMLWriter w = XMLFactory.getInstance().createXMLWriter(resource);
			try {
				w.writeDeclaration(true, "UTF-8");
				w.writeComment("Calimero 2 " + Settings.getLibraryVersion()
					+ " KNX property definitions, saved on " + new Date().toString());
				w.writeElement(PROPDEFS_TAG, null, null);
				final int noType = -2;
				int objType = noType;
				for (final Iterator i = properties.iterator(); i.hasNext();) {
					final Property p = (Property) i.next();
					if (p.objType != objType) {
						if (objType != noType)
							w.endElement();
						objType = p.objType;
						final List att = new ArrayList();
						att.add(new Attribute(OBJECTTYPE_ATTR, objType == -1 ? "global"
							: Integer.toString(objType)));
						w.writeElement(OBJECT_TAG, att, null);
					}
					// property attributes
					final List att = new ArrayList();
					att.add(new Attribute(PID_ATTR, Integer.toString(p.id)));
					att.add(new Attribute(PIDNAME_ATTR, p.name));
					att.add(new Attribute(NAME_ATTR, p.propName));
					att.add(new Attribute(PDT_ATTR, p.pdt == -1 ? "<tbd>" : Integer.toString(p.pdt)));
					if (p.dpt != null && p.dpt.length() > 0)
						att.add(new Attribute(DPT_ATTR, p.dpt));
					// TOOD why don't we add r/w attribute values?
					att.add(new Attribute(RW_ATTR, ""));
					att.add(new Attribute(WRITE_ATTR, ""));
					// write property
					w.writeElement(PROPERTY_TAG, att, null);
					w.writeElement(USAGE_TAG, null, null);
					w.endElement();
					w.endElement();
				}
			}
			finally {
				w.close();
			}
		}

		private static int parseRW(final String rw)
		{
			final String s = rw.toLowerCase();
			int read = 0;
			int write = 0;
			boolean slash = false;
			for (int i = 0; i < s.length(); i++) {
				final char c = s.charAt(i);
				if (c == '/')
					slash = true;
				else if (c >= '0' && c <= '9')
					if (slash)
						write = write * 10 + c - '0';
					else
						read = read * 10 + c - '0';
			}
			return read << 8 | write;
		}

		private static int toInt(final String s) throws KNXFormatException
		{
			try {
				if (s != null) {
					if (s.equals("<tbd>"))
						return -1;
					return s.length() == 0 ? 0 : Integer.decode(s).intValue();
				}
			}
			catch (final NumberFormatException e) {}
			throw new KNXFormatException("can not convert to number: " + s, s);
		}
	}

	private static final class Pair
	{
		final int oindex;
		final int otype;

		Pair(final int objIndex, final int objType)
		{
			oindex = objIndex;
			otype = objType;
		}
	}
}
