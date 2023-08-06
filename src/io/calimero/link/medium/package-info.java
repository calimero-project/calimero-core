/**
 * Contains KNX device and medium-specific support, and KNX frame types for different KNX communication media.
 * <p>
 * Specializations of {@link io.calimero.link.medium.KNXMediumSettings} are supplied to a KNX link during link
 * creation, and used for KNX message interaction. Medium settings differentiate between KNX communication media by
 * keeping information necessary for a particular medium. KNX frame types encapsulate a raw frame used on a particular
 * communication medium.
 */

package io.calimero.link.medium;
