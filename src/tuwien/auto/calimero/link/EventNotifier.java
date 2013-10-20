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

package tuwien.auto.calimero.link;

import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.log.LogService;

/**
 * Threaded event notifier for network link and monitor.
 * <p>
 * 
 * @author B. Malinowsky
 */
abstract class EventNotifier extends Thread implements KNXListener
{
	static interface EventCallback
	{
		/**
		 * Invokes the appropriate listener method with the event contained in this event
		 * callback.
		 * <p>
		 * 
		 * @param l the listener to notify
		 */
		void invoke(LinkListener l);
	}

	static final class Indication implements EventCallback
	{
		private final FrameEvent event;

		Indication(final FrameEvent e)
		{
			event = e;
		}

		public void invoke(final LinkListener l)
		{
			l.indication(event);
		}
	}

	static final class Confirmation implements EventCallback
	{
		private final FrameEvent event;

		Confirmation(final FrameEvent e)
		{
			event = e;
		}

		public void invoke(final LinkListener l)
		{
			((NetworkLinkListener) l).confirmation(event);
		}
	}

	static final class Closed implements EventCallback
	{
		private final CloseEvent event;

		Closed(final CloseEvent e)
		{
			event = e;
		}

		public void invoke(final LinkListener l)
		{
			l.linkClosed(event);
		}
	}

	final LogService logger;
	final Object source;

	private final EventListeners listeners;

	private final List events = new LinkedList();
	private volatile boolean stop;

	EventNotifier(final Object source, final LogService logger)
	{
		super("Link notifier");
		this.logger = logger;
		this.source = source;
		listeners = new EventListeners(logger);
		setDaemon(true);
		start();
	}

	public final void run()
	{
		try {
			while (true) {
				EventCallback ec;
				synchronized (events) {
					while (events.isEmpty())
						events.wait();
					ec = (EventCallback) events.remove(0);
				}
				fire(ec);
			}
		}
		catch (final InterruptedException e) {}
		// empty event queue
		synchronized (events) {
			while (!events.isEmpty())
				fire((EventCallback) events.remove(0));
		}
	}

	public abstract void frameReceived(FrameEvent e);

	public void connectionClosed(final CloseEvent e)
	{
		addEvent(new Closed(new CloseEvent(source, e.getInitiator(), e.getReason())));
		quit();
	}

	final void addEvent(final EventCallback ec)
	{
		if (!stop) {
			synchronized (events) {
				events.add(ec);
				events.notify();
			}
		}
	}

	final void addListener(final LinkListener l)
	{
		if (stop)
			return;
		listeners.add(l);
	}

	final void removeListener(final LinkListener l)
	{
		listeners.remove(l);
	}

	final void quit()
	{
		interrupt();
		if (currentThread() != this) {
			try {
				join();
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void fire(final EventCallback ec)
	{
		final EventListener[] el = listeners.listeners();
		for (int i = 0; i < el.length; i++) {
			final LinkListener l = (LinkListener) el[i];
			try {
				ec.invoke(l);
			}
			catch (final RuntimeException rte) {
				removeListener(l);
				logger.error("removed event listener", rte);
			}
		}
	}
}
