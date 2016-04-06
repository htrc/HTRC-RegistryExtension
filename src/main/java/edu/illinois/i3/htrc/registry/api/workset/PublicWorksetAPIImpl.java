package edu.illinois.i3.htrc.registry.api.workset;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.LogUtils;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;

/**
 * JAX-RS Implementation for {@link PublicWorksetAPI}
 *
 * @author capitanu
 *
 */
public class PublicWorksetAPIImpl implements PublicWorksetAPI {

	private static final Log Log = LogFactory.getLog(PublicWorksetAPIImpl.class);

	private final String _worksetId;
	private final UserRegistry _registry;
	private final RegistryExtension _registryExtension;
	private final RegistryExtensionConfig _config;

	public PublicWorksetAPIImpl(String worksetId, UserRegistry registry, RegistryExtension registryExtension) throws RegistryException {
		_worksetId = worksetId;
		_registry = registry;
		_registryExtension = registryExtension;
		_config = registryExtension.getConfig();
	}

	@GET
	public Response getPublicWorkset(@QueryParam("author") String author) {
		Log.debug(String.format("getPublicWorkset: id=%s, author=%s", _worksetId, author));

		if (author == null)
			return Response.status(Status.BAD_REQUEST)
					.entity("author parameter is mandatory")
					.type(MediaType.TEXT_PLAIN)
					.build();

		try {
			String resPath = _config.getWorksetPath(_worksetId, author);

			// check if public workset
			if (!RegistryUtils.isEveryoneAuthorized(resPath, _registry, ActionConstants.GET))
				throw new AuthorizationFailedException(String.format("%s is not a public workset", _worksetId));

			Resource resource = _registry.get(resPath);

			if (Log.isDebugEnabled())
				LogUtils.logResource(Log, resource);

			Workset workset = WorksetUtils.getWorksetFromResource(resource, _registry);

			return Response.ok(workset).build();
		}
		catch (AuthorizationFailedException e) {
			return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions").type(MediaType.TEXT_PLAIN).build();
		}
		catch (ResourceNotFoundException e) {
			String errorMsg = "Unable to locate workset: " + _worksetId;
			return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getPublicWorkset", e);
			String errorMsg = String.format("Cannot retrieve workset: %s", e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@Path("/volumes")
	public PublicVolumesAPI getPublicVolumesAPI() {
		return new PublicVolumesAPIImpl(_worksetId, _registry, _registryExtension);
	}

	@GET
	@Path("/metadata")
	public Response getPublicWorksetMeta(@QueryParam("author") String author) {
		Log.debug(String.format("getPublicWorksetMeta: id=%s, author=%s", _worksetId, author));

		if (author == null)
			return Response.status(Status.BAD_REQUEST)
					.entity("author parameter is mandatory")
					.type(MediaType.TEXT_PLAIN)
					.build();

		try {
			String resPath = _config.getWorksetPath(_worksetId, author);

			// check if public workset
			if (!RegistryUtils.isEveryoneAuthorized(resPath, _registry, ActionConstants.GET))
				throw new AuthorizationFailedException(String.format("%s is not a public workset", _worksetId));

			Resource resource = _registry.get(resPath);

			if (Log.isDebugEnabled())
				LogUtils.logResource(Log, resource);

			WorksetMeta worksetMeta = WorksetUtils.getWorksetMetaFromResource(resource, _registry);
			Workset workset = new Workset();
			workset.setMetadata(worksetMeta);

			return Response.ok(workset).build();
		}
		catch (AuthorizationFailedException e) {
			return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions").type(MediaType.TEXT_PLAIN).build();
		}
		catch (ResourceNotFoundException e) {
			String errorMsg = "Unable to locate workset: " + _worksetId;
			return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getPublicWorksetMeta", e);
			String errorMsg = String.format("Cannot retrieve workset: %s", e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@Path("/tags")
	public TagsAPI getTagsAPI() {
		return new TagsAPIImpl(_worksetId, _registry, _registryExtension);
	}

}
