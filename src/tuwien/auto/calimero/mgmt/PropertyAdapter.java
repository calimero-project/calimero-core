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

package tuwien.auto.calimero.mgmt;

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalStateException;

/**
 * Adapter hiding protocol specifics and internals of accessing interface object properties.
 * <p>
 * A property adapter is created for one communication partner (KNX device, KNXnet/IP router). If {@link #close()} is
 * called by a user on an open adapter, all methods which do interface object property access are allowed to throw
 * {@link KNXIllegalStateException} if invoked on that closed adapter.<br>
 *
 * @author B. Malinowsky
 */
public interface PropertyAdapter extends AutoCloseable
{
	/**
	 * Sets property value elements in an interface object property.
	 *
	 * @param objIndex interface object index
	 * @param pid property identifier
	 * @param start start index in the property value to start writing to
	 * @param elements number of elements to set
	 * @param data byte array containing the property value data
	 * @throws KNXException on error setting the interface object property
	 * @throws KNXIllegalStateException if adapter was already closed
	 * @throws InterruptedException on interrupted thread
	 */
	void setProperty(int objIndex, int pid, int start, int elements, byte[] data)
		throws KNXException, InterruptedException;

	/**
	 * Gets property value elements in an interface object property.
	 *
	 * @param objIndex interface object index
	 * @param pid property identifier
	 * @param start start index in the property value to start reading from
	 * @param elements number of elements to get
	 * @return byte array containing the property value data
	 * @throws KNXException on error getting the interface object property
	 * @throws KNXIllegalStateException if adapter was already closed
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] getProperty(int objIndex, int pid, int start, int elements) throws KNXException, InterruptedException;

	/**
	 * Reads the description of a property of an interface object, returning a property description layout according the
	 * application layer property description service.
	 *
	 * @param objIndex interface object index
	 * @param pid property identifier, specify 0 to use the property index
	 * @param propIndex property index, starts with index 0 for the first property
	 * @return byte array containing the property description, starting with the property object index
	 * @throws KNXException on error getting the property description
	 * @throws KNXIllegalStateException if adapter was already closed
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] getDescription(int objIndex, int pid, int propIndex) throws KNXException, InterruptedException;

	/**
	 * Returns the name for identifying this adapter and its destination.
	 *
	 * @return adapter name as string
	 */
	String getName();

	/**
	 * Returns whether this adapter can be used for property access and is not closed.
	 *
	 * @return <code>true</code> if adapter open, <code>false</code> if closed
	 */
	boolean isOpen();

	/**
	 * Closes the adapter.
	 * <p>
	 * Depending on the adapter, necessary steps to terminate a connection might be done and owned resources will be
	 * freed. A closed adapter can't be used for property access anymore.<br>
	 * Currently, this method does not invoke
	 * {@link PropertyAdapterListener#adapterClosed(tuwien.auto.calimero.CloseEvent)}.
	 */
	@Override
	void close();
}
