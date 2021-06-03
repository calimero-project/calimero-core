/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2021, 2021 B. Malinowsky

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

package tuwien.auto.calimero.dptxlator;

import java.util.EnumSet;
import java.util.regex.Pattern;

import tuwien.auto.calimero.KNXIllegalArgumentException;

class EnumDptBase<T extends Enum<T> & EnumDptBase.EnumBase<T>> extends DPT {
	// use a common base to store and access enumeration elements independent of specific type
	interface EnumBase<E extends Enum<E> & EnumBase<E>> {
		@SuppressWarnings("unchecked")
		default int value() { return 1 << ((Enum<E>) this).ordinal(); }

		@SuppressWarnings("unchecked")
		default String description() { return friendly(((Enum<E>) this).name()); }
	}

	// using a class reference here, we only instantiate the enum when actually queried
	final Class<T> elements;

	EnumDptBase(final String typeId, final Class<T> elements, final String lower, final String upper) {
		this(typeId, elements.getSimpleName().replaceAll("\\B([A-Z])", " $1"), elements, lower, upper);
	}

	EnumDptBase(final String typeId, final String description, final Class<T> elements, final String lower,
		final String upper) {
		super(typeId, description, lower, upper);
		this.elements = elements;
	}

	public T[] values() { return elements.getEnumConstants(); }

	private T find(final int element) {
		for (final T e : EnumSet.allOf(elements)) {
			if (e.value() == element)
				return e;
		}
		return null;
	}

	T find(final String description) {
		for (final T e : EnumSet.allOf(elements)) {
			if (e.name().equalsIgnoreCase(description))
				return e;
			if (e.description().equalsIgnoreCase(description))
				return e;
			if (friendly(e.name()).equalsIgnoreCase(description))
				return e;
		}
		return null;
	}

	boolean contains(final int element) {
		return find(element) != null;
	}

	String textOf(final int element) {
		final T e = find(element);
		if (e != null)
			return e.description();
		throw new KNXIllegalArgumentException(
				getID() + " " + elements.getSimpleName() + " has no element " + element + " specified");
	}

	String name() { return elements.getSimpleName(); }

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(30);
		sb.append(getID()).append(": ").append(getDescription()).append(", enumeration [");
		sb.append(getLowerValue()).append("..").append(getUpperValue()).append("]");
		return sb.toString();
	}

	private static final String regex = "(\\p{Lower})\\B([A-Z])";
	private static final Pattern friendlyPattern = Pattern.compile(regex);

	private static String friendly(final String name) {
		return friendlyPattern.matcher(name).replaceAll("$1 $2").toLowerCase();
	}
}
