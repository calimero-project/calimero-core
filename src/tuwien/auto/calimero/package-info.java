/**
 * Base package of the Calimero library. All functionality part of Calimero is located in here and in sub packages. This
 * package contains functionality used throughout the library, as well as the {@code Settings} class for querying
 * library-related information.
 * <p>
 * This package contains common types of (checked) exceptions thrown throughout the library by {@code public} or
 * {@code protected} visible methods.<br>
 * In general, checked exceptions used in Calimero should extend the base exception {@code KNXException}.
 * <p>
 * <b>Unchecked exceptions only:</b><br>
 * Since a method might throw any type of runtime exception, even those not specified here, there is no
 * Calimero-specific base exception for that.<br>
 * Nevertheless, internal error states and failures originating from Calimero itself should be signaled through
 * library-specific exceptions if possible.
 */

package tuwien.auto.calimero;
