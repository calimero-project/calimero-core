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

package io.calimero.dptxlator;

import org.junit.jupiter.api.Test;

import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.KNXIllegalArgumentException;

class DptXlators {

	@Test
	void setDoubleValue() throws KNXException {
		final var mainTypes = TranslatorTypes.getAllMainTypes().keySet();
		for (final var mainNumber : mainTypes) {
			try {
				final var xlator = TranslatorTypes.createTranslator(mainNumber, 0);
				try {
					final double v = 1;
					xlator.setValue(v);
				}
				catch (final KNXIllegalArgumentException e) {
					if (xlator instanceof DPTXlatorDateTime || xlator instanceof DPTXlatorDate) {
						xlator.setValue(System.currentTimeMillis());
					}
					else
						throw e;
				}
			}
			catch (final KNXFormatException expected) {}
		}
	}
}
