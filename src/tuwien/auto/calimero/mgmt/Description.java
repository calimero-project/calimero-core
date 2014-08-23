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

package tuwien.auto.calimero.mgmt;

/**
 * Holds description information of a KNX interface object property.
 * <p>
 * The supported description information and the expected description structure layout is
 * according to the application layer property description read service.<br>
 * This Description type also supports the property object type and the number of current
 * elements.
 * <p>
 * When used together with local device management, not all description information will
 * be available.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 */
public final class Description
{
	private final int oindex;
	private final int otype;
	private final int id;
	private final int pindex;
	private final int maxElems;
	// current elements are set after object creation
	private int currElems;
	// data type is set to -1 after object creation if not available
	private int pdt;
	private final int rLevel;
	private final int wLevel;
	private final boolean write;

	/**
	 * Creates a new description object for a property out of a byte array.
	 * <p>
	 * The description structure layout of <code>data</code> is according to the
	 * application layer property description read service.
	 * 
	 * @param objType interface object type the property belongs to
	 * @param data byte array containing property description, starting at
	 *        <code>data[0]</code>
	 */
	public Description(final int objType, final byte[] data)
	{
		otype = objType;
		oindex = data[0] & 0xff;
		id = data[1] & 0xff;
		pindex = data[2] & 0xff;
		write = (data[3] & 0x80) == 0x80 ? true : false;
		pdt = data[3] & 0x3f;
		maxElems = (data[4] & 0xff) << 8 | data[5] & 0xff;
		rLevel = (data[6] & 0xff) >> 4;
		wLevel = data[6] & 0x0f;
	}

	/**
	 * Creates a new description object for a property out of a data byte array, together
	 * with object type and number of current elements.
	 * <p>
	 * 
	 * @param objType interface object type the property belongs to
	 * @param currentElements current number of elements in the property
	 * @param data byte array holding the description information, the structure is
	 *        according to the ASDU of a property description service response
	 */
	public Description(final int objType, final int currentElements, final byte[] data)
	{
		this(objType, data);
		currElems = currentElements;
	}

	/**
	 * Creates a new description object for a property using the given parameters.
	 * <p>
	 * 
	 * @param objIndex index of the object in the device, starting with 0
	 * @param objType interface object type the property belongs to
	 * @param pid property identifier, a 6 Bit identifier
	 * @param propIndex property index in the object, starting with 0
	 * @param pdt property data type
	 * @param writeEnabled specifies if the property is write-enabled or read only
	 * @param currentElements current number of elements in the property
	 * @param maxElements maximum number of elements allowed in the property
	 * @param readLevel read access level, 0 &lt;= level &lt;= 15
	 * @param writeLevel write access level, 0 &lt;= level &lt;= 15
	 */
	public Description(final int objIndex, final int objType, final int pid, final int propIndex,
		final int pdt, final boolean writeEnabled, final int currentElements,
		final int maxElements, final int readLevel, final int writeLevel)
	{
		otype = objType;
		oindex = objIndex;
		id = pid;
		pindex = propIndex;
		this.pdt = pdt;
		write = writeEnabled;
		currElems = currentElements;
		maxElems = maxElements;
		rLevel = readLevel;
		wLevel = writeLevel;
	}

	/**
	 * Returns the interface object server unique object index of the interface object
	 * containing the property.
	 * <p>
	 * 
	 * @return the object index
	 */
	public int getObjectIndex()
	{
		return oindex;
	}

	/**
	 * Returns the object type to which the property belongs to.
	 * 
	 * @return the object type
	 */
	public int getObjectType()
	{
		return otype;
	}

	/**
	 * Returns the property index.
	 * <p>
	 * 
	 * @return the property index
	 */
	public int getPropIndex()
	{
		return pindex;
	}

	/**
	 * Returns the property identifier.
	 * <p>
	 * 
	 * @return the PID
	 */
	public int getPID()
	{
		return id;
	}

	/**
	 * Returns the property data type.
	 * <p>
	 * With local device management, the PDT is not available and -1 is returned.
	 * 
	 * @return the PDT or -1 for no PDT
	 */
	public int getPDT()
	{
		return pdt;
	}

	/**
	 * Returns the current number of elements in the property.
	 * <p>
	 * 
	 * @return current elements
	 */
	public int getCurrentElements()
	{
		return currElems;
	}

	/**
	 * Returns the maximum number of elements allowed in the property.
	 * <p>
	 * With local device management, this attribute is not available and 0 is returned.
	 * 
	 * @return maximum elements, or 0
	 */
	public int getMaxElements()
	{
		return maxElems;
	}

	/**
	 * Returns the read access level for the property.
	 * <p>
	 * The level is between 0 (maximum access rights) and 15 (minimum access rights).
	 * 
	 * @return the read level as 4 bit value
	 */
	public int getReadLevel()
	{
		return rLevel;
	}

	/**
	 * Returns the write access level for the property.
	 * <p>
	 * The level is between 0 (maximum access rights) and 15 (minimum access rights).
	 * 
	 * @return the write level as 4 bit value
	 */
	public int getWriteLevel()
	{
		return wLevel;
	}

	/**
	 * Returns whether the property is write-enabled or read-only.
	 * <p>
	 * 
	 * @return <code>true</code> if write enabled, <code>false</code> otherwise
	 */
	public boolean isWriteEnabled()
	{
		return write;
	}

	/**
	 * Returns the property description in textual representation.
	 * <p>
	 * 
	 * @return a string representation of the description
	 */
	@Override
	public String toString()
	{
		return "OT " + otype + " OI " + oindex + " PID " + id + " PI " + pindex + " PDT "
				+ (pdt == -1 ? "-" : Integer.toString(getPDT())) + ", curr elements " + currElems
				+ " (max " + maxElems + "), r/w access " + rLevel + "/" + wLevel
				+ (write ? " write-enabled" : " read-only");
	}

	/**
	 * Returns the byte representation of this description structure.
	 * <p>
	 * 
	 * @return byte array containing structure data
	 */
	public byte[] toByteArray()
	{
		final byte[] data = new byte[7];
		data[0] = (byte) oindex;
		data[1] = (byte) id;
		data[2] = (byte) pindex;
		data[3] = (byte) (write ? 0x80 : 0x00);
		data[3] |= pdt & 0x3f;
		data[4] = (byte) (maxElems >> 8);
		data[5] = (byte) maxElems;
		data[6] = (byte) (rLevel << 4 | wLevel & 0x0f);
		return data;
	}

	// set 2 or 4 byte data array with element count, big endian
	void setCurrentElements(final byte[] data)
	{
		int elems = 0;
		for (int i = 0; i < data.length; ++i)
			elems = elems << 8 | data[i] & 0xff;
		currElems = elems;
	}

	void setPDT(final int type)
	{
		pdt = type;
	}
}
