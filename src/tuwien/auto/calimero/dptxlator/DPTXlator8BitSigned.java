/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2006, 2012 B. Malinowsky

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

import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.log.LogLevel;

/**
 * Translator for KNX DPTs with main number 6, type <b>8 Bit signed value</b>.
 * <p>
 * The KNX data type width is 1 byte.<br>
 * The default return value after creation is 0.<br>
 * 
 * @author A. Christian, info@root1.de
 *
 */
public class DPTXlator8BitSigned extends DPTXlator {

    /**
     * DPT ID 6.001, Percent 8 Bit; values from <b>-128</b> to <b>127</b> %.
     * <p>
     */
    public static final DPT DPT_PERCENT_V8 = new DPT("6.001", "Percent (8 Bit)", "-128", "127", "%");

    /**
     * DPT ID 6.010, Value 1 signed count; values from <b>-128</b> to <b>127</b>
     * counter pulses.
     * <p>
     */
    public static final DPT DPT_VALUE_1_UCOUNT = new DPT("6.010", "signed count", "-128", "127",
            "counter pulses");

//    /**
//     * DPT ID 6.020, Status with Mode; 
//     * <p>
//     */
//    public static final DPT DPT_STATUS_MODE3 = new DPT("6.020", "signed count", "-128", "127",
//            "counter pulses");
    private static final Map types;

    static {
        types = new HashMap();
        types.put(DPT_PERCENT_V8.getID(), DPT_PERCENT_V8);
        types.put(DPT_VALUE_1_UCOUNT.getID(), DPT_VALUE_1_UCOUNT);
//        types.put(DPT_STATUS_MODE3.getID(), DPT_STATUS_MODE3);
    }

    /**
     * Creates a translator for the given datapoint type.
     * <p>
     *
     * @param dpt the requested datapoint type
     * @throws KNXFormatException on not supported or not available DPT
     */
    public DPTXlator8BitSigned(final DPT dpt) throws KNXFormatException {
        this(dpt.getID());
    }

    /**
     * Creates a translator for the given datapoint type ID.
     * <p>
     *
     * @param dptID available implemented datapoint type ID
     * @throws KNXFormatException on wrong formatted or not expected (available)
     * <code>dptID</code>
     */
    public DPTXlator8BitSigned(final String dptID) throws KNXFormatException {
        super(1);
        setSubType(dptID);
    }

    /* (non-Javadoc)
     * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
     */
    @Override
    public String getValue() {
        return makeString(0);
    }

    // overwritten to avoid conversion from signed to unsigned
    @Override
    public void setData(final byte[] data, final int offset) {
        if (offset < 0 || offset > data.length) {
            throw new KNXIllegalArgumentException("illegal offset " + offset);
        }
        final int size = Math.max(1, getTypeSize());
        final int length = (data.length - offset) / size * size;
        if (length == 0) {
            throw new KNXIllegalArgumentException("data length " + (data.length - offset)
                    + " < required KNX data type width " + size);
        }
        this.data = new short[length];
        for (int i = 0; i < length; ++i) {
            this.data[i] = data[offset + i];
        }
    }

    /**
     * Sets one new translation item from an signed value, replacing any old
     * items.
     * <p>
     *
     * @param value signed value, 0 &lt;= <code>value</code> &lt;= 255, the
     * higher bytes are ignored
     */
    public final void setValue(final int value) {
        data = new short[]{(short) value};
    }

    @Override
    public double getNumericValue() throws KNXFormatException {
        return data[0];
    }

    /* (non-Javadoc)
     * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
     */
    @Override
    public String[] getAllValues() {
        final String[] s = new String[data.length];
        for (int i = 0; i < data.length; ++i) {
            s[i] = makeString(i);
        }
        return s;
    }

    /* (non-Javadoc)
     * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
     */
    @Override
    public final Map getSubTypes() {
        return types;
    }

    /**
     * @return the subtypes of the 8 Bit signed translator type
     * @see DPTXlator#getSubTypesStatic()
     */
    protected static Map getSubTypesStatic() {
        return types;
    }

    /**
     * Sets a new subtype to use for translating items.
     * <p>
     * The translator is reset into default state, all currently contained items
     * are removed (default value is set).
     *
     * @param dptID new subtype ID to set
     * @throws KNXFormatException on wrong formatted or not expected (available)
     * <code>dptID</code>
     */
    private void setSubType(final String dptID) throws KNXFormatException {
        setTypeID(types, dptID);
        data = new short[1];
    }

    private short fromDPT(final short data) {
        short value = data;
        return value;
    }

    private String makeString(final int index) {
        return appendUnit(Short.toString(fromDPT(data[index])));
    }

    @Override
    protected void toDPT(final String value, final short[] dst, final int index)
            throws KNXFormatException {
        try {
            dst[index] = toDPT(Short.decode(removeUnit(value)));
        } catch (final NumberFormatException e) {
            logThrow(LogLevel.WARN, "wrong value format " + value, null, value);
        }
    }

    private short toDPT(final int value) throws KNXFormatException {

        int convert = value;
        return (short) convert;
    }

}
