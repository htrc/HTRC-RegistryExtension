package edu.illinois.i3.htrc.registry.api;

import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.illinois.i3.htrc.registry.api.exceptions.RegistryExtensionConfigurationException;

/**
 * Class to hold the Registry Extension configuration settings
 *
 * @author capitanu
 *
 */
public class RegistryExtensionConfig {

	private static final Log Log = LogFactory.getLog(RegistryExtensionConfig.class);

	/**
	 * The array of required config parameters
	 */
	public static final String[] REQUIRED_CONFIG_PARAMS = new String[] {
		Constants.HTRC_CONFIG_BASE_PATH,
		Constants.HTRC_CONFIG_PUBLIC_HOME,
		Constants.HTRC_CONFIG_PUBLIC_FILES,
		Constants.HTRC_CONFIG_USER_WORKSETS,
		Constants.HTRC_CONFIG_USER_FILES
	};

	private final String _cfgBasePath;
	private final String _cfgPublicFilesPath;
	private final String _cfgPublicPath;
	private final String _cfgUserWorksetsPath;
	private final String _cfgUserFilesPath;

	/**
	 * Constructor
	 *
	 * @param configProps The registry extension configuration properties
	 * @throws RegistryExtensionConfigurationException Thrown if incomplete configuration
	 */
	public RegistryExtensionConfig(Properties configProps) throws RegistryExtensionConfigurationException {
		if (!configProps.keySet().containsAll(Arrays.asList(REQUIRED_CONFIG_PARAMS)))
			throw new RegistryExtensionConfigurationException("Incomplete configuration - required parameters: " + Arrays.toString(REQUIRED_CONFIG_PARAMS));

		if (Log.isDebugEnabled()) {
			Log.debug("== Configuration ==");
			for (String cfgParam : REQUIRED_CONFIG_PARAMS)
				Log.debug("== " + cfgParam + ": " + configProps.getProperty(cfgParam));
		}

		_cfgBasePath = configProps.getProperty(Constants.HTRC_CONFIG_BASE_PATH);
		_cfgPublicPath = configProps.getProperty(Constants.HTRC_CONFIG_PUBLIC_HOME);
		_cfgPublicFilesPath = configProps.getProperty(Constants.HTRC_CONFIG_PUBLIC_FILES);
		_cfgUserWorksetsPath = configProps.getProperty(Constants.HTRC_CONFIG_USER_WORKSETS);
		_cfgUserFilesPath = configProps.getProperty(Constants.HTRC_CONFIG_USER_FILES);
	}

	/**
	 * Return the base path of where the registry extension stores artifacts in the registry
	 *
	 * @return The base path of where the registry extension stores artifacts in the registry
	 */
	public String getBasePath() {
		return _cfgBasePath;
	}

	/**
	 * Return the base path of where public artifacts are stored in the registry
	 *
	 * @return The base path of where public artifacts are stored in the registry
	 */
	public String getPublicPath() {
		return _cfgPublicPath;
	}

	/**
	 * Return the path where public (shared) files are stored in the registry
	 *
	 * @return The path where public (shared) files are stored in the registry
	 */
	public String getPublicFilesPath() {
		return _cfgPublicFilesPath;
	}

	/**
	 * Return the location where user worksets are stored in the registry
	 *
	 * @param userName The user name
	 * @return The location where user worksets are stored in the registry
	 */
	public String getUserWorksetsPath(String userName) {
		return String.format(_cfgUserWorksetsPath, userName);
	}

	/**
	 * Return the location where user files are stored in the registry
	 *
	 * @param userName The user name
	 * @return The location where user files are stored in the registry
	 */
	public String getUserFilesPath(String userName) {
		return String.format(_cfgUserFilesPath, userName);
	}

	/**
	 * Return the registry path for the given workset, for the given user
	 *
	 * @param worksetId The workset id (name)
	 * @param userName The user name
	 * @return The registry path for the given workset, for the given user
	 */
	public String getWorksetPath(String worksetId, String userName) {
		return getUserWorksetsPath(userName) + "/" + worksetId;
	}
}
