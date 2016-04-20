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

import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.buffer.cache.Cache;
import tuwien.auto.calimero.cemi.CEMI;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.DatapointModel;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.link.KNXNetworkLink;

/**
 * Configuration with settings associated with one network buffer.
 * <p>
 * Each network buffer is represented by one or more configurations, maintaining settings
 * to accomplish a certain buffering task. Users of the configuration have the ability to
 * adjust and to control the filter mechanism used for KNX messages, to specify the cache
 * (using some arbitrary replacement policy) and to control buffer activity, and set or
 * query configuration related information.<br>
 * Different configurations associated with one network buffer are independent of each
 * other.
 *
 * @author B. Malinowsky
 */
public interface Configuration
{
	/**
	 * Filter applied on incoming KNX messages from the KNX network.
	 * <p>
	 * In general, the methods of filter implementations should not throw any (runtime)
	 * exception, since there probably won't be any reasonable error handling in the
	 * surrounding method invoking the filter.
	 */
	public interface NetworkFilter
	{
		/**
		 * Allows the filter to get a reference to its owning configuration, and do all
		 * necessary filter initializations based on the current configuration settings.
		 * <p>
		 * This method is invoked by the owning configuration on activities which might
		 * require the filter to initialize itself or update its initialization.
		 *
		 * @param c the configuration this filter is set for
		 */
		void init(Configuration c);

		/**
		 * Invoked on occurrence of a new KNX messages, supplied as cEMI frame.
		 * <p>
		 * The filter will apply its individual rules to decide whether the frame is
		 * accepted or rejected, using provided information of the configuration if
		 * necessary. On acceptance, it is the filter's task to store the frame for
		 * subsequent access in the configuration cache.
		 *
		 * @param frame cEMI frame to accept for buffering
		 * @param c the configuration this filter belongs to
		 */
		void accept(CEMI frame, Configuration c);
	}

	/**
	 * Filter applied on requests to the network buffer from users or components working
	 * with the network buffer.
	 * <p>
	 * In general, the methods of filter implementations should not throw any (runtime)
	 * exception, since there probably won't be any reasonable error handling in the
	 * surrounding method invoking the filter.
	 */
	public interface RequestFilter
	{
		/**
		 * Requests data for address <code>dst</code> from the network buffer.
		 * <p>
		 * How a buffer is searched and data is matched depends on the individual filter
		 * rules. A filter might use provided information of the configuration if
		 * necessary.
		 *
		 * @param dst address to request data for
		 * @param c the configuration this filter belongs to
		 * @return the cEMI data on match in buffer, <code>null</code> otherwise
		 */
		CEMILData request(KNXAddress dst, Configuration c);
	}

	/**
	 * Sets the cache to be used in this configuration for buffering KNX messages.
	 * <p>
	 * A set cache object will be used by this configuration, until the cache is removed
	 * or another cache is set. Contents of a cache are only modified through the filters
	 * set in the configuration. So, for example, a configuration will never try to empty
	 * cache contents not used anymore, when buffering is deactivated.<br>
	 * Setting a new cache does not change the buffering activation state (for example
	 * enable caching if not already enabled). Use {@link #activate(boolean)} for this. To
	 * remove a currently used cache, invoke with argument <code>null</code>. Applying
	 * this method to a <code>null</code> argument will always deactivate buffering to
	 * prevent filters trying to access a non existing cache object.
	 *
	 * @param c cache to use subsequently for buffering message; use <code>null</code>
	 *        to remove the currently used cache, if any (this also deactivates buffering)
	 */
	void setCache(Cache c);

	/**
	 * Returns the currently used cache object or <code>null</code>, if no cache was
	 * set.
	 * <p>
	 *
	 * @return the cache, or <code>null</code> if no cache in use
	 */
	Cache getCache();

	/**
	 * Sets a datapoint model for this configuration.
	 * <p>
	 * The supplied model will not get modified in any way by this configuration.
	 *
	 * @param model datapoint model containing an arrangement with datapoints of interest;
	 *        use <code>null</code> to remove a currently set model (if any)
	 */
	void setDatapointModel(DatapointModel<? extends Datapoint> model);

	/**
	 * Returns the datapoint model set for this configuration, if any.
	 *
	 * @return datapoint model or <code>null</code>
	 */
	DatapointModel<? extends Datapoint> getDatapointModel();

	/**
	 * Sets the filters to be applied on incoming messages from the KNX network or to
	 * answer requests to the network buffer from the user or other components.
	 * <p>
	 *
	 * @param nf network filter for incoming KNX messages
	 * @param rf the filter to handle requests
	 */
	void setFilter(NetworkFilter nf, RequestFilter rf);

	/**
	 * Returns the filter applied on incoming messages from the KNX network.
	 * <p>
	 * If no filter was set in the first place, <code>null</code> is returned.
	 *
	 * @return the network filter or <code>null</code>
	 */
	NetworkFilter getNetworkFilter();

	/**
	 * Returns the filter responsible for handling requests.
	 * <p>
	 * If no filter was set in the first place, <code>null</code> is returned.
	 *
	 * @return the request filter or <code>null</code>
	 */
	RequestFilter getRequestFilter();

	/**
	 * Returns the KNX network link used to create this configuration.
	 * <p>
	 *
	 * @return the KNX network link
	 */
	KNXNetworkLink getBaseLink();

	/**
	 * Returns the buffered network link for the network buffer of this configuration.
	 * <p>
	 * A buffered link uses the base network link to access the KNX network, with
	 * additional handling for group read requests before issuing a send: the network
	 * buffer is queried whether it contains a KNX message for the requested KNX group
	 * address. If it does, the buffered message is returned skipping the read request to
	 * the base link (no access to the KNX network is done). Otherwise the read request is
	 * forwarded to the base link, which sends the request to the KNX network.<br>
	 * If possible, the buffered link checks the expiration timeout of a state based
	 * datapoint value using {@link StateDP#getExpirationTimeout()}, and on expired value
	 * requests an update from the KNX network.<br>
	 * So a buffered link utilizes the network buffer as shortcut for group communication
	 * by adding these buffer queries (when thinking in ways of design patterns, the
	 * buffered link would be a decorator object).
	 * <p>
	 * The buffered link behavior might be of advantage when doing process communication.
	 * For frequently queried datapoint states this might save bandwidth on the KNX
	 * network and shorten the response time of the answer.
	 * <p>
	 * Calling close() on the returned buffered link deactivates this configuration and
	 * will also close the base network link.
	 *
	 * @return the buffered KNX network link
	 */
	KNXNetworkLink getBufferedLink();

	/**
	 * Sets the answer behavior for queries of a buffered network link.
	 * <p>
	 * This method allows to control the behavior, whether queries which can not be
	 * answered from the network buffer should be forwarded to the KNX network or not.<br>
	 * By default, queries are forwarded to the KNX network whenever necessary, and not
	 * restricted to the network buffer (<code>bufferOnly</code> defaults to
	 * <code>false</code>).<br>
	 * Setting this behavior does not influence nor prevent write requests to the KNX
	 * network.<br>
	 * If this option is enabled (queries will get answered from network buffer only), a
	 * {@link KNXTimeoutException} thrown by the invoked method in the buffered network
	 * link indicates that the buffer can not answer the query.
	 * <p>
	 * When issuing a read request from a buffered link, the network buffer is checked at
	 * first, whether an answer to the request is found there. If the query is not
	 * answered successfully from the buffer, the request is passed on to the KNX network.
	 * <p>
	 * A use case where a limitation of queries to the network buffer might be applied, is
	 * when fetching data for a table or a visualization in a graphical user interface. It
	 * usually does not matter, whether some entries are present at the beginning or not.
	 * This will prevent a peak in the KNX network load when reading a big amount of data,
	 * and it will speed up the initialization using already buffered data. Missing, but
	 * required data entries might be explicitly queried on demand by the user.
	 * <p>
	 *
	 * @param bufferOnly <code>true</code> to limit all queries to network buffer,
	 *        <code>false</code> to forward unanswered queries to the KNX network
	 * @see #getBufferedLink
	 */
	void setQueryBufferOnly(boolean bufferOnly);

	/**
	 * Sets the buffer activation state of the network buffer controlled by this
	 * configuration.
	 * <p>
	 * The activation state controls whether filters are applied on new (incoming) KNX
	 * messages, allowing new messages to be buffered. Note that this does not affect
	 * already buffered content nor the behavior of the buffered link (for example, when
	 * doing a send to the KNX network or requesting a buffered message). So a deactivated
	 * buffer effectively prevents new KNX messages to get cached.<br>
	 * If the network buffer gets activated and no cache was set in the first place, a
	 * default cache might be created if considered necessary by an actual implementation
	 * to be used for subsequent caching operations. A deactivation will not remove or
	 * empty any cached contents.
	 *
	 * @param activate <code>true</code> to activate the buffer, <code>false</code>
	 *        otherwise
	 */
	void activate(boolean activate);

	/**
	 * Returns whether the network buffer is activated.
	 * <p>
	 *
	 * @return <code>true</code> iff buffer is active, <code>false</code> otherwise
	 */
	boolean isActive();
}
