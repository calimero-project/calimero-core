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

package tuwien.auto.calimero;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import tuwien.auto.calimero.knxnetip.Discoverer;
import tuwien.auto.calimero.knxnetip.Discoverer.Result;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;
import tuwien.auto.calimero.serial.SerialConnectionFactory;


public final class Util
{
	/**
	 * Sets whether NAT functionality should be tested (false for devices without NAT support).
	 */
	public static final boolean TEST_NAT = true;

	// supply address here to prevent automatic router discovery
	private static InetSocketAddress server;
	private static IndividualAddress device;

	// KNX test devices, for connection-less and connection-oriented mode
	private static final IndividualAddress testDeviceCL = new IndividualAddress(1, 1, 4);
	private static final IndividualAddress testDeviceCO = new IndividualAddress(1, 1, 5);

	// make sure its the same subnet as the test device (for tests that set the address)
	private static final IndividualAddress nonExisting = new IndividualAddress(1, 1, 200);

	private Util()
	{}

	private static boolean printToSystemOut;

	/**
	 * Standard out desc and toHexDec(bytes).
	 *
	 * @param desc description
	 * @param bytes bytes to print hex and decimal
	 */
	public static void out(final String desc, final byte[] bytes)
	{
		if (printToSystemOut)
			System.out.println(desc + ": " + toHexDec(bytes));
	}

	public static void out(final Object print)
	{
		if (printToSystemOut)
			System.out.println(print.toString());
	}

	/**
	 * Format first 200 bytes into hex, followed by decimal presentation.
	 *
	 * @param bytes bytes to format
	 * @return formatted bytes as string
	 */
	public static String toHexDec(final byte[] bytes)
	{
		final StringBuilder buf = new StringBuilder();
		final int max = Math.min(200, bytes.length);
		for (int i = 0; i < max; ++i) {
			final String hex = Integer.toHexString(bytes[i] & 0xff);
			if (hex.length() == 1)
				buf.append("0");
			buf.append(hex);
			buf.append(" ");
		}
		if (max < bytes.length)
			buf.append("...");
		buf.append("(");
		for (int i = 0; i < max; ++i) {
			final String no = Integer.toString(bytes[i] & 0xff);
			buf.append(no);
			if (i < bytes.length - 1)
				buf.append(" ");
		}
		if (max < bytes.length)
			buf.append("...");
		buf.append(")");
		return buf.toString();
	}

	/**
	 * Returns KNXnet/IP router address used for testing.
	 * <p>
	 *
	 * @return router individual address
	 */
	public static IndividualAddress getRouterAddress()
	{
		if (device == null) {
			final Discoverer d;
			try {
				d = new Discoverer(getLocalHost().getAddress(), getLocalHost().getPort(), false, false);
				d.startSearch(2, true);
				if (d.getSearchResponses().size() == 0)
					return null;
				device = d.getSearchResponses().get(0).getResponse().getDevice().getAddress();
			}
			catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		return device;
	}

	/**
	 * @return the individual address of the remote KNX device used for tests
	 */
	public static IndividualAddress getKnxDevice()
	{
		return testDeviceCL;
	}

	/**
	 * @return the individual address of the remote KNX device used for tests, device supports Layer
	 *         4 connection-oriented mode
	 */
	public static IndividualAddress getKnxDeviceCO()
	{
		return testDeviceCO;
	}

	/**
	 * @return the individual address for a non-existing KNX device used for tests
	 */
	public static IndividualAddress getNonExistingKnxDevice()
	{
		return nonExisting;
	}

	/**
	 * @return the local host used for testing
	 */
	public static InetSocketAddress getLocalHost()
	{
		return new InetSocketAddress(0);
	}

	/**
	 * @return the local endpoint used for connecting to the test server
	 */
	public static InetSocketAddress localEndpoint()
	{
		return new InetSocketAddress(0);
	}

	/**
	 * @return the local network interface used for connecting to the test server
	 * @throws SocketException
	 */
	public static NetworkInterface localInterface() throws SocketException
	{
		return NetworkInterface.getByInetAddress(onSameSubnet(getServer().getAddress()).get());
	}

	// finds a local IPv4 address with its network prefix "matching" the remote address
	private static Optional<InetAddress> onSameSubnet(final InetAddress remote)
	{
		try {
			return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
					.flatMap(ni -> ni.getInterfaceAddresses().stream())
					.filter(ia -> ia.getAddress() instanceof Inet4Address).filter(ia -> matchesPrefix(ia, remote))
					.map(InterfaceAddress::getAddress).findFirst();
		}
		catch (final SocketException ignore) {}
		return Optional.empty();
	}

	private static boolean matchesPrefix(final InterfaceAddress ia, final InetAddress remote)
	{
		final byte[] a1 = ia.getAddress().getAddress();
		final byte[] a2 = remote.getAddress();
		final long mask = (0xffffffffL >> ia.getNetworkPrefixLength()) ^ 0xffffffffL;
		for (int i = 0; i < a1.length; i++) {
			final int byteMask = (int) ((mask >> (24 - 8 * i)) & 0xff);
			if ((a1[i] & byteMask) != (a2[i] & byteMask))
				return false;
		}
		return true;
	}

	// we initially assume that our test server was started
	private static boolean testServerRunning = true;
	// identify test server among other interfaces that might lurk around
	private static final String testServerId = "calimero-core knx test-server";

	/**
	 * Returns the socket address of the KNXnet/IP router to use for testing.
	 *
	 * @return socket address
	 */
	public synchronized static InetSocketAddress getServer()
	{
		// we try once to find our running test server, on failure subsequent calls will
		// immediately return to speed up tests
		if (!testServerRunning)
			fail("no KNXnet/IP test-server available!");
		if (server == null) {
			testServerRunning = false;
			final Discoverer d = new Discoverer(null, 0, false, false);
			try {
				d.startSearch(2, true);
			}
			catch (final InterruptedException e) {
				e.printStackTrace();
			}
			for (final Result<SearchResponse> r : d.getSearchResponses()) {
				final SearchResponse res = r.getResponse();
				if (testServerId.equals(res.getDevice().getName())) {
					final InetAddress addr = res.getControlEndpoint().getAddress();
					server = new InetSocketAddress(addr, res.getControlEndpoint().getPort());
					device = res.getDevice().getAddress();
					testServerRunning = true;
					return server;
				}
			}
			System.err.println("\nA unit test case requests the KNX test server, but no running instance was found!\n"
					+ "\t--> Most tests requiring KNXnet/IP will fail.\n");
			fail("no KNXnet/IP test-server found!");
		}
		return server;
	}

	/**
	 * Returns the serial port identifier to use for testing the FT1.2 protocol.
	 *
	 * @return port ID, {@code null} if no ID found
	 */
	public static String getSerialPortID()
	{
		final String[] ids = SerialConnectionFactory.portIdentifiers().toArray(String[]::new);
		return ids.length > 0 ? ids[0] : null;
	}

	/**
	 * @return the base directory used for unit testing
	 */
	public static String getPath()
	{
		return "test/resources/";
	}

	private static final Path temp;
	static {
		try {
			temp = Files.createTempDirectory("calimero-junit-");
			temp.toFile().deleteOnExit();
		}
		catch (final IOException e) {
			throw new RuntimeException("could not create temp directory", e);
		}
	}

	/**
	 * @return the base output directory used for unit testing
	 */
	public static String getTargetPath()
	{
		return temp.toString() + "/";
	}
}
