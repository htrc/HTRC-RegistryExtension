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
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Registry Extension webapp listener, used to initialize the extension during webapp startup
 *
 * @author capitanu
 */

public class RegistryExtension implements ServletContextListener {

    private static final Log Log = LogFactory.getLog(RegistryExtension.class);

    protected RegistryUtils _registryUtils;
    protected RegistryExtensionConfig _config;


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
            } catch (ConfigException.Missing e) {
                throw new RegistryExtensionConfigurationException(
                        "Missing configuration section: " + Constants.HTRC_CONFIG_PARAM);
            }

            RegistryContext registryContext = RegistryContext.getBaseInstance();
            if (registryContext == null) {
                throw new RegistryExtensionException(
                        "Could not obtain a RegistryContext instance!");
            }

            RegistryService registryService = registryContext.getEmbeddedRegistryService();
            RealmService realmService = registryContext.getRealmService();

            _config = new RegistryExtensionConfig(htrcConfig);
            _registryUtils = new RegistryUtils(registryService, realmService);

            createRequiredPaths();

            context.setAttribute(this.getClass().getName(), this);

            Log.info(webappName + " successfully initialized");
        } catch (Exception e) {
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
     * Create required registry paths
     *
     * @throws RegistryException Thrown if a registry error occurs
     * @throws UserStoreException Thrown if a user store error occurs
     */
    protected void createRequiredPaths() throws RegistryException, UserStoreException {
        UserRegistry registry = _registryUtils.getAdminRegistry();
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
            } catch (RegistryException e) {
                registry.rollbackTransaction();
                throw e;
            }
        }

        RegistryUtils.authorizeEveryone(_config.getPublicPath(), registry,
                ActionConstants.GET, ActionConstants.PUT, ActionConstants.DELETE);
    }

    /**
     * Return the {@link RegistryUtils} instance used to access helper Registry functionality
     *
     * @return The {@link RegistryUtils} instance
     */
    public RegistryUtils getRegistryUtils() {
        return _registryUtils;
    }

    /**
     * Return the {@link RegistryExtensionConfig} instance used to access the Registry Extension
     * configuration settings
     *
     * @return The {@link RegistryExtensionConfig} instance
     */
    public RegistryExtensionConfig getConfig() {
        return _config;
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

}
