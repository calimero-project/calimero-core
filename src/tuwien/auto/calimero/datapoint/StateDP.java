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

package tuwien.auto.calimero.datapoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XmlReader;
import tuwien.auto.calimero.xml.XmlWriter;

/**
 * Represents a state based KNX datapoint.
 * <p>
 * State based datapoint interaction leads to transitions between states (so a datapoint
 * value associated to a certain datapoint represents a particular state over an amount of
 * time). The state of such a datapoint can be updated/invalidated by different (i.e.,
 * more than one) KNX group addresses. An address marked as updating the state of this
 * datapoint will allow KNX indication and response messages with that destination address
 * to set a new datapoint state value. An address marked as invalidating the state of this
 * datapoint will allow KNX indication messages with that destination address to delete
 * any state value information of the datapoint.<br>
 * This datapoint does not check for mutually exclusive containment of an address in one
 * of those both categories. The behavior when adding a KNX group address to both updating
 * and invalidating the datapoint state is undefined.<br>
 *
 * @author B. Malinowsky
 */
public class StateDP extends Datapoint
{
	private static final String TAG_EXPIRATION = "expiration";
	private static final String ATTR_TIMEOUT = "timeout";
	private static final String TAG_UPDATING = "updatingAddresses";
	private static final String TAG_INVALIDATING = "invalidatingAddresses";

	// list of group addresses, whose .ind messages invalidate the data point
	private final List<GroupAddress> invalidating;
	// list of group addresses, whose .ind and .res messages update the data point
	private final List<GroupAddress> updating;
	// timeout in seconds, how long a set state value stays valid since reception
	private volatile int timeout;
	private final List<String> locations = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Creates a new state based datapoint with a name.
	 *
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 */
	public StateDP(final GroupAddress main, final String name)
	{
		super(main, name, true);
		invalidating = Collections.synchronizedList(new ArrayList<>());
		updating = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * Creates a new datapoint with a name and specifies datapoint translation type.
	 *
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 * @param mainNumber main number of the data type used for translation of a datapoint
	 *        value; if the used <code>dptID</code> argument unambiguously identifies a
	 *        DPT translator, main number might be left 0
	 * @param dptID the datapoint type ID used for translation in a DPT translator
	 */
	public StateDP(final GroupAddress main, final String name, final int mainNumber,
		final String dptID)
	{
		this(main, name);
		setDPT(mainNumber, dptID);
	}

	/**
	 * Creates a new state based datapoint and adds invalidating and updating addresses.
	 *
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 * @param invalidatingAddresses KNX group addresses, whose indication messages
	 *        invalidate this datapoint state
	 * @param updatingAddresses KNX group addresses, whose indication and response
	 *        messages lead to an update of this datapoint state
	 */
	public StateDP(final GroupAddress main, final String name,
		final Collection<GroupAddress> invalidatingAddresses,
		final Collection<GroupAddress> updatingAddresses)
	{
		super(main, name, true);
		invalidating = Collections.synchronizedList(new ArrayList<>(invalidatingAddresses));
		updating = Collections.synchronizedList(new ArrayList<>(updatingAddresses));
	}

	/**
	 * Creates a new state based datapoint from XML input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read. The
	 * datapoint element is then expected to be the current element in the reader.
	 *
	 * @param r a XML reader
	 * @throws KNXMLException if the XML element is no datapoint or could not be read correctly
	 */
	public StateDP(final XmlReader r) throws KNXMLException
	{
		super(r);
		if (!isStateBased())
			throw new KNXMLException("no state based KNX datapoint element", r);
		invalidating = Collections.synchronizedList(new ArrayList<>());
		updating = Collections.synchronizedList(new ArrayList<>());
		doLoad(r);
	}

	/**
	 * Sets the expiration timeout for datapoint state values related to this datapoint.
	 * <p>
	 * This timeout specifies how long a local datapoint value is considered valid since
	 * it was received or got updated the last time in Calimero.<br>
	 * When working with state values related to this datapoint, this timeout should be
	 * queried using {@link #getExpirationTimeout()} to check whether there is a timeout
	 * set, and a value can still be considered valid / up to date. An expired state value
	 * is not intended to be used anymore, it should be discarded and requested/updated
	 * from the KNX network.<br>
	 * A timeout of 0 indicates no timeout limit set.
	 *
	 * @param timeout timeout in seconds, 0 for no timeout limit
	 */
	public final void setExpirationTimeout(final int timeout)
	{
		this.timeout = timeout;
	}

	/**
	 * Returns the expiration timeout for datapoint state values related to this
	 * datapoint.
	 * <p>
	 * If no timeout limit is set, 0 is returned.
	 *
	 * @return timeout in seconds, 0 for no timeout set
	 */
	public final int getExpirationTimeout()
	{
		return timeout;
	}

	/**
	 * Adds an updating group address to this datapoint to indicate that KNX messages with that destination address are
	 * allowed to update the associated datapoint state (i.e., the state value related to this datapoint). An address is
	 * added at most once.
	 *
	 * @param ga the KNX group address to add
	 */
	public void addUpdatingAddress(final GroupAddress ga)
	{
		if (getMainAddress().equals(ga))
			throw new KNXIllegalArgumentException(
					"updating address " + ga + " equals main address of this datapoint '" + getName() + "'");
		if (!updating.contains(ga))
			updating.add(ga);
	}

	/**
	 * Adds an invalidating group address to this datapoint to indicate that KNX messages with that address are allowed
	 * to invalidate the associated datapoint state (i.e., the state value related to this datapoint). An address is
	 * added at most once.
	 *
	 * @param ga the KNX group address to add
	 */
	public void addInvalidatingAddress(final GroupAddress ga)
	{
		if (getMainAddress().equals(ga))
			throw new KNXIllegalArgumentException(
					"updating address " + ga + " equals main address of this datapoint '" + getName() + "'");
		if (!invalidating.contains(ga))
			invalidating.add(ga);
	}

	/**
	 * Removes an updating/invalidating group address from this datapoint, so the group address is no longer contained
	 * in the corresponding updating/invalidating category.
	 *
	 * @param ga the KNX group address to remove
	 */
	public final void removeAddress(final GroupAddress ga)
	{
		updating.remove(ga);
		invalidating.remove(ga);
	}

	/**
	 * Returns the collection of KNX group addresses which are allowed to alter the state
	 * of this datapoint.
	 *
	 * @param updatingAddresses <code>true</code> if the updating addresses should be
	 *        returned, <code>false</code> for the invalidating addresses
	 * @return an unmodifiable collection with entries of type {@link GroupAddress}
	 */
	public Collection<GroupAddress> getAddresses(final boolean updatingAddresses)
	{
		return Collections.unmodifiableCollection(updatingAddresses ? updating : invalidating);
	}

	/**
	 * Returns whether KNX indication messages with destination group address
	 * <code>a</code> will invalidate the associated datapoint state of this datapoint.
	 *
	 * @param a the address to check
	 * @return <code>true</code> iff address is invalidating, <code>false</code> otherwise
	 */
	public final boolean isInvalidating(final GroupAddress a)
	{
		return invalidating.contains(a);
	}

	/**
	 * Returns whether KNX indication or response messages with destination address
	 * <code>a</code> will update the associated datapoint state of this datapoint.
	 *
	 * @param a the address to check
	 * @return <code>true</code> iff address is updating, <code>false</code> otherwise
	 */
	public final boolean isUpdating(final GroupAddress a)
	{
		return updating.contains(a);
	}

	@Override
	public String toString()
	{
		return "state DP " + locations() + " " + super.toString();
	}

	@Override
	void doLoad(final XmlReader r) throws KNXMLException
	{
		boolean main = false;
		for (int event = r.getEventType(); r.hasNext(); event = r.next()) {
			if (event == XmlReader.START_ELEMENT) {
				final String tag = r.getLocalName();
				if (tag.equals(TAG_EXPIRATION)) {
					final String a = r.getAttributeValue("", ATTR_TIMEOUT);
					r.getElementText();
					if (a != null)
						try {
							timeout = Integer.decode(a).intValue();
						}
						catch (final NumberFormatException e) {
							throw new KNXMLException("malformed attribute timeout", r);
						}
				}
				else if (tag.equals("location"))
					locations.add(r.getElementText());
				else if (tag.equals(TAG_UPDATING))
					while (r.nextTag() == XmlReader.START_ELEMENT)
						updating.add(new GroupAddress(r));
				else if (tag.equals(TAG_INVALIDATING))
					while (r.nextTag() == XmlReader.START_ELEMENT)
						invalidating.add(new GroupAddress(r));
				else if (!main) {
					super.doLoad(r);
					main = true;
				}
				else
					throw new KNXMLException("invalid element", r);
			}
			else if (event == XmlReader.END_ELEMENT && r.getLocalName().equals(TAG_DATAPOINT))
				break;
		}
		if (!main)
			throw new KNXMLException("Datapoint is missing its address", r);
	}

	@Override
	void doSave(final XmlWriter w) throws KNXMLException
	{
		// <expiration timeout=int />
		w.writeEmptyElement(TAG_EXPIRATION);
		w.writeAttribute(ATTR_TIMEOUT, Integer.toString(timeout));
		w.writeStartElement(TAG_UPDATING);
		synchronized (updating) {
			for (final Iterator<GroupAddress> i = updating.iterator(); i.hasNext();)
				i.next().save(w);
		}
		w.writeEndElement();
		w.writeStartElement(TAG_INVALIDATING);
		synchronized (invalidating) {
			for (final Iterator<GroupAddress> i = invalidating.iterator(); i.hasNext();)
				i.next().save(w);
		}
		w.writeEndElement();
	}

	private Collection<String> locations()
	{
		return locations;
	}
}
