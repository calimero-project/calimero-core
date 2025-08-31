/*
    Calimero 3 - A library for KNX network access
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

package io.calimero.knxnetip;

import io.calimero.DataUnitBuilder;
import io.calimero.Util;
import io.calimero.cemi.CEMIBusMon;
import io.calimero.cemi.CEMIDevMgmt;
import io.calimero.cemi.CEMILData;
import io.calimero.link.medium.RawAckBase;
import io.calimero.link.medium.RawFrame;
import io.calimero.link.medium.RawFrameBase;
import io.calimero.link.medium.TP1Ack;
import io.calimero.link.medium.TP1LData;
import io.calimero.link.medium.TP1LPollData;


public final class Debug
{
	private static boolean printToSystemOut;

	private Debug()
	{}

	public static void printLData(final String prefix, final CEMILData f)
	{
		final StringBuilder buf = new StringBuilder();
		buf.append(prefix);
		buf.append("hopcount=" + f.getHopCount());
		buf.append(", dst=" + f.getDestination());
		buf.append(", prio=" + f.getPriority());
		buf.append(", mc=" + f.getMessageCode());
		buf.append(", src=" + f.getSource());
		buf.append(", len=" + f.getStructLength());
		buf.append(", ackreq=" + f.isAckRequested());
		buf.append(", pos.con=" + f.isPositiveConfirmation());
		buf.append(", rep=" + f.isRepetition());
		buf.append("L-data").append(Util.toHexDec(f.getPayload()));
		buf.append("\ntoString(): ");
		buf.append(f);
		buf.append(": ").append(DataUnitBuilder.decode(f.getPayload(), f.getDestination()));
		print(buf);
	}

	public static void printLData(final CEMILData f)
	{
		printLData("", f);
	}

	public static void printMData(final CEMIDevMgmt f)
	{
		final StringBuilder buf = new StringBuilder();
		buf.append("mc=" + f.getMessageCode());
		buf.append(", neg.res=" + f.isNegativeResponse());
		buf.append(", errmsg=" + f.getErrorMessage());
		buf.append(", no elems=" + f.getElementCount());
		buf.append(", oi=" + f.getObjectInstance());
		buf.append(", ot=" + f.getObjectType());
		buf.append(", pid=" + f.getPID());
		buf.append(", start=" + f.getStartIndex());
		buf.append("DevMgmt-data").append(Util.toHexDec(f.getPayload()));
		buf.append("\ntoString(): ");
		buf.append(f);
		print(buf);
	}

	public static void printMonData(final CEMIBusMon ind)
	{
		final StringBuilder buf = new StringBuilder();
		buf.append("seq=" + ind.getSequenceNumber());
		buf.append(", timestamp=" + ind.getTimestamp());
		buf.append(", tstamptype=" + ind.getTimestampType());
		buf.append(", lost=" + ind.getLost());
		buf.append(", biterr=" + ind.getBitError());
		buf.append(", frameerr=" + ind.getFrameError());
		buf.append(", parityerr=" + ind.getParityError());
		buf.append("Mon-data").append(Util.toHexDec(ind.getPayload()));
		buf.append("\ntoString(): ");
		buf.append(ind);
		print(buf);
	}

	public static void printTP1Frame(final RawFrame tp1)
	{
		final StringBuilder buf = new StringBuilder();
		final String type = switch (tp1.getFrameType()) {
			case RawFrame.ACK_FRAME -> "ack";
			case RawFrame.LDATA_FRAME -> "L-data";
			case RawFrame.LPOLLDATA_FRAME -> "poll-data";
			default -> "unknown";
		};
		buf.append("frametype=" + type);
		switch (tp1) {
			case TP1LData f -> {
				buf.append(", src=" + f.getSource());
				buf.append(", dst=" + f.getDestination());
				buf.append(", prio=" + f.getPriority());
				buf.append(", rep=" + f.isRepetition());
				buf.append(", chksum=" + f.getChecksum());
			}
			case TP1Ack f -> {
				final int acktype = f.getAckType();
				final String ack = switch (acktype) {
					case RawAckBase.ACK  -> "ACK";
					case RawAckBase.NAK  -> "NAK";
					case TP1Ack.BUSY     -> "BUSY";
					case TP1Ack.NAK_BUSY -> "NAK_BUSY";
					default -> "unknown";
				};
				buf.append(", ack=" + ack);
			}
			case TP1LPollData f -> {
				buf.append(", src=" + f.getSource());
				buf.append(", dst=" + f.getDestination());
				buf.append(", prio=" + f.getPriority());
				buf.append(", rep=" + f.getExpectedDataLength());
				buf.append(", chksum=" + f.getChecksum());
			}
			default -> {}
		}
		if (printToSystemOut) {
			synchronized (System.out) {
				System.out.print(buf);
				if (tp1 instanceof RawFrameBase)
					Util.out("tpdu", ((TP1LData) tp1).getTPDU());
				System.out.println();
				System.out.print("toString: " + tp1);
				if (tp1 instanceof RawFrameBase)
					System.out.println(": "
							+ DataUnitBuilder.decode(((TP1LData) tp1).getTPDU(), ((TP1LData) tp1).getDestination()));
				System.out.println();
			}
		}
	}

	public static void out(final Object print)
	{
		print(print.toString());
	}

	private static void print(final CharSequence buf)
	{
		if (printToSystemOut) {
			synchronized (System.out) {
				System.out.println(buf);
				System.out.println();
			}
		}
	}
}
