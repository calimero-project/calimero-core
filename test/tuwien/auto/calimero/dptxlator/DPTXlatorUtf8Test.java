/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2016 B. Malinowsky

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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.Assert;

import junit.framework.TestCase;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.Util;

/**
 * @author B. Malinowsky
 */
public class DPTXlatorUtf8Test extends TestCase
{
	private DPTXlator t;

	private final String string1 = "test1";
	private final String string2 = "test2";
	private final String empty = "";
	// ab cd ef
	private final String nonASCII = "ab\u00fccd\u00a7ef";
	// ab[uppercase weird A]cd[lowercase greek a]
	private final String nonLatin = "ab\u0100cd\u03b1";

	private final byte[] dataEmpty = new byte[1];
	private final byte[] data1 = new byte[6];
	private final byte[] data2 = new byte[6];

	private byte[] data;
	private final String[] strings = new String[] { string1, string2, nonASCII, nonLatin };

	/**
	 * @param name
	 */
	public DPTXlatorUtf8Test(final String name)
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
		TranslatorTypes.createTranslator(TranslatorTypes.TYPE_UTF8, "28.001");
		t = new DPTXlatorUtf8(DPTXlatorUtf8.DPT_UTF8);

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

		final byte[] nonAsciiBytes = nonASCII.getBytes("utf-8");
		final byte[] nonLatinBytes = nonLatin.getBytes("utf-8");
		final int nonAsciiLength = nonAsciiBytes.length;
		final int nonLatinLength = nonLatinBytes.length;
		data = new byte[data1.length + data2.length + nonAsciiLength + nonLatinLength + 2];
		int k = 0;
		for (int i = 0; i < data1.length; ++i)
			data[k++] = data1[i];
		for (int i = 0; i < data2.length; ++i)
			data[k++] = data2[i];
		for (int i = 0; i < nonAsciiLength; ++i)
			data[k++] = nonAsciiBytes[i];
		data[k++] = 0;
		for (int i = 0; i < nonLatinLength; ++i)
			data[k++] = nonLatinBytes[i];
		data[k++] = 0;
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorUtf8#getAllValues()}.
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
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlatorUtf8#setData(byte[], int)}.
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
			t.setData(new byte[1024*1024*10], 0);
			fail("insane data length");
		}
		catch (final KNXIllegalArgumentException e) {
			// ok
		}
		assertTrue(Arrays.equals(data, t.getData()));
		final byte[] dataOffset = new byte[9];
		System.arraycopy(data1, 0, dataOffset, 3, data1.length);
		t.setData(dataOffset, 3);
		byte[] d = t.getData();
		assertEquals(6, d.length);
		assertTrue(Arrays.equals(data1, d));

		final byte[] array = new byte[data.length + 1];
		System.arraycopy(data, 0, array, 1, data.length);
		t.setData(array, 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		assertTrue(Arrays.equals(data, d));
		assertEquals(4, t.getItems());
		assertArrayEquals(strings, t.getAllValues());
	}

	/**
	 * Test method for
	 * {@link tuwien.auto.calimero.dptxlator.DPTXlator#setValues(java.lang.String[])}.
	 *
	 * @throws KNXFormatException
	 * @throws UnsupportedEncodingException
	 */
	public final void testSetValues() throws KNXFormatException, UnsupportedEncodingException
	{
		final String signs = "ÆÐÑØý";
		final String german = "ü ö ä Ä Ü Ö ß";
		final String greek = "ΕΘΨθψϘϨϸ";
		final String[] values = new String[] { signs, german, greek };
		t.setValues(values);
		final byte[] utfdata = signs.getBytes("utf-8");
		final byte[] utfdata2 = german.getBytes("utf-8");
		final byte[] utfdata3 = greek.getBytes("utf-8");
		final byte[] data = new byte[utfdata.length + utfdata2.length + utfdata3.length + 3];
		System.arraycopy(utfdata, 0, data, 0, utfdata.length);
		System.arraycopy(utfdata2, 0, data, utfdata.length + 1, utfdata2.length);
		System.arraycopy(utfdata3, 0, data, utfdata.length + 1 + utfdata2.length + 1,
				utfdata3.length);
		Assert.assertArrayEquals(data, t.getData());
		assertArrayEquals(values, t.getAllValues());
		System.out.println(new String(t.getData(), "utf-8"));

		t.setValues(new String[0]);
		Assert.assertArrayEquals(data, t.getData());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()}.
	 */
	public final void testGetNumericValue()
	{
		try {
			t.getNumericValue();
			fail("no numeric representation");
		}
		catch (final KNXFormatException expected) {
			// we're good
		}
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getData(byte[], int)}.
	 */
	public final void testGetDataByteArrayInt()
	{
		assertEquals(25, t.getData(new byte[25], 4).length);
		final byte[] buf = new byte[20];
		assertTrue(Arrays.equals(buf, t.getData(new byte[20], 3)));

		t.setData(data);
		final byte[] d = new byte[45];
		Arrays.fill(d, (byte) 0xCC);
		t.getData(d, 2);
		for (int i = 0; i < 2; i++)
			assertEquals((byte) 0xCC, d[i]);
		for (int i = 2; i < data.length + 2; i++)
			assertEquals(data[i - 2], d[i]);
		for (int i = data.length + 2; i < 45; i++)
			assertEquals((byte) 0xCC, d[i]);
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getItems()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetItems() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		t.setValue(string1);
		assertEquals(1, t.getItems());
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
	}

	/**
	 * Test method for {@link tuwien.auto.calimero.dptxlator.DPTXlator#getTypeSize()}.
	 *
	 * @throws KNXFormatException
	 */
	public final void testGetTypeSize() throws KNXFormatException
	{
		assertEquals(1, t.getTypeSize());
		t.setValue(string1);
		// XXX that's actually not intuitive
		assertEquals(1, t.getTypeSize());
		t.setValues(strings);
		assertEquals(1, t.getTypeSize());
		//assertEquals(string1.length() + string2.length() + nonASCII.length() + nonLatin.length(),
		//		xlator.getTypeSize());
	}

	private void assertArrayEquals(final String[] exp, final String[] actual)
	{
		assertEquals(exp.length, actual.length);
		for (int i = 0; i < exp.length; ++i)
			assertEquals(exp[i], actual[i]);
	}
}
