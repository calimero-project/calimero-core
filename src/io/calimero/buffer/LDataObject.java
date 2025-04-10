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

package io.calimero.buffer;

import io.calimero.KNXAddress;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.buffer.cache.CacheObject;
import io.calimero.cemi.CEMILData;

/**
 * Used for holding {@link CEMILData} frames.
 * <p>
 *
 * @author B. Malinowsky
 */
public class LDataObject extends CacheObject
{
	/**
	 * Creates a {@link LDataObject} holding the {@code frame} argument.
	 * <p>
	 *
	 * @param frame {@link CEMILData} frame
	 */
	public LDataObject(final CEMILData frame)
	{
		super(frame.getDestination(), frame);
	}

	/**
	 * Creates a {@link LDataObject} set to key {@code addrKey} and holding
	 * {@code value}.
	 * <p>
	 * If {@code value} is not of type {@link CEMILData}, the methods
	 * {@link #setFrame(CEMILData)} and {@link #getFrame()} have to be overridden.
	 *
	 * @param key key of this cache object
	 * @param value value hold by this cache object
	 */
	protected LDataObject(final KNXAddress key, final Object value)
	{
		super(key, value);
	}

	/**
	 * Sets a new {@link CEMILData} {@code frame} for this cache object.
	 * <p>
	 * The key generated out of {@code frame} (i.e., out of the KNX address of
	 * {@code frame}) has to be equal to the key of this {@link LDataObject},
	 * as returned from {@link #getKey()}. Otherwise, a
	 * {@link KNXIllegalArgumentException} will be thrown.<br>
	 * Note that on successful set the timestamp is renewed.
	 *
	 * @param frame the new {@link CEMILData} frame to set
	 */
	public synchronized void setFrame(final CEMILData frame)
	{
		if (!frame.getDestination().equals(getKey()))
			throw new KNXIllegalArgumentException("frame key differs from cache key");
		value = frame;
		resetTimestamp();
	}

	/**
	 * Returns a {@link CEMILData} frame contained in this cache object.
	 * <p>
	 * On no frame available, {@code null} is returned.
	 *
	 * @return the {@link CEMILData} frame, or {@code null}
	 */
	public synchronized CEMILData getFrame()
	{
		return (CEMILData) value;
	}
}
