/*
    Calimero 2 - A library for KNX network access
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

package tuwien.auto.calimero.mgmt;

import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.SerialNumber;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;

/**
 * Application layer services providing management related tasks in a KNX network for a client.
 *
 * @author B. Malinowsky
 */
public interface ManagementClient extends AutoCloseable
{
	/**
	 * @deprecated Use {@link #responseTimeout(Duration)}
	 *
	 * @param timeout time in seconds, {@code timeout > 0}
	 */
	@Deprecated(forRemoval = true)
	default void setResponseTimeout(final int timeout) { responseTimeout(Duration.ofSeconds(timeout)); }

	/**
	 * @deprecated Use {@link #responseTimeout()}
	 *
	 * @return timeout in seconds
	 */
	@Deprecated(forRemoval = true)
	default int getResponseTimeout() { return (int) responseTimeout().toSeconds(); }

	/**
	 * Returns the response timeout used when waiting for a KNX response message to arrive.
	 *
	 * @return response timeout
	 */
	Duration responseTimeout();

	/**
	 * Sets the response timeout to wait for a KNX response message to arrive to complete a message exchange.
	 *
	 * @param timeout {@code timeout > 0}
	 */
	void responseTimeout(Duration timeout);

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
	 * @param connectionOriented {@code true} for connection-oriented mode,
	 *        {@code false} for connectionless mode
	 * @return destination representing the logical connection
	 */
	default Destination createDestination(final IndividualAddress remote, final boolean connectionOriented) {
		return createDestination(remote, connectionOriented, false, false);
	}

	/**
	 * Creates a new destination using the remote KNX address and connection settings for
	 * management communication.
	 * <p>
	 * A management client will use the transport layer for creating the destination.
	 *
	 * @param remote destination KNX individual address
	 * @param connectionOriented {@code true} for connection-oriented mode,
	 *        {@code false} for connectionless mode
	 * @param keepAlive {@code true} to prevent a timing out of the logical
	 *        connection in connection-oriented mode, {@code false} to use default
	 *        connection timeout
	 * @param verifyMode {@code true} to indicate the destination has verify mode
	 *        enabled, {@code false} otherwise
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
	 * {@code oneAddressOnly} is set to {@code false} and the device addresses
	 * are listed in the returned address array. In this case, the whole response timeout
	 * is waited for read responses. If {@code oneAddressOnly} is {@code true},
	 * the array size of returned addresses is 1, and the method returns after receiving
	 * the first read response.
	 *
	 * @param oneAddressOnly {@code true} if method should return after receiving the
	 *        first read response, {@code false} to wait the whole response timeout
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

	void writeAddress(SerialNumber serialNo, IndividualAddress newAddress) throws KNXTimeoutException,
			KNXLinkClosedException;

	/**
	 * Modifies the individual address of a communication partner identified using a
	 * unique serial number in the KNX network.
	 * <p>
	 * This service uses broadcast communication mode.<br>
	 *
	 * @param serialNo byte array with serial number, {@code serialNo.length} = 6
	 * @param newAddress new address
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	default void writeAddress(final byte[] serialNo, final IndividualAddress newAddress) throws KNXTimeoutException,
			KNXLinkClosedException {
		writeAddress(SerialNumber.from(serialNo), newAddress);
	}

	IndividualAddress readAddress(SerialNumber serialNumber) throws KNXException, InterruptedException;

	/**
	 * Reads the individual address of a communication partner identified using a unique
	 * serial number in the KNX network.
	 * <p>
	 * This service uses broadcast communication mode.<br>
	 *
	 * @param serialNo byte array with serial number, {@code serialNo.length} = 6
	 * @return the individual address
	 * @throws KNXTimeoutException on a timeout during send or no address response was
	 *         received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read address errors
	 * @throws InterruptedException on interrupted thread
	 */
	default IndividualAddress readAddress(final byte[] serialNo) throws KNXException, InterruptedException {
		return readAddress(SerialNumber.from(serialNo));
	}

	/**
	 * Modifies the domain address of a communication partner in the KNX network which is in programming mode.
	 * <p>
	 * This service uses system broadcast communication mode.
	 *
	 * @param domain byte array with domain address, {@code domain.length} = 2 (on
	 *        powerline medium) or {@code domain.length} = 6 (on RF medium)
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void writeDomainAddress(byte[] domain) throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Modifies the domain address of the communication partner with the specified serial number in the KNX network.
	 * This service uses system broadcast communication mode.
	 *
	 * @param serialNumber device serial number
	 * @param domain byte array with domain address, {@code domain.length} = 2 on
	 *        powerline medium, {@code domain.length} = 6 on RF medium, {@code domain.length = 4 or 21} on IP medium
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 */
	void writeDomainAddress(SerialNumber serialNumber, byte[] domain) throws KNXTimeoutException, KNXLinkClosedException;

	/**
	 * Reads the domain address of a communication partner in the KNX network.
	 * <p>
	 * This service uses system broadcast communication mode.<br>
	 * The communication partner is a device in programming mode. In situations necessary
	 * to read domain addresses from more than one device in programming mode,
	 * {@code oneAddressOnly} is set to {@code false} and all received domain
	 * addresses are returned in the list. In this case, the whole response timeout is
	 * waited for address responses. If {@code oneAddressOnly} is {@code true},
	 * the method returns after receiving the first read response, and the list contains
	 * one domain address.
	 *
	 * @param oneAddressOnly {@code true} if method should return after receiving the
	 *        first read response, {@code false} to wait the whole response timeout
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
	 * the device and its domain address for all devices from which a response is received.
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
	 * A note on answering behavior when the specified {@code range} is &lt; 255:<br>
	 * If an answering device 'A' receives a domain address response from another
	 * answering device 'B', 'A' will terminate the transmission of its response.
	 *
	 * @param domain byte array with domain address to check for,
	 *        {@code domain.length} = 2 (power-line medium only)
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
	 * Reads the current configuration of a network parameter in point-to-point communication mode from a {@code remote}
	 * endpoint.
	 * The remote endpoint will answer with a negative response if 1) the
	 * interface object type is not supported, 2) the PID is not supported, or 3) on a negative check of the
	 * investigated parameters against the test information.
	 *
	 * @param remote address of remote endpoint
	 * @param objectType interface object type, {@code 0 ≤ objectType < 0xffff}
	 * @param pid KNX property identifier, {@code 0 ≤ pid < 0xff}
	 * @param testInfo test information, {@code 0 < testInfo.length <} parameter-specific
	 * @return received responses as (empty) list of byte arrays, {@code byte array length > 0}
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send or waiting for a response
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws InterruptedException on interrupted thread
	 */
	List<byte[]> readNetworkParameter(IndividualAddress remote, int objectType, int pid, byte... testInfo)
		throws KNXException, InterruptedException;

	/**
	 * Test result of reading the configuration of a network parameter using parameter-specific test information.
	 *
	 * @param remote address of the remote endpoint which answered the read request
	 * @param result test result, {@code length > 0}
	 */
	record TestResult(IndividualAddress remote, byte[] result) {}

	/**
	 * Reads the current configuration of a network parameter using broadcast communication mode.
	 * The remote endpoint will ignore a network parameter read request if 1) reading a network parameter that is
	 * not supported by the remote endpoint in question, or 2) on a negative check with respect to the supplied
	 * parameters against the test information {@code testInfo}.
	 *
	 * @param objectType interface object type, {@code 0 ≤ objectType < 0xffff}
	 * @param pid KNX property identifier, {@code 0 ≤ pid < 0xff}
	 * @param testInfo test information, {@code 0 < testInfo.length <} parameter-specific
	 * @return received responses with test results as (empty) list
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send or waiting for a response
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws InterruptedException on interrupted thread
	 */
	List<TestResult> readNetworkParameter(int objectType, int pid, byte... testInfo)
			throws KNXException, InterruptedException;

	/**
	 * Writes a network parameter to a {@code remote} endpoint. The remote endpoint will neglect unknown parameter types
	 * without any further action.
	 *
	 * @param remote address of remote endpoint, or {@code null} to use broadcast communication
	 * @param objectType interface object type, {@code 0 ≤ objectType < 0xffff}
	 * @param pid KNX property identifier, {@code 0 ≤ pid < 0xff}
	 * @param value value to write
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send
	 */
	void writeNetworkParameter(IndividualAddress remote, int objectType, int pid, byte... value)
		throws KNXLinkClosedException, KNXTimeoutException;

	/**
	 * Reads the current configuration of a network parameter.
	 * <p>
	 * On open communication medium, a system broadcast (point to all-point) is used; on closed communication medium, a
	 * broadcast (point to domain) is used.
	 *
	 * @param objectType interface object type, {@code 0 ≤ objectType < 0xffff}
	 * @param pid KNX property identifier, {@code 0 ≤ pid < 0xfff}
	 * @param operand operand, being the first byte of the test information
	 * @param additionalTestInfo any additional test information after the operand
	 * @return list of test results as byte array, with each {@code result.length > 0}
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send or waiting for a response
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws InterruptedException on interrupted thread
	 */
	List<byte[]> readSystemNetworkParameter(int objectType, int pid, int operand, byte... additionalTestInfo)
		throws KNXException, InterruptedException;

	/**
	 * Writes network configuration information in one or multiple management servers.
	 * <p>
	 * On open communication medium, a system broadcast (point to all-point) is used; on closed communication medium, a
	 * broadcast (point to domain) is used. A remote endpoint will neglect write requests with unknown parameter types
	 * without any further action.
	 *
	 * @param objectType interface object type, {@code 0 ≤ objectType < 0xffff}
	 * @param pid KNX property identifier, {@code 0 ≤ pid < 0xfff}
	 * @param value value to write
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXTimeoutException on timeout during send
	 */
	void writeSystemNetworkParameter(int objectType, int pid, byte... value)
			throws KNXLinkClosedException, KNXTimeoutException;

	/**
	 * Reads the device descriptor information of a communication partner its controller.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.
	 * <p>
	 * The returned descriptor information format for device descriptor type 0 is as
	 * follows (MSB to LSB):<br>
	 * {@code | mask type (8 bit) | firmware version (8 bit) |}<br>
	 * with the mask type split up into<br>
	 * {@code | Medium Type (4 bit) | Firmware Type (4 bit)|}<br>
	 * and the firmware version split up into<br>
	 * {@code | version (4 bit) | sub code (4 bit) |}<br>
	 * <br>
	 * The returned descriptor information format for device descriptor type 2 is as
	 * follows (MSB to LSB):<br>
	 * <code>| application manufacturer (16 bit) | device type (16 bit) | version (8 bit) |<br>
	 * | Link Mgmt Service support (2 bit) | Logical Tag (LT) base value (6 bit) |<br>
	 * | CI 1 (16 bit) | CI 2 (16 bit) | CI 3 (16 bit) | CI 4 (16 bit) |</code><br>
	 * with {@code CI = channel info}
	 *
	 * @param dst destination to read from
	 * @param descType device descriptor type, 0 for type 0 or 2 for type 2
	 * @return byte array containing device descriptor information
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
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
	 * @throws InterruptedException on interrupted thread
	 */
	void restart(Destination dst) throws KNXTimeoutException, KNXLinkClosedException, InterruptedException;

	/**
	 * Erase codes used with a master reset restart service.
	 */
	enum EraseCode {
		/** Confirmed alternative to the unconfirmed basic restart. */
		ConfirmedRestart,
		/** Reset the device to its ex-factory state. */
		FactoryReset,
		/** Reset the device address to the medium-specific default address. */
		ResetIndividualAddress,
		/** Reset the application program memory to the default application. */
		ResetApplicationProgram,
		/** Reset the application parameter memory to its default value. */
		ResetApplicationParameters,
		/**
		 * Reset link information for group objects (Group Address Table, Group Object Association Table) to its
		 * default state.
		 */
		ResetLinks,
		/** Reset the device to its ex-factory state, the device address(es) shall not be reset. */
		FactoryResetWithoutIndividualAddress,
		/**
		 * Persistently stored application data shall become invalid.
		 * Depending on the channel number, reset behaves as follows:
		 * <ul>
		 *     <li>channel = 0: persistently stored application data of all channels shall be reset</li>
		 *     <li>channel ≠ 0: persistently stored application data of only this channel shall be reset</li>
		 * </ul>
		 */
		ErasePersistentlyStoredApplicationData;

		public static EraseCode of(final int eraseCode) {
			if (eraseCode > 0 && eraseCode <= values().length)
				return values()[eraseCode - 1];
			throw new KNXIllegalArgumentException("unsupported erase code " + eraseCode);
		}

		public int code() { return ordinal() + 1; }
	}

	/**
	 * Initiates a master reset of the controller of a communication partner.
	 * <p>
	 * Depending on the erase code, a master reset clears link information in the group address table and group object
	 * association table, resets application parameters, resets the application to the default application, resets the
	 * device individual address to the (medium-dependent) default address, and subsequently performs a basic
	 * restart.<br>
	 * The {@code channel} parameter is used with erase codes {@link EraseCode#FactoryReset},
	 * {@link EraseCode#ResetApplicationParameters}, {@link EraseCode#ResetLinks},
	 * {@link EraseCode#FactoryResetWithoutIndividualAddress}, and {@link EraseCode#ErasePersistentlyStoredApplicationData}.
	 * For erase codes {@link EraseCode#ConfirmedRestart},
	 * {@link EraseCode#ResetIndividualAddress}, and {@link EraseCode#ResetApplicationProgram}, {@code channel} should
	 * be 0.<br>
	 * If the requested master reset exceeds a processing time of 5 seconds, this is indicated by the KNX device using a
	 * worst-case process time, which is returned by this method. Otherwise, the process time indication might be left
	 * at its default value of 0 by the remote endpoint.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode.<br>
	 * Invoking this method may result in a termination of the transport layer connection (i.e., transition to
	 * disconnected state for the supplied destination).
	 *
	 * @param dst destination to reset
	 * @param eraseCode specifies the resources that shall be reset prior to restarting the device
	 * @param channel the number of the application channel that shall be reset and the application parameters set to
	 *        default values, use 0 to clear all link information in the group address table and group object
	 *        association table and reset all application parameters
	 * @return the worst case execution time of the device for the requested master reset or a default of 0, with time
	 *         &ge; 0
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException on error to restart the communication partner
	 * @throws KNXDisconnectException on transport layer disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws InterruptedException on interrupted thread
	 */
	Duration restart(Destination dst, EraseCode eraseCode, int channel) throws KNXTimeoutException,
			KNXRemoteException, KNXDisconnectException, KNXLinkClosedException, InterruptedException;

	/**
	 * Initiates a master reset of the controller of a communication partner.
	 * <p>
	 * Available erase codes:
	 * <ul>
	 * <li>1: confirmed restart (basic restart with confirmation)</li>
	 * <li>2: factory reset (used together with {@code channel})</li>
	 * <li>3: reset the device individual address to its default</li>
	 * <li>4: reset application program memory to default application</li>
	 * <li>5: reset application parameter memory (used together with {@code channel})</li>
	 * <li>6: reset links (used together with {@code channel}</li>
	 * <li>7: factory reset without resetting the device individual address (used together with {@code channel})</li>
	 * </ul>
	 *
	 * @param dst destination to reset
	 * @param eraseCode specifies the resources that shall be reset prior to restarting the device
	 * @param channel the number of the application channel that shall be reset and the application
	 *        parameters set to default values, use 0 to clear all link information in the group
	 *        address table and group object association table and reset all application parameters
	 * @return the worst case execution time of the device for the requested master reset in seconds
	 *         or a default of 0, with time &ge; 0 (time is returned conforming to the datapoint
	 *         encoding with DPT ID 7.005 (DPT_TimePeriodSec))
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException on error to restart the communication partner
	 * @throws KNXDisconnectException on transport layer disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws InterruptedException on interrupted thread
	 * @see #restart(Destination, EraseCode, int)
	 */
	default int restart(final Destination dst, final int eraseCode, final int channel) throws KNXTimeoutException,
			KNXRemoteException, KNXDisconnectException, KNXLinkClosedException, InterruptedException {
		return (int) restart(dst, EraseCode.of(eraseCode), channel).toSeconds();
	}

	/**
	 * Reads the value of a property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * One value element in the returned data byte array consumes<br>
	 * {@code (data.length / elements)} bytes.<br>
	 * The byte offset into the returned data to access a property value element with
	 * index {@code i} (zero based) is calculated the following way:<br>
	 * {@code offset = (data.length / elements) * i}.<br>
	 * Note that interface objects with active access protection are only accessible over
	 * connection-oriented communication.
	 *
	 * @param dst destination to read from
	 * @param objIndex interface object index
	 * @param propertyId property identifier
	 * @param start start index in the property value to start reading from
	 * @param elements number of elements to read
	 * @return byte array containing the property value data
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if tried to access a non-existing property or forbidden
	 *         property access (not sufficient access rights)
	 * @throws KNXInvalidResponseException if received number of elements differ
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
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
	 * The value of the written property is explicitly read back after writing.
	 * Reading back the property value shall be skipped for properties returning a resulting
	 * state code which may differ from the written value (e.g., properties of type {@code PDT_CONTROL}).<br>
	 * Note that interface objects with active access protection are only accessible over
	 * connection-oriented communication.
	 *
	 * @param dst destination to write to
	 * @param objIndex interface object index
	 * @param propertyId property identifier
	 * @param start start index in the property value to start writing to
	 * @param elements number of elements to write
	 * @param data byte array containing property value data to write
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if tried to access a non-existing property or forbidden
	 *         property access (not sufficient access rights) or erroneous property data
	 *         was written
	 * @throws KNXInvalidResponseException if received number of elements differ or the
	 *         data length read back differs from the written data length
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property error
	 * @throws InterruptedException on interrupted thread
	 */
	void writeProperty(Destination dst, int objIndex, int propertyId, int start, int elements,
		byte[] data) throws KNXException, InterruptedException;

	/**
	 * Modifies the value of a property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode. Interface objects with
	 * active access protection are only accessible over connection-oriented communication.
	 *
	 * @param dst destination to write to
	 * @param objectType interface object type
	 * @param objectInstance interface object instance
	 * @param propertyId property identifier
	 * @param start start index in the property value to start writing to
	 * @param elements number of elements to write
	 * @param data byte array containing property value data to write
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if tried to access a non-existing property or forbidden property access (not
	 *         sufficient access rights) or erroneous property data was written
	 * @throws KNXInvalidResponseException if received number of elements differ or the data length read back differs
	 *         from the written data length
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property error
	 * @throws InterruptedException on interrupted thread
	 */
	void writeProperty(Destination dst, int objectType, int objectInstance, int propertyId, int start, int elements,
			byte[] data) throws KNXException, InterruptedException;

	/**
	 * Reads the description of a property of an interface object of a communication partner. The interface object
	 * is specified using its object type and object instance.
	 * <p>
	 * This service corresponds to A_PropertyExtDescription_Read and uses point-to-point connectionless or
	 * connection-oriented communication mode.<br>
	 * The property of the object is addressed either with the {@code propertyId}
	 * or with the {@code propertyIndex}. The property index is only used if the property
	 * identifier is 0, otherwise the index is ignored.
	 * When using the property ID for access, the property index in the returned
	 * description is either the correct property index of the addressed property or 0.
	 *
	 * @param dst destination to read from
	 * @param objectType interface object type
	 * @param objInstance interface object instance
	 * @param propertyId property identifier, specify 0 to use the property index
	 * @param propertyIndex property index, starts with index 0 for the first property
	 * @return the property description
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if the response contains no description (e.g. if tried
	 *         to access a non-existing property)
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws InterruptedException on interrupted thread
	 */
	Description readPropertyDescription(Destination dst, int objectType, int objInstance, int propertyId,
			int propertyIndex) throws KNXTimeoutException, KNXRemoteException, KNXDisconnectException,
			KNXLinkClosedException, InterruptedException;

	/**
	 * Reads the description of a property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * The property of the object is addressed either with a the {@code propertyId}
	 * or with the {@code propIndex}. The property index is only used if the property
	 * identifier is 0, otherwise the index is ignored. When using the property ID for access, the property index in
	 * the returned description is either the correct property index of the addressed property or 0.
	 *
	 * @param dst destination to read from
	 * @param objIndex interface object index
	 * @param propertyId property identifier, specify 0 to use the property index
	 * @param propertyIndex property index, starts with index 0 for the first property
	 * @return the property description
	 * @throws KNXTimeoutException on a timeout during send
	 * @throws KNXRemoteException if the response contains no description (e.g. if tried
	 *         to access a non-existing property)
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property description error
	 * @throws InterruptedException on interrupted thread
	 */
	default Description readPropertyDescription(final Destination dst, final int objIndex, final int propertyId,
			final int propertyIndex) throws KNXException, InterruptedException {
		return Description.from(0, readPropertyDesc(dst, objIndex, propertyId, propertyIndex));
	}

	/**
	 * Reads the description of a property of an interface object of a communication
	 * partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented
	 * communication mode.<br>
	 * The property of the object is addressed either with a the {@code propertyId}
	 * or with the {@code propIndex}. The property index is only used if the property
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
	 *         to access a non-existing property)
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read property description error
	 * @throws InterruptedException on interrupted thread
	 * @see #readPropertyDescription(Destination, int, int, int)
	 */
	byte[] readPropertyDesc(Destination dst, int objIndex, int propertyId, int propIndex)
		throws KNXException, InterruptedException;

	/**
	 * Calls a function property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode. Note that interface
	 * objects with active access protection are only accessible over connection-oriented communication.
	 *
	 * @param dst destination for which to call function property
	 * @param objIndex interface object index
	 * @param propertyId property identifier
	 * @param data input data to the function
	 * @return function property response containing the positive return code and function result
	 * @throws KNXTimeoutException on send or service timeout
	 * @throws KNXRemoteException if accessing a non-existing property or forbidden property access (not
	 *         sufficient access rights)
	 * @throws KNXInvalidResponseException if received response has invalid length
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read error
	 * @throws InterruptedException on interrupted thread
	 */
	FuncPropResponse callFunctionProperty(Destination dst, int objIndex, int propertyId, byte... data)
			throws KNXException, InterruptedException;

	/**
	 * Reads the state of a function property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode. Note that interface
	 * objects with active access protection are only accessible over connection-oriented communication.
	 *
	 * @param dst destination to read from
	 * @param objIndex interface object index
	 * @param propertyId property identifier
	 * @param data input data to the function for reading
	 * @return function property response containing the return code and function property state
	 * @throws KNXTimeoutException on send or service timeout
	 * @throws KNXRemoteException if accessing a non-existing property or forbidden property access (not
	 *         sufficient access rights)
	 * @throws KNXInvalidResponseException if received response has invalid length
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read error
	 * @throws InterruptedException on interrupted thread
	 */
	FuncPropResponse readFunctionPropertyState(Destination dst, int objIndex, int propertyId, byte... data)
			throws KNXException, InterruptedException;

	/**
	 * Calls a function property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode. Note that interface
	 * objects with active access protection are only accessible over connection-oriented communication.
	 *
	 * @param dst destination for which to call function property
	 * @param objectType interface object type
	 * @param objInstance object instance, {@code objInstance > 0}
	 * @param propertyId property identifier
	 * @param serviceId service identifier for call service
	 * @param serviceInfo service info
	 * @return function property response containing the positive return code and function result
	 * @throws KNXTimeoutException on send or service timeout
	 * @throws KNXRemoteException if accessing a non-existing property or forbidden property access (not
	 *         sufficient access rights)
	 * @throws KNXInvalidResponseException if received response has invalid length
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read error
	 * @throws InterruptedException on interrupted thread
	 */
	FuncPropResponse callFunctionProperty(Destination dst, int objectType, int objInstance, int propertyId, int serviceId,
			byte... serviceInfo) throws KNXException, InterruptedException;

	/**
	 * Reads the state of a function property of an interface object of a communication partner.
	 * <p>
	 * This service uses point-to-point connectionless or connection-oriented communication mode. Note that interface
	 * objects with active access protection are only accessible over connection-oriented communication.
	 *
	 * @param dst destination to read from
	 * @param objectType interface object type
	 * @param objInstance object instance, {@code objInstance > 0}
	 * @param propertyId property identifier
	 * @param serviceId service identifier for read service
	 * @param serviceInfo service info
	 * @return function property response containing the return code and function property state
	 * @throws KNXTimeoutException on send or service timeout
	 * @throws KNXRemoteException if accessing a non-existing property or forbidden property access (not
	 *         sufficient access rights)
	 * @throws KNXInvalidResponseException if received response has invalid length
	 * @throws KNXDisconnectException on disconnect in connection-oriented mode
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read error
	 * @throws InterruptedException on interrupted thread
	 */
	FuncPropResponse readFunctionPropertyState(Destination dst, int objectType, int objInstance, int propertyId, int serviceId,
			byte... serviceInfo) throws KNXException, InterruptedException;

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
	 * This service uses point-to-point connectionless or point-to-point connection-oriented communication mode.<br>
	 * Note that a remote application layer shall ignore a memory read if the amount of
	 * read memory does not fit into an APDU of maximum length.
	 *
	 * @param dst destination to read from
	 * @param startAddr 16 bit start address to read in memory
	 * @param bytes number of data bytes to read (with increasing addresses),
	 *        {@code bytes > 0}
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
	 * This service uses point-to-point connectionless or point-to-point connection-oriented communication mode.<br>
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
	 * This service corresponds to A_Authorize and uses point-to-point connection-oriented communication mode.<br>
	 * The returned access level is between 0 (maximum access rights) and 3 (i.e., minimum
	 * access rights) or 0 (maximum access rights) and 15 (minimum access rights).<br>
	 * If no authorization is done at all or the supplied key is not valid, the default
	 * access level used is set to minimum. A set access level is valid until disconnected
	 * from the partner or a new authorization request is done.
	 *
	 * @param dst destination at which to authorize
	 * @param key byte array containing authorization key, {@code key.length = 4}
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
	 * If the supplied key is 0xFFFFFFFF, the key for the given access {@code level}
	 * is removed. The write request has to be done using equal or higher access rights
	 * than the access rights of the {@code level} which is to be modified (i.e.
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
	 *
	 * @return {@code true} if link attached, {@code false} if detached
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
	 * @return the formerly attached KNX network link, or {@code null} if already
	 *         detached
	 * @see TransportLayer#detach()
	 */
	KNXNetworkLink detach();

	@Override
	default void close() { detach(); }
}
