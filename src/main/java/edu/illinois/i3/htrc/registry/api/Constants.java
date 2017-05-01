package edu.illinois.i3.htrc.registry.api;

/**
 * Constants used in the Registry Extension
 *
 * @author capitanu
 */
public class Constants {

    /**
     * Default configuration file, if not specified in configuration
     */
    public static final String DEFAULT_CONFIG_LOCATION = "WEB-INF/htrc.conf";

    /**
     * The web.xml parameter name for specifying the configuration file location
     */
    public static final String WEBXML_CONFIG_PARAM = "htrcConfig";

    /**
     * The section name in the configuration file used to store registry extension configuration
     */
    public static final String HTRC_CONFIG_PARAM = "htrc";

    /**
     * True/False indicating whether the configuration settings will be dumped to the log file
     * on startup, for debug purposes
     */
    public static final String HTRC_CONFIG_DEBUG = "config.debug";

    /**
     * The base path where registry data is stored
     */
    public static final String HTRC_CONFIG_BASE_PATH = "base.path";

    /**
     * The user home path where user-specific artifacts are stored
     */
    public static final String HTRC_CONFIG_USER_HOME = "user.home";

    /**
     * The location where user files are stored in the registry
     */
    public static final String HTRC_CONFIG_USER_FILES = "user.files";

    /**
     * The location where user worksets are stored in the registry
     */
    public static final String HTRC_CONFIG_USER_WORKSETS = "user.worksets";

    /**
     * The base path where public artifacts are stored
     */
    public static final String HTRC_CONFIG_PUBLIC_HOME = "public.home";

    /**
     * The location where public files are stored
     */
    public static final String HTRC_CONFIG_PUBLIC_FILES = "public.files";

    /**
     * The regular expression used to validate registry resource names
     */
    public static final String ILLEGAL_CHARACTERS_FOR_PATH = ".*[~!@#;%^*+={}\\|\\\\<>\",\'].*";

    /**
     * The property used for workset resources to store the volume count of volumes in the workset
     */
    public static final String HTRC_PROP_VOLCOUNT = "htrc.volumeCount";

}
