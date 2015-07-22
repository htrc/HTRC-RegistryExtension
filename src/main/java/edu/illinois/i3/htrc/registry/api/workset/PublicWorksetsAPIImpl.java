package edu.illinois.i3.htrc.registry.api.workset;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.UserStoreException;

import edu.illinois.i3.htrc.registry.api.Constants;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.LogUtils;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.Worksets;

/**
 * JAX-RS Implementation for {@link PublicWorksetsAPI}
 *
 * @author capitanu
 *
 */
public class PublicWorksetsAPIImpl implements PublicWorksetsAPI {

    private static final Log Log = LogFactory.getLog(PublicWorksetsAPIImpl.class);

    protected @Context HttpServletRequest _request;

    private RegistryExtension _registryExtension;
    private RegistryUtils _registryUtils;
    private RegistryExtensionConfig _config;

    /**
     * Injected ServletContext
     *
     * @param context The servlet context
     */
    @javax.annotation.Resource
    public void setServletContext(ServletContext context) {
        _registryExtension = (RegistryExtension)context.getAttribute(RegistryExtension.class.getName());
        _registryUtils = _registryExtension.getRegistryUtils();
        _config = _registryExtension.getConfig();
    }

    @Override
    @GET
    public Response getPublicWorksets() {
        Log.debug("getPublicWorksets");

        Worksets worksets = new Worksets();
        List<Workset> worksetList = worksets.getWorksets();
        try {
            UserRegistry adminRegistry = _registryUtils.getAdminRegistry();
            Collection htrc = (Collection)adminRegistry.get(_config.getBasePath());
            String[] users = htrc.getChildren();
            for (int i = 0; i < users.length; i++)
                // normalize user names
                users[i] = users[i].substring(users[i].lastIndexOf("/") + 1);

            for (String user : users) {
                String userWorksetsPath = _config.getUserWorksetsPath(user);
                if (!adminRegistry.resourceExists(userWorksetsPath)) {
                    Log.warn("Missing user worksets path: " + userWorksetsPath);
                    continue;
                }

                Collection userWorksetCollection = (Collection)adminRegistry.get(userWorksetsPath);
                worksetList.addAll(getPublicUserWorksets(userWorksetCollection, adminRegistry));
            }
        }
        catch (Exception e) {
            Log.error("getPublicWorksets", e);
            String errorMsg = String.format("Cannot retrieve worksets: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok(worksets).build();
    }

    @Override
    @Path("/{worksetId}")
    public PublicWorksetAPI getPublicWorksetAPI(@PathParam("worksetId") String worksetId) {
        try {
            worksetId = URLDecoder.decode(worksetId, "UTF-8");
            UserRegistry registry = _registryUtils.getAdminRegistry();
            return new PublicWorksetAPIImpl(worksetId, registry, _registryExtension);
        }
        catch (RegistryException | UnsupportedEncodingException e) {
            Log.error("getPublicWorksetAPI", e);
            String errorMsg = String.format("Error processing workset API: %s", e.toString());
            throw new WebApplicationException(
                    Response.serverError()
                        .entity(errorMsg)
                        .type(MediaType.TEXT_PLAIN)
                        .build());
        }
    }

    /**
     * Get the list of public worksets in a registry collection
     *
     * @param worksetCollection The registry collection
     * @param registry The {@link UserRegistry} instance
     * @return The list of public worksets
     * @throws RegistryException Thrown if a registry error occurs
     */
    private List<Workset> getPublicUserWorksets(Collection worksetCollection, UserRegistry registry) throws RegistryException {
        List<Workset> worksets = new ArrayList<Workset>();
        for (String child : worksetCollection.getChildren()) {
            Resource resource;
            try {
                resource = registry.get(child);
            }
            catch (AuthorizationFailedException afe) {
                Log.warn(String.format("getUserWorksets: Registry authorization failure for '%s'. This should not happen." +
                        " But latest Registry version's Collection/getChildren returns private resources.", child));
                continue;
            }

            if (Log.isDebugEnabled())
                LogUtils.logResource(Log, resource);

            try {
                // check if public workset
                if (RegistryUtils.isEveryoneAuthorized(resource.getPath(), registry, ActionConstants.GET)) {
                    String sWorksetClass = resource.getProperty(Constants.HTRC_PROP_WORKSETCLASS);
                    if (sWorksetClass == null) sWorksetClass = "1";

                    Workset workset = new Workset();
                    workset.setMetadata(WorksetUtils.getWorksetMetaFromResource(resource, registry));
                    workset.setWorksetClass(Integer.parseInt(sWorksetClass));
                    worksets.add(workset);
                }
            }
            catch (UserStoreException e) {
                throw new RegistryException("getPublicUserWorksets", e);
            }
        }

        return worksets;
    }
}
