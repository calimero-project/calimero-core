/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2023 B. Malinowsky

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

package io.calimero;

/**
 * General settings used in Calimero as well as library user information.
 *
 * @author B. Malinowsky
 */
public final class Settings
{
	private static final String version = "3.0-SNAPSHOT";
	private static final String library = "Calimero";
	private static final String desc = "A library for KNX network access";

	private static final String tuwien = "Vienna University of Technology";
	private static final String group = "Automation Systems Group";
	private static final String copyright = "Copyright © 2006-2023";

	// just use newline, it's easier to deal with
	private static final String sep = "\n";


	private Settings() {}

	/**
	 * Returns the core library version as string representation.
	 * <p>
	 * The returned version is formatted something similar to
	 * "main.minor[.milli][-phase]", for example, "2.0" or "2.0.0-alpha".
	 *
	 * @return version as string
	 */
	public static String getLibraryVersion()
	{
		return version;
	}

	/**
	 * Returns a default library header representation with general/usage information.
	 * <p>
	 * It includes stuff like the library name, library version, name and institute of the
	 * Vienna University of Technology where the library was developed, and copyright.
	 * The returned information parts are divided using the newline ('\n') character.
	 *
	 * @param verbose {@code true} to return all header information just mentioned,
	 *        {@code false} to only return library name and version comprised of
	 *        one line (no line separators)
	 * @return header as string
	 */
	public static String getLibraryHeader(final boolean verbose)
	{
		if (!verbose)
			return library + " " + version;
		return library + " version " + version + " - " + desc + sep
				+ group + ", " + tuwien + sep
				+ copyright;
	}

	/**
	 * This entry routine of the library prints information to the standard
	 * output stream (System.out), mainly for user information.
	 * <p>
	 * Recognized options for output:
	 * <ul>
	 * <li>no options: default library header information and bundle listing</li>
	 * <li>-v, --version: prints library name and version</li>
	 * </ul>
	 *
	 * @param args argument list with options controlling output information
	 */
	public static void main(final String[] args)
	{
		if (args.length > 0 && ("--version".equals(args[0]) || "-v".equals(args[0])))
			out(getLibraryHeader(false));
		else
			out(getLibraryHeader(true));
	}

	private static void out(final String s)
	{
		System.out.println(s);
	}
}
