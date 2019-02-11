/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2019 B. Malinowsky

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

package tuwien.auto.calimero;

import static java.util.stream.Collectors.joining;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * General settings used in Calimero as well as library user information.
 *
 * @author B. Malinowsky
 */
public final class Settings
{
	private static final String version = "2.4";
	private static final String library = "Calimero";
	private static final String desc = "A library for KNX network access";

	private static final String tuwien = "Vienna University of Technology";
	private static final String group = "Automation Systems Group";
	private static final String copyright = "Copyright \u00A9 2006-2019";

	// aligns the bundle package name following the friendly name,
	// works for friendly name with max length of 20 chars
	private static final String bundleAlignment = "                      ";
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
	 * @param verbose <code>true</code> to return all header information just mentioned,
	 *        <code>false</code> to only return library name and version comprised of
	 *        one line (no line separators)
	 * @return header as string
	 */
	public static String getLibraryHeader(final boolean verbose)
	{
		if (!verbose)
			return library + " version " + version;
		final StringBuilder buf = new StringBuilder();
		buf.append(library).append(" version ").append(version).append(" - ");
		buf.append(desc).append(sep);
		buf.append(group).append(", ");
		buf.append(tuwien).append(sep);
		buf.append(copyright);
		return buf.toString();
	}

	/**
	 * @deprecated No replacement.
	 * @return the bundle listing as string
	 */
	@Deprecated
	public static String getBundleListing()
	{
		final StringBuilder buf = new StringBuilder();
		buf.append(getBundle("cEMI", "tuwien.auto.calimero.cemi.CEMI", 1)).append(sep);
		buf.append(getBundle("KNXnet/IP", "tuwien.auto.calimero.knxnetip.KNXnetIPConnection", 1)).append(sep);
		buf.append(getBundle("FT1.2", "tuwien.auto.calimero.serial.FT12Connection", 1)).append(sep);
		buf.append(getBundle("TP-Uart", "tuwien.auto.calimero.serial.TpuartConnection", 1)).append(sep);
		buf.append(getBundle("USB", "tuwien.auto.calimero.serial.usb.UsbConnection", 1)).append(sep);
		buf.append(getBundle("KNX network link", "tuwien.auto.calimero.link.KNXNetworkLink", 1)).append(sep);
		buf.append(getBundle("DPT translators", "tuwien.auto.calimero.dptxlator.DPTXlator", 1)).append(sep);
		buf.append(getBundle("Datapoints", "tuwien.auto.calimero.datapoint.Datapoint", 1)).append(sep);
		buf.append(getBundle("Network buffer", "tuwien.auto.calimero.buffer.NetworkBuffer", 1)).append(sep);
		buf.append(getBundle("Process communication", "tuwien.auto.calimero.process." + "ProcessCommunicator", 1))
				.append(sep);
		buf.append(getBundle("Management", "tuwien.auto.calimero.mgmt.ManagementClient", 1)).append(sep);
		buf.append(getBundle("XML", "tuwien.auto.calimero.xml.DefaultXmlReader", 1));
		return buf.toString();
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
		if (args.length > 0 && (args[0].equals("--version") || args[0].equals("-v")))
			out(getLibraryHeader(false));
		else {
			out(getLibraryHeader(true));
			out(sep + "Supported protocols: " + supportedProtocols().collect(joining(", ")));
		}
	}

	private static Stream<String> supportedProtocols() {
		final String[] proto = { "KNXnet/IP", "KNX IP", "FT1.2", "TP-Uart", "KNX USB", "KNX RF USB" };
		final String prefix = "tuwien.auto.calimero.";
		final String[] check = { "knxnetip.KNXnetIPConnection", "knxnetip.KNXnetIPConnection", "serial.FT12Connection",
			"serial.TpuartConnection", "serial.usb.UsbConnection", "serial.usb.UsbConnection" };

		return IntStream.range(0, proto.length).filter(i -> loadable(prefix + check[i])).mapToObj(i -> proto[i]);
	}

	private static boolean loadable(final String className) {
		try {
			final ClassLoader cl = Settings.class.getClassLoader();
			cl.loadClass(className);
			return true;
		}
		catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
		return false;
	}

	// for now, this works by loading one class as representative from a bundle
	// to check availability, then class name is truncated to bundle id
	private static String getBundle(final String friendlyName, final String className, final int truncate)
	{
		try {
			final ClassLoader cl = Settings.class.getClassLoader();
			cl.loadClass(className);
			int start = className.length();
			for (int i = 0; i < truncate; ++i)
				start = className.lastIndexOf('.', start - 1);
			final String bundle = className.substring(0, start);
			return "+ " + friendlyName + align(friendlyName) + "- " + bundle;
		}
		catch (final ClassNotFoundException e) {}
		catch (final NoClassDefFoundError e) {}
		return "- " + friendlyName + align(friendlyName) + "- not available";
	}

	private static String align(final String friendlyName)
	{
		return bundleAlignment.substring(friendlyName.length());
	}

	private static void out(final String s)
	{
		System.out.println(s);
	}
}
