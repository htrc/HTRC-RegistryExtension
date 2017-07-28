package edu.illinois.i3.htrc.registry.api;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import edu.illinois.i3.htrc.registry.api.exceptions.RegistryExtensionConfigurationException;
import edu.illinois.i3.htrc.registry.api.exceptions.RegistryExtensionException;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserStoreException;

/**
 * Registry Extension webapp listener, used to initialize the extension during webapp startup
 *
 * @author capitanu
 */

public class RegistryExtension implements ServletContextListener {

    private static final Log Log = LogFactory.getLog(RegistryExtension.class);

    protected static RegistryExtensionConfig _config;
    protected static RegistryContext _registryContext;

    /**
     * Return the {@link RegistryExtensionConfig} instance used to access the Registry Extension
     * configuration settings
     *
     * @return The {@link RegistryExtensionConfig} instance
     */
    public static RegistryExtensionConfig getConfig() {
        return _config;
    }

    /**
     * Return the {@link RegistryContext} instance
     *
     * @return The {@link RegistryContext} instance
     */
    public static RegistryContext getRegistryContext() { return _registryContext; }

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext context = servletContextEvent.getServletContext();
        String webappName = context.getServletContextName();

        try {
            // attempt to load the configuration file
            Config config = getConfigProperties(context);
            Config htrcConfig;
            try {
                htrcConfig = config.getConfig(Constants.HTRC_CONFIG_PARAM);
            }
            catch (ConfigException.Missing e) {
                throw new RegistryExtensionConfigurationException(
                    "Missing configuration section: " + Constants.HTRC_CONFIG_PARAM);
            }

            _config = new RegistryExtensionConfig(htrcConfig);
            _registryContext = RegistryContext.getBaseInstance();

            if (_registryContext == null) {
                throw new RegistryExtensionException(
                    "Could not obtain a RegistryContext instance!");
            }

            RegistryUtils.initialize(_registryContext);
            setupRegistry(context);

            Log.info(webappName + " successfully initialized");
        }
        catch (Exception e) {
            Log.error("Error initializing " + webappName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }

    /**
     * Load the registry extension configuration
     *
     * @param servletContext The {@link ServletContext} instance
     * @return The {@link Config} instance holding the registry extension configuration
     * @throws IOException Thrown if the configuration file is not found or is invalid
     */
    private Config getConfigProperties(ServletContext servletContext) throws IOException {
        String htrcConfig = servletContext.getInitParameter(Constants.WEBXML_CONFIG_PARAM);
        if (htrcConfig == null) {
            htrcConfig = Constants.DEFAULT_CONFIG_LOCATION;
        }

        URL configUrl = servletContext.getResource(htrcConfig);
        if (configUrl == null) {
            throw new FileNotFoundException("Missing configuration file: " + htrcConfig);
        }

        Log.info("Loading Registry Extension configuration from " + htrcConfig);

        return ConfigFactory.parseURL(configUrl).resolve();
    }

    /**
     * Do setup tasks for the registry, if necessary
     *
     * @param context The servlet context
     * @throws RegistryException  Thrown if a registry error occurs
     * @throws UserStoreException Thrown if a user store error occurs
     */
    protected void setupRegistry(ServletContext context)
        throws RegistryException, UserStoreException {
        UserRegistry registry = RegistryUtils.getAdminRegistry();
        createRequiredPaths(registry);
    }

    private void createRequiredPaths(UserRegistry registry)
        throws RegistryException, UserStoreException {
        // create required paths
        String publicFilesPath = _config.getPublicFilesPath();
        if (!registry.resourceExists(publicFilesPath)) {
            Log.info("Creating " + publicFilesPath + " collection");
            registry.beginTransaction();
            try {
                Collection col = registry.newCollection();
                if (col != null) {
                    col.setDescription("Public files");
                    registry.put(publicFilesPath, col);
                }
                registry.commitTransaction();
            }
            catch (RegistryException e) {
                registry.rollbackTransaction();
                throw e;
            }
        }

        RegistryUtils.authorizeEveryone(_config.getPublicPath(), registry,
                                        ActionConstants.GET, ActionConstants.PUT,
                                        ActionConstants.DELETE);
    }

}
