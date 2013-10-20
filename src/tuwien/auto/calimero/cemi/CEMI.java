/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2005 B. Erb
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

package tuwien.auto.calimero.cemi;

/**
 * Common External Message Interface (cEMI).
 * <p>
 * Acts as basic interface to the cEMI abstract data type.<br>
 * The cEMI message format aims to be a frame structure for KNX information transport,
 * independent of various KNX media.
 * 
 * @author Bernhard Erb
 * @author B. Malinowsky
 */
public interface CEMI
{
	/**
	 * Returns the payload carried in this cEMI message.
	 * <p>
	 * In general, the payload refers to that content of a cEMI frame, which is not
	 * interpreted nor parsed by the different types of cEMI. See the corresponding
	 * subtypes for a more specific description.
	 * 
	 * @return subset of frame data as byte array
	 */
	byte[] getPayload();

	/**
	 * Returns the cEMI message code.
	 * <p>
	 * The codes of the different cEMI message types can be looked up in the according
	 * subtype implementations.
	 * 
	 * @return the message code as unsigned byte
	 */
	int getMessageCode();

	/**
	 * Returns the length of this cEMI message frame.
	 * <p>
	 * 
	 * @return the message length in bytes
	 */
	int getStructLength();

	/**
	 * Returns the byte representation of the whole cEMI message structure.
	 * <p>
	 * 
	 * @return frame as byte array
	 */
	byte[] toByteArray();
}
