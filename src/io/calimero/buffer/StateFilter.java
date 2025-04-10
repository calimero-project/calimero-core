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

import static java.lang.System.Logger.Level.ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.calimero.GroupAddress;
import io.calimero.KNXAddress;
import io.calimero.KNXFormatException;
import io.calimero.buffer.Configuration.NetworkFilter;
import io.calimero.buffer.Configuration.RequestFilter;
import io.calimero.buffer.cache.Cache;
import io.calimero.buffer.cache.CacheObject;
import io.calimero.cemi.CEMI;
import io.calimero.cemi.CEMIFactory;
import io.calimero.cemi.CEMILData;
import io.calimero.cemi.CEMILDataEx;
import io.calimero.datapoint.ChangeListener;
import io.calimero.datapoint.ChangeNotifier;
import io.calimero.datapoint.Datapoint;
import io.calimero.datapoint.DatapointMap;
import io.calimero.datapoint.DatapointModel;
import io.calimero.datapoint.StateDP;
import io.calimero.log.LogService;

/**
 * Predefined filter for filtering KNX messages of datapoints with state semantic into the
 * associated network buffer configuration.
 * <p>
 * This filter implements both
 * {@link io.calimero.buffer.Configuration.NetworkFilter} and
 * {@link io.calimero.buffer.Configuration.RequestFilter} for state-based
 * datapoint messages.<br>
 * This filter might be used in a configuration to build up and maintain a process image
 * of the KNX network the used network link communicates with. The buffer will keep the
 * most up-to-date state to a KNX group address / datapoint.<br>
 * KNX messages are buffered using a {@link LDataObject} (an object of this type is also
 * expected when the request method is invoked).
 * <p>
 * If a datapoint model is available in the {@link Configuration}, the filter uses that
 * model in its {@link #init(Configuration)} method. It initializes its local lookup
 * references with necessary updating/invalidating information of other datapoints stored
 * in that model. Thus, the filter will update or invalidate all other associated
 * datapoint state values in the network buffer configuration when receiving a new KNX
 * message.<br>
 * To reflect subsequent changes of the datapoint model in the filter, the filter has to
 * be reinitialized (using {@link #init(Configuration)}).
 *
 * @author B. Malinowsky
 */
public class StateFilter implements NetworkFilter, RequestFilter
{
	// contains cross-references of datapoints: which datapoint (key, of
	// type KNXAddress) invalidates/updates which datapoints (value,
	// of type List with GroupAddress entries)
	private Map<KNXAddress, List<GroupAddress>> invalidate;
	private Map<KNXAddress, List<GroupAddress>> update;

	// keep a reference to a notifying model used by the change listener
	private DatapointModel<? extends Datapoint> model;
	private final ChangeListener cl = new ChangeListener()
	{
		/**
		 * @param m
		 * @param dp
		 */
		@Override
		public void onDatapointRemoved(final DatapointModel<? extends Datapoint> m,
			final Datapoint dp)
		{
			if (dp instanceof StateDP dP)
				destroyReferences(dP);
		}

		/**
		 * @param m
		 * @param dp
		 */
		@Override
		public void onDatapointAdded(final DatapointModel<? extends Datapoint> m, final Datapoint dp)
		{
			if (dp instanceof StateDP dP)
				createReferences(dP);
		}
	};

	/**
	 * Creates a new state based filter.
	 */
	public StateFilter() {}

	@Override
	public void init(final Configuration c)
	{
		// check if we have a current model which emits change notifications
		if (model instanceof final ChangeNotifier notifier)
			notifier.removeChangeListener(cl);
		final DatapointModel<?> m = c.getDatapointModel();
		if (m instanceof final ChangeNotifier notifier) {
			model = m;
			notifier.addChangeListener(cl);
		}
		if (m != null)
			createReferences(m);
	}

	/**
	 * Applies state based filter rules on frame.
	 * <p>
	 * Criteria for accept:
	 * <ul>
	 * <li>the KNX message destination address is a group address</li>
	 * <li>there is <b>no</b> datapoint model available in the configuration, or</li>
	 * <li>there is a datapoint model available with a datapoint identified by the
	 * destination address <b>and</b> the datapoint is state based</li>
	 * <li>the message is an application layer group write or group response</li>
	 * </ul>
	 * On acceptance, the frame is stored into the configuration cache using a
	 * {@link LDataObject}. For easier handling of subsequent read requests on such a
	 * buffered frame, all frames are converted to L-data indications with application
	 * layer group response service code before getting stored.
	 * <p>
	 * If update and invalidation information is available, other dependent datapoint
	 * state values will be updated or invalidated appropriately.
	 *
	 * @param frame {@inheritDoc}
	 * @param c {@inheritDoc}
	 */
	@Override
	public void accept(final CEMI frame, final Configuration c)
	{
		final Cache cache = c.getCache();
		if (cache == null || !(frame instanceof final CEMILData f))
			return;

		if (!(f.getDestination() instanceof final GroupAddress dst))
			return;
		final DatapointModel<?> m = c.getDatapointModel();
		Datapoint dp;
		if (m != null && ((dp = m.get(dst)) == null || !dp.isStateBased()))
			return;
		final byte[] d = f.getPayload();
		// filter for A-Group write (0x80) and read.res (0x40) services
		final int svc = d[0] & 0x03 | d[1] & 0xC0;
		CEMILData copy;
		if (svc == 0x40) {
			// read.res could be in an L-Data.con, too
			if (f.getMessageCode() == CEMILData.MC_LDATA_CON)
				try {
					copy = (CEMILData) CEMIFactory.create(CEMILData.MC_LDATA_IND, d, f);
				}
				catch (final KNXFormatException e) {
					LogService.getLogger("io.calimero").log(ERROR, "create L_Data.ind for network buffer: " + f, e);
					return;
				}
			else
				copy = f;
		}
		else if (svc == 0x80) {
			// adjust to read response frame
			d[1] = (byte) (d[1] & 0x3f | 0x40);
			try {
				copy = (CEMILData) CEMIFactory.create(CEMILData.MC_LDATA_IND, d, f);
			}
			catch (final KNXFormatException e) {
				LogService.getLogger("io.calimero").log(ERROR, "create L_Data.ind for network buffer: " + f, e);
				return;
			}
		}
		else
			return;

		// adjust some fields of the frame to buffer: hop count, repetition
		// make sure the frame hop count is 6
		final int hops = 6;
		if (copy instanceof CEMILDataEx ex) {
			if (copy.getHopCount() != hops)
				ex.setHopCount(hops);
			if (copy.isRepetition())
				copy = CEMIFactory.create(null, null, copy, false, false);
		}
		else {
			if (copy.getHopCount() != hops || copy.isRepetition())
				copy = new CEMILData(copy.getMessageCode(), copy.getSource(),
						copy.getDestination(), copy.getPayload(), copy.getPriority(), false, hops);
		}

		// put into cache object
		CacheObject co = cache.get(dst);
		if (co != null)
			((LDataObject) co).setFrame(copy);
		else
			co = new LDataObject(copy);
		cache.put(co);

		// do invalidation/update of other datapoints
		// a write updates and invalidates, read.res only updates
		update(copy, cache);
		if (svc == 0x80)
			invalidate(copy, cache);
	}

	/* (non-Javadoc)
	 * @see io.calimero.buffer.Configuration.RequestFilter#request(
	 * io.calimero.KNXAddress, io.calimero.buffer.Configuration)
	 */
	@Override
	public CEMILData request(final KNXAddress dst, final Configuration c)
	{
		final Cache cache = c.getCache();
		if (cache == null || !(dst instanceof GroupAddress))
			return null;
		final LDataObject o = (LDataObject) cache.get(dst);
		if (o == null)
			return null;
		// check if there is an expiration timeout for a state based value
		final Datapoint dp;
		final DatapointModel<?> m = c.getDatapointModel();
		if (m != null && (dp = m.get((GroupAddress) dst)) != null && dp.isStateBased()) {
			final int t = ((StateDP) dp).getExpirationTimeout() * 1000;
			if (t != 0 && System.currentTimeMillis() > o.getTimestamp() + t)
				return null;
		}
		return o.getFrame();
	}

	private void update(final CEMILData f, final Cache c)
	{
		if (update != null) {
			final List<GroupAddress> upd = update.get(f.getDestination());
			if (upd != null)
				for (final var groupAddress : upd) {
					final CacheObject co = c.get(groupAddress);
					if (co != null)
						((LDataObject) co).setFrame(CEMIFactory.create(null,
								(KNXAddress) co.getKey(), f, false));
				}
		}
	}

	private void invalidate(final CEMILData f, final Cache c)
	{
		if (invalidate != null) {
			final List<GroupAddress> inv = invalidate.get(f.getDestination());
			if (inv != null)
				for (final var groupAddress : inv)
					c.remove(groupAddress);
		}
	}

	private void createReferences(final DatapointModel<? extends Datapoint> m)
	{
		invalidate = new HashMap<>();
		update = new HashMap<>();
		final Collection<? extends Datapoint> c = ((DatapointMap<? extends Datapoint>) m)
				.getDatapoints();
		synchronized (c) {
			for (final Datapoint dp : c) {
				if (dp instanceof StateDP dP)
					createReferences(dP);
			}
		}
	}

	private void createReferences(final StateDP dp)
	{
		createReferences(invalidate, dp.getAddresses(false), dp.getMainAddress());
		createReferences(update, dp.getAddresses(true), dp.getMainAddress());
	}

	private static void createReferences(final Map<KNXAddress, List<GroupAddress>> map,
		final Collection<GroupAddress> forAddr, final GroupAddress toAddr)
	{
		for (final GroupAddress ga : forAddr) {
			final List<GroupAddress> l = map.computeIfAbsent(ga, k -> new ArrayList<>());
			l.add(toAddr);
		}
	}

	private void destroyReferences(final StateDP dp)
	{
		destroyReferences(invalidate, dp.getAddresses(false), dp.getMainAddress());
		destroyReferences(update, dp.getAddresses(true), dp.getMainAddress());
	}

	private static void destroyReferences(final Map<KNXAddress, List<GroupAddress>> map,
		final Collection<GroupAddress> forAddr, final GroupAddress toAddr)
	{
		for (final GroupAddress ga : forAddr) {
			final List<GroupAddress> l = map.get(ga);
			if (l != null) {
				l.remove(toAddr);
				if (l.isEmpty())
					map.remove(ga);
			}
		}
	}
}
