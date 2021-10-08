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

package tuwien.auto.calimero;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Experimental.
 */
public final class LteHeeTag {
	private final Type type;
	private final int tag;


	// currently only geo tags with wildcards, broadcast, and unassigned (peripheral) in hex notation is supported
	public static LteHeeTag from(final String tag) {
		final String[] split = tag.replaceAll("\\*", "0").split("/", -1);
		final List<Integer> levels = Arrays.stream(split).map(Integer::decode).collect(Collectors.toList());
		if (levels.size() == 1)
			return new LteHeeTag(0x04 | 3, new GroupAddress(levels.get(0)));
		return geoTag(levels.get(0), levels.get(1), levels.get(2));
	}

	public static LteHeeTag from(final int eff, final GroupAddress addr) {
		return new LteHeeTag(eff, addr);
	}

	public static LteHeeTag geoTag(final int floor, final int room, final int subzone) {
		return new LteHeeTag(floor, room, subzone);
	}

	public static LteHeeTag hvacTag(final HvacType type, final int segmentOrZone) {
		return new LteHeeTag(type, segmentOrZone);
	}

	private LteHeeTag(final int eff, final GroupAddress addr) {
		if ((eff & 0x04) == 0)
			throw new KNXIllegalArgumentException("not an LTE-HEE address");

		type = Type.values()[eff & 0x03];
		tag = addr.getRawAddress();
	}

	private LteHeeTag(final int floor, final int room, final int subzone) {
		final boolean upper = floor >= 0x40;
		type = upper ? Type.UpperGeo : Type.LowerGeo;
		tag = (upper ? floor - 0x40 : floor) << 10 | room << 4 | subzone;
	}

	private LteHeeTag(final HvacType type, final int segmentOrZone) {
		this.type = Type.App;
		int hvacTypeId = 0;
		for (final var e : HvacType.map.entrySet()) {
			if (e.getValue().equals(type))
				hvacTypeId = e.getKey();
		}

		tag = hvacTypeId << 5 | segmentOrZone;
	}

	public enum Type { LowerGeo, UpperGeo, App, Unassigned }

	public Type type() { return type; }

	public boolean broadcast() { return tag == 0; }



	// accessors for lower/upper geographical tags

	public int floor() {
		int aptFloor = (tag & 0b1111110000000000) >> 10;
		if (aptFloor != 0)
			aptFloor += type == Type.LowerGeo ? 0 : 0x40;
		return aptFloor;
	}

	public int room() { return (tag & 0b1111110000) >> 4; }

	public int subzone() { return tag & 0b1111; }



	// accessors for application-specific tags

	public int appDomain() { return tag & 0xf000; }

	public enum HvacType { Reserved, HotWaterDistribution, ColdWaterDistribution, VentDistribution, DhwZone,
		OutsideSensorZone, CalendarZone, ProdSegHotWater, ProdSegColdWater;

		private static final Map<Integer, HvacType> map = Map.of(1, HotWaterDistribution, 2, ColdWaterDistribution,
				3, VentDistribution, 4, DhwZone, 5, OutsideSensorZone, 6, CalendarZone, 0x10, ProdSegHotWater,
				0x20, ProdSegColdWater);

		public String friendly() { return name().replaceAll("([A-Z])", " $1").trim(); }
	}

	public HvacType hvacType() {
		final int mapping = tag >> 5;
		return HvacType.map.getOrDefault(mapping, HvacType.Reserved);
	}

	public int hvacZone() { return tag & 0b11111; }

	public int distributionSegment() { return tag & 0b11111; }

	public int producerSegment() { return (tag >> 5) & 0xf; }

	public int producer() { return tag & 0b11111; }



	// accessors for unassigned (peripheral) tags

	// ??? maybe configurableTag
	public int unassignedTag() { return tag & 0xfff; }



	public GroupAddress toGroupAddress() { return new GroupAddress(tag); }

	@Override
	public String toString() {
		if (tag == 0)
			return "broadcast";

		switch (type()) {
		case LowerGeo:
		case UpperGeo:
			return "floor/room/subzone " + wildcard(floor()) + "/" + wildcard(room()) + "/" + wildcard(subzone());

		case App:
			if (appDomain() != 0)
				return appDomain() + "/0x" + Integer.toHexString(unassignedTag()) + " (app)";

			final var sb = new StringBuilder("HVAC ").append(hvacType()).append(' ');
			switch (hvacType()) {
			case ProdSegHotWater:
			case ProdSegColdWater:
				// producer segment doesn't have wildcards
				return sb.append(producerSegment()).append(' ').append(wildcard(hvacZone())).toString();

			case Reserved:
				return sb.append(String.format("0b%8s", Integer.toBinaryString(unassignedTag())).replace(' ', '0'))
						.toString();

			default:
				return sb.append(wildcard(hvacZone())).toString();
			}

		case Unassigned:
			return "unassigned tag 0x" + Integer.toHexString(unassignedTag());

		default: throw new KnxRuntimeException("" + type());
		}
	}

	private static String wildcard(final int zone) { return zone == 0 ? "*" : "" + zone; }
}
