package edu.illinois.i3.htrc.registry.api.workset;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.LogUtils;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.Worksets;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * JAX-RS Implementation for {@link WorksetsAPI}
 *
 * @author capitanu
 *
 */
public class WorksetsAPIImpl implements WorksetsAPI {

	private static final Log Log = LogFactory.getLog(WorksetsAPIImpl.class);

	protected @Context HttpServletRequest _request;
	protected @QueryParam("user") String debugUserName;

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

	@GET
	public Response getWorksets(@DefaultValue("false") @QueryParam("public") boolean includePublic) {
		String userName = getAuthenticatedUser();
		Log.debug(String.format("getWorksets: user=%s, public=%s", userName, includePublic));

		if (userName == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		Worksets worksets = new Worksets();
		List<Workset> worksetList = worksets.getWorksets();
		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);

			String[] users;
			if (includePublic) {
				UserRegistry adminRegistry = _registryUtils.getAdminRegistry();
				Collection htrc = (Collection)adminRegistry.get(_config.getBasePath());
				users = htrc.getChildren();
				for (int i = 0; i < users.length; i++)
					// normalize user names
					users[i] = users[i].substring(users[i].lastIndexOf("/") + 1);
			} else
				users = new String[] { userName };

			for (String user : users) {
				String userWorksetsPath = _config.getUserWorksetsPath(user);
				if (!registry.resourceExists(userWorksetsPath)) {
					Log.warn("Missing user worksets path: " + userWorksetsPath);
					continue;
				}

				Collection userWorksetCollection = (Collection)registry.get(userWorksetsPath);
				worksetList.addAll(getUserWorksets(userWorksetCollection, registry));
			}
		}
		catch (ResourceNotFoundException e) {
			Log.error("getWorksets", e);
			String errorMsg = String.format("User workset collection path for user '%s' does not exist!", userName);
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
	public Response newWorkset(Workset workset, @DefaultValue("false") @QueryParam("public") boolean isPublic) {
		String userName = getAuthenticatedUser();
		Log.debug(String.format("newWorkset: user=%s, public=%s", userName, isPublic));

		if (userName == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		String worksetName = workset.getMetadata().getName();
		if (!WorksetUtils.isLegalWorksetName(worksetName)) {
			String errorMsg = "Illegal workset name: " + worksetName;
			return Response.status(Status.BAD_REQUEST).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}

		if (Log.isDebugEnabled())
			LogUtils.logWorkset(Log, workset);

		URI resUri = null;

		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			String resPath = _config.getWorksetPath(worksetName, userName);

			if (registry.resourceExists(resPath))
				return Response.status(Status.CONFLICT)
							.entity("Workset " + worksetName + " already exists. Must use PUT if an update is intended")
							.type(MediaType.TEXT_PLAIN)
							.build();

			registry.beginTransaction();
			try {
				Resource resource = WorksetUtils.createResourceFromWorkset(workset, registry);
				resPath = registry.put(resPath, resource);
				resUri = WorksetUtils.getWorksetUri(resPath);
				Log.debug("Created workset: " + resPath);
				workset.setMetadata(WorksetUtils.updateResourceCommunityMeta(resource, workset.getMetadata(), registry, _registryUtils));

				if (isPublic)
					RegistryUtils.authorizeEveryone(resPath, registry, ActionConstants.GET);
				else
					RegistryUtils.denyEveryone(resPath, registry, ActionConstants.GET);

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
		if (userName == null)
			throw new WebApplicationException(
					Response.status(Status.UNAUTHORIZED)
						.entity("Not authenticated")
						.type(MediaType.TEXT_PLAIN).build());

		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			return new WorksetAPIImpl(worksetId, registry, _registryExtension);
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
		String remoteUser = _request.getRemoteUser();

		if(remoteUser != null){
			// Extracting user name part from username with tenant (e.g. admin@carbon.super)
			return MultitenantUtils.getTenantAwareUsername(remoteUser);
		}

		return null;
	}

	/**
	 * Get the list of worksets in a registry collection
	 *
	 * @param worksetCollection The registry collection
	 * @param registry The {@link UserRegistry} instance
	 * @return The list of worksets
	 * @throws RegistryException Thrown if a registry error occurs
	 */
	private List<Workset> getUserWorksets(Collection worksetCollection, UserRegistry registry) throws RegistryException {
		List<Workset> worksets = new ArrayList<Workset>();
		for (String child : worksetCollection.getChildren()) {
			Resource resource;
            try {
                resource = registry.get(child);
            } catch (AuthorizationFailedException afe) {
                Log.warn("getUserWorksets: Registry authorization failure. This should not happen." +
                        " But latest Registry version's Collection/getChildren returns private resources.");
                continue;
            }

			if (Log.isDebugEnabled())
				LogUtils.logResource(Log, resource);

			Workset workset = new Workset();
			workset.setMetadata(WorksetUtils.getWorksetMetaFromResource(resource, registry));
			worksets.add(workset);
		}

		return worksets;
	}
}
