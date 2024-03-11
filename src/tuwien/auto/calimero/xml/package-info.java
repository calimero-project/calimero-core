/**
 * Provides a simple interface for reading and writing XML documents. The default lookup will use any provided StaX
 * reader/writer implementation ({@code javax.xml.stream}).
 * <p>
 * In case no StaX implementation is found, an internal implementation is used as fallback: reading is non-validating,
 * writing only ensures a valid structure of the XML document, but does not check name formats of tags and similar.
 * Entities predefined in XML will be substituted with its references and vice versa. Namespaces are not supported. All
 * Calimero XML documents can be processed with only the internal implementation.
 */

package tuwien.auto.calimero.xml;
