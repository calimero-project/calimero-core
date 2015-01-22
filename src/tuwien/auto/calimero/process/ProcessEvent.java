/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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
	// 	one of GROUP_READ (0x0), GROUP_RESPONSE (0x40), GROUP_WRITE (0x80)
	private final int svcCode;
	// We provide the ASDU only to avoid the need of masking out the service code in
	// the user application. The service code is implicitly known through the context
	// of the called method anyway, or using getServiceCode.
	private final byte[] asdu;
	private final boolean optimized;

	/**
	 * Creates a new process event with the KNX message source address, destination
	 * address, service code, and ASDU.
	 *
	 * @param source the process communicator object on which the event initially occurred
	 * @param src KNX source individual address of the corresponding KNX message
	 * @param dst KNX destination address of the corresponding KNX message
	 * @param svcCode the process communication service code
	 * @param asdu byte array with the application layer service data unit (ASDU), no copy is
	 *        created
	 * @param optimized <code>true</code> iff ASDU is optimized, i.e., length of data &le; 6 Bits;
	 *        <code>false</code> otherwise
	 */
	public ProcessEvent(final ProcessCommunicator source, final IndividualAddress src,
		final GroupAddress dst, final int svcCode, final byte[] asdu, final boolean optimized)
	{
		super(source);
		this.src = src;
		this.dst = dst;
		this.svcCode = svcCode;
		this.asdu = asdu;
		this.optimized = optimized;
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
		return asdu.clone();
	}

	/**
	 * Returns the process communication service code.
	 * <p>
	 *
	 * @return the service code, indicating either a group read, group write, or group response
	 */
	public final int getServiceCode()
	{
		return svcCode;
	}

	/**
	 * Returns whether the APDU is length-optimized, i.e., contains &le; 6 Bits data.
	 * <p>
	 *
	 * @return <code>true</code> if optimized, <code>false</code> otherwise
	 */
	public final boolean isLengthOptimizedAPDU()
	{
		return optimized;
	}
}
