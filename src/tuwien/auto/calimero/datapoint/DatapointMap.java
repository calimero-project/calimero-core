/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2015 B. Malinowsky

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XmlReader;
import tuwien.auto.calimero.xml.XmlWriter;

/**
 * A datapoint model storing datapoints with no defined order or hierarchy using a map
 * implementation.
 * <p>
 *
 * @author B. Malinowsky
 */
public class DatapointMap<T extends Datapoint> implements DatapointModel<T>, ChangeNotifier
{
	private static final String TAG_DATAPOINTS = "datapoints";

	private final Map<GroupAddress, T> points;
	private final EventListeners<ChangeListener> listeners = new EventListeners<>();

	// NYI ensure we only load valid types based on parameterization, assign from constructor
	// parameter or factory method
	private final Class<Datapoint> dpTypeRef = Datapoint.class;

	/**
	 * Creates a new empty datapoint map.
	 */
	public DatapointMap()
	{
		points = Collections.synchronizedMap(new HashMap<>(20));
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
	public DatapointMap(final Collection<T> datapoints)
	{
		// not all HashSets put additional capacity in HashSet(Collection) ctor
		final Map<GroupAddress, T> m = new HashMap<>(Math.max(2 * datapoints.size(), 11));
		for (final Iterator<T> i = datapoints.iterator(); i.hasNext();) {
			final T dp = i.next();
			if (m.containsKey(dp.getMainAddress()))
				throw new KNXIllegalArgumentException("duplicate datapoint " + dp.getMainAddress());
			m.put(dp.getMainAddress(), dp);
		}
		points = Collections.synchronizedMap(m);
	}

	@Override
	public void add(final T dp)
	{
		synchronized (points) {
			if (points.containsKey(dp.getMainAddress()))
				throw new KNXIllegalArgumentException("duplicate datapoint "
					+ dp.getMainAddress());
			points.put(dp.getMainAddress(), dp);
			fireChangeNotification(dp, true);
		}
	}

	@Override
	public void remove(final T dp)
	{
		if (points.remove(dp.getMainAddress()) != null)
			fireChangeNotification(dp, false);
	}

	@Override
	public void removeAll()
	{
		points.clear();
	}

	@Override
	public T get(final GroupAddress main)
	{
		return points.get(main);
	}

	// ??? make this a super type interface method
	/**
	 * Returns all datapoints currently contained in this map.
	 * <p>
	 *
	 * @return unmodifiable collection with entries of type {@link Datapoint}
	 */
	public Collection<T> getDatapoints()
	{
		return Collections.unmodifiableCollection(points.values());
	}

	@Override
	public boolean contains(final GroupAddress main)
	{
		return points.containsKey(main);
	}

	@Override
	public boolean contains(final T dp)
	{
		return points.containsKey(dp.getMainAddress());
	}

	@Override
	public void load(final XmlReader r) throws KNXMLException
	{
		if (r.getEventType() != XmlReader.START_ELEMENT)
			r.nextTag();
		if (r.getEventType() != XmlReader.START_ELEMENT || !r.getLocalName().equals(TAG_DATAPOINTS))
			throw new KNXMLException(TAG_DATAPOINTS + " element not found", r);
		synchronized (points) {
			while (r.nextTag() == XmlReader.START_ELEMENT) {
				final Datapoint dp = Datapoint.create(r);
				if (points.containsKey(dp.getMainAddress()))
					throw new KNXMLException("KNX address " + dp.getMainAddress().toString()
							+ " in datapoint \"" + dp.getName() + "\" already used", r);
				if (!dpTypeRef.isAssignableFrom(dp.getClass()))
					throw new KNXMLException("datapoint not of type " + dpTypeRef.getTypeName(), r);
				@SuppressWarnings("unchecked")
				final T castDp = (T) dp;
				points.put(dp.getMainAddress(), castDp);
			}
		}
	}

	@Override
	public void save(final XmlWriter w) throws KNXMLException
	{
		w.writeStartElement(TAG_DATAPOINTS);
		synchronized (points) {
			for (final Iterator<T> i = points.values().iterator(); i.hasNext();)
				i.next().save(w);
		}
		w.writeEndElement();
	}

	@Override
	public void addChangeListener(final ChangeListener l)
	{
		listeners.add(l);
	}

	@Override
	public void removeChangeListener(final ChangeListener l)
	{
		listeners.remove(l);
	}

	private void fireChangeNotification(final T dp, final boolean added)
	{
		if (added)
			listeners.fire(l -> l.onDatapointAdded(this, dp));
		else
			listeners.fire(l -> l.onDatapointRemoved(this, dp));
	}
}
