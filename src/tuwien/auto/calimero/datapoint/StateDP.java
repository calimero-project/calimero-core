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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.xml.Attribute;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

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
	private final List invalidating;
	// list of group addresses, whose .ind and .res messages update the data point
	private final List updating;
	// timeout in seconds, how long a set state value stays valid since reception
	private volatile int timeout;

	/**
	 * Creates a new state based datapoint with a name.
	 * <p>
	 * 
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 */
	public StateDP(final GroupAddress main, final String name)
	{
		super(main, name, true);
		invalidating = Collections.synchronizedList(new ArrayList());
		updating = Collections.synchronizedList(new ArrayList());
	}

	/**
	 * Creates a new datapoint with a name and specifies datapoint translation type.
	 * <p>
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
	 * <p>
	 * 
	 * @param main the group address used to identify this datapoint
	 * @param name user defined datapoint name
	 * @param invalidatingAddresses KNX group addresses, whose indication messages
	 *        invalidate this datapoint state
	 * @param updatingAddresses KNX group addresses, whose indication and response
	 *        messages lead to an update of this datapoint state
	 */
	public StateDP(final GroupAddress main, final String name,
		final Collection invalidatingAddresses, final Collection updatingAddresses)
	{
		super(main, name, true);
		invalidating = Collections.synchronizedList(new ArrayList(invalidatingAddresses));
		checkGAs(invalidating);
		updating = Collections.synchronizedList(new ArrayList(updatingAddresses));
		checkGAs(updating);
	}

	/**
	 * Creates a new state based datapoint from XML input.
	 * <p>
	 * If the current XML element position is no start tag, the next element tag is read.
	 * The datapoint element is then expected to be the current element in the reader.
	 * 
	 * @param r a XML reader
	 * @throws KNXMLException if the XML element is no datapoint or could not be read
	 *         correctly
	 */
	public StateDP(final XMLReader r) throws KNXMLException
	{
		super(r);
		if (!isStateBased())
			throw new KNXMLException("no state based KNX datapoint element", null,
					r.getLineNumber());
		invalidating = Collections.synchronizedList(new ArrayList());
		updating = Collections.synchronizedList(new ArrayList());
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
	 * Adds a group address to this datapoint to indicate that KNX messages with that
	 * address are allowed to alter the associated datapoint state (i.e., a state value
	 * related to this datapoint).
	 * <p>
	 * A group address can be marked as updating a state or invalidating a state. An
	 * address is added at most once to each category.
	 * 
	 * @param a the KNX group address to add
	 * @param isUpdating <code>true</code> to mark the address as updating this datapoint
	 *        state, <code>false</code> to mark it as state invalidating
	 */
	public final void add(final GroupAddress a, final boolean isUpdating)
	{
		if (getMainAddress().equals(a))
			throw new KNXIllegalArgumentException("address equals datapoint main address");
		if (isUpdating) {
			if (!updating.contains(a))
				updating.add(a);
		}
		else {
			if (!invalidating.contains(a))
				invalidating.add(a);
		}
	}

	/**
	 * Removes a state updating/invalidating group address from this datapoint.
	 * <p>
	 * The group address is no longer contained in the corresponding updating/invalidating
	 * category.
	 * 
	 * @param a the KNX group address to remove
	 * @param fromUpdating <code>true</code> to remove from updating this datapoint state,
	 *        <code>false</code> to remove from invalidating this datapoint state
	 */
	public final void remove(final GroupAddress a, final boolean fromUpdating)
	{
		if (fromUpdating)
			updating.remove(a);
		else
			invalidating.remove(a);
	}

	/**
	 * Returns the collection of KNX group addresses which are allowed to alter the state
	 * of this datapoint.
	 * <p>
	 * 
	 * @param updatingAddresses <code>true</code> if the updating addresses should be
	 *        returned, <code>false</code> for the invalidating addresses
	 * @return an unmodifiable collection with entries of type {@link GroupAddress}
	 */
	public Collection getAddresses(final boolean updatingAddresses)
	{
		return Collections.unmodifiableCollection(updatingAddresses ? updating : invalidating);
	}

	/**
	 * Returns whether KNX indication messages with destination group address
	 * <code>a</code> will invalidate the associated datapoint state of this datapoint.
	 * <p>
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
	 * <p>
	 * 
	 * @param a the address to check
	 * @return <code>true</code> iff address is updating, <code>false</code> otherwise
	 */
	public final boolean isUpdating(final GroupAddress a)
	{
		return updating.contains(a);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.Datapoint#toString()
	 */
	public String toString()
	{
		return "state DP " + super.toString();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.Datapoint#doLoad(
	 * tuwien.auto.calimero.xml.XMLReader)
	 */
	void doLoad(final XMLReader r) throws KNXMLException
	{
		boolean main = false;
		while (r.getPosition() == XMLReader.START_TAG) {
			final String tag = r.getCurrent().getName();
			if (tag.equals(TAG_EXPIRATION)) {
				final String a = r.getCurrent().getAttribute(ATTR_TIMEOUT);
				if (a != null)
					try {
						timeout = Integer.decode(a).intValue();
					}
					catch (final NumberFormatException e) {
						throw new KNXMLException("malformed attribute timeout, " + a, null,
								r.getLineNumber());
					}
			}
			else if (tag.equals(TAG_UPDATING))
				while (r.read() == XMLReader.START_TAG)
					updating.add(new GroupAddress(r));
			else if (tag.equals(TAG_INVALIDATING))
				while (r.read() == XMLReader.START_TAG)
					invalidating.add(new GroupAddress(r));
			else if (!main) {
				super.doLoad(r);
				main = true;
			}
			else
				throw new KNXMLException("invalid element", tag, r.getLineNumber());
			r.read();
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.Datapoint#doSave(
	 * tuwien.auto.calimero.xml.XMLWriter)
	 */
	void doSave(final XMLWriter w) throws KNXMLException
	{
		// <expiration timeout=int />
		w.writeEmptyElement(TAG_EXPIRATION, Arrays.asList(new Attribute[] { new Attribute(
				ATTR_TIMEOUT, Integer.toString(timeout)) }));
		w.writeElement(TAG_UPDATING, Collections.EMPTY_LIST, null);
		synchronized (updating) {
			for (final Iterator i = updating.iterator(); i.hasNext();)
				((GroupAddress) i.next()).save(w);
		}
		w.endElement();
		w.writeElement(TAG_INVALIDATING, Collections.EMPTY_LIST, null);
		synchronized (invalidating) {
			for (final Iterator i = invalidating.iterator(); i.hasNext();)
				((GroupAddress) i.next()).save(w);
		}
		w.endElement();
	}

	// iteration not synchronized
	private void checkGAs(final List l)
	{
		for (final Iterator i = l.iterator(); i.hasNext();)
			if (!(i.next() instanceof GroupAddress))
				throw new KNXIllegalArgumentException("not a group address list");
	}
}
