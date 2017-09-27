package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.Constants;
import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.LogUtils;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Volume;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetContent;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;
import edu.illinois.i3.htrc.registry.entities.workset.Worksets;
import java.net.URI;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * JAX-RS Implementation for {@link WorksetsAPI}
 *
 * @author capitanu
 */
public class WorksetsAPIImpl implements WorksetsAPI {

    private static final Log Log = LogFactory.getLog(WorksetsAPIImpl.class);

    protected @Context
    HttpServletRequest _request;

    protected @HeaderParam("X-Auth-Username")
    String debugUserName;

    private RegistryExtensionConfig _config;
    private ServletContext _context;

    /**
     * Injected ServletContext
     *
     * @param context The servlet context
     */
    @javax.annotation.Resource
    public void setServletContext(ServletContext context) {
        _config = RegistryExtension.getConfig();
        _context = context;
    }

    @GET
    public Response getWorksets(
        @DefaultValue("false") @QueryParam("public") boolean includePublic) {

        String userName = getAuthenticatedUser();
        Log.debug(String.format("getWorksets: user=%s, public=%s", userName, includePublic));

        if (userName == null) {
            return Response.status(Status.UNAUTHORIZED).entity("Not authenticated")
                           .type(MediaType.TEXT_PLAIN).build();
        }

        Worksets worksets = new Worksets();
        List<Workset> worksetList = worksets.getWorksets();

        try {
            UserRegistry registry = RegistryUtils.getUserRegistry(userName);
            String userWorksetsPath = _config.getUserWorksetsPath(userName);
            Collection userWorksetCollection = (Collection) registry.get(userWorksetsPath);

            Set<String> worksetsPaths =
                WorksetUtils.getWorksetsPaths(userWorksetCollection, registry);

            if (includePublic) {
                Set<String> publicWorksetsPaths =
                    WorksetUtils.getPublicWorksetsPathsViaSQL(_context);
                worksetsPaths.addAll(publicWorksetsPaths);
            }

            Collection combined = WorksetUtils.getCollectionFromPaths(worksetsPaths);
            List<WorksetMeta> metas = WorksetUtils.getWorksetsMeta(combined, registry);
            for (WorksetMeta meta : metas) {
                Workset workset = new Workset();
                workset.setMetadata(meta);
                worksetList.add(workset);
            }
        }
        catch (ResourceNotFoundException e) {
            Log.error("getWorksets", e);
            String errorMsg = String
                .format("User workset collection path for user '%s' does not exist!", userName);
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }
        catch (Exception e) {
            Log.error("getWorksets", e);
            String errorMsg = String.format("Cannot retrieve worksets: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok(worksets).build();
    }

    @POST
    @Consumes({
        HTRCMediaTypes.WORKSET_XML,
        HTRCMediaTypes.WORKSET_JSON
    })
    public Response newWorkset(
        Workset workset,
        @DefaultValue("false") @QueryParam("public") boolean isPublic) {

        String userName = getAuthenticatedUser();
        Log.debug(String.format("newWorkset: user=%s, public=%s", userName, isPublic));

        if (userName == null) {
            return Response.status(Status.UNAUTHORIZED).entity("Not authenticated")
                           .type(MediaType.TEXT_PLAIN).build();
        }

        String worksetName = workset.getMetadata().getName();
        if (!WorksetUtils.isLegalWorksetName(worksetName)) {
            String errorMsg = "Illegal workset name: " + worksetName;
            return Response.status(Status.BAD_REQUEST).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                           .build();
        }

        WorksetContent worksetContent = workset.getContent();
        //
        //        if (worksetContent == null || worksetContent.getVolumes().isEmpty()) {
        //            String errorMsg = "The workset does not contain any volumes!";
        //            return Response.status(Status.BAD_REQUEST).entity(errorMsg).type(MediaType.TEXT_PLAIN)
        //                           .build();
        //        }

        if (worksetContent != null) {
            // check for obviously-invalid volume IDs
            List<Volume> volumes = worksetContent.getVolumes();
            for (Volume volume : volumes) {
                String volumeId = volume.getId();
                if (!Constants.VALID_HTRC_ID_REGEX.matcher(volumeId).matches()) {
                    String errorMsg = "Invalid volume ID detected in workset: " + volumeId;
                    return Response.status(Status.BAD_REQUEST).entity(errorMsg)
                                   .type(MediaType.TEXT_PLAIN)
                                   .build();
                }
            }
        }

        if (Log.isDebugEnabled()) {
            LogUtils.logWorkset(Log, workset, false);
        }

        URI resUri;

        try {
            UserRegistry registry = RegistryUtils.getUserRegistry(userName);
            String resPath = _config.getWorksetPath(worksetName, userName);

            if (registry.resourceExists(resPath)) {
                return Response.status(Status.CONFLICT)
                               .entity(
                                   String.format("Workset %s already exists. Must use PUT if an "
                                                     + "update is intended", worksetName)
                               )
                               .type(MediaType.TEXT_PLAIN)
                               .build();
            }

            registry.beginTransaction();
            try {
                Resource resource = WorksetUtils.createResourceFromWorkset(workset, registry);
                resource.setProperty(Constants.HTRC_PROP_PUBLIC, Boolean.toString(isPublic));
                resPath = registry.put(resPath, resource);
                resUri = WorksetUtils.getWorksetUri(resPath);
                Log.debug("Created workset: " + resPath);
                WorksetMeta updatedMeta = WorksetUtils.updateResourceCommunityMeta(
                    resource, workset.getMetadata(), registry);
                workset.setMetadata(updatedMeta);

                if (isPublic) {
                    RegistryUtils.authorizeEveryone(resPath, registry, ActionConstants.GET);
                }
                else {
                    RegistryUtils.denyEveryone(resPath, registry, ActionConstants.GET);
                }

                registry.commitTransaction();
            }
            catch (Exception e) {
                registry.rollbackTransaction();
                throw e;
            }
        }
        catch (Exception e) {
            Log.error("newWorkset", e);
            String errorMsg = String.format("Cannot create new workset: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.created(resUri).entity(workset).build();
    }

    @Path("/{worksetId}")
    public WorksetAPI getWorksetAPI(@PathParam("worksetId") String worksetId) {
        String userName = getAuthenticatedUser();
        if (userName == null) {
            throw new WebApplicationException(
                Response.status(Status.UNAUTHORIZED)
                        .entity("Not authenticated")
                        .type(MediaType.TEXT_PLAIN).build());
        }

        try {
            UserRegistry registry = RegistryUtils.getUserRegistry(userName);
            return new WorksetAPIImpl(worksetId, registry);
        }
        catch (RegistryException e) {
            Log.error("getWorksetAPI", e);
            String errorMsg = String.format("Error processing workset API: %s", e.toString());
            throw new WebApplicationException(
                Response.serverError()
                        .entity(errorMsg)
                        .type(MediaType.TEXT_PLAIN)
                        .build());
        }
    }

    /**
     * Return the currently authenticated user
     *
     * @return The currently authenticated user, or null if not authenticated
     */
    protected String getAuthenticatedUser() {
        String remoteUser = _request.getRemoteUser(); // debugUserName;

        return (remoteUser != null) ?
            // Extract user name part from username with tenant (e.g. admin@carbon.super)
            MultitenantUtils.getTenantAwareUsername(remoteUser) :
            null;
    }
}
