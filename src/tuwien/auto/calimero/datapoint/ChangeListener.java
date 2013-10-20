/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2011, 2012 B. Malinowsky

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

import java.util.EventListener;

/**
 * A listener interface to receive notifications about datapoint model events.
 * <p>
 * 
 * @see DatapointModel
 * @author B. Malinowsky
 */
public interface ChangeListener extends EventListener
{
	/**
	 * A new datapoint was added to the datapoint model.
	 * <p>
	 * On receiving this notification, {@link DatapointModel#contains(Datapoint)} returns
	 * true.
	 * 
	 * @param m the datapoint model emitting the notification
	 * @param dp the datapoint added
	 */
	void onDatapointAdded(DatapointModel m, Datapoint dp);

	/**
	 * A datapoint was removed from the datapoint model.
	 * <p>
	 * Prior this notification, the datapoint was contained in the datapoint model; on
	 * receiving this notification, {@link DatapointModel#contains(Datapoint)} returns
	 * false.
	 * 
	 * @param m the datapoint model emitting the notification
	 * @param dp the datapoint removed
	 */
	void onDatapointRemoved(DatapointModel m, Datapoint dp);
}
