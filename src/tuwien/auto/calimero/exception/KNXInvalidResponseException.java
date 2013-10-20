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

package tuwien.auto.calimero.exception;

/**
 * Thrown to indicate a message format mismatch of a response sent by a remote endpoint.
 * <p>
 * Particularly, if received data from the network is parsed and the structure read does
 * not match.
 * 
 * @author B. Malinowsky
 */
public class KNXInvalidResponseException extends KNXRemoteException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new <code>KNXInvalidResponseException</code> without a detail
	 * message.
	 * <p>
	 */
	public KNXInvalidResponseException()
	{}

	/**
	 * Constructs a new <code>KNXInvalidResponseException</code> with the specified
	 * detail message.
	 * <p>
	 * 
	 * @param s the detail message
	 */
	public KNXInvalidResponseException(final String s)
	{
		super(s);
	}
}
