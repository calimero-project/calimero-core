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

package tuwien.auto.calimero.dptxlator;

import java.util.Objects;

import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean.Step;

/**
 * Represents a step (decrease, increase) together with its stepcode.
 */
public final class StepControl {
	public static final StepControl Break = new StepControl(0);

	private final Step step;
	private final int stepcode;

	public static final StepControl increase(final int stepcode) {
		return new StepControl(Step.Increase, stepcode);
	}

	public static final StepControl decrease(final int stepcode) {
		return new StepControl(Step.Decrease, stepcode);
	}

	static final StepControl from(final int raw) {
		return new StepControl(raw);
	}

	StepControl(final int raw) {
		if ((raw & ~0xf) != 0)
			throw new KNXIllegalArgumentException("reserved bits not 0: " + Integer.toHexString(raw));
		step = (raw & 0x8) != 0 ? Step.Increase : Step.Decrease;
		stepcode = raw & 0x7;
	}

	private StepControl(final Step step, final int stepcode) {
		if (stepcode < 1 || stepcode > 7)
			throw new KNXIllegalArgumentException("stepcode out of range [1..7]");
		this.step = step;
		this.stepcode = stepcode;
	}

	public Step step() { return step; }

	public int stepcode() { return stepcode; }

	public boolean isBreak() { return stepcode == 0; }

	@Override
	public boolean equals(final Object other) {
		if (this == other)
			return true;
		if (other instanceof StepControl)	{
			final StepControl sc = (StepControl) other;
			return step == sc.step && stepcode == sc.stepcode;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(step, stepcode);
	}

	@Override
	public String toString() {
		return step + " " + stepcode;
	}
}
