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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.xml.Element;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XMLReader;
import tuwien.auto.calimero.xml.XMLWriter;

/**
 * A datapoint model storing datapoints with no defined order or hierarchy using a map
 * implementation.
 * <p>
 * 
 * @author B. Malinowsky
 */
public class DatapointMap implements DatapointModel, ChangeNotifier
{
	private static final String TAG_DATAPOINTS = "datapoints";

	private final Map points;
	private final EventListeners listeners = new EventListeners();

	/**
	 * Creates a new empty datapoint map.
	 * <p>
	 */
	public DatapointMap()
	{
		points = Collections.synchronizedMap(new HashMap(20));
	}

	/**
	 * Creates a new datapoint map and adds all <code>datapoints</code> to the map.
	 * <p>
	 * A datapoint to be added has to be unique according its main address, the attempt to
	 * add two datapoints using the same main address results in a
	 * KNXIllegalArgumentException.
	 * 
	 * @param datapoints collection with entries of type {@link Datapoint}
	 * @throws KNXIllegalArgumentException on duplicate datapoint
	 */
	public DatapointMap(final Collection datapoints)
	{
		// not all HashSets put additional capacity in HashSet(Collection) ctor
		final Map m = new HashMap(Math.max(2 * datapoints.size(), 11));
		for (final Iterator i = datapoints.iterator(); i.hasNext();) {
			final Datapoint dp = (Datapoint) i.next();
			if (m.containsKey(dp.getMainAddress()))
				throw new KNXIllegalArgumentException("duplicate datapoint "
					+ dp.getMainAddress());
			m.put(dp.getMainAddress(), dp);
		}
		points = Collections.synchronizedMap(m);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#add
	 * (tuwien.auto.calimero.datapoint.Datapoint)
	 */
	public void add(final Datapoint dp)
	{
		synchronized (points) {
			if (points.containsKey(dp.getMainAddress()))
				throw new KNXIllegalArgumentException("duplicate datapoint "
					+ dp.getMainAddress());
			points.put(dp.getMainAddress(), dp);
			fireChangeNotification(dp, true);
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#remove
	 * (tuwien.auto.calimero.datapoint.Datapoint)
	 */
	public void remove(final Datapoint dp)
	{
		if (points.remove(dp.getMainAddress()) != null)
			fireChangeNotification(dp, false);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#removeAll()
	 */
	public void removeAll()
	{
		points.clear();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#get
	 * (tuwien.auto.calimero.GroupAddress)
	 */
	public Datapoint get(final GroupAddress main)
	{
		return (Datapoint) points.get(main);
	}

	// ??? make this a super type interface method
	/**
	 * Returns all datapoints currently contained in this map.
	 * <p>
	 * 
	 * @return unmodifiable collection with entries of type {@link Datapoint}
	 */
	public Collection getDatapoints()
	{
		return Collections.unmodifiableCollection(points.values());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#contains
	 * (tuwien.auto.calimero.GroupAddress)
	 */
	public boolean contains(final GroupAddress main)
	{
		return points.containsKey(main);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#contains
	 * (tuwien.auto.calimero.datapoint.Datapoint)
	 */
	public boolean contains(final Datapoint dp)
	{
		return points.containsKey(dp.getMainAddress());
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#load
	 * (tuwien.auto.calimero.xml.XMLReader)
	 */
	public void load(final XMLReader r) throws KNXMLException
	{
		if (r.getPosition() != XMLReader.START_TAG)
			r.read();
		final Element e = r.getCurrent();
		if (r.getPosition() != XMLReader.START_TAG || !e.getName().equals(TAG_DATAPOINTS))
			throw new KNXMLException(TAG_DATAPOINTS + " element not found", e != null ? e.getName()
					: null, r.getLineNumber());
		synchronized (points) {
			while (r.read() == XMLReader.START_TAG) {
				final Datapoint dp = Datapoint.create(r);
				if (points.containsKey(dp.getMainAddress()))
					throw new KNXMLException("list contains "
							+ "duplicate KNX address in datapoint " + dp.getName(), dp.getMainAddress().toString(),
							r.getLineNumber());
				points.put(dp.getMainAddress(), dp);
			}
		}
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.DatapointModel#save
	 * (tuwien.auto.calimero.xml.XMLWriter)
	 */
	public void save(final XMLWriter w) throws KNXMLException
	{
		w.writeElement(TAG_DATAPOINTS, Collections.EMPTY_LIST, null);
		synchronized (points) {
			for (final Iterator i = points.values().iterator(); i.hasNext();)
				((Datapoint) i.next()).save(w);
		}
		w.endElement();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.ChangeNotifier#
	 * addChangeListener(tuwien.auto.calimero.datapoint.ChangeListener)
	 */
	public void addChangeListener(final ChangeListener l)
	{
		listeners.add(l);
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.datapoint.ChangeNotifier#
	 * removeChangeListener(tuwien.auto.calimero.datapoint.ChangeListener)
	 */
	public void removeChangeListener(final ChangeListener l)
	{
		listeners.remove(l);
	}

	private void fireChangeNotification(final Datapoint dp, final boolean added)
	{
		for (final Iterator i = listeners.iterator(); i.hasNext();) {
			final ChangeListener l = (ChangeListener) i.next();
			if (added)
				l.onDatapointAdded(this, dp);
			else
				l.onDatapointRemoved(this, dp);
		}
	}
}
