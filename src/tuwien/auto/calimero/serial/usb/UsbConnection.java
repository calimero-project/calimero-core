/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2023 B. Malinowsky

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

package tuwien.auto.calimero.serial.usb;

import java.util.EnumSet;

import tuwien.auto.calimero.Connection;
import tuwien.auto.calimero.DeviceDescriptor.DD0;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.serial.KNXPortClosedException;


public interface UsbConnection extends Connection<byte[]> {
	/** EMI IDs for KNX tunnel. */
	enum EmiType {
		Emi1, Emi2, Cemi;

		@Override
		public String toString() { return this == Emi1 ? "EMI1" : this == Emi2 ? "EMI2" : "cEMI"; }
	}

	/**
	 * {@return the KNX device descriptor type 0 of the USB interface}
	 * @throws KNXPortClosedException on closed port
	 * @throws KNXTimeoutException on response timeout
	 * @throws InterruptedException on interrupt
	 * @see tuwien.auto.calimero.DeviceDescriptor
	 */
	DD0 deviceDescriptor() throws KNXPortClosedException, KNXTimeoutException, InterruptedException;

	/**
	 * {@return the EMI types supported by the KNX USB device}
	 * @throws KNXPortClosedException on closed port
	 * @throws KNXTimeoutException on response timeout
	 * @throws InterruptedException on interrupt
	 */
	EnumSet<EmiType> supportedEmiTypes() throws KNXPortClosedException, KNXTimeoutException, InterruptedException;

	/**
	 * {@return the currently active EMI type in the KNX USB device}
	 * @throws KNXPortClosedException on closed port
	 * @throws KNXTimeoutException on response timeout
	 * @throws InterruptedException on interrupt
	 */
	EmiType activeEmiType() throws KNXPortClosedException, KNXTimeoutException, InterruptedException;

	/**
	 * Sets the active EMI type for communication. Before setting an active EMI type, the supported EMI types should be
	 * checked using {@link #supportedEmiTypes()}. If only one EMI type is supported, KNX USB device support for this
	 * method is optional.
	 *
	 * @param active the EMI type to activate for communication
	 * @throws KNXPortClosedException on closed port
	 * @throws KNXTimeoutException on response timeout
	 */
	void setActiveEmiType(EmiType active) throws KNXPortClosedException, KNXTimeoutException;

	/**
	 * {@return the current state of the KNX connection, active/not active}
	 * @throws KNXPortClosedException on closed port
	 * @throws KNXTimeoutException on response timeout
	 * @throws InterruptedException on interrupt
	 */
	boolean isKnxConnectionActive() throws KNXPortClosedException, KNXTimeoutException, InterruptedException;

	/**
	 * {@return the KNX USB manufacturer code as 16 bit unsigned value}
	 * @throws KNXPortClosedException on closed port
	 * @throws KNXTimeoutException on response timeout
	 * @throws InterruptedException on interrupt
	 */
	int manufacturerCode() throws KNXPortClosedException, KNXTimeoutException, InterruptedException;

	@Override
	void send(byte[] frame, BlockingMode blockingMode) throws KNXPortClosedException, KNXTimeoutException;
}
