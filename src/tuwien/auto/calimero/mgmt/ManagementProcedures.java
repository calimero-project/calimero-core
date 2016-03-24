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
import java.util.function.Consumer;

import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXInvalidResponseException;
import tuwien.auto.calimero.KNXRemoteException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;

/**
 * KNX management procedures.
 * <p>
 * Offers services following the KNX management procedures, for KNX network management and
 * KNX device management.<br>
 * For procedures performed in connection-oriented mode, each management procedure takes
 * care of satisfying the required connection modes.<br>
 * If an object implementing this interface is not used any longer, {@link #detach()} has
 * to be called to release its resources and detach from the KNX network link.
 *
 * @author B. Malinowsky
 */
public interface ManagementProcedures extends AutoCloseable
{
	// addressing procedures

	/**
	 * Reads out the individual addresses of all KNX devices in programming mode.
	 * <p>
	 * This method corresponds to the KNX <i>NM_IndividualAddress_Read</i> procedure.<br>
	 * Depending on whether none, one, or several devices are in programming mode, the
	 * returned array length of addresses is 0, 1, or &gt; 1, respectively. If more than one
	 * device with the same individual address is in programming mode, the returned array
	 * will contain the same addresses several times.<br>
	 * The read timeout is 3 seconds.
	 *
	 * @return a new array with {@link IndividualAddress}es of devices in programming
	 *         mode, with the array length equal to the number of device responses
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXRemoteException on invalid response behavior from a remote endpoint
	 * @throws KNXException and subtypes on other errors
	 * @throws InterruptedException on interrupted thread
	 */
	IndividualAddress[] readAddress() throws KNXException, InterruptedException;

	/**
	 * Writes the individual address of a single KNX device set to programming mode.
	 * <p>
	 * This method corresponds to the KNX <i>NM_IndividualAddress_Write</i> procedure.<br>
	 * This procedure verifies that no other devices share the same individual address as
	 * supplied by <code>newAddress</code>, and waits until exactly one device is in
	 * programming mode. It checks for successful address programming and requests a
	 * restart of the remote endpoint (thereby switching off its programming).
	 *
	 * @param newAddress the new address for the device in programming mode
	 * @return <code>true</code> if address was written successfully, <code>false</code>
	 *         if device with address exists but was not set to programming mode
	 * @throws KNXException on error attempting to write the new individual device address
	 * @throws InterruptedException on interrupted thread
	 */
	boolean writeAddress(IndividualAddress newAddress) throws KNXException, InterruptedException;

	/**
	 * Sets the individual address of all devices which are in programming mode to the
	 * default individual address.
	 * <p>
	 * This method corresponds to the KNX <i>NM_IndividualAddress_Reset</i> procedure.<br>
	 * The default individual address is 0xffff. After setting the default address,
	 * devices in programming mode are restarted.
	 *
	 * @throws KNXException
	 * @throws InterruptedException on interrupted thread
	 */
	void resetAddress() throws KNXException, InterruptedException;

	/**
	 * Determines whether the supplied <code>devAddr</code> is occupied by a device in the
	 * KNX network or not.
	 * <p>
	 * This method corresponds to the KNX <i>NM_IndividualAddress_Check</i> procedure.<br>
	 *
	 * @param devAddr the individual address to check
	 * @return <code>true</code> if address is occupied, <code>false</code> if not
	 *         occupied
	 * @throws KNXException on network or send errors
	 * @throws InterruptedException on interrupted thread
	 */
	boolean isAddressOccupied(IndividualAddress devAddr) throws KNXException, InterruptedException;

	/**
	 * Reads the individual address of a KNX device with known serial number.
	 * <p>
	 * This method corresponds to the KNX <i>NM_IndividualAddress_SerialNumber_Read</i> procedure.
	 * <br>
	 *
	 * @param serialNo byte array with serial number, <code>serialNo.length</code> = 6
	 * @return the individual address
	 * @throws KNXTimeoutException on a timeout during send or no address response was received
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException if network link to KNX network is closed
	 * @throws KNXException on other read address errors
	 * @throws InterruptedException on interrupted thread
	 */
	IndividualAddress readAddress(byte[] serialNo) throws KNXException, InterruptedException;

	/**
	 * Writes the individual address of a single KNX device with known serial number.
	 * <p>
	 * This method corresponds to the KNX <i>NM_IndividualAddress_SerialNumber_Write</i> procedure.<br>
	 * Before writing the address, the procedure verifies the assigned individual address is unique,
	 * and checks for successful address programming.<br>
	 * Note that this procedure, in contrast to {@link #writeAddress(IndividualAddress)}, does not
	 * restart the programmed device.
	 *
	 * @param serialNo the device serial number to be programmed, <code>serialNo.length = 6</code>
	 * @param newAddress the new address for the device identified by <code>serialNo</code>
	 * @return <code>true</code> if the new address is set and was verified successfully,
	 *         <code>false</code> if the device reports back a differing (e.g., old) address on
	 *         verification
	 * @throws KNXException on any errors attempting to write or verify the written individual
	 *         device address
	 * @throws InterruptedException on interrupted thread
	 */
	boolean writeAddress(byte[] serialNo, IndividualAddress newAddress) throws KNXException, InterruptedException;

	/**
	 * Reads the domain address of a communication partner in the KNX network, providing both the individual address of
	 * the device as well as its domain address for all devices from which a response is received.
	 * <p>
	 * This service is designed for open media and uses system broadcast communication mode.<br>
	 * The communication partner is one or more devices in programming mode. The total response timeout granted to any
	 * device responding is 3 seconds.
	 *
	 * @param device consumer called for every responding device, with the first argument being the device address, the
	 *        second argument its domain address
	 * @throws KNXTimeoutException on any timeout during sending the request
	 * @throws KNXInvalidResponseException on invalid read response message
	 * @throws KNXLinkClosedException on closed KNX network link
	 * @throws KNXException on other errors during read domain address
	 * @throws InterruptedException on interrupted thread
	 */
	void readDomainAddress(BiConsumer<IndividualAddress, byte[]> device) throws KNXException, InterruptedException;

	// scanning procedures

	/**
	 * Determines the installed KNX routers in a KNX network.
	 * <p>
	 * This method corresponds to the KNX network management router scan procedure
	 * <i>NM_Router_Scan</i>.<br>
	 * A list of device addresses of the responding routers is returned, representing the installed
	 * KNX routers.<br>
	 * For routers to be found by this procedure, the individual address and domain address have to
	 * be configured.
	 *
	 * @return a new array with {@link IndividualAddress}es of the installed KNX routers, with the
	 *         array length equal to the number of installed routers
	 * @throws KNXTimeoutException on communication timeouts during the scanning
	 * @throws KNXLinkClosedException on a closed KNXNetworkLink to the KNX network
	 * @throws InterruptedException if this thread was interrupted during scanning the network
	 *         routers
	 */
	IndividualAddress[] scanNetworkRouters() throws KNXTimeoutException, KNXLinkClosedException,
			InterruptedException;

	/**
	 * Determines the existing KNX network devices on a specific KNX subnetwork.
	 * <p>
	 * This method corresponds to the KNX network management subnetwork devices scan
	 * procedure <i>NM_SubnetworkDevices_Scan</i>.<br>
	 * It scans a specific KNX subnetwork, identified by the <code>area</code> and
	 * <code>line</code> of the KNX network.<br>
	 * For this procedure, the individual address of used routers in the KNX network and
	 * the domain address have to be configured.
	 *
	 * @param area the KNX network area to scan for network devices,
	 *        <code>0 &le; area &le; 0x0F</code>, devices in the backbone line of areas are
	 *        assigned area address 0; for a definition of area, see
	 *        {@link IndividualAddress}
	 * @param line the KNX network line to scan for network devices,
	 *        <code>0 &le; line &le; 0x0F</code>, devices in the main line of an area are
	 *        assigned line address 0; for a definition of line, see
	 *        {@link IndividualAddress}
	 * @return an array of {@link IndividualAddress}es of the existing network devices,
	 *         with the array length equal to the number of existing devices
	 * @throws KNXTimeoutException on communication timeouts during the scanning
	 * @throws KNXLinkClosedException on a closed KNXNetworkLink to the KNX network
	 * @throws InterruptedException if this thread was interrupted during scanning the
	 *         network devices
	 */
	IndividualAddress[] scanNetworkDevices(int area, int line) throws KNXTimeoutException,
			KNXLinkClosedException, InterruptedException;

	/**
	 * Determines the existing KNX network devices on a specific KNX subnetwork. This method corresponds to the KNX
	 * network management subnetwork devices scan procedure <i>NM_SubnetworkDevices_Scan</i>. This procedure scans a
	 * specific KNX subnetwork, identified by the <code>area</code> and <code>line</code> of the KNX network.<br>
	 * For this procedure to work, the individual address (and the domain address for open media) of the used routers in
	 * the KNX network have to be configured.
	 *
	 * @param area the KNX network area to scan for network devices, <code>0 &le; area &le; 0x0F</code>; devices in the
	 *        backbone line of areas are assigned area address 0. For a definition of area, see
	 *        {@link IndividualAddress}.
	 * @param line the KNX network line to scan for network devices, <code>0 &le; line &le; 0x0F</code>; devices in the
	 *        main line of an area are assigned line address 0. for a definition of line, see {@link IndividualAddress}
	 * @param device consumer called for every device found during the scan
	 * @throws KNXTimeoutException on communication timeouts during the scan
	 * @throws KNXLinkClosedException on a closed KNXNetworkLink to the KNX network
	 * @throws InterruptedException if this thread was interrupted while scanning the network devices
	 */
	void scanNetworkDevices(int area, int line, Consumer<IndividualAddress> device)
		throws KNXTimeoutException, KNXLinkClosedException, InterruptedException;

	/**
	 * Determines the serial numbers of all KNX devices that have its individual address set to the
	 * default individual address.
	 * <p>
	 * This method corresponds to the KNX <i>NM_SerialNumberDefaultIA_Scan</i> procedure.<br>
	 * The default individual address differs (in its subnet part) according to the KNX network
	 * medium.<br>
	 * Recognized network media:
	 * <ul>
	 * <li>1: TP 0</li>
	 * <li>2: TP 1</li>
	 * <li>3: PL 132</li>
	 * <li>4: PL 110</li>
	 * <li>5: RF</li>
	 * </ul>
	 * Choose one of the listed entries for the <code>medium</code> parameter.<br>
	 * Implementation note: The number of each list entry for a medium equals the default subnetwork
	 * address part for that medium.
	 *
	 * @param medium KNX network medium (for the medium-dependent default subnetwork identifier),
	 *        <code>0 &lt; medium &lt; 6</code>
	 * @return a new list with byte arrays of serial numbers, corresponding to KNX devices having
	 *         the default individual address set
	 * @throws KNXException on network or reading error obtaining the serial numbers
	 * @throws InterruptedException on interrupted thread
	 */
	// ??? can we automatically detect the medium in this procedure?
	List<byte[]> scanSerialNumbers(int medium) throws KNXException, InterruptedException;

	// mode querying/setting procedures

	/**
	 * Sets the programming mode of a KNX network device.
	 * <p>
	 * This method corresponds to the KNX <i>DM_ProgMode_Switch</i>
	 * (<i>DMP_ProgModeSwitch_RCo</i>) procedure.<br>
	 *
	 * @param device the addressed KNX network device
	 * @param programming <code>true</code> to set the device into programming mode,
	 *        <code>false</code> to switch off programming mode
	 * @throws KNXException on communication error or device access problems
	 * @throws InterruptedException on interrupted thread
	 */
	void setProgrammingMode(IndividualAddress device, boolean programming) throws KNXException,
			InterruptedException;

	// memory procedures

	/**
	 * Writes a contiguous block of data to a the specified memory address of a device.
	 * <p>
	 * This method corresponds to the KNX <i>DM_MemWrite</i> (<i>DMP_MemWrite_RCo</i> and
	 * <i>DMP_MemWrite_RCoV</i>) procedure.<br>
	 * This procedure allows to write bigger blocks of contiguous memory, splitting up the data into
	 * suitable packets for transfer over the KNX network, if necessary.<br>
	 * The memory is written in the order given by <code>data</code>, starting from
	 * <code>data[0]</code>, to the device memory, starting from <code>startAddress</code>
	 * (inclusive).
	 *
	 * @param device the destination device address
	 * @param startAddress the memory destination start address,
	 *        <code>0 &le; startAddress &le; 0xFFFFFFFF</code>
	 * @param data the data to be written, with <code>data.length</code> equal to the number of
	 *        bytes to write
	 * @param verifyWrite <code>true</code> to read back and compare any written memory for
	 *        equality, <code>false</code> otherwise; if <code>true</code>,
	 *        <code>verifyByServer</code> has to be set to <code>false</code>
	 * @param verifyByServer <code>true</code> to enable verification by the management server of
	 *        any written memory, <code>false</code> otherwise; if <code>true</code>,
	 *        <code>verifyWrite</code> has to be set to <code>false</code>
	 * @throws KNXException on communication error or device access problems
	 * @throws InterruptedException on interrupted thread
	 */
	void writeMemory(IndividualAddress device, long startAddress, byte[] data, boolean verifyWrite,
			boolean verifyByServer) throws KNXException, InterruptedException;

	/**
	 * Reads a contiguous block of data from the specified memory address of a device.
	 * <p>
	 * This method corresponds to the KNX <i>DM_MemRead</i> (<i>DMP_MemRead_RCo</i>) procedure.<br>
	 * This method allows to read bigger blocks of contiguous memory, splitting up the
	 * data into suitable packets for transfer over the KNX network, if necessary.<br>
	 * The memory is read in increasing steps of memory addresses, starting from
	 * <code>startAddress</code> (inclusive).
	 *
	 * @param device the destination device address
	 * @param startAddress the memory source start address,
	 *        <code>0 &le; startAddress &le; 0xFFFFFFFF</code>
	 * @param bytes number of bytes to read, <code>0 &lt; bytes</code>
	 * @return an array of bytes, with the length of the array equal to <code>bytes</code>
	 * @throws KNXException on communication error or device access problems
	 * @throws InterruptedException on interrupted thread
	 */
	byte[] readMemory(IndividualAddress device, long startAddress, int bytes) throws KNXException,
			InterruptedException;

	/**
	 * Detaches the network link from this management procedures instance.
	 * <p>
	 * Calling this method will not detach or release any helper objects not created by
	 * this instance, e.g., a management client passed during construction.<br>
	 * The state of a KNX network link being detached is not changed. If no network link
	 * is attached, no action is performed.
	 * <p>
	 * Note that a detach does not close the used network link.
	 *
	 * @see ManagementClient#detach()
	 */
	void detach();

	@Override
	default void close() { detach(); }
}
