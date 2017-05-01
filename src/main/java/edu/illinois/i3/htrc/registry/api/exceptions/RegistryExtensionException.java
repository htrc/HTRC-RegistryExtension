package edu.illinois.i3.htrc.registry.api.exceptions;

/**
 * Exception class used to indicate error conditions for the Registry Extension
 *
 * @author capitanu
 * @see Exception
 */
@SuppressWarnings("serial")
public class RegistryExtensionException extends Exception {

    /**
     * @param msg The error message
     * @see Exception#Exception(String)
     */
    public RegistryExtensionException(String msg) {
        super(msg);
    }

    /**
     * @param msg The error message
     * @param throwable The inner exception
     * @see Exception#Exception(String, Throwable)
     */
    public RegistryExtensionException(String msg, Throwable throwable) {
        super(msg, throwable);
    }

    /**
     * @param throwable The inner exception
     * @see Exception#Exception(Throwable)
     */
    public RegistryExtensionException(Throwable throwable) {
        super(throwable);
    }

}
