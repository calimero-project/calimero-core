/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2016 B. Malinowsky

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

import java.util.Arrays;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class DPTXlatorStringTest extends TestCase
{
	private DPTXlatorString t;
	private final String string1 = "test1";
	private final String string2 = "test2";
	private final String max = "14 char-string";
	private final String empty = "";
	// ab cd ef
	private final String nonASCII = "ab\u00fccd\u00a7ef";
	// ab[uppercase weird A]cd[lowercase greek a]
	private final String nonLatin = "ab\u0100cd\u03b1";
	private final byte[] dataEmpty = new byte[14];
	private final byte[] data1 = new byte[14];
	private final byte[] data2 = new byte[14];

	private final byte[] dataMax =
		new byte[] { (byte) '1', (byte) '4', (byte) ' ', (byte) 'c', (byte) 'h',
			(byte) 'a', (byte) 'r', (byte) '-', (byte) 's', (byte) 't', (byte) 'r',
			(byte) 'i', (byte) 'n', (byte) 'g', };

	private final byte[] data = new byte[42];
	private final String[] strings = new String[] { string1, max, string2 };

	/**
	 * @param name name of test case
	 */
	public DPTXlatorStringTest(final String name)
	{
		super(name);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		Util.setupLogging("DPTXlator");
		t = new DPTXlatorString(DPTXlatorString.DPT_STRING_8859_1);
		data1[0] = (byte) 't';
		data1[1] = (byte) 'e';
		data1[2] = (byte) 's';
		data1[3] = (byte) 't';
		data1[4] = (byte) '1';

		data2[0] = (byte) 't';
		data2[1] = (byte) 'e';
		data2[2] = (byte) 's';
		data2[3] = (byte) 't';
		data2[4] = (byte) '2';

		for (int i = 0; i < 14; ++i)
			data[i] = data1[i];
		for (int i = 14; i < 28; ++i)
			data[i] = dataMax[i - 14];
		for (int i = 28; i < 42; ++i)
			data[i] = data2[i - 28];
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#setValues(java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValues() throws KNXFormatException
	{
		t.setValues(strings);
		String[] ret = t.getAllValues();
		assertArrayEquals(strings, ret);

		String[] buf = new String[] { t.getValue(), t.getValue() };
		t.setValues(buf);
		ret = t.getAllValues();
		assertArrayEquals(buf, ret);

		buf = new String[] { empty, max };
		t.setValues(buf);
		ret = t.getAllValues();
		assertArrayEquals(buf, ret);

		try {
			t.setValues(new String[] { "ok", "tooooooooo long" });
		}
		catch (final KNXFormatException e) {}
		assertArrayEquals(buf, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#getAllValues()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetAllValues() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		assertEquals(t.getItems(), t.getItems());
		assertEquals(empty, t.getAllValues()[0]);

		t.setData(data);
		assertArrayEquals(strings, t.getAllValues());
		assertEquals(t.getItems(), t.getItems());

		t.setValue(string1);
		assertEquals(1, t.getItems());
		assertEquals(string1, t.getAllValues()[0]);
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#setValue(java.lang.String)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testSetValue() throws KNXFormatException
	{
		t.setValue(string1);
		assertEquals(string1, t.getValue());
		final String s = t.getValue();
		t.setValue(s);
		assertEquals(string1, t.getValue());
		assertEquals(s, t.getValue());
		t.setValue(empty);
		assertEquals(empty, t.getValue());
		assertTrue(Arrays.equals(dataEmpty, t.getData()));
		t.setValue(max);
		assertEquals(max, t.getValue());

		try {
			t.setValue("tooooooooo long");
		}
		catch (final KNXFormatException e) {}
		assertEquals(max, t.getValue());

		final DPTXlatorString t2 = new DPTXlatorString(DPTXlatorString.DPT_STRING_ASCII);
		t2.setValue(nonASCII);
		assertEquals("ab?cd?ef", t2.getValue());
		final byte[] nonAsciiData = t2.getData();
		final byte[] expected =
			{ (byte) 'a', (byte) 'b', (byte) '?', (byte) 'c', (byte) 'd', (byte) '?',
				(byte) 'e', (byte) 'f', 0, 0, 0, 0, 0, 0 };
		assertTrue(Arrays.equals(expected, nonAsciiData));

		t.setValue(nonASCII);
		assertEquals(nonASCII, t.getValue());

		t.setValue(nonLatin);
		assertEquals("ab?cd?", t.getValue());

		t2.setValue(nonLatin);
		assertEquals("ab?cd?", t.getValue());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#setData(byte[], int)}.
	 */
	public final void testSetDataByteArrayInt()
	{
		t.setData(data, 0);
		try {
			t.setData(new byte[] {}, 0);
			fail("zero data length");
		}
		catch (final KNXIllegalArgumentException e) {
			// ok
		}
		try {
			t.setData(new byte[] {0, 1, 2, 3, 4, 5}, 0);
			fail("only 6 bytes data length");
		}
		catch (final KNXIllegalArgumentException e) {
			// ok
		}
		assertTrue(Arrays.equals(data, t.getData()));
		final byte[] dataOffset = new byte[28];
		System.arraycopy(data1, 0, dataOffset, 3, data1.length);
		try {
			t.setData(dataOffset, 3);
			fail("length 25 not an exact match");
		}
		catch (final KNXIllegalArgumentException expected) {}
		t.setData(Arrays.copyOf(dataOffset, 17), 3);
		byte[] d = t.getData();
		assertEquals(14, d.length);
		assertTrue(Arrays.equals(data1, d));

		final byte[] array = new byte[data.length + 8];
		System.arraycopy(data, 0, array, 1, data.length);
		try {
			t.setData(array, 1);
			fail("length 49 not an exact match");
		}
		catch (final KNXIllegalArgumentException expected) {}
		t.setData(Arrays.copyOf(array, data.length + 1), 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertTrue(Arrays.equals(data, d));
		assertArrayEquals(strings, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#getData(byte[], int)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetDataByteArrayInt() throws KNXFormatException
	{
		assertEquals(25, t.getData(new byte[25], 4).length);
		final byte[] buf = new byte[20];
		assertTrue(Arrays.equals(buf, t.getData(new byte[20], 3)));

		t.setData(data);
		byte[] d = new byte[45];
		Arrays.fill(d, (byte) 0xCC);
		t.getData(d, 2);
		for (int i = 0; i < 2; i++)
			assertEquals((byte) 0xCC, d[i]);
		for (int i = 2; i < 44; i++)
			assertEquals(data[i - 2], d[i]);
		for (int i = 44; i < 45; i++)
			assertEquals((byte) 0xCC, d[i]);

		for (int i = 0; i < strings.length; i++) {
			t.setValue(strings[i]);
			d = t.getData(new byte[14 + i], i);
			for (int k = 0; k < 14; ++k)
				assertEquals(data[14 * i + k], d[i + k]);
		}
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#getSubTypes()}.
	 */
	public final void testGetSubTypes()
	{
		assertEquals(2, t.getSubTypes().size());
		t.getSubTypes().containsKey(DPTXlatorString.DPT_STRING_ASCII.getID());
		t.getSubTypes().containsKey(DPTXlatorString.DPT_STRING_8859_1.getID());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlatorString#DPTXlatorString
	 * (tuwien.auto.calimero.dptxlator.DPT)}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testDPTXlatorStringDPT() throws KNXFormatException
	{
		new DPTXlatorString(DPTXlatorString.DPT_STRING_8859_1);
	}

	private void assertArrayEquals(final String[] exp, final String[] actual)
	{
		assertEquals(exp.length, actual.length);
		for (int i = 0; i < exp.length; ++i)
			assertEquals(exp[i], actual[i]);
	}
}
