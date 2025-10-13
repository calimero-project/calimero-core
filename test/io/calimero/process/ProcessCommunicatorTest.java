/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2006, 2025 B. Malinowsky

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

package io.calimero.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.calimero.DetachEvent;
import io.calimero.GroupAddress;
import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;
import io.calimero.KNXTimeoutException;
import io.calimero.Priority;
import io.calimero.Util;
import io.calimero.datapoint.Datapoint;
import io.calimero.datapoint.StateDP;
import io.calimero.dptxlator.DPTXlator2ByteFloat;
import io.calimero.dptxlator.DPTXlator4ByteFloat;
import io.calimero.dptxlator.DPTXlator8BitUnsigned;
import io.calimero.dptxlator.DPTXlatorString;
import io.calimero.internal.Executor;
import io.calimero.link.KNXNetworkLink;
import io.calimero.link.KNXNetworkLinkIP;
import io.calimero.link.medium.TPSettings;
import tag.KnxnetIP;

@KnxnetIP
@ResourceLock("calimero.datapoint")
class ProcessCommunicatorTest {
	private ProcessCommunicator pc;
	private ProcessCommunicator pc2;
	private KNXNetworkLink link;

	private final GroupAddress dpBool;
	private final GroupAddress dpBool2;
	private final GroupAddress dpControl;
	private final GroupAddress dpUnsigned1;
	private final GroupAddress dpString;
	private final GroupAddress dpFloat2;
	private final GroupAddress dpFloat4;

	private final String dpStringValue = "Hello KNX!";

	ProcessCommunicatorTest() throws KNXFormatException {
		dpBool = new GroupAddress("1/0/1");
		dpBool2 = new GroupAddress("1/0/11");
		dpControl = new GroupAddress("1/0/2");
		dpUnsigned1 = new GroupAddress("1/0/3");
		dpString = new GroupAddress("1/0/5");
		dpFloat2 = new GroupAddress("1/0/6");
		dpFloat4 = new GroupAddress("1/0/7");
	}

	@BeforeEach
	void init() throws Exception {
		link = KNXNetworkLinkIP.newTunnelingLink(Util.getLocalHost(), Util.getServer(), false, new TPSettings());
		pc = new ProcessCommunicatorImpl(link);
		pc2 = new ProcessCommunicatorImpl(link);
	}

	@AfterEach
	void tearDown() {
		if (pc != null)
			pc.detach();
		if (pc2 != null)
			pc2.detach();
		if (link != null)
			link.close();
	}

	@Test
	void setResponseTimeout() {
		final var two = Duration.ofSeconds(2);
		pc.responseTimeout(two);
		assertEquals(two, pc.responseTimeout());
		assertThrows(KNXIllegalArgumentException.class, () -> pc.responseTimeout(Duration.ZERO));
		assertEquals(two, pc.responseTimeout());

		final var five = Duration.ofSeconds(5);
		pc.responseTimeout(five);
		assertEquals(five, pc.responseTimeout());
	}

	@Test
	void defaultResponseTimeout() {
		// test for correct standard timeout
		assertEquals(5, pc.responseTimeout().toSeconds());
	}

	@Test
	void setPriority() {
		pc.setPriority(Priority.SYSTEM);
		assertEquals(Priority.SYSTEM, pc.getPriority());

		pc.setPriority(Priority.LOW);
		assertEquals(Priority.LOW, pc.getPriority());
	}

	@Test
	void getPriority() {
		// test for default priority
		assertEquals(Priority.LOW, pc.getPriority());
	}

	@Test
	void addProcessListener() {
		final ProcessListener l = new ProcessListener() {
			@Override
			public void groupReadRequest(final ProcessEvent e) {}

			@Override
			public void groupReadResponse(final ProcessEvent e) {}

			@Override
			public void groupWrite(final ProcessEvent e) {}

			@Override
			public void detached(final DetachEvent e) {}
		};
		pc.addProcessListener(l);
		pc.removeProcessListener(l);

		pc.addProcessListener(l);
		pc.addProcessListener(l);
	}

	@Test
	void removeProcessListener() {
		final ProcessListener l = new ProcessListener() {
			@Override
			public void groupReadRequest(final ProcessEvent e) {}

			@Override
			public void groupReadResponse(final ProcessEvent e) {}

			@Override
			public void groupWrite(final ProcessEvent e) {}

			@Override
			public void detached(final DetachEvent e) {}
		};
		pc.removeProcessListener(l);
		pc.addProcessListener(l);
		pc.removeProcessListener(l);
		pc.removeProcessListener(l);
	}

	@Test
	void readBool() throws KNXException, InterruptedException {
		// test concurrent read, needs breakpoints in waitForResponse method
		// useful to see behavior when more than one indication is in the queue

		// read from same address
		// if we are testing with a virtual network make sure we have some value set
		pc.write(dpBool, true);

		final Runnable task = () -> {
			try {
				pc2.readBool(dpBool);
			}
			catch (KNXException | InterruptedException e) {
				fail(Thread.currentThread().getName() + ": read bool");
			}
		};
		Executor.execute(task, "testReadBool Concurrent 1");
		pc.readBool(dpBool);

		// read from different address
		// if we are testing with a virtual network make sure we have some value set
		pc.write(dpBool2, false);
		Executor.execute(task, "testReadBool Concurrent 2");
		pc.readBool(dpBool2);

		// read from different address using same process communicator
		final Runnable task2 = () -> {
			try {
				pc.readBool(dpBool);
			}
			catch (KNXException | InterruptedException e) {
				fail(Thread.currentThread().getName() + ": " + e);
			}
		};
		Executor.execute(task2, "testReadBool Concurrent 3");
		pc.readBool(dpBool2);
	}

	@Test
	void writeGroupAddressBoolean() throws KNXException, InterruptedException {
		// read from same address
		// if we are testing with a virtual network make sure we have some value set
		pc.write(dpBool, true);
		Thread.sleep(100);
		assertTrue(pc.readBool(dpBool));
		pc.write(dpBool, false);
		Thread.sleep(100);
		assertFalse(pc.readBool(dpBool));
	}

	@Test
	void readUnsigned() throws KNXException, InterruptedException {
		// read from same address
		pc.readUnsigned(dpUnsigned1, ProcessCommunication.SCALING);
	}

	@Test
	void writeGroupAddressIntString() throws KNXException, InterruptedException {
		final int v = 80;
		pc.write(dpUnsigned1, v, ProcessCommunication.SCALING);
		Thread.sleep(100);
		final int i = pc.readUnsigned(dpUnsigned1, ProcessCommunication.SCALING);
		assertEquals(v, i);
	}

	@Test
	void readControl() throws KNXException, InterruptedException {
		pc.readControl(dpControl);
	}

	@Test
	void writeGroupAddressBooleanInt() throws KNXException {
		final GroupAddress addr = new GroupAddress(1, 0, 1);
		pc.write(addr, true, 4);
	}

	@Test
	void readFloat() throws KNXException, InterruptedException {
		final double f2 = pc.readFloat(dpFloat2);
		new DPTXlator2ByteFloat(DPTXlator2ByteFloat.DPT_RAIN_AMOUNT).setValue(f2);
		final double f4 = pc.readFloat(dpFloat4);
		new DPTXlator4ByteFloat(DPTXlator4ByteFloat.DPT_TEMPERATURE_DIFFERENCE).setValue((float) f4);
	}

	@Test
	void writeFloatingPoint() throws KNXException {
		final float f = (float) 0.01;
		pc.write(dpFloat2, f, false);
		pc.write(dpFloat4, f, true);
	}

	@Test
	void readString() throws KNXException, InterruptedException {
		final String s = pc.readString(dpString);
		assertTrue(s.length() > 0);
		assertEquals(dpStringValue, s);
	}

	@Test
	void writeGroupAddressString() throws KNXException, InterruptedException {
		pc.write(dpString, "test");
		pc.write(dpString, "test2");
		Thread.sleep(100);
		assertEquals("test2", pc.readString(dpString));
		pc.write(dpString, dpStringValue);
	}

	@Test
	void read() throws KNXException, InterruptedException {
		final Datapoint dp = new StateDP(dpString, "test datapoint", 0, DPTXlatorString.DPT_STRING_8859_1.getID());
		final String res = pc2.read(dp);
		assertTrue(res.length() > 0);
	}

	@Test
	void concurrentRead() throws InterruptedException, ExecutionException {
		final Datapoint dp = new StateDP(dpString, "test datapoint", 0, DPTXlatorString.DPT_STRING_8859_1.getID());

		final Callable<Integer> task = () -> pc2.read(dp).length() > 0 ? 1 : 0;
		final var tasks = Collections.nCopies(10, task);
		final var all = Executor.executor().invokeAll(tasks);
		int count = 0;
		for (final var f : all)
			count += f.get();
		assertEquals(tasks.size(), count);
	}

	@Test
	void concurrentRead2() throws KNXException, InterruptedException, ExecutionException {
		final boolean b = pc2.readBool(dpBool);
		final double d = pc2.readFloat(dpFloat2);

		final Callable<Integer> task1 = () -> pc2.readBool(dpBool) == b ? 1 : 0;
		final Callable<Integer> task2 = () -> pc2.readFloat(dpFloat2) == d ? 1 : 0;

		final List<Callable<Integer>> tasks = new ArrayList<>();
		tasks.addAll(Collections.nCopies(10, task1));
		tasks.addAll(Collections.nCopies(10, task2));
		final var all = Executor.executor().invokeAll(tasks);
		int count = 0;
		for (final var f : all)
			count += f.get();
		assertEquals(tasks.size(), count);
	}

	@Test
	void concurrentReadNonExistingDestination() throws InterruptedException {
		final LocalTime start = LocalTime.now();

		final Callable<Integer> task = () -> pc2.readBool(new GroupAddress(7, 7, 7)) ? 1 : 1;
		final var tasks = Collections.nCopies(5, task);
		final var all = Executor.executor().invokeAll(tasks);
		for (final var f : all) {
			final var t = assertThrows(ExecutionException.class, f::get);
			assertEquals(KNXTimeoutException.class, t.getCause().getClass());
		}

		final LocalTime now = LocalTime.now();
		final var timeout = pc2.responseTimeout();
		assertTrue(now.isAfter(start.plus(timeout)));
		assertTrue(now.isBefore(start.plus(timeout).plusSeconds(2)));
	}

	@Test
	void writeDatapointString() throws KNXException {
		final Datapoint dp = new StateDP(dpUnsigned1, "test datapoint", 0, DPTXlator8BitUnsigned.DPT_PERCENT_U8.getID());
		pc2.write(dp, "80");
	}

	@Test
	void detach() {
		final KNXNetworkLink ret = pc.detach();
		assertEquals(link, ret);
	}
}
