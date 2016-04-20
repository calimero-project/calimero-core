/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2010, 2016 B. Malinowsky

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

import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.dptxlator.DPTXlator;

/**
 * Common property services for accessing KNX properties and property descriptions.
 * <p>
 *
 * @author B. Malinowsky
 */
public interface PropertyAccess
{
	/**
	 * Defines property identifiers (PIDs) for global properties (interface object type =
	 * global) and PIDs of the KNXnet/IP parameter object (interface object type 11).
	 * <p>
	 * Users are encouraged to use these more descriptive name constants instead of plain
	 * PID integer values for better readability.<br>
	 * The PIDs are put into their own class to create an encapsulation besides the
	 * property access interface, since these constants are solely for the user's sake,
	 * implementations of the {@link PropertyAccess} interface do not depend on them.<br>
	 *
	 * @author B. Malinowsky
	 */
	final class PID
	{
		//
		// global properties (server object type = global)
		//

		/**
		 * Global property "Interface Object Type".
		 * <p>
		 * Object Type Device Object.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT,DPT_PropDataType
		public static final int OBJECT_TYPE = 1;

		/**
		 * Global property "Interface Object Name".
		 * <p>
		 * Name of the Interface Object.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR[]
		public static final int OBJECT_NAME = 2;

		/**
		 * ! to be defined ! Property name:
		 */
		// PDT,DPT: <tbd>
		// int SEMAPHOR = 3;
		/**
		 * ! to be defined ! Property name:
		 */
		// PDT,DPT: <tbd>
		// int GROUP_OBJECT_REFERENCE = 4;
		/**
		 * Global property "Load Control".
		 * <p>
		 * Access to load state machines.
		 */
		// PDT,DPT: PDT_CONTROL
		public static final int LOAD_STATE_CONTROL = 5;

		/**
		 * Global property "Run Control".
		 * <p>
		 * Access to run state machines.
		 */
		// PDT,DPT: PDT_CONTROL
		public static final int RUN_STATE_CONTROL = 6;

		/**
		 * Global property "Table Reference".
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int TABLE_REFERENCE = 7;

		/**
		 * Global property "Service Control".
		 * <p>
		 * Service Control, Permanent Control field for the Device.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int SERVICE_CONTROL = 8;

		/**
		 * Global property "Firmware Revision".
		 * <p>
		 * Revision number of the Firmware.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int FIRMWARE_REVISION = 9;

		/**
		 * ! to be defined ! Property name:
		 * <p>
		 * Services Supported.
		 */
		// PDT,DPT: <tbd>
		// int SERVICES_SUPPORTED = 10;
		/**
		 * Global property "KNX Serial Number".
		 * <p>
		 * KNX Serial Number of the device.
		 */
		// PDT,DPT: PDT_GENERIC_06
		public static final int SERIAL_NUMBER = 11;

		/**
		 * Global property "Manufacturer Identifier".
		 * <p>
		 * Manufacturer code.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int MANUFACTURER_ID = 12;

		/**
		 * Global property "Application Version".
		 * <p>
		 * Version of the Application Program.
		 */
		// PDT,DPT: PDT_GENERIC_05
		public static final int PROGRAM_VERSION = 13;

		/**
		 * Global property "Device Control".
		 * <p>
		 * Device Control, Temporary Control field for the Device.
		 */
		// PDT,DPT: PDT_BISET8,DPT_Device_Control
		public static final int DEVICE_CONTROL = 14;

		/**
		 * Global property "Order Info".
		 * <p>
		 * OrderInfo, Manufacturer specific Order ID.
		 */
		// PDT,DPT: PDT_GENERIC_10
		public static final int ORDER_INFO = 15;

		/**
		 * Global property "PEI Type".
		 * <p>
		 * Connected or required PEI-type.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int PEI_TYPE = 16;

		/**
		 * Global property "PortADDR".
		 * <p>
		 * PortAddr, Direction bits for Port A.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int PORT_CONFIGURATION = 17;

		/**
		 * Global property "Pollgroup Settings".
		 * <p>
		 * Pollgroup settings (polling group and slot number).
		 */
		// PDT,DPT: PDT_POLL_GROUP_SETTINGS
		public static final int POLL_GROUP_SETTINGS = 18;

		/**
		 * Global property "Manufacturer Data".
		 * <p>
		 * Manufacturer Data.
		 */
		// PDT,DPT: PDT_GENERIC_04
		public static final int MANUFACTURER_DATA = 19;

		/**
		 * ! to be defined ! Property name:
		 */
		// PDT,DPT: <tbd>
		// int ENABLE = 20;
		/**
		 * Global property "Description".
		 * <p>
		 * Description, Description of the device.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR[ ]
		public static final int DESCRIPTION = 21;

		/**
		 * ! to be defined ! Property name:
		 */
		// PDT,DPT: <tbd>
		// int FILE = 22;
		/**
		 * Global property "Address Table Format 0".
		 * <p>
		 * Group Address Table, Association Table.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT[]
		public static final int TABLE = 23;

		/**
		 * Global property "Interface Object Link".
		 * <p>
		 * enrollment status, A-Mode enrollment.
		 */
		// PDT,DPT: PDT_FUNCTION
		// this property is specified uses British spelling
		public static final int ENROL = 24;

		/**
		 * Global property "Version".
		 * <p>
		 * Generic version information.
		 */
		// PDT,DPT: PDT_VERSION (U6U6U6)
		public static final int VERSION = 25;

		/**
		 * ! to be defined ! Property name: Group Address Assignment.
		 */
		// PDT,DPT: PDT_FUNCTION
		// int GROUP_OBJECT_LINK = 26;
		/**
		 * Global property "Memory Control Table".
		 * <p>
		 * Sub-segmentation of memory space and checksum.
		 */
		// PDT,DPT: PDT_GENERIC_07[]
		public static final int MCB_TABLE = 27;

		/**
		 * Global property "Error code".
		 * <p>
		 * Error code when load state machine indicates "error".
		 */
		// PDT,DPT: PDT_GENERIC_01
		public static final int ERROR_CODE = 28;

		/**
		 * Global property "Object Index".
		 * <p>
		 * Access to the object index of a function block.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int OBJECT_INDEX = 29;

		//
		// properties of object type 0, Device Object
		//

		/**
		 * Device object property "Programming Mode".
		 * <p>
		 * Bit Set:<br>
		 * Bit 0 (LSB) - programming mode: 1 = device is in programming mode, 0 = device
		 * is not in programming mode (i.e., equal to KNX property of device status)<br>
		 * Bits 1 to 7 - reserved
		 */
		// PDT,DPT: PDT_BITSET8
		public static final int PROGMODE = 54;

		/**
		 * Device object property "Max. APDU-Length".
		 * <p>
		 * Maximum APDU length.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int MAX_APDULENGTH = 56;

		/**
		 * Device object property "Subnetwork Address".
		 * <p>
		 * KNX subnet address, i.e., the high octet of the KNX individual address.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int SUBNET_ADDRESS = 57;

		/**
		 * Device object property "Device Address".
		 * <p>
		 * Device address, i.e., the low octet of the KNX individual address.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int DEVICE_ADDRESS = 58;

		/**
		 * Device object property "Domain Address".
		 * <p>
		 * Domain address of a KNX device.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int DOMAIN_ADDRESS = 70;

		/**
		 * Device object property "Interface Object List".
		 * <p>
		 * Used with cEMI functionality.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int IO_LIST = 71;

		/**
		 * Device object property "Device Descriptor Type 0".
		 */
		// PDT,DPT: PDT_GENERIC_02
		public static final int DEVICE_DESCRIPTOR = 83;


		//
		// properties of object type 6, Router object
		//

		/**
		 * Router object property "Line Status", status of the subnetwork connected to the secondary
		 * side of the coupler. Bit 0 = PowerDownSubline: 0 = power up, 1 = power down.
		 */
		public static final int LINE_STATUS = 51;

		/**
		 * Router object property "Main Line Coupler Config", for point-to-point and broadcast
		 * frames on the primary side subnetwork of the coupler.
		 */
		public static final int MAIN_LCCONFIG = 52;

		/**
		 * Router object property "Sub Line Coupler Config", for point-to-point and broadcast
		 * frames on the secondary side subnetwork of the coupler.
		 */
		public static final int SUB_LCCONFIG = 53;

		/**
		 * Router object property "Main Line Coupler Group Config", for multicast
		 * frames (group communication) on the primary side subnetwork of the coupler.
		 */
		public static final int MAIN_LCGROUPCONFIG = 54;

		/**
		 * Router object property "Sub Line Coupler Group Config", for multicast
		 * frames (group communication) on the secondary side subnetwork of the coupler.
		 */
		public static final int SUB_LCGROUPCONFIG = 55;

		//
		// properties of object type 9, CEMI server object
		//

		/**
		 * Object type 8 property "Medium Type".
		 * <p>
		 * KNX media types supported by server.
		 * Bit 0 (LSB): reserved (formerly TP0)<br>
		 * Bit 1: TP1<br>
		 * Bit 2: PL110<br>
		 * Bit 3: RF
		 * Bit 4 - 15 (MSB): reserved
		 */
		public static final int MEDIUM_TYPE = 51;

		//
		// properties of object type 11, KNXnet/IP parameter object
		//

		/**
		 * Object type 11 property "Project Installation Identification".
		 * <p>
		 * Identification of the project and the installation in which this KNXnet/IP
		 * device resides.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int PROJECT_INSTALLATION_ID = 51;

		/**
		 * Object type 11 property "KNX Individual Address".
		 * <p>
		 * Individual Address of the KNXnet/IP server.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int KNX_INDIVIDUAL_ADDRESS = 52;

		/**
		 * Object type 11 property "Additional Individual Addresses".
		 * <p>
		 * Sorted list of additional KNX Individual Addresses for KNXnet/IP routers and
		 * tunneling servers.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT[]
		public static final int ADDITIONAL_INDIVIDUAL_ADDRESSES = 53;

		/**
		 * Object type 11 property "Current IP Assignment Method".
		 * <p>
		 * Used IP address assignment method.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int CURRENT_IP_ASSIGNMENT_METHOD = 54;

		/**
		 * Object type 11 property "IP Assignment Method".
		 * <p>
		 * Enabled IP assignment methods that can be used.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int IP_ASSIGNMENT_METHOD = 55;

		/**
		 * Object type 11 property "IP Capabilities".
		 * <p>
		 * Capabilities of the KNXnet/IP device for obtaining an IP address.
		 */
		// PDT,DPT: PDT_BITSET_8
		public static final int IP_CAPABILITIES = 56;

		/**
		 * Object type 11 property "Current IP Address".
		 * <p>
		 * Currently used IP address.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int CURRENT_IP_ADDRESS = 57;

		/**
		 * Object type 11 property "Current Subnet Mask".
		 * <p>
		 * Currently used IP subnet mask.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int CURRENT_SUBNET_MASK = 58;

		/**
		 * Object type 11 property "Current Default Gateway".
		 * <p>
		 * IP Address of the KNXnet/IP device's default gateway.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int CURRENT_DEFAULT_GATEWAY = 59;

		/**
		 * Object type 11 property "IP Address".
		 * <p>
		 * Configured fixed IP Address.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int IP_ADDRESS = 60;

		/**
		 * Object type 11 property "Subnet Mask".
		 * <p>
		 * Configured IP subnet mask.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int SUBNET_MASK = 61;

		/**
		 * Object type 11 property "Default Gateway".
		 * <p>
		 * Configured IP address of the default gateway.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int DEFAULT_GATEWAY = 62;

		/**
		 * Object type 11 property "DHCP/BootP Server".
		 * <p>
		 * IP address of the DHCP or BootP server from which the KNXnet/IP device received
		 * its IP address.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int DHCP_BOOTP_SERVER = 63;

		/**
		 * Object type 11 property "MAC Address".
		 * <p>
		 * MAC address of the KNXnet/IP device.
		 */
		// PDT,DPT: PDT_GENERIC_06
		public static final int MAC_ADDRESS = 64;

		/**
		 * Object type 11 property "System Setup Multicast Address".
		 * <p>
		 * KNXnet/IP system set-up multicast address. Value is fixed to 224.0.23.12.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int SYSTEM_SETUP_MULTICAST_ADDRESS = 65;

		/**
		 * Object type 11 property "Routing Multicast Address".
		 * <p>
		 * Routing multicast address.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int ROUTING_MULTICAST_ADDRESS = 66;

		/**
		 * Object type 11 property "Time To Live".
		 * <p>
		 * TTL value to be used by KNXnet/IP devices.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int TTL = 67;

		/**
		 * Object type 11 property "KNXnet/IP Device Capabilities".
		 * <p>
		 * KNXnet/IP protocols supported by the KNXnet/IP device.
		 */
		// PDT,DPT: PDT_BITSET_16
		public static final int KNXNETIP_DEVICE_CAPABILITIES = 68;

		/**
		 * Object type 11 property "KNXnet/IP Device State".
		 * <p>
		 * Various KNXnet/IP device status info, like KNX or IP network connection failure.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int KNXNETIP_DEVICE_STATE = 69;

		/**
		 * Object type 11 property "KNXnet/IP Routing Capabilities".
		 * <p>
		 * Supported features by the KNXnet/IP Router.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int KNXNETIP_ROUTING_CAPABILITIES = 70;

		/**
		 * Object type 11 property "Priority FIFO Enabled".
		 * <p>
		 * Indication of whether the priority FIFO is enabled of disabled.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR
		public static final int PRIORITY_FIFO_ENABLED = 71;

		/**
		 * Object type 11 property "Queue Overflow to IP".
		 * <p>
		 * Number of telegrams lost due to overflow of queue to IP.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int QUEUE_OVERFLOW_TO_IP = 72;

		/**
		 * Object type 11 property "Queue overflow to KNX".
		 * <p>
		 * Number of telegrams lost due to overflow of queue to KNX.
		 */
		// PDT,DPT: PDT_UNSIGNED_INT
		public static final int QUEUE_OVERFLOW_TO_KNX = 73;

		/**
		 * Object type 11 property "Telegrams Transmitted to IP".
		 * <p>
		 * Number of telegrams successfully transmitted to IP.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int MSG_TRANSMIT_TO_IP = 74;

		/**
		 * Object type 11 property "Telegrams Transmitted to KNX".
		 * <p>
		 * Number of telegrams successfully transmitted to KNX.
		 */
		// PDT,DPT: PDT_UNSIGNED_LONG
		public static final int MSG_TRANSMIT_TO_KNX = 75;

		/**
		 * Object type 11 property "Friendly Name".
		 * <p>
		 * Human Readable Friendly Name.
		 */
		// PDT,DPT: PDT_UNSIGNED_CHAR[30]
		public static final int FRIENDLY_NAME = 76;

		/**
		 * Object type 11 property "Routing Busy Wait Time".
		 * <p>
		 * The time wait value sent with a routing busy indication, with default value 100 ms.<br>
		 * PDT: PDT_UNSIGNED_INT<br>
		 * Implementations of masks 0x091A and 0x5705 may not have this property.
		 */
		public static final int ROUTING_BUSY_WAIT_TIME = 78;


		// enfore non-instantiability
		private PID() {}
	}

	/**
	 * Sets one element of a property, with the value given as string representation.
	 * <p>
	 * The value is translated according the associated property data type.
	 *
	 * @param objIndex interface object index in the device
	 * @param pid property identifier
	 * @param position property index in the array where to set the element value
	 * @param value string representation of the element value
	 * @throws KNXException on adapter errors while setting the property elements or
	 *         translation problems
	 * @throws InterruptedException on thread interrupt
	 */
	void setProperty(int objIndex, int pid, int position, String value)
		throws KNXException, InterruptedException;

	/**
	 * Sets one or more elements of a property.
	 *
	 * @param objIndex interface object index in the device
	 * @param pid property identifier
	 * @param start index of the first array element to set
	 * @param elements number of elements to set in the property
	 * @param data byte array holding the element data
	 * @throws KNXException on adapter errors while setting the property elements
	 * @throws InterruptedException on thread interrupt
	 */
	void setProperty(int objIndex, int pid, int start, int elements, byte[] data)
		throws KNXException, InterruptedException;

	/**
	 * Gets one or more elements of a property.
	 *
	 * @param objIndex interface object index in the device
	 * @param pid property identifier
	 * @param start index of the first array element to get
	 * @param elements number of elements to get in the property
	 * @return byte array holding the retrieved element data
	 * @throws KNXException on adapter errors while querying the property element
	 * @throws InterruptedException on thread interrupt
	 */
	byte[] getProperty(int objIndex, int pid, int start, int elements)
		throws KNXException, InterruptedException;

	/**
	 * Gets one or more elements of a property with the returned data set in a DPT
	 * translator of the associated data type.
	 *
	 * @param objIndex interface object index in the device
	 * @param pid property identifier
	 * @param start index of the first array element to get
	 * @param elements number of elements to get in the property
	 * @return a DPT translator containing the returned the element data
	 * @throws KNXException on adapter errors while querying the property element or data
	 *         type translation problems
	 * @throws InterruptedException on thread interrupt
	 */
	DPTXlator getPropertyTranslated(int objIndex, int pid, int start, int elements)
		throws KNXException, InterruptedException;

	/**
	 * Gets the property description based on the property ID.
	 * <p>
	 * It is not always possible to supply all description information:<br>
	 * If a description read is done using a property identifier (PID) like in this
	 * method, the description response is not required to contain the correct property
	 * index associated with the PID, even though recommended. The default index is 0
	 * then.
	 *
	 * @param objIndex interface object index in the device
	 * @param pid property identifier, pid &gt; 0
	 * @return the property description
	 * @throws KNXException on adapter errors while querying the description
	 * @throws InterruptedException on thread interrupt
	 */
	Description getDescription(int objIndex, int pid) throws KNXException, InterruptedException;

	/**
	 * Gets the property description based on the property index.
	 *
	 * @param objIndex interface object index in the device
	 * @param propIndex property index in the object, propIndex &ge; 0
	 * @return a property description object
	 * @throws KNXException on adapter errors while querying the description
	 * @throws InterruptedException on thread interrupt
	 */
	Description getDescriptionByIndex(int objIndex, int propIndex) throws KNXException,
		InterruptedException;
}
