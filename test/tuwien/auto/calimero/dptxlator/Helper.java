/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2014 B. Malinowsky

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
*/

package tuwien.auto.calimero.dptxlator;

import org.junit.Assert;

import tuwien.auto.calimero.exception.KNXException;

/**
 * @author B. Malinowsky
 */
public final class Helper
{
	private Helper()
	{}

	/**
	 * Assert similar for String array.
	 * <p>
	 *
	 * @param expected expected result
	 * @param actual actual result
	 */
	public static void assertSimilar(final String[] expected, final String[] actual)
	{
		Assert.assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++)
			assertSimilar(expected[i], actual[i]);
	}

	/**
	 * Assert similar for two strings.
	 * <p>
	 * Case insensitive check whether <code>expected</code> is contained in
	 * <code>actual</code>.
	 *
	 * @param expected expected result
	 * @param actual actual result
	 */
	public static void assertSimilar(final String expected, final String actual)
	{
		Assert.assertTrue("expected: " + expected + ", actual: " + actual, actual
			.toLowerCase().indexOf(expected.toLowerCase()) > -1);
	}

	/**
	 * Creates DPT translator for given dpts and sets the dpts lower and upper value in
	 * the translator.
	 * <p>
	 *
	 * @param dpts dpts to check in translator
	 * @param testSimilarity <code>true</code> to check if getValue() of translator
	 *        returns the expected exact value set before
	 */
	public static void checkDPTs(final DPT[] dpts, final boolean testSimilarity)
	{
		try {
			for (int i = 0; i < dpts.length; i++) {
				final DPTXlator t = TranslatorTypes.createTranslator(0, dpts[i].getID());
				t.setValue(dpts[i].getLowerValue());
				if (testSimilarity)
					assertSimilar(dpts[i].getLowerValue(), t.getValue());
				t.setValue(dpts[i].getUpperValue());
				if (testSimilarity)
					assertSimilar(dpts[i].getUpperValue(), t.getValue());
			}
		}
		catch (final KNXException e) {
			Assert.fail(e.getMessage());
		}
	}
}
