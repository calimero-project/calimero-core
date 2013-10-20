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

package tuwien.auto.calimero.datapoint;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

/**
 * A container for keeping {@link Datapoint}s, using some particular hierarchical
 * structure or order between those datapoints.
 * <p>
 * Its purpose is to imitate a real world datapoint layout, to build and maintain some
 * kind of model for it, allowing a user to create and work with an arrangement of
 * datapoints being adequate for the application requirements.<br>
 * This interface aims to act as the base for building more complex layouts, for example
 * to model part of a KNX network.
 * 
 * @author B. Malinowsky
 * @see DatapointMap
 */
public interface DatapointModel
{
	/**
	 * Adds a datapoint to this model.
	 * <p>
	 * An implementation might throw KNXIllegalArgumentException if tried to add a
	 * duplicate datapoint.
	 * 
	 * @param dp datapoint to add
	 * @throws KNXIllegalArgumentException on duplicate datapoint
	 */
	void add(Datapoint dp);

	/**
	 * Removes the specified datapoint from this model, if that datapoint is found.
	 * <p>
	 * 
	 * @param dp datapoint to remove
	 */
	void remove(Datapoint dp);

	/**
	 * Removes all datapoints contained in this model.
	 * <p>
	 */
	void removeAll();

	/**
	 * Returns the datapoint identified using the specified <code>main</code> address,
	 * if found in this model.
	 * <p>
	 * If no datapoint is found with that address, <code>null</code> is returned.
	 * 
	 * @param main KNX group address to get the datapoint for
	 * @return the datapoint or <code>null</code>
	 */
	Datapoint get(GroupAddress main);

	/**
	 * Checks whether this model contains the datapoint identified using the specified
	 * <code>main</code> address.
	 * <p>
	 * 
	 * @param main KNX group address to look up
	 * @return <code>true</code> iff such datapoint is found, <code>false</code>
	 *         otherwise
	 */
	boolean contains(GroupAddress main);

	/**
	 * Checks whether this model contains the specified datapoint.
	 * <p>
	 * 
	 * @param dp datapoint to look up
	 * @return <code>true</code> iff such datapoint is found, <code>false</code>
	 *         otherwise
	 */
	boolean contains(Datapoint dp);

	/**
	 * Loads a datapoint model from XML input into this model.
	 * <p>
	 * Datapoints already contained in this model are not required to be removed before
	 * loading, the loaded model will be added to the already existing datapoints.
	 * 
	 * @param r a XML reader
	 * @throws KNXMLException on error loading the datapoint model, or on duplicate loaded
	 *         datapoint
	 */
	void load(XMLReader r) throws KNXMLException;

	/**
	 * Saves the datapoint model to XML using the supplied writer.
	 * <p>
	 * 
	 * @param w a XML writer
	 * @throws KNXMLException on error saving the datapoint model
	 */
	void save(XMLWriter w) throws KNXMLException;
}
