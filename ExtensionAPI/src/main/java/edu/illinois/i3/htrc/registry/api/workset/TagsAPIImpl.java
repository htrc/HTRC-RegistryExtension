package edu.illinois.i3.htrc.registry.api.workset;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Tag;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Tags;

/**
 * JAX-RS Implementation for {@link TagsAPI}
 *
 * @author capitanu
 *
 */
public class TagsAPIImpl implements TagsAPI {

	private static final Log Log = LogFactory.getLog(TagsAPIImpl.class);

	private final String _worksetId;
	private final UserRegistry _registry;
	private final String _userName;
	private final RegistryExtensionConfig _config;

	public TagsAPIImpl(String worksetId, UserRegistry registry, RegistryExtension registryExtension) {
		_worksetId = worksetId;
		_registry = registry;
		_userName = registry.getUserName();
		_config = registryExtension.getConfig();
	}

	@GET
	public Response getTags(@QueryParam("author") String author) {
		Log.debug(String.format("getTags: id=%s, author=%s, user=%s", _worksetId, author, _userName));

		try {
			if (author == null) author = _userName;
			String resPath = _config.getWorksetPath(_worksetId, author);

			if (!RegistryUtils.isAuthorized(resPath, _registry, ActionConstants.GET))
				throw new AuthorizationFailedException(String.format("User %s is not authorized to read the resource %s.", _userName, resPath));

			Tags tags = new Tags();
			for (Tag tag : _registry.getTags(resPath))
				tags.getTags().add(tag.getTagName());

			return Response.ok(tags).build();
		}
		catch (AuthorizationFailedException e) {
			Log.warn(e.getMessage());
			return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions").type(MediaType.TEXT_PLAIN).build();
		}
		catch (ResourceNotFoundException e) {
			String errorMsg = "Unable to locate workset: " + _worksetId;
			return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getTags", e);
			String errorMsg = String.format("Cannot retrieve tags: %s", e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

}
