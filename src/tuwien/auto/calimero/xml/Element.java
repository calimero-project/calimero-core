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

package tuwien.auto.calimero.xml;

/**
 * Represents a XML element holding necessary information for reading XML data.
 * <p>
 * 
 * @author B. Malinowsky
 */
public interface Element
{
	/**
	 * Returns the name of this element, the element type.
	 * <p>
	 * 
	 * @return name as string, the element's type
	 */
	String getName();

	/**
	 * Adds a new attribute associated with this element.
	 * <p>
	 * 
	 * @param a the attribute to add
	 */
	void addAttribute(Attribute a);

	/**
	 * Returns the value of the attribute with <code>name</code>.
	 * <p>
	 * 
	 * @param name name of attribute to get attribute value
	 * @return value as String, or <code>null</code> if no such attribute
	 */
	String getAttribute(String name);

	/**
	 * Returns whether an attribute with the given name is associated with this element.
	 * <p>
	 * 
	 * @param name attribute name
	 * @return <code>true</code> if an attribute with the given name is associated with
	 *         this element, <code>false</code> otherwise
	 */
	boolean hasAttribute(String name);

	/**
	 * Sets the character data contained in this element.
	 * <p>
	 * If new character data is set, the empty element tag is set <code>false</code>.
	 * 
	 * @param data character data of this element, <code>null</code> for no data
	 */
	void setCharacterData(String data);

	/**
	 * Returns the character data contained in this element.
	 * <p>
	 * Character data is the element's content without markup. No formatting is done on
	 * character data.<br>
	 * Put it more simple, character data is the actual value information (in textual
	 * format) of an element.
	 * 
	 * @return character data as String or <code>null</code> if no data available
	 */
	String getCharacterData();

	/**
	 * Determines if this element uses an empty element tag.
	 * <p>
	 * If empty element tag is used, this element also has no content.
	 * 
	 * @param empty <code>true</code> if empty element, <code>false</code> otherwise
	 */
	void setEmptyElementTag(boolean empty);

	/**
	 * Returns whether this element uses an empty element tag to indicate an empty
	 * element.
	 * <p>
	 * An element with no content is said to be empty. An empty element tag is of the form
	 * <code>{@literal '&lt;' Name (S Attribute)* S? '/&gt;'}</code>, with S being
	 * space. Note that if this method returns <code>false</code>, it does not imply
	 * this element has any content, returned by {@link #getCharacterData()}.
	 * 
	 * @return <code>true</code> if empty element with empty element tag,
	 *         <code>false</code> otherwise
	 */
	boolean isEmptyElementTag();
}
