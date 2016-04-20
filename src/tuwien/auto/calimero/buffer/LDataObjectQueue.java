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

package tuwien.auto.calimero.buffer;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.buffer.cache.CacheObject;
import tuwien.auto.calimero.cemi.CEMILData;

/**
 * A {@link CacheObject} used for holding a list of {@link CEMILData} frames.
 * <p>
 * A KNX address is specified on creation, which will be used to generate the cache object
 * key. A LDataObjectQueue is used for frames having all the same key like the one
 * supplied on creation of a new queue. For each frame inserted into the queue, its own
 * timestamp will get stored.<br>
 * These different queue behaviors are available (optional):<br>
 * A limit might be set for the maximum size of the queue. Reading of the queue can be
 * consuming, i.e., a frame and its timestamp is removed after reading the frame. With a
 * maximum queue size set, overwriting of old frames (according to insertion order) can be
 * enabled, to allow storing a new frame when the queue got filled up by removing the
 * oldest frames (i.e., a ring buffer).<br>
 *
 * @author B. Malinowsky
 */
public class LDataObjectQueue extends LDataObject
{
	/**
	 * Represents an item in the queue, holding one L-Data frame and its timestamp.
	 */
	public static final class QueueItem
	{
		private final CEMILData f;
		private final long ts;

		/**
		 * Creates a new QueueItem.
		 *
		 * @param frame item L-Data frame
		 * @param timestamp item timestamp
		 */
		public QueueItem(final CEMILData frame, final long timestamp)
		{
			f = frame;
			ts = timestamp;
		}

		/**
		 * Returns the timestamp for this item.
		 * <p>
		 *
		 * @return timestamp as long
		 */
		public long getTimestamp()
		{
			return ts;
		}

		/**
		 * Returns the L-Data frame for this item.
		 * <p>
		 *
		 * @return L-Data frame
		 */
		public CEMILData getFrame()
		{
			return f;
		}
	}

	/**
	 * Listener for queue events.
	 * <p>
	 * A notification callback is invoked synchronized with the event source, so the
	 * listener will have a consistent view of the queue during that time, allowing to
	 * take appropriate actions. During the callback no concurrent changes to the queue
	 * object are possible.
	 */
	public interface QueueListener
	{
		/**
		 * Called on a full queue. A notification is sent only once every time a queue got
		 * filled up. In other words, no notifying is done by an already full queue on
		 * receiving another frame.
		 *
		 * @param queue event source, the queue which reached its maximum size
		 */
		void queueFilled(LDataObjectQueue queue);
	}

	private static final int STD_BUFSIZE = 30;
	private int next;
	private int size;
	private long[] timestamps;

	// wrap specifies ring buffer (with overwrite)
	private final boolean overwrite;
	private final boolean consuming;
	private final boolean max;
	private final QueueListener listener;

	/**
	 * Creates a new LDataObjectQueue for KNX address <code>addr</code>.
	 * <p>
	 * There is no limit on the queue size used.
	 *
	 * @param addr KNX address to create the queue for, this address is used to generate
	 *        the cache object key
	 */
	public LDataObjectQueue(final GroupAddress addr)
	{
		this(addr, false);
	}

	/**
	 * Creates a new LDataObjectQueue for KNX address <code>addr</code>.
	 * <p>
	 * There is no limit on the queue size used.
	 *
	 * @param addr KNX address to create the queue for, this address is used to generate
	 *        the cache object key
	 * @param consumingRead set <code>true</code> to remove a frame from the queue the
	 *        first time it gets requested for read (i.e., returned by a frame getter
	 *        method), <code>false</code> to leave it in the queue
	 */
	public LDataObjectQueue(final GroupAddress addr, final boolean consumingRead)
	{
		super(addr, new CEMILData[STD_BUFSIZE]);
		timestamps = new long[STD_BUFSIZE];
		consuming = consumingRead;
		overwrite = false;
		max = false;
		listener = null;
	}

	/**
	 * Creates a new LDataObjectQueue for KNX address <code>addr</code> with a fixed
	 * maximum queue size.
	 * <p>
	 *
	 * @param addr KNX address to create the queue for, this address is used to generate
	 *        the cache object key
	 * @param consumingRead set <code>true</code> to remove a frame from the queue the
	 *        first time it gets requested for read (i.e., returned by a frame getter
	 *        method), <code>false</code> to leave it in the queue
	 * @param maxSize the maximum queue size, i.e., max. number of frames hold by the
	 *        queue, maxSize has to be &gt; 0
	 * @param overwrite set <code>true</code> if on full queue a new frame should
	 *        replace the oldest frame in the queue (i.e., ring buffer semantics),<br>
	 *        set <code>false</code> if on full queue new frames should be ignored
	 * @param l sets a queue listener to receive events from this queue, use
	 *        <code>null</code> if no notifications are required
	 */
	public LDataObjectQueue(final GroupAddress addr, final boolean consumingRead, final int maxSize,
		final boolean overwrite, final QueueListener l)
	{
		super(addr, new CEMILData[maxSize]);
		// maxSize < 0 is already checked at array creation
		if (maxSize == 0)
			throw new KNXIllegalArgumentException("queue size 0 not allowed");
		timestamps = new long[maxSize];
		this.overwrite = overwrite;
		consuming = consumingRead;
		max = true;
		listener = l;
	}

	/**
	 * {@inheritDoc}<br>
	 * If a maximum size is set and the queue already reached maximum size, if
	 * {@link #isOverwrite()} evaluates to<br> - <code>true</code>, <code>frame</code>
	 * will replace the oldest inserted frame<br> - <code>false</code>,
	 * <code>frame</code> is ignored and not queued.
	 */
	@Override
	public synchronized void setFrame(final CEMILData frame)
	{
		if (!frame.getDestination().equals(getKey()))
			throw new KNXIllegalArgumentException("frame key differs from this key");
		if (!max)
			ensureCapacity();
		else if (!overwrite && size == timestamps.length)
			return;
		final CEMILData[] c = (CEMILData[]) value;
		// notify on first time queue fills up
		final boolean notifyListener = max && size == c.length - 1;

		resetTimestamp();
		c[next] = frame;
		timestamps[next] = getTimestamp();
		++next;
		next %= c.length;
		if (size < c.length)
			++size;
		if (notifyListener)
			fireQueueFilled();
	}

	/**
	 * {@inheritDoc} Frames are returned in insertion order. If consuming read behavior is
	 * enabled, the frame will be removed from the queue before return.
	 */
	@Override
	public synchronized CEMILData getFrame()
	{
		final CEMILData[] c = (CEMILData[]) value;
		final int first = first();
		final CEMILData f = c[first];
		if (size > 0 && consuming) {
			--size;
			c[first] = null;
			timestamps[first] = 0;
			if (size == 0)
				next = 0;
		}
		return f;
	}

	/**
	 * {@inheritDoc}<br>
	 * Note that the length of the returned buffer might be 0.<br>
	 * This method is equal to invoking {@link #getFrames()}.
	 *
	 * @return the queue as CEMILData array
	 */
	@Override
	public Object getValue()
	{
		return getFrames();
	}

	/**
	 * Returns the next queued item in insertion order.
	 * <p>
	 * The item consists of the L-Data frame and its associated timestamp. This method
	 * behaves equal to {@link #getFrame()} with respect to queueing behavior.<br>
	 * If the queue is empty, an empty QueueItem is returned.
	 *
	 * @return queued item as QueueItem object
	 */
	public synchronized QueueItem getItem()
	{
		final long ts = timestamps[first()];
		return new QueueItem(getFrame(), ts);
	}

	/**
	 * Returns all L-Data frames currently hold by the queue.
	 * <p>
	 * The frames are returned in insertion order. If consuming read behavior is enabled,
	 * the internal queue buffer is cleared before return.<br>
	 * Note that on consuming read the associated timestamps of the frames will be
	 * cleared, too, before return of this method. In order to obtain the timestamps,
	 * {@link #getTimestamps()} has to be called at first.
	 *
	 * @return the frame queue as CEMILData array
	 */
	public final synchronized CEMILData[] getFrames()
	{
		final CEMILData[] buf = new CEMILData[size];
		final CEMILData[] c = (CEMILData[]) value;
		copyFifo(c, buf);
		if (consuming)
			clear();
		return buf;
	}

	/**
	 * Returns the timestamps of the frames hold by the queue.
	 * <p>
	 * The timestamps are returned in insertion order of the associated frames.<br>
	 * This means, if no {@link #setFrame(CEMILData)} is called in between,<br>
	 * <ul>
	 * <li>{@link #getTimestamps()}[i] belongs to {@link #getFrames()}[i], where 0 &le; i &lt;
	 * {@link #getSize()}</li>
	 * <li>{@link #getTimestamps()}.length == {@link #getFrames()}.length</li>
	 * </ul>.
	 * Note that on consuming read, reading of timestamps does not trigger any removal of
	 * frames (see {@link #getFrames()}).
	 *
	 * @return the timestamps of all contained frames as array of type long
	 * @see #getTimestamp()
	 */
	public final synchronized long[] getTimestamps()
	{
		final long[] buf = new long[size];
		copyFifo(timestamps, buf);
		return buf;
	}

	/**
	 * Returns the number of frames currently in the queue.
	 * <p>
	 * Since each frame has its associated timestamp, this is also the number of
	 * timestamps at the same time.
	 *
	 * @return number of frames (timestamps) currently in the queue
	 */
	public final synchronized int getSize()
	{
		return size;
	}

	/**
	 * Returns mode for overwrite used for this queue.
	 * <p>
	 *
	 * @return <code>true</code> if overwrite is used, <code>false</code> otherwise
	 */
	public final boolean isOverwrite()
	{
		return overwrite;
	}

	/**
	 * Returns mode for consuming read behavior.
	 * <p>
	 *
	 * @return <code>true</code> if queued items get consumed on read,
	 *         <code>false</code> if a read does not modify the queue
	 */
	public final boolean isConsuming()
	{
		return consuming;
	}

	/**
	 * Clears the frame and timestamp queue.
	 * <p>
	 * The queue will be empty on return.
	 */
	public final synchronized void clear()
	{
		timestamps = new long[max ? timestamps.length : STD_BUFSIZE];
		value = new CEMILData[timestamps.length];
		next = 0;
		size = 0;
	}

	private void copyFifo(final Object src, final Object dst)
	{
		final int start = first();
		final int length = Math.min(timestamps.length - start, size);
		System.arraycopy(src, start, dst, 0, length);
		if (length < size)
			System.arraycopy(src, 0, dst, length, size - length);
	}

	private void ensureCapacity()
	{
		if (size == timestamps.length) {
			// someone might have an old buffer size of 1...
			final int newCapacity = size * 3 / 2 + 1;
			final CEMILData[] newData = new CEMILData[newCapacity];
			copyFifo(value, newData);
			value = newData;
			// timestamps.length is used in first() calculation,
			// assign the new array only after copy!
			final long[] newTS = new long[newCapacity];
			copyFifo(timestamps, newTS);
			timestamps = newTS;
			next = size;
		}
	}

	private synchronized void fireQueueFilled()
	{
		if (listener != null) {
			try {
				listener.queueFilled(this);
			}
			catch (final RuntimeException e) {}
		}
	}

	private int first()
	{
		return (next + timestamps.length - size) % timestamps.length;
	}
}
