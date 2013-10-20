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

package tuwien.auto.calimero.xml;

/**
 * XML element attribute.
 * <p>
 * Objects of this type are immutable.
 * 
 * @author B. Malinowsky
 */
public class Attribute
{
	private final String name;
	private final String value;

	/**
	 * Creates an attribute with its name and value.
	 * <p>
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 */
	public Attribute(final String name, final String value)
	{
		this.name = name;
		this.value = value;
	}

	/**
	 * Returns the attribute name.
	 * <p>
	 * 
	 * @return attribute name
	 */
	public final String getName()
	{
		return name;
	}

	/**
	 * Returns the value of this attribute.
	 * <p>
	 * 
	 * @return value as String
	 */
	public final String getValue()
	{
		return value;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name + "=" + value;
	}
}
