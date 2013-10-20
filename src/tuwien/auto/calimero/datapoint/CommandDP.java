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
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

/**
 * Represents a command based KNX datapoint.
 * <p>
 * Interaction of command based datapoints is done solely with events (i.e., "commands"),
 * not telling anything about a datapoint state.
 * 
 * @author B. Malinowsky
 */
public class CommandDP extends Datapoint
{
	/**
	 * Creates a new command based datapoint with a name.
	 * <p>
	 * 
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 */
	public CommandDP(final GroupAddress main, final String name)
	{
		super(main, name, false);
	}

	/**
	 * Creates a new command based datapoint with a name, and specifies datapoint
	 * translation type.
	 * <p>
	 * 
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 * @param mainNumber main number of the data type used for translation of a datapoint
	 *        value; if the used <code>dptID</code> argument unambiguously identifies a
	 *        DPT translator, main number might be left 0
	 * @param dptID the datapoint type ID used for translation in a DPT translator
	 */
	public CommandDP(final GroupAddress main, final String name, final int mainNumber,
		final String dptID)
	{
		super(main, name, false);
		setDPT(mainNumber, dptID);
	}

	/**
	 * Creates a new command based datapoint from XML input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The datapoint element is then expected to be the current element in the reader.
	 * 
	 * @param r a XML reader
	 * @throws KNXMLException if the XML element is no datapoint or could not be read
	 *         correctly
	 */
	public CommandDP(final XMLReader r) throws KNXMLException
	{
		super(r);
		if (isStateBased())
			throw new KNXMLException("no command based KNX datapoint element", null,
					r.getLineNumber());
		doLoad(r);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.Datapoint#toString()
	 */
	public String toString()
	{
		return "command DP " + super.toString();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.Datapoint#doSave(
	 * tuwien.auto.calimero.xml.XMLWriter)
	 */
	void doSave(final XMLWriter w)
	{}
}
