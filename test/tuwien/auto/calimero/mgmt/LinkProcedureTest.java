/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2016 B. Malinowsky

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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tag.KnxnetIP;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.Util;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.medium.PLSettings;

@KnxnetIP
class LinkProcedureTest
{
	private KNXNetworkLink link;
	private ManagementClient mgmt;
	private final IndividualAddress self = new IndividualAddress(0);

	@BeforeEach
	void init() throws KNXException, InterruptedException
	{
		link = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, new PLSettings());
		mgmt = new ManagementClientImpl(link);
	}

	@AfterEach
	void tearDown()
	{
		mgmt.detach();
		link.close();
	}

	@Test
	void runActuator()
	{
		final Destination d = mgmt.createDestination(Util.getKnxDevice(), false);
		final int CH_PB_Toggle = 2;
		final LinkProcedure lp = LinkProcedure.forActuator(mgmt, self, d, CH_PB_Toggle);
		lp.setLinkFunction(this::onSetDeleteLink);
		lp.run();
	}

	private int onSetDeleteLink(final int flags, final Map<Integer, GroupAddress> groupObjects)
	{
//		System.out.println("set delete link: flags " + flags + ", group objects " + groupObjects);
		return LinkProcedure.LinkAdded;
	}

	@Test
	void runSensor()
	{
		final Destination d = mgmt.createDestination(Util.getKnxDevice(), false);
		final Map<Integer, GroupAddress> groupObjects = new HashMap<>();
		final int CC_Switch_OnOff = 1;
		final int CC_Dimming_Ctrl = 5;
		groupObjects.put(CC_Switch_OnOff, new GroupAddress(7, 3, 10));
		groupObjects.put(CC_Dimming_Ctrl, new GroupAddress(7, 3, 11));
		final LinkProcedure lp = LinkProcedure.forSensor(mgmt, self, d, false, 0xbeef, groupObjects);
		lp.setLinkFunction(this::onLinkResponse);
		lp.run();
	}

	private int onLinkResponse(final int flags, final Map<Integer, GroupAddress> groupObjects)
	{
//		System.out.println("link response: flags " + flags + ", group objects " + groupObjects);
		return 0;
	}
}
