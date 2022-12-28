/**
 * Support for temporary storage of KNX network messages.<br>
 * A network buffer with one or more configurations added to it allows to build up and maintain a local image of process
 * communication messages to answer user queries. Main benefits are a decreased network load and short response
 * times for buffered messages.<br>
 * Using a network buffer allows polled mode of communication, since processing incoming messages can be delayed to some
 * later point in time.<br>
 * A network buffer configuration determines what messages to keep and what messages to ignore by the use of filters.
 * These filters might be user-defined implementations set for a particular configuration. Two predefined filters are
 * available to allow state based and command based network buffering.
 */

package io.calimero.buffer;
