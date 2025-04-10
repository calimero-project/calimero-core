/*
    Calimero 3 - A library for KNX network access
    Copyright (c) 2015, 2023 B. Malinowsky

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

package io.calimero.dptxlator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;


class DPTXlatorUtf8Test
{
	private DPTXlator t;

	private final String string1 = "test1";
	private final String string2 = "test2";
	private final String empty = "";
	// ab cd ef
	private final String nonASCII = "ab\u00fccd\u00a7ef";
	// ab[uppercase weird A]cd[lowercase greek a]
	private final String nonLatin = "ab\u0100cd\u03b1";

	private final byte[] data1 = new byte[6];
	private final byte[] data2 = new byte[6];

	private byte[] data;
	private final String[] strings = new String[] { string1, string2, nonASCII, nonLatin };


	@BeforeEach
	void init() throws Exception
	{
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

		final byte[] nonAsciiBytes = nonASCII.getBytes(StandardCharsets.UTF_8);
		final byte[] nonLatinBytes = nonLatin.getBytes(StandardCharsets.UTF_8);
		final int nonAsciiLength = nonAsciiBytes.length;
		final int nonLatinLength = nonLatinBytes.length;
		data = new byte[data1.length + data2.length + nonAsciiLength + nonLatinLength + 2];
		int k = 0;
		for (final byte value : data1)
			data[k++] = value;
		for (final byte b : data2)
			data[k++] = b;
		for (final byte nonAsciiByte : nonAsciiBytes)
			data[k++] = nonAsciiByte;
		data[k++] = 0;
		for (final byte nonLatinByte : nonLatinBytes)
			data[k++] = nonLatinByte;
		data[k++] = 0;
	}

	@Test
	void getAllValues() throws KNXFormatException
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

	@Test
	void setDataByteArrayInt()
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
		Assertions.assertArrayEquals(data, t.getData());
		final byte[] dataOffset = new byte[9];
		System.arraycopy(data1, 0, dataOffset, 3, data1.length);
		t.setData(dataOffset, 3);
		byte[] d = t.getData();
		assertEquals(6, d.length);
		Assertions.assertArrayEquals(data1, d);

		final byte[] array = new byte[data.length + 1];
		System.arraycopy(data, 0, array, 1, data.length);
		t.setData(array, 1);
		d = t.getData();
		assertEquals(data.length, d.length);
		Assertions.assertArrayEquals(data, d);
		assertEquals(4, t.getItems());
		assertArrayEquals(strings, t.getAllValues());
	}

	@Test
	void setValues() throws KNXFormatException {
		final String signs = "ÆÐÑØý";
		final String german = "ü ö ä Ä Ü Ö ß";
		final String greek = "ΕΘΨθψϘϨϸ";
		final String[] values = new String[] { signs, german, greek };
		t.setValues(values);
		final byte[] utfdata = signs.getBytes(StandardCharsets.UTF_8);
		final byte[] utfdata2 = german.getBytes(StandardCharsets.UTF_8);
		final byte[] utfdata3 = greek.getBytes(StandardCharsets.UTF_8);
		final byte[] data = new byte[utfdata.length + utfdata2.length + utfdata3.length + 3];
		System.arraycopy(utfdata, 0, data, 0, utfdata.length);
		System.arraycopy(utfdata2, 0, data, utfdata.length + 1, utfdata2.length);
		System.arraycopy(utfdata3, 0, data, utfdata.length + 1 + utfdata2.length + 1,
				utfdata3.length);
		Assertions.assertArrayEquals(data, t.getData());
		assertArrayEquals(values, t.getAllValues());

		t.setValues();
		Assertions.assertArrayEquals(data, t.getData());
	}

	@Test
	void getNumericValue()
	{
		try {
			t.getNumericValue();
			fail("no numeric representation");
		}
		catch (final KNXFormatException expected) {
			// we're good
		}
	}

	@Test
	void getDataByteArrayInt()
	{
		assertEquals(25, t.getData(new byte[25], 4).length);
		final byte[] buf = new byte[20];
		Assertions.assertArrayEquals(buf, t.getData(new byte[20], 3));

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

	@Test
	void getItems() throws KNXFormatException
	{
		assertEquals(1, t.getItems());
		t.setValue(string1);
		assertEquals(1, t.getItems());
		t.setValues(strings);
		assertEquals(strings.length, t.getItems());
	}

	@Test
	void getTypeSize() throws KNXFormatException
	{
		assertEquals(1, t.getTypeSize());
		t.setValue(string1);
		assertEquals(string1.length() + 1, t.getTypeSize());
		t.setValues(strings);
		assertEquals(strings[0].length() + 1, t.getTypeSize());
	}

	private void assertArrayEquals(final String[] exp, final String[] actual)
	{
		assertEquals(exp.length, actual.length);
		for (int i = 0; i < exp.length; ++i)
			assertEquals(exp[i], actual[i]);
	}
}
