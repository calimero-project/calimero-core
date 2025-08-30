/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

package io.calimero.link;

import java.lang.System.Logger;
import java.util.function.Consumer;

import io.calimero.CloseEvent;
import io.calimero.FrameEvent;
import io.calimero.KNXListener;
import io.calimero.internal.EventListeners;
import io.calimero.internal.Executor;

/**
 * Threaded event notifier for network link and monitor.
 *
 * @author B. Malinowsky
 */
public abstract class EventNotifier<T extends LinkListener> implements KNXListener
{
	final Logger logger;
	final Object source;

	private final EventListeners<T> listeners = new EventListeners<>(LinkEvent.class);

	EventNotifier(final Object source, final Logger logger)
	{
		this.logger = logger;
		this.source = source;
	}

	@Override
	public abstract void frameReceived(FrameEvent e);

	@Override
	public void connectionClosed(final CloseEvent e)
	{
		addEvent(l -> l.linkClosed(new CloseEvent(source, e.getInitiator(), e.getReason())));
		quit();
	}

	public EventListeners<T> getListeners()
	{
		return listeners;
	}

	public void registerEventType(final Class<?> eventType) {
		listeners.registerEventType(eventType);
	}

	private interface CustomEventConsumer<T> extends Consumer<T> {}

	public void dispatchCustomEvent(final Object event) {
		final CustomEventConsumer<T> cec = __ -> listeners.dispatchCustomEvent(event);
		addEvent(cec);
	}

	final void addEvent(final Consumer<? super T> c)
	{
		final var name = "Calimero link notifier";
		if (c instanceof CustomEventConsumer)
			Executor.execute(() -> fireCustomEvent(c), name);
		else
			Executor.execute(() -> fire(c), name);
	}

	final void addListener(final T l)
	{
		listeners.add(l);
	}

	final void removeListener(final T l)
	{
		listeners.remove(l);
	}

	final void quit() {}

	private void fire(final Consumer<? super T> c)
	{
		 listeners.fire(c);
	}

	private void fireCustomEvent(final Consumer<? super T> c) { c.accept(null); }
}
