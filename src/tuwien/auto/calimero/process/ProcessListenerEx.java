/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2011 B. Malinowsky

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

package tuwien.auto.calimero.process;

import tuwien.auto.calimero.dptxlator.DPTXlator;
import tuwien.auto.calimero.dptxlator.DPTXlator2ByteFloat;
import tuwien.auto.calimero.dptxlator.DPTXlator3BitControlled;
import tuwien.auto.calimero.dptxlator.DPTXlator8BitUnsigned;
import tuwien.auto.calimero.dptxlator.DPTXlatorBoolean;
import tuwien.auto.calimero.dptxlator.DPTXlatorString;
import tuwien.auto.calimero.dptxlator.TranslatorTypes;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * Extended process listener interface with additional group read event handler methods,
 * as well as basic ASDU type translation capabilities.
 * <p>
 * This listener contains predefined methods equal to the ones in the ProcessCommunicator
 * interface to convert received ASDUs into common Java data types.<br>
 * Usage example for group reads from datapoints with DPT main number 1:<br>
 * 
 * <pre>
 * public void groupWrite(ProcessEvent e)
 * {
 *     // the member variable <i>model</i> (of type DatapointModel, declared by the user)
 *     // contains datapoints of light switch objects in the KNX network (boolean type)
 *     if (model.contains(e.getDestination()))
 *         try {
 *             final boolean switch = asBool(e);
 *             System.out.println(model.get(e.getDestination()).getName()
 *                 + &quot; state = &quot; + switch);
 *             if (switch) {
 *                 // light switch is in position on
 *                 // do some visual feedback for the user ...
 *             }
 *         }
 *         catch (KNXFormatException kfe) { /* DPT not found ... *<!-- -->/ }
 * }
 * </pre>
 * 
 * @author B. Malinowsky
 * @see ProcessCommunicator
 */
public abstract class ProcessListenerEx implements ProcessListener
{
	/**
	 * Indicates that a KNX group read request message was received from the KNX network.
	 * <p>
	 * 
	 * @param e process event object
	 */
	public abstract void groupReadRequest(ProcessEvent e);

	/**
	 * Indicates that a KNX group read response message was received from the KNX network.
	 * <p>
	 * 
	 * @param e process event object
	 */
	public abstract void groupReadResponse(ProcessEvent e);

	/**
	 * Returns the ASDU of the received process event as boolean datapoint value.
	 * <p>
	 * This method has to be invoked manually by the user (either in
	 * {@link #groupReadResponse(ProcessEvent)} or
	 * {@link ProcessListener#groupWrite(ProcessEvent)}), depending on the received
	 * datapoint type.
	 * 
	 * @param e the process event with the ASDU to translate
	 * @return the received value of type boolean
	 * @throws KNXFormatException on not supported or not available boolean DPT
	 */
	public boolean asBool(final ProcessEvent e) throws KNXFormatException
	{
		final DPTXlatorBoolean t = new DPTXlatorBoolean(DPTXlatorBoolean.DPT_BOOL);
		t.setData(e.getASDU());
		return t.getValueBoolean();
	}

	/**
	 * Returns the ASDU of the received process event as unsigned 8 Bit datapoint value.
	 * <p>
	 * This method has to be invoked manually by the user (either in
	 * {@link #groupReadResponse(ProcessEvent)} or
	 * {@link ProcessListener#groupWrite(ProcessEvent)}), depending on the received
	 * datapoint type.
	 * 
	 * @param e the process event with the ASDU to translate
	 * @param scale see {@link ProcessCommunicator#readUnsigned(
	 *        tuwien.auto.calimero.GroupAddress, String)}
	 * @return the received value of type 8 Bit unsigned
	 * @throws KNXFormatException on not supported or not available 8 Bit unsigned DPT
	 */
	public int asUnsigned(final ProcessEvent e, final String scale) throws KNXFormatException
	{
		final DPTXlator8BitUnsigned t = new DPTXlator8BitUnsigned(scale);
		t.setData(e.getASDU());
		return t.getValueUnsigned();
	}

	/**
	 * Returns the ASDU of the received process event as 3 Bit controlled datapoint value.
	 * <p>
	 * This method has to be invoked manually by the user (either in
	 * {@link #groupReadResponse(ProcessEvent)} or
	 * {@link ProcessListener#groupWrite(ProcessEvent)}), depending on the received
	 * datapoint type.
	 * 
	 * @param e the process event with the ASDU to translate
	 * @return the received value of type 3 Bit controlled
	 * @throws KNXFormatException on not supported or not available 3 Bit controlled DPT
	 */
	public int asControl(final ProcessEvent e) throws KNXFormatException
	{
		final DPTXlator3BitControlled t = new DPTXlator3BitControlled(
				DPTXlator3BitControlled.DPT_CONTROL_DIMMING);
		t.setData(e.getASDU());
		return t.getValueSigned();
	}

	/**
	 * Returns the ASDU of the received process event as 2-byte KNX float datapoint value.
	 * <p>
	 * This method has to be invoked manually by the user (either in
	 * {@link #groupReadResponse(ProcessEvent)} or
	 * {@link ProcessListener#groupWrite(ProcessEvent)}), depending on the received
	 * datapoint type.
	 * 
	 * @param e the process event with the ASDU to translate
	 * @return the received value of type float
	 * @throws KNXFormatException on not supported or not available float DPT
	 */
	public float asFloat(final ProcessEvent e) throws KNXFormatException
	{
		final DPTXlator2ByteFloat t = new DPTXlator2ByteFloat(
				DPTXlator2ByteFloat.DPT_TEMPERATURE_DIFFERENCE);
		t.setData(e.getASDU());
		return t.getValueFloat();
	}

	/**
	 * Returns the ASDU of the received process event as string datapoint value.
	 * <p>
	 * The used character set is ISO-8859-1 (Latin 1), with an allowed string length of 14
	 * characters.
	 * <p>
	 * This method has to be invoked manually by the user (either in
	 * {@link #groupReadResponse(ProcessEvent)} or
	 * {@link ProcessListener#groupWrite(ProcessEvent)}), depending on the received
	 * datapoint type.
	 * 
	 * @param e the process event with the ASDU to translate
	 * @return the received value of type String
	 * @throws KNXFormatException on not supported or not available ISO-8859-1 DPT
	 */
	public String asString(final ProcessEvent e) throws KNXFormatException
	{
		final DPTXlatorString t = new DPTXlatorString(DPTXlatorString.DPT_STRING_8859_1);
		t.setData(e.getASDU());
		return t.getValue();
	}

	/**
	 * Returns the ASDU of the received process event as datapoint value of the requested
	 * DPT in String representation.
	 * <p>
	 * This method has to be invoked manually by the user (either in
	 * {@link #groupReadResponse(ProcessEvent)} or
	 * {@link ProcessListener#groupWrite(ProcessEvent)}), depending on the received
	 * datapoint type.
	 * 
	 * @param e the process event with the ASDU to translate
	 * @param dptMainNumber datapoint type main number, number >= 0; use 0 to infer
	 *        translator type from <code>dptID</code> argument only
	 * @param dptID datapoint type ID for selecting a particular kind of value translation
	 * @return the received value of the requested type as String representation
	 * @throws KNXException on not supported or not available DPT
	 * @see TranslatorTypes#createTranslator(int, String)
	 */
	public String asString(final ProcessEvent e, final int dptMainNumber, final String dptID)
		throws KNXException
	{
		final DPTXlator t = TranslatorTypes.createTranslator(dptMainNumber, dptID);
		t.setData(e.getASDU());
		return t.getValue();
	}
}
