/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2022 B. Malinowsky

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

package tuwien.auto.calimero.mgmt;

import java.util.Objects;
import java.util.function.Consumer;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.secure.Security;

/**
 * Property adapter for remote property services.
 *
 * @author B. Malinowsky
 */
public class RemotePropertyServiceAdapter implements PropertyAdapter
{
	private final ManagementClient mc;
	private final Destination dst;
	private final byte[] key;
	private final int accessLevel;

	private final Consumer<CloseEvent> adapterClosed;
	private final NetworkLinkListener nll = new NetworkLinkListener() {
		@Override
		public void linkClosed(final CloseEvent e) {
			adapterClosed.accept(new CloseEvent(RemotePropertyServiceAdapter.this, e.getInitiator(), e.getReason()));
		}
	};


	/**
	 * Creates a new property adapter for remote property access, using {@link Security#defaultInstallation()} for
	 * KNX Data Secure property services.
	 *
	 * @param link KNX network link used for communication with the KNX network
	 * @param remote KNX individual address to access its interface objects
	 * @param adapterClosed receives notification about adapter close event, which happens if {@code link} got closed
	 * @param connOriented {@code true} to use connection-oriented mode for access,
	 *        {@code false} to use connectionless mode
	 * @throws KNXLinkClosedException if the network link is closed
	 */
	public RemotePropertyServiceAdapter(final KNXNetworkLink link,
		final IndividualAddress remote, final Consumer<CloseEvent> adapterClosed,
		final boolean connOriented) throws KNXLinkClosedException
	{
		mc = new ManagementClientImpl(link);
		dst = mc.createDestination(remote, connOriented);
		key = new byte[0];
		accessLevel = 15;

		this.adapterClosed = Objects.requireNonNull(adapterClosed);
		link.addLinkListener(nll);
	}

	/**
	 * Creates a new property adapter for remote property access in connection-oriented
	 * mode with {@link ManagementClient#authorize authorization}, using {@link Security#defaultInstallation()} for
	 * KNX Data Secure property services.
	 *
	 * @param link KNX network link used for communication with the KNX network
	 * @param remote KNX individual address to access its interface objects
	 * @param adapterClosed receives notification about adapter close event, which happens if {@code link} got closed
	 * @param authorizeKey authorization key to obtain a specific access level using {@link ManagementClient#authorize}
	 *        for certain get or set operations, {@code authorizeKey.length = 4}
	 * @throws KNXLinkClosedException if the network link is closed
	 * @throws KNXException on failure during authorization
	 * @throws InterruptedException on interrupted thread
	 */
	public RemotePropertyServiceAdapter(final KNXNetworkLink link,
			final IndividualAddress remote, final Consumer<CloseEvent> adapterClosed,
			final byte[] authorizeKey) throws KNXException, InterruptedException {
		this(link, remote, Security.defaultInstallation(), true, validateAuthKeyLength(authorizeKey), adapterClosed);
	}

	/**
	 * Creates a new property adapter for remote property access, using {@code security} for KNX Data Secure property
	 * services, and optionally {@link ManagementClient#authorize authorization}.
	 * Note, that access control and authorization are not mutually exclusive, and can be used together
	 * in a device implementing KNX Data Security.
	 *
	 * @param link KNX network link used for communication with the KNX network
	 * @param remote KNX individual address to access its interface objects
	 * @param security security containing the device tool key to use for KNX Data Secure property services with the
	 *        {@code remote} endpoint
	 * @param connOriented {@code true} to use connection-oriented mode for access,
	 *        {@code false} to use connectionless mode
	 * @param authorizeKey authorization key to obtain a specific access level using {@link ManagementClient#authorize}
	 *        for certain get or set operations, {@code authorizeKey.length = 4};
	 *        use {@code authorizeKey.length = 0} to skip authorization
	 * @param adapterClosed receives notification about adapter close event
	 * @throws KNXLinkClosedException if the network link is closed
	 * @throws InterruptedException on interrupted thread
	 */
	public RemotePropertyServiceAdapter(final KNXNetworkLink link, final IndividualAddress remote,
			final Security security, final boolean connOriented, final byte[] authorizeKey,
			final Consumer<CloseEvent> adapterClosed) throws KNXException, InterruptedException {
		mc = new ManagementClientImpl(link, security);
		dst = mc.createDestination(remote, connOriented);
		key = authorizeKey.clone();
		accessLevel = authorize();

		this.adapterClosed = Objects.requireNonNull(adapterClosed);
		link.addLinkListener(nll);
	}

	/**
	 * @return the access level granted by the remote communication endpoint, or minimum access rights
	 *         (implementation note: access level 15) if remote property access was not authorized
	 */
	public final int accessLevel() { return accessLevel; }

	public ManagementClient managementClient() { return mc; }

	public Destination destination() { return dst; }

	@Override
	public void setProperty(final int objIndex, final int pid, final int start,
		final int elements, final byte... data) throws KNXException, InterruptedException
	{
		mc.writeProperty(dst, objIndex, pid, start, elements, data);
	}

	@Override
	public byte[] getProperty(final int objIndex, final int pid, final int start,
		final int elements) throws KNXException, InterruptedException
	{
		return mc.readProperty(dst, objIndex, pid, start, elements);
	}

	@Override
	public byte[] getDescription(final int objIndex, final int pid, final int propIndex)
		throws KNXException, InterruptedException
	{
		return mc.readPropertyDesc(dst, objIndex, pid, propIndex);
	}

	/**
	 * {@inheritDoc} The name for this adapter starts with "remote PS " + remote KNX
	 * individual address, allowing easier distinction of adapter types.
	 */
	@Override
	public String getName()
	{
		return "remote PS " + dst.getAddress();
	}

	@Override
	public boolean isOpen()
	{
		return mc.isOpen();
	}

	@Override
	public void close()
	{
		final KNXNetworkLink lnk = mc.detach();
		if (lnk != null)
			lnk.removeLinkListener(nll);
	}

	private int authorize() throws InterruptedException, KNXException {
		if (key.length == 0)
			return 15;
		try {
			return mc.authorize(dst, key);
		}
		catch (final KNXException e) {
			close();
			throw e;
		}
	}

	private static byte[] validateAuthKeyLength(final byte[] authorizeKey) {
		if (authorizeKey.length == 4) // also covers null ptr
			return authorizeKey;
		throw new KNXIllegalArgumentException("length of authorize key not 4 bytes");
	}
}
