/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.datapoint;

import java.util.Objects;

import io.calimero.GroupAddress;
import io.calimero.Priority;
import io.calimero.dptxlator.DptId;
import io.calimero.xml.KNXMLException;
import io.calimero.xml.XmlReader;
import io.calimero.xml.XmlWriter;

/**
 * Represents a KNX datapoint configuration.
 * <p>
 * It stores knowledge about a datapoint in the KNX network, used for communication within the
 * Calimero library, to the KNX network, and with the user.<br>
 * The datapoint is identified through a KNX group address. A name is supplied to allow a
 * more friendly interaction with the user, the selected name does not have to be unique.
 * Information exchanged between datapoints consists of a certain encoding, defined by a
 * datapoint type (DPT). This information exchange is done through messages, which are
 * sent with a {@link Priority} associated with the respective datapoint. Every datapoint
 * object can have its own DPT and priority set by using datapoint methods.<br>
 * Note, only information for how to handle interaction is stored within a datapoint type.
 * Neither datapoint values (states or events), nor any datapoint messages are stored.
 * <p>
 * A KNX datapoint is either state based {@link StateDP} or command based
 * {@link CommandDP}.
 *
 * @author B. Malinowsky
 */
public abstract class Datapoint
{
	static final String TAG_DATAPOINT = "datapoint";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_STATEBASED = "stateBased";
	private static final String ATTR_MAINNUMBER = "mainNumber";
	private static final String ATTR_DPTID = "dptID";
	private static final String ATTR_PRIORITY = "priority";

	// main group address is actually final, but left mutable for easier XML loading
	private GroupAddress main;
	private volatile String name;

	private final DptId dptId;

	// message priority for this datapoint
	private volatile Priority priority = Priority.LOW;

	/**
	 * Creates a new datapoint with a name and datapoint type ID.
	 *
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 * @param dptId datapoint type ID
	 */
	Datapoint(final GroupAddress main, final String name, final DptId dptId)
	{
		this.main = main;
		this.name = name;
		this.dptId = dptId;
	}

	/**
	 * Creates a new datapoint from XML input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The datapoint element is then expected to be the current element in the reader. It
	 * reads the start tag and attributes of a datapoint element, and sets the reader to
	 * the next position.
	 *
	 * @param r a XML reader
	 * @throws KNXMLException if the XML element is no datapoint or could not be read correctly
	 */
	Datapoint(final XmlReader r) throws KNXMLException
	{
		if (r.getEventType() != XmlReader.START_ELEMENT)
			r.nextTag();
		if (r.getEventType() != XmlReader.START_ELEMENT || !r.getLocalName().equals(TAG_DATAPOINT))
			throw new KNXMLException("no KNX datapoint element", r);
		final boolean stateBased = readDPType(r);
		if (stateBased != this instanceof StateDP) {
			final String type = this instanceof StateDP ? "state" : "command";
			throw new KNXMLException("no %s-based KNX datapoint element".formatted(type), r);
		}
		if ((name = r.getAttributeValue(null, ATTR_NAME)) == null)
			throw new KNXMLException("missing attribute " + ATTR_NAME, r);
		final String dptAttr = r.getAttributeValue(null, ATTR_DPTID);
		if (dptAttr == null)
			throw new KNXMLException("missing attribute " + ATTR_DPTID, r);
		try {
			String a = r.getAttributeValue(null, ATTR_MAINNUMBER);
			int mainNo = 0;
			if (a != null)
				mainNo = Integer.decode(a);
			a = r.getAttributeValue(null, ATTR_PRIORITY);
			if (a != null)
				priority = Priority.get(a);
			dptId = dptId(mainNo, dptAttr);
		}
		catch (final RuntimeException rte) {
			throw new KNXMLException("malformed attribute, " + rte.getMessage(), r);
		}
		r.nextTag();
	}

	/**
	 * Creates a new datapoint from XML input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The datapoint element is then expected to be the current element in the reader.
	 *
	 * @param r a XML reader
	 * @return the created datapoint, either of type {@link StateDP} or {@link CommandDP}
	 * @throws KNXMLException if the XML element is no datapoint or could not be read correctly
	 */
	public static Datapoint create(final XmlReader r) throws KNXMLException
	{
		if (r.getEventType() != XmlReader.START_ELEMENT)
			r.nextTag();
		if (r.getEventType() == XmlReader.START_ELEMENT) {
			if (readDPType(r))
				return new StateDP(r);
			return new CommandDP(r);
		}
		throw new KNXMLException("no KNX datapoint", r);
	}

	/**
	 * {@return the datapoint main address, a KNX group address identifying this datapoint}
	 */
	public final GroupAddress getMainAddress()
	{
		return main;
	}

	/**
	 * Sets the datapoint name.
	 * <p>
	 * The datapoint name might be any user defined name, it is only used for interaction
	 * with the user.
	 *
	 * @param friendlyName user-friendly name of the datapoint
	 */
	public final void setName(final String friendlyName)
	{
		name = friendlyName;
	}

	/**
	 * {@return the datapoint name as string}
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Sets the priority used for KNX messages of this datapoint.
	 *
	 * @param p the new priority to assign
	 */
	public final void setPriority(final Priority p)
	{
		priority = p;
	}

	/**
	 * {@return the KNX message priority assigned to this datapoint}
	 */
	public final Priority getPriority()
	{
		return priority;
	}

	/**
	 * Returns the datapoint type ID of a DPT translator to use for datapoint value
	 * translation.
	 * <p>
	 *
	 * @return the datapoint type ID as string
	 */
	public final String getDPT()
	{
		return dptId.toString();
	}

	/** {@return the datapoint type ID of this datapoint} */
	public final DptId dptId() { return dptId; }

	/**
	 * Saves this datapoint in XML format to the supplied XML writer.
	 *
	 * @param w an XML writer
	 * @throws KNXMLException on error saving this datapoint
	 */
	public void save(final XmlWriter w) throws KNXMLException
	{
		/* XML layout:
		 <datapoint stateBased=[true|false] name=string mainNumber=int dptID=string
		 priority=string>
		 knxAddress
		 ...
		 </datapoint>
		*/
		w.writeStartElement(TAG_DATAPOINT);
		w.writeAttribute(ATTR_STATEBASED, Boolean.toString(this instanceof StateDP));
		w.writeAttribute(ATTR_NAME, name);
		w.writeAttribute(ATTR_MAINNUMBER, Integer.toString(dptId.mainNumber()));
		w.writeAttribute(ATTR_DPTID, dptId.toString());
		w.writeAttribute(ATTR_PRIORITY, priority.toString());
		main.save(w);
		doSave(w);
		w.writeEndElement();
	}

	@Override
	public boolean equals(final Object o) {
		return (this == o) || (o instanceof final Datapoint dp) && Objects.equals(main, dp.main)
				&& Objects.equals(name, dp.name) && Objects.equals(dptId, dp.dptId) && priority == dp.priority;
	}

	@Override
	public int hashCode() { return Objects.hash(main, name, dptId, priority); }

	@Override
	public String toString() {
		return main.toString() + (name.isEmpty() ? "" : " '" + name + "'") + ", DPT " + dptId + ", "
				+ priority.toString() + " priority";
	}

	void doLoad(final XmlReader r) throws KNXMLException
	{
		if (main != null)
			throw new KNXMLException("main address already set", r);
		if (r.getEventType() != XmlReader.START_ELEMENT)
			r.nextTag();
		main = new GroupAddress(r);
	}

	abstract void doSave(XmlWriter w) throws KNXMLException;

	/* returns true for state based DP, false for command based DP */
	private static boolean readDPType(final XmlReader r) throws KNXMLException
	{
		final String a = r.getAttributeValue(null, ATTR_STATEBASED);
		if ("false".equalsIgnoreCase(a))
			return false;
		if ("true".equalsIgnoreCase(a))
			return true;
		throw new KNXMLException("malformed attribute " + ATTR_STATEBASED, r);
	}

	static DptId dptId(final int mainNumber, final String dptId) {
		final var mainSub = dptId.split("\\.", 0);
		final int main = mainNumber != 0 ? mainNumber : mainSub.length > 0 && !mainSub[0].isEmpty()
				? Integer.parseUnsignedInt(mainSub[0]) : 0xffff;
		final int subNumber = mainSub.length > 1 ? Integer.parseUnsignedInt(mainSub[1]) : 0xffff;
		return new DptId(main, subNumber);
	}
}
