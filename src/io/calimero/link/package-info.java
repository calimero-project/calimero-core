/**
 * Link interface to access a KNX network. Two different types of links are provided:
 * <ul>
 * <li>send L-data messages to the KNX network and receive L-data messages from the network</li>
 * <li>monitor a KNX network by receiving monitor indication messages (completely passive)</li>
 * </ul>
 * A link implementation uses a specific underlying protocol to access the KNX network, hiding the particular type of
 * connection establishment and protocol interaction.<br>
 * Also, a link is configured with the KNX network medium used to interconnect KNX devices, to enable transparent
 * handling of network medium-specific message parts.
 */

package io.calimero.link;
