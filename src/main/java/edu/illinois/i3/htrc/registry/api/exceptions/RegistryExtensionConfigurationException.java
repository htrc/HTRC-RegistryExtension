package edu.illinois.i3.htrc.registry.api.exceptions;

/**
 * Exception class used to indicate configuration problems
 *
 * @author capitanu
 * @see Exception
 */
@SuppressWarnings("serial")
public class RegistryExtensionConfigurationException extends RegistryExtensionException {

	/**
	 * @param msg The error message
	 * @see Exception#Exception(String)
	 */
	public RegistryExtensionConfigurationException(String msg) {
		super(msg);
	}

}
