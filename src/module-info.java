/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2022, 2022 B. Malinowsky

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

/**
 * Defines the Calimero API.
 *
 * @uses io.calimero.serial.SerialConnectionFactory
 * @uses io.calimero.serial.usb.spi.UsbConnectionProvider
 */
module io.calimero.core {
	requires static java.xml;
	requires transitive org.slf4j;

	exports io.calimero;
	exports io.calimero.baos;
	exports io.calimero.baos.ip;
	exports io.calimero.buffer.cache;
	exports io.calimero.buffer;
	exports io.calimero.cemi;
	exports io.calimero.datapoint;
	exports io.calimero.dptxlator;
	exports io.calimero.knxnetip;
	exports io.calimero.knxnetip.servicetype;
	exports io.calimero.knxnetip.util;
	exports io.calimero.link;
	exports io.calimero.link.medium;
	exports io.calimero.log;
	exports io.calimero.mgmt;
	exports io.calimero.process;
	exports io.calimero.secure;
	exports io.calimero.serial;
	exports io.calimero.serial.spi;
	exports io.calimero.serial.usb;
	exports io.calimero.serial.usb.spi;
	exports io.calimero.xml;

	exports io.calimero.internal to
		io.calimero.device,
		io.calimero.server,
		io.calimero.usb;

	opens io.calimero.link to
		io.calimero.device; // AbstractLink.conn

	uses io.calimero.serial.spi.SerialConnectionProvider;
	uses io.calimero.serial.usb.spi.UsbConnectionProvider;
}
