/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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
*/

package tuwien.auto.calimero.process;

import java.util.EventObject;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

/**
 * Contains information about a process message event.
 * <p>
 * 
 * @author B. Malinowsky
 * @see ProcessCommunicator
 */
public class ProcessEvent extends EventObject
{
	private static final long serialVersionUID = 1L;

	private final IndividualAddress src;
	private final GroupAddress dst;
	// We provide the ASDU only to avoid the need of masking out the service code in
	// the user application (the service code is implicitly known through the context
	// of the called method anyway).
	// Nevertheless, if the service code should be of interest at some time, we will
	// just add a getter method for it to this event.
	private final byte[] asdu;

	/**
	 * Creates a new process event with the KNX message source address, destination
	 * address and ASDU.
	 * <p>
	 * 
	 * @param source the receiving process communicator
	 * @param src KNX source individual address of message
	 * @param dst KNX destination address of message
	 * @param asdu byte array with the application layer service data unit (ASDU), no
	 *        copy is created
	 */
	public ProcessEvent(final ProcessCommunicator source, final IndividualAddress src,
		final GroupAddress dst, final byte[] asdu)
	{
		super(source);
		this.src = src;
		this.dst = dst;
		this.asdu = asdu;
	}

	/**
	 * Returns the KNX individual source address.
	 * <p>
	 * 
	 * @return address as IndividualAddress
	 */
	public final IndividualAddress getSourceAddr()
	{
		return src;
	}

	/**
	 * Returns the KNX destination group address.
	 * <p>
	 * 
	 * @return address as GroupAddress
	 */
	public final GroupAddress getDestination()
	{
		return dst;
	}

	/**
	 * Returns the application layer service data unit (ASDU).
	 * <p>
	 * 
	 * @return copy of ASDU as byte array
	 */
	public final byte[] getASDU()
	{
		return (byte[]) asdu.clone();
	}
}
