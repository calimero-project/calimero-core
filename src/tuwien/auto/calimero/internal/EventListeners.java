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

package tuwien.auto.calimero.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import tuwien.auto.calimero.log.LogService;

/**
 * Container for keeping event listeners.
 * <p>
 * The assumption for implementation of this class is that iterating over event listeners
 * is the predominant operation, adding and removing listeners not.
 * 
 * @author B. Malinowsky
 */
public class EventListeners
{
	private final List listeners = new ArrayList();
	private EventListener[] listenersCopy = new EventListener[0];
	private final LogService logger;

	/**
	 * Creates a new event listeners container object.
	 * <p>
	 */
	public EventListeners()
	{
		this(null);
	}

	/**
	 * Creates a new event listeners container object.
	 * <p>
	 * 
	 * @param logger optional logger for log output
	 */
	public EventListeners(final LogService logger)
	{
		this.logger = logger;
	}

	/**
	 * Adds the specified event listener <code>l</code> to this container.
	 * <p>
	 * If <code>l</code> is
	 * <code>null<code> or was already added as listener, no action is performed.
	 * 
	 * @param l the listener to add
	 */
	public void add(final EventListener l)
	{
		if (l == null)
			return;
		synchronized (listeners) {
			if (!listeners.contains(l)) {
				listeners.add(l);
				listenersCopy = (EventListener[]) listeners
					.toArray(new EventListener[listeners.size()]);
			}
			else if (logger != null)
				logger.warn("event listener already registered");
		}
	}

	/**
	 * Removes the specified event listener <code>l</code> from this container.
	 * <p>
	 * If <code>l</code> was not added in the first place, no action is performed.
	 * 
	 * @param l the listener to remove
	 */
	public void remove(final EventListener l)
	{
		synchronized (listeners) {
			if (listeners.remove(l))
				listenersCopy = (EventListener[]) listeners
					.toArray(new EventListener[listeners.size()]);
		}
	}

	/**
	 * Removes all event listeners from this container.
	 * <p>
	 */
	public void removeAll()
	{
		synchronized (listeners) {
			listeners.clear();
			listenersCopy = new EventListener[0];
		}
	}

	/**
	 * Returns an array with all event listeners.
	 * <p>
	 * While modifying the returned array will have no impact on the event listeners kept
	 * by this class, the array might be reused for subsequent callers, who will be
	 * affected.
	 * 
	 * @return array with all event listeners in this container, with array size equal to
	 *         the number of contained listeners
	 */
	public EventListener[] listeners()
	{
		return listenersCopy;
	}

	/**
	 * Returns an iterator for the contained event listeners.
	 * <p>
	 * 
	 * @return the iterator for the listeners
	 */
	public Iterator iterator()
	{
		return Arrays.asList(listenersCopy).iterator();
	}

	// not for general use, quite slow due to reflection mechanism
	/*
	void fire(final Object event, final Method method)
	{
		final Object[] objs = new Object[] { event };
		for (final Iterator i = iterator(); i.hasNext();) {
			final EventListener l = (EventListener) i.next();
			try {
				method.invoke(l, objs);
			}
			catch (final RuntimeException rte) {
				remove(l);
				logger.error("removed event listener", rte);
			}
			catch (final IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (final InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	*/
}
