/**
 * Connection support for communication over serial ports, using the FT 1.2 protocol, TP-UART, or USB (in sub-package
 * usb).
 * <ul>
 * <li>FT 1.2 connection mode is supported by BCU2 devices.</li>
 * <li>USB connections are used by KNX USB and KNX RF USB.</li>
 * <li>Note, a KNX TP-UART interface which connects via USB and creates a virtual serial port is still accessed as
 * {@link io.calimero.serial.TpuartConnection}, not KNX USB.</li>
 * </ul>
 */

package io.calimero.serial;
