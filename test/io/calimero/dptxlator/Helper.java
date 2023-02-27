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

package io.calimero.dptxlator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.calimero.KNXException;


public final class Helper
{
	private Helper() {}

	/**
	 * Assert similar for String array.
	 *
	 * @param expected expected result
	 * @param actual actual result
	 */
	public static void assertSimilar(final String[] expected, final String[] actual)
	{
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++)
			assertSimilar(expected[i], actual[i]);
	}

	/**
	 * Assert similar for two strings.
	 * <p>
	 * Case insensitive check whether {@code expected} is contained in
	 * {@code actual}.
	 *
	 * @param expected expected result
	 * @param actual actual result
	 */
	public static void assertSimilar(final String expected, final String actual)
	{
		assertTrue(actual.toLowerCase().contains(expected.toLowerCase()),
				"expected: " + expected + ", actual: " + actual);
	}

	/**
	 * Creates DPT translator for given dpts and sets the dpts lower and upper value in
	 * the translator.
	 *
	 * @param dpts dpts to check in translator
	 * @param testSimilarity {@code true} to check if getValue() of translator
	 *        returns the expected exact value set before
	 */
	public static void checkDPTs(final DPT[] dpts, final boolean testSimilarity)
	{
		try {
            for (DPT dpt : dpts) {
                final DPTXlator t = TranslatorTypes.createTranslator(0, dpt.getID());
                t.setValue(dpt.getLowerValue());
                if (testSimilarity)
                    assertSimilar(dpt.getLowerValue(), t.getValue());
                t.setValue(dpt.getUpperValue());
                if (testSimilarity)
                    assertSimilar(dpt.getUpperValue(), t.getValue());
            }
		}
		catch (final KNXException e) {
			fail(e.getMessage());
		}
	}
}
