package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.Constants;
import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.exceptions.RegistryExtensionException;
import edu.illinois.i3.htrc.registry.api.utils.LogUtils;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetContent;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 * JAX-RS Implementation for {@link WorksetAPI}
 *
 * @author capitanu
 */
public class WorksetAPIImpl implements WorksetAPI {

    private static final Log Log = LogFactory.getLog(WorksetAPIImpl.class);

    private final String _worksetId;
    private final UserRegistry _registry;
    private final String _userName;
    private final RegistryExtensionConfig _config;

    public WorksetAPIImpl(String worksetId, UserRegistry registry) {
        _worksetId = worksetId;
        _registry = registry;
        _userName = registry.getUserName();
        _config = RegistryExtension.getConfig();
    }

    @HEAD
    public Response checkWorksetExists() {
        Log.debug(
            String.format("checkWorksetExists: id=%s, user=%s", _worksetId, _userName)
        );

        try {
            String resPath = _config.getWorksetPath(_worksetId, _userName);
            if (_registry.resourceExists(resPath))
                return Response.ok().build();
            else
                return Response.status(Status.NOT_FOUND).build();
        }
        catch (AuthorizationFailedException e) {
            return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions")
                           .type(MediaType.TEXT_PLAIN).build();
        }
        catch (ResourceNotFoundException e) {
            String errorMsg = "Unable to locate workset: " + _worksetId;
            return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                           .build();
        }
        catch (RegistryException e) {
            Log.error("getWorkset", e);
            String errorMsg = String.format("Cannot retrieve workset: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @GET
    public Response getWorkset(@QueryParam("author") String author) {
        Log.debug(
            String.format("getWorkset: id=%s, author=%s, user=%s",
                          _worksetId, author, _userName)
        );

        try {
            if (author == null) {
                author = _userName;
            } else {
                String userName = RegistryUtils.getUserIdForAlias(author);
                if (userName == null) {
                    String errorMsg = "Unknown author: " + author;
                    return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                                   .build();
                }
                author = userName;
            }
            String resPath = _config.getWorksetPath(_worksetId, author);
            Resource resource = _registry.get(resPath);

            if (Log.isDebugEnabled()) {
                LogUtils.logResource(Log, resource);
            }

            Workset workset = WorksetUtils.getWorksetFromResource(resource, _registry);

            return Response.ok(workset).build();
        }
        catch (AuthorizationFailedException e) {
            return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions")
                           .type(MediaType.TEXT_PLAIN).build();
        }
        catch (ResourceNotFoundException e) {
            String errorMsg = "Unable to locate workset: " + _worksetId;
            return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                           .build();
        }
        catch (RegistryException | RegistryExtensionException e) {
            Log.error("getWorkset", e);
            String errorMsg = String.format("Cannot retrieve workset: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @PUT
    @Consumes({
        HTRCMediaTypes.WORKSET_XML,
        HTRCMediaTypes.WORKSET_JSON
    })
    public Response updateWorkset(
        Workset workset,
        @DefaultValue("false") @QueryParam("public") boolean isPublic) {

        Log.debug(
            String.format("updateWorkset: id=%s, public=%s, user=%s",
                          _worksetId, isPublic, _userName)
        );

        WorksetMeta worksetMeta = workset.getMetadata();
        WorksetContent worksetContent = workset.getContent();

        String worksetName = workset.getMetadata().getName();
        if (!WorksetUtils.isLegalWorksetName(worksetName)) {
            String errorMsg = "Illegal workset name: " + worksetName;
            return Response.status(Status.BAD_REQUEST).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                           .build();
        }

        if (!_worksetId.equalsIgnoreCase(worksetMeta.getName())) {
            Log.warn(String.format(
                "API call for workset name '%s' does not match payload workset name '%s'",
                _worksetId, worksetMeta.getName()));
        }

        try {
            _registry.beginTransaction();

            String resPath = _config.getWorksetPath(_worksetId, _userName);

            try {
                Resource resource = _registry.get(resPath);

                // Check whether to update the workset metadata or replace the workset altogether
                if (worksetContent == null) {
                    // Update
                    Log.debug("Updating workset");
                    resource.setDescription(worksetMeta.getDescription());
                }
                else {
                    // Replace
                    Log.debug("Replacing workset");
                    resource = WorksetUtils.createResourceFromWorkset(workset, _registry);
                }

                resource.setProperty(Constants.HTRC_PROP_PUBLIC, Boolean.toString(isPublic));
                resPath = _registry.put(resPath, resource);
                WorksetMeta updatedMeta =
                    WorksetUtils.updateResourceCommunityMeta(resource, worksetMeta, _registry);
                workset.setMetadata(updatedMeta);

                if (isPublic) {
                    RegistryUtils.authorizeEveryone(resPath, _registry, ActionConstants.GET);
                }
                else {
                    RegistryUtils.denyEveryone(resPath, _registry, ActionConstants.GET);
                }

                _registry.commitTransaction();

                return Response.ok(workset).build();
            }
            catch (Exception e) {
                _registry.rollbackTransaction();
                throw e;
            }
        }
        catch (ResourceNotFoundException e) {
            String errorMsg = "Unable to locate workset: " + _worksetId;
            return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                           .build();
        }
        catch (Exception e) {
            Log.error("updateWorkset", e);
            String errorMsg = String.format("Cannot update workset: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @DELETE
    @Produces(MediaType.WILDCARD)
    public Response deleteWorkset() {
        Log.debug(String.format("deleteWorkset: id=%s, user=%s", _worksetId, _userName));

        try {
            String resPath = _config.getWorksetPath(_worksetId, _userName);

            if (!_registry.resourceExists(resPath)) {
                return Response.status(Status.NOT_FOUND).build();
            }

            _registry.delete(resPath);

            return Response.noContent().build();
        }
        catch (Exception e) {
            Log.error("deleteWorkset", e);
            String errorMsg = String.format("Cannot delete workset: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("/volumes")
    public VolumesAPI getVolumesAPI() {
        return new VolumesAPIImpl(_worksetId, _registry);
    }

    @GET
    @Path("/metadata")
    public Response getWorksetMeta(@QueryParam("author") String author) {
        Log.debug(
            String.format("getWorksetMeta: id=%s, author=%s, user=%s",
                          _worksetId, author, _userName)
        );

        try {
            if (author == null) {
                author = _userName;
            } else {
                String userName = RegistryUtils.getUserIdForAlias(author);
                if (userName == null) {
                    String errorMsg = "Unknown author: " + author;
                    return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                                   .build();
                }
                author = userName;
            }
            String resPath = _config.getWorksetPath(_worksetId, author);
            Resource resource = _registry.get(resPath);

            if (Log.isDebugEnabled()) {
                LogUtils.logResource(Log, resource);
            }

            WorksetMeta worksetMeta = WorksetUtils.getWorksetMetaFromResource(resource, _registry);
            Workset workset = new Workset();
            workset.setMetadata(worksetMeta);

            return Response.ok(workset).build();
        }
        catch (AuthorizationFailedException e) {
            return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions")
                           .type(MediaType.TEXT_PLAIN).build();
        }
        catch (ResourceNotFoundException e) {
            String errorMsg = "Unable to locate workset: " + _worksetId;
            return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN)
                           .build();
        }
        catch (RegistryException | RegistryExtensionException e) {
            Log.error("getWorksetMeta", e);
            String errorMsg = String.format("Cannot retrieve workset meta: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }
    }

    @Path("/tags")
    public TagsAPI getTagsAPI() {
        return new TagsAPIImpl(_worksetId, _registry);
    }

}
