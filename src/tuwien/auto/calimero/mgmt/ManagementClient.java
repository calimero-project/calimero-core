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

package tuwien.auto.calimero.mgmt;

import java.util.List;
import java.util.function.BiConsumer;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;

/**
 * Application layer services providing management related tasks in a KNX network for a
 * client.
 * <p>
 *
 * @author B. Malinowsky
 */
public interface ManagementClient extends AutoCloseable
{
	/**
	 * Sets the response timeout to wait for a KNX response message to arrive to complete
	 * a message exchange.
	 *
	 * @param timeout time in seconds, <code>timeout &gt; 0</code>
	 */
	void setResponseTimeout(int timeout);

	/**
	 * Returns the response timeout used when waiting for a KNX response message to
	 * arrive.
	 * <p>
	 *
	 * @return timeout in seconds
	 */
	int getResponseTimeout();

	/**
	 * Sets the KNX message priority for KNX messages to send.
	 *
	 * @param p new priority to use
	 */
	void setPriority(Priority p);

	/**
	 * Returns the current used KNX message priority for KNX messages.
	 * <p>
	 *
	 * @return message Priority
	 */
	Priority getPriority();

	/**
	 * Creates a new destination using the remote KNX address for management
	 * communication.
	 * <p>
	 * A management client will use the transport layer for creating the destination.
	 *
	 * @param remote destination KNX individual address
	 * @param connectionOriented <code>true</code> for connection oriented mode,
	 *        <code>false</code> for connectionless mode
	 * @return destination representing the logical connection
	 */
	Destination createDestination(IndividualAddress remote, boolean connectionOriented);

	/**
	 * Creates a new destination using the remote KNX address and connection settings for
	 * management communication.
	 * <p>
	 * A management client will use the transport layer for creating the destination.
	 *
	 * @param remote destination KNX individual address
	 * @param connectionOriented <code>true</code> for connection oriented mode,
	 *        <code>false</code> for connectionless mode
	 * @param keepAlive <code>true</code> to prevent a timing out of the logical
	 *        connection in connection oriented mode, <code>false</code> to use default
	 *        connection timeout
	 * @param verifyMode <code>true</code> to indicate the destination has verify mode
	 *        enabled, <code>false</code> otherwise
	 * @return destination representing the logical connection
	 */
	Destination createDestination(IndividualAddress remote, boolean connectionOriented,
		boolean keepAlive, boolean verifyMode);

	/**
	 * Modifies the individual address of a communication partner in the KNX network.
	 * <p>
	 * This service uses broadcast communication mode.<br>
	 * The communication partner is a device in programming mode.
	 *
	 * @param newAddress new address
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void writeAddress(IndividualAddress newAddress) throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Reads the individual address of a communication partner in the KNX network.
	 * <p>
	 * This service uses broadcast communication mode.<br>
	 * The communication partner is a device in programming mode. In situations necessary
	 * to know whether more than one device is in programming mode,
	 * <code>oneAddressOnly</code> is set to <code>false</code> and the device addresses
	 * are listed in the returned address array. In this case, the whole response timeout
	 * is waited for read responses. If <code>oneAddressOnly</code> is <code>true</code>,
	 * the array size of returned addresses is 1, and the method returns after receiving
	 * the first read response.
	 *
	 * @param oneAddressOnly <code>true</code> if method should return after receiving the
	 *        first read response, <code>false</code> to wait the whole response timeout
	 *        for read responses
	 * @return array of individual addresses, in the order of reception
	 * @throws KNXTimeoutException on a timeout during send or no address response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read address errors
	 * @throws InterruptedException on interrupted thread
	 */
	IndividualAddress[] readAddress(boolean oneAddressOnly) throws KNXException, InterruptedException;

	/**
	 * Modifies the individual address of a communication partner identified using an
	 * unique serial number in the KNX network.
	 * <p>
	 * This service uses broadcast communication mode.<br>
	 *
	 * @param serialNo byte array with serial number, <code>serialNo.length</code> = 6
	 * @param newAddress new address
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void writeAddress(byte[] serialNo, IndividualAddress newAddress)
		throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Reads the individual address of a communication partner identified using an unique
	 * serial number in the KNX network.
	 * <p>
	 * This service uses broadcast communication mode.<br>
	 *
	 * @param serialNo byte array with serial number, <code>serialNo.length</code> = 6
	 * @return the individual address
	 * @throws KNXTimeoutException on a timeout during send or no address response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read address errors
	 * @throws InterruptedException on interrupted thread
	 */
	IndividualAddress readAddress(byte[] serialNo) throws KNXException,
		InterruptedException;

	/**
	 * Modifies the domain address of a communication partner in the KNX network.
	 * <p>
	 * This service uses system broadcast communication mode.<br>
	 * The communication partner is a device in programming mode.
	 *
	 * @param domain byte array with domain address, <code>domain.length</code> = 2 (on
	 *        powerline medium) or <code>domain.length</code> = 6 (on RF medium)
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void writeDomainAddress(byte[] domain) throws KNXTimeoutException,
		KNXLinkClosedException;

	/**
	 * Reads the domain address of a communication partner in the KNX network.
	 * <p>
	 * This service uses system broadcast communication mode.<br>
	 * The communication partner is a device in programming mode. In situations necessary
	 * to read domain addresses from more than one device in programming mode,
	 * <code>oneAddressOnly</code> is set to <code>false</code> and all received domain
	 * addresses are returned in the list. In this case, the whole response timeout is
	 * waited for address responses. If <code>oneAddressOnly</code> is <code>true</code>,
	 * the method returns after receiving the first read response, and the list contains
	 * one domain address.
	 *
	 * @param oneAddressOnly <code>true</code> if method should return after receiving the
	 *        first read response, <code>false</code> to wait the whole response timeout
	 *        for read responses
	 * @return list of byte arrays with domain addresses, ordered according to time of
	 *         reception
	 * @throws KNXTimeoutException on a timeout during send or no address response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read domain address errors
	 * @throws InterruptedException on interrupted thread
	 */
	List<byte[]> readDomainAddress(boolean oneAddressOnly) throws KNXException, InterruptedException;

	/**
	 * Reads the domain address of a communication partner in the KNX network, providing both the individual address of
	 * the device as well as its domain address for all devices from which a response is received.
	 * <p>
	 * This service is designed for open media and uses system broadcast communication mode.<br>
	 * The communication partner is one or more devices in programming mode.
	 *
	 * @param domain consumer called for every response received for this request, with the first argument being the
	 *        device address, the second argument its domain address
	 * @throws KNXTimeoutException on any timeout during sending the request
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException on closed KNX network link
	 * @throws InterruptedException on interrupted thread
	 */
	void readDomainAddress(BiConsumer<IndividualAddress, byte[]> domain)
		throws KNXLinkClosedException, KNXInvalidResponseException, KNXTimeoutException, InterruptedException;

	/**
	 * Reads the domain address of a communication partner identified using an address
	 * range.
	 * <p>
	 * This method is used to check existence of a device with the specified domain on a
	 * power-line medium and paying attention to more installations.<br>
	 * This service uses system broadcast communication mode.<br>
	 * <p>
	 * A note on answering behavior when the specified <code>range</code> is &lt; 255:<br>
	 * If an answering device 'A' receives a domain address response from another
	 * answering device 'B', 'A' will terminate the transmission of its response.
	 *
	 * @param domain byte array with domain address to check for,
	 *        <code>domain.length</code> = 2 (power-line medium only)
	 * @param startAddress start from this individual address, lower bound of checked
	 *        range
	 * @param range address range, specifies upper bound address (startAddress + range)
	 * @return list of byte arrays with domain addresses, ordered according to time of
	 *         reception
	 * @throws KNXTimeoutException on a timeout during send or no address response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read domain address errors
	 * @throws InterruptedException on interrupted thread
	 */
	List<byte[]> readDomainAddress(byte[] domain, IndividualAddress startAddress, int range)
		throws KNXException, InterruptedException;

	/**
	 * Reads the current configuration of a network parameter from a {@code remote} endpoint. In broadcast communication
	 * mode, the remote endpoint will ignore a network parameter read request on 1) reading a network parameter that is
	 * not supported by the remote endpoint in question, or 2) on a negative check with respect to the supplied
	 * parameters against the test information {@code testInfo}. In both cases, a timeout exception will occur.<br>
	 * In point-to-point communication mode, the remote endpoint will answer with a negative response if 1) the
	 * interface object type is not supported, 2) the PID is not supported, or 3) on a negative check of the
	 * investigated parameters against the test information. In both cases, <code>KNXInvalidResponseException</code> is
	 * thrown.
	 *
	 * @param remote address of remote endpoint, or <code>null</code> to use broadcast communication mode
	 * @param objectType interface object type, <code>0 &le; iot &lt; 0xffff</code>
	 * @param pid KNX property identifier, <code>0 &le; pid &lt; 0xff</code>
	 * @param testInfo test information, <code>0 &lt; testInfo.length &lt; ???</code>
	 * @return test result as byte array, <code>result.length &gt; 0</code>
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send or waiting for a response
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] readNetworkParameter(IndividualAddress remote, int objectType, int pid, byte[] testInfo)
		throws KNXException, InterruptedException;

	/**
	 * Writes a network parameter to a {@code remote} endpoint. The remote endpoint will neglect unknown parameter types
	 * without any further action.
	 *
	 * @param remote address of remote endpoint, or <code>null</code> to use broadcast communication
	 * @param objectType interface object type
	 * @param pid KNX property identifier
	 * @param value value to write
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send
	 */
	void writeNetworkParameter(final IndividualAddress remote, final int objectType, final int pid, final byte[] value)
		throws KNXLinkClosedException, KNXTimeoutException;

	/**
	 * Reads the device descriptor information of a communication partner its controller.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.
	 * <p>
	 * The returned descriptor information format for device descriptor type 0 is as
	 * follows (MSB to LSB):<br>
	 * <code>| mask type (8 bit) | firmware version (8 bit) |</code><br>
	 * with the mask type split up into<br>
	 * <code>| Medium Type (4 bit) | Firmware Type (4 bit)|</code><br>
	 * and the firmware version split up into<br>
	 * <code>| version (4 bit) | sub code (4 bit) |</code><br>
	 * <br>
	 * The returned descriptor information format for device descriptor type 2 is as
	 * follows (MSB to LSB):<br>
	 * <code>| application manufacturer (16 bit) | device type (16 bit) | version (8 bit) |<br>
	 * | Link Mgmt Service support (2 bit) | Logical Tag (LT) base value (6 bit) |<br>
	 * | CI 1 (16 bit) | CI 2 (16 bit) | CI 3 (16 bit) | CI 4 (16 bit) |</code><br>
	 * with <code>CI = channel info</code>
	 *
	 * @param dst destination to read from
	 * @param descType device descriptor type, 0 for type 0 or 2 for type 2
	 * @return byte array containing device descriptor information
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXDisconnectException on disconnect in connection oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read device descriptor errors
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] readDeviceDesc(Destination dst, int descType) throws KNXException, InterruptedException;

	/**
	 * Initiates a basic restart of the controller of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * Invoking this method may result in a termination of the transport layer connection
	 * (i.e., state transition into disconnected for the supplied destination).
	 *
	 * @param dst destination to reset
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void restart(Destination dst) throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Initiates a master reset of the controller of a communication partner.
	 * <p>
	 * A master reset clears link information in the group address table and group object
	 * association table, resets application parameters, resets the application to the default
	 * application, resets the device individual address to the (medium-dependent) default address,
	 * and subsequently performs a basic restart.<br>
	 * If the requested master reset exceeds a processing time of 5 seconds, this is indicated by
	 * the KNX device using a worst-case process time, which is returned by this method. Otherwise,
	 * the process time indication might be left to its default of 0 by the remote endpoint.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode.<br>
	 * Invoking this method may result in a termination of the transport layer connection (i.e.,
	 * state transition into disconnected for the supplied destination).
	 * <p>
	 * Available Erase Codes:
	 * <ul>
	 * <li>1: confirmed restart (basic restart with confirmation)</li>
	 * <li>2: factory reset (used together with <code>channel</code>)</li>
	 * <li>3: reset the device individual address to its default</li>
	 * <li>4: reset application program memory to default application</li>
	 * <li>5: reset application parameter memory (used together with <code>channel</code>)</li>
	 * <li>6: reset links (used together with <code>channel</code></li>
	 * <li>7: factory reset without resetting the device individual address (used together with
	 * <code>channel</code>)</li>
	 * </ul>
	 *
	 * @param dst destination to reset
	 * @param eraseCode specifies the resources that shall be reset prior to resetting the device
	 * @param channel the number of the application channel that shall be reset and the application
	 *        parameters set to default values, use 0 to clear all link information in the group
	 *        address table and group object association table and reset all application parameters
	 * @return the worst case execution time of the device for the requested master reset in seconds
	 *         or a default of 0, with time &ge; 0 (time is returned conforming to the datapoint
	 *         encoding with DPT ID 7.005 (DPT_TimePeriodSec))
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException
	 * @throws KNXDisconnectException
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws InterruptedException on interrupted thread
	 */
	int restart(final Destination dst, int eraseCode, int channel) throws KNXTimeoutException,
		KNXRemoteException, KNXDisconnectException, KNXLinkClosedException, InterruptedException;

	/**
	 * Reads the value of a property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * One value element in the returned data byte array consumes<br>
	 * <code>(data.length / elements)</code> bytes.<br>
	 * The byte offset into the returned data to access a property value element with
	 * index <code>i</code> (zero based) is calculated the following way:<br>
	 * <code>offset = (data.length / elements) * i</code>.<br>
	 * Note that interface objects with active access protection are only accessible over
	 * connection oriented communication.
	 *
	 * @param dst destination to read from
	 * @param objIndex interface object index
	 * @param propertyId property identifier
	 * @param start start index in the property value to start reading from
	 * @param elements number of elements to read
	 * @return byte array containing the property value data
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if tried to access a non existing property or forbidden
	 *         property access (not sufficient access rights)
	 * @throws KNXInvalidResponseException if received number of elements differ
	 * @throws KNXDisconnectException on disconnect in connection oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property error
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] readProperty(Destination dst, int objIndex, int propertyId, int start, int elements)
		throws KNXException, InterruptedException;

	/**
	 * Modifies the value of a property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * The value of the written property is explicitly read back after writing.<br>
	 * Note that interface objects with active access protection are only accessible over
	 * connection oriented communication.
	 *
	 * @param dst destination to write to
	 * @param objIndex interface object index
	 * @param propertyId property identifier
	 * @param start start index in the property value to start writing to
	 * @param elements number of elements to write
	 * @param data byte array containing property value data to write
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if tried to access a non existing property or forbidden
	 *         property access (not sufficient access rights) or erroneous property data
	 *         was written
	 * @throws KNXInvalidResponseException if received number of elements differ or the
	 *         data length read back differs from the written data length
	 * @throws KNXDisconnectException on disconnect in connection oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property error
	 * @throws InterruptedException on interrupted thread
	 */
	void writeProperty(Destination dst, int objIndex, int propertyId, int start, int elements,
		byte[] data) throws KNXException, InterruptedException;

	/**
	 * Reads the description of a property of an interface object of a communication
	 * partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * The property of the object is addressed either with a the <code>propertyId</code>
	 * or with the <code>propIndex</code>. The property index is only used if the property
	 * identifier is 0, otherwise the index is ignored.<br>
	 * When using the property ID for access, the property index in the returned
	 * description is either the correct property index of the addressed property or 0.
	 *
	 * @param dst destination to read from
	 * @param objIndex interface object index
	 * @param propertyId property identifier, specify 0 to use the property index
	 * @param propIndex property index, starts with index 0 for the first property
	 * @return byte array containing the property description, starting with the property
	 *         object index
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if the response contains no description (e.g. if tried
	 *         to access a non existing property)
	 * @throws KNXDisconnectException on disconnect in connection oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property description error
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] readPropertyDesc(Destination dst, int objIndex, int propertyId, int propIndex)
		throws KNXException, InterruptedException;

	/**
	 * Reads the value of the A/D converter of a communication partner.
	 * <p>
	 * This service uses point-to-point connection-oriented communication mode.<br>
	 *
	 * @param dst destination to read from
	 * @param channel channel number of the A/D converter
	 * @param repeat number of consecutive converter read operations
	 * @return the calculated A/D conversion value
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException on remote converter read problem (e.g. overflow or wrong
	 *         channel)
	 * @throws KNXDisconnectException on disconnect during read
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read A/D converter error
	 * @throws InterruptedException on interrupted thread
	 */
	int readADC(Destination dst, int channel, int repeat) throws KNXException, InterruptedException;

	/**
	 * Reads memory data from the address space of a communication partner its controller.
	 * <p>
	 * This service uses point-to-point connection-oriented communication mode.<br>
	 * Note that a remote application layer shall ignore a memory read if the amount of
	 * read memory does not fit into an APDU of maximum length.
	 *
	 * @param dst destination to read from
	 * @param startAddr 16 bit start address to read in memory
	 * @param bytes number of data bytes to read (with increasing addresses),
	 *        <code>bytes &gt; 0</code>
	 * @return byte array containing the data read from the memory
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException on problems of the partner reading (part of) the memory
	 *         (e.g. access to illegal or protected address space, invalid number of
	 *         bytes)
	 * @throws KNXDisconnectException on disconnect during read
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read memory problems
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] readMemory(Destination dst, int startAddr, int bytes) throws KNXException, InterruptedException;

	/**
	 * Writes memory data in the address space of a communication partner its controller.
	 * <p>
	 * This service uses point-to-point connection-oriented communication mode.<br>
	 * If verify mode is enabled for the destination, this method will wait for a memory
	 * write response and do an explicit read back of the written memory.<br>
	 * Note that a remote application layer shall ignore a memory write if the amount of
	 * memory does not fit into an APDU of maximum length the remote layer can handle.
	 *
	 * @param dst destination to write to
	 * @param startAddr 16 bit start address to write in memory
	 * @param data byte array containing the memory data to write
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException in verify mode on problems of the partner writing the
	 *         memory data (e.g., access to illegal or protected address space, invalid
	 *         number of bytes) or erroneous memory data was written
	 * @throws KNXInvalidResponseException in verify mode if the size of memory read back
	 *         differs from the written size of memory
	 * @throws KNXDisconnectException on disconnect during read
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write memory problems
	 * @throws InterruptedException on interrupted thread
	 */
	void writeMemory(Destination dst, int startAddr, byte[] data) throws KNXException, InterruptedException;

	/**
	 * Authorizes at a communication partner using an authorization key to obtain a
	 * certain access level.
	 * <p>
	 * This service uses point-to-point connection-oriented communication mode.<br>
	 * The returned access level is between 0 (maximum access rights) and 3 (i.e., minimum
	 * access rights) or 0 (maximum access rights) and 15 (minimum access rights).<br>
	 * If no authorization is done at all or the supplied key is not valid, the default
	 * access level used is set to minimum. A set access level is valid until disconnected
	 * from the partner or a new authorization request is done.
	 *
	 * @param dst destination at which to authorize
	 * @param key byte array containing authorization key
	 * @return the granted access level
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXInvalidResponseException if the received access level is out of the
	 *         allowed value range
	 * @throws KNXDisconnectException on disconnect during authorize
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other authorization error
	 * @throws InterruptedException on interrupted thread
	 */
	int authorize(Destination dst, byte[] key) throws KNXException, InterruptedException;

	/**
	 * Modifies or deletes an authorization key associated to an access level of a
	 * communication partner.
	 * <p>
	 * This service uses point-to-point connection-oriented communication mode.<br>
	 * If the supplied key is 0xFFFFFFFF, the key for the given access <code>level</code>
	 * is removed. The write request has to be done using equal or higher access rights
	 * than the access rights of the <code>level</code> which is to be modified (i.e.
	 * current level &lt;= level to change).
	 *
	 * @param dst destination to write to
	 * @param level access level to modify
	 * @param key new key for the access level or 0xFFFFFFFF to remove key
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if the current access level &gt; necessary access level for
	 *         writing a key
	 * @throws KNXDisconnectException on disconnect during write
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other write key error
	 * @throws InterruptedException on interrupted thread
	 */
	void writeKey(Destination dst, int level, byte[] key) throws KNXException, InterruptedException;

	/**
	 * Returns whether a network link is attached to this management client.
	 * <p>
	 *
	 * @return <code>true</code> if link attached, <code>false</code> if detached
	 */
	boolean isOpen();

	/**
	 * Detaches the network link from this management client.
	 * <p>
	 * A detach will also detach an internally used transport layer with all its
	 * consequences. If no network link is attached, no action is performed.
	 * <p>
	 * Note that a detach does not trigger a close of the used network link.
	 *
	 * @return the formerly attached KNX network link, or <code>null</code> if already
	 *         detached
	 * @see TransportLayer#detach()
	 */
	KNXNetworkLink detach();

	@Override
	default void close() { detach(); }
}
