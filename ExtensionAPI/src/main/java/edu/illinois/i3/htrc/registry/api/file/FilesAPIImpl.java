package edu.illinois.i3.htrc.registry.api.file;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.FileUtils;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.entities.file.Entry;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * JAX-RS implementation for {@link FilesAPI}
 *
 * @author capitanu
 *
 */
public class FilesAPIImpl implements FilesAPI {

	private static final Log Log = LogFactory.getLog(FilesAPIImpl.class);

	protected RegistryExtension _registryExtension;
	protected RegistryUtils _registryUtils;
	protected RegistryExtensionConfig _config;

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

	@OPTIONS
	@Path("{path:.*}")
	@Produces({
		HTRCMediaTypes.ENTRY_XML,
		HTRCMediaTypes.ENTRY_JSON,
		MediaType.APPLICATION_XML,
		MediaType.APPLICATION_JSON
	})
	public Response listContents(
			@Context HttpServletRequest request,
			@Context UriInfo uriInfo,
			@PathParam("path") String path,
			@DefaultValue("false") @QueryParam("recursive") boolean recursive,
			@DefaultValue(".*") @QueryParam("name") String nameFilterRegexp,
			@DefaultValue(".*") @QueryParam("type") String typeFilterRegexp,
			@DefaultValue("false") @QueryParam("public") boolean listPublic) {

		String userName = request.getRemoteUser();

		if(userName != null){
			userName = MultitenantUtils.getTenantAwareUsername(userName);
		}

		if (!path.startsWith("/")) path = "/" + path;

		Log.debug(String.format("listContents: path=%s, nameFilter=%s, typeFilter=%s, recursive=%s, public=%s, user=%s",
				path, nameFilterRegexp, typeFilterRegexp, recursive, listPublic, userName));

		if (userName == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		String filesPath = listPublic ? _config.getPublicFilesPath() : _config.getUserFilesPath(userName);
		String resPath = filesPath + path;

		try {
			Pattern fileNamePattern = Pattern.compile(nameFilterRegexp);
			Pattern mediaTypePattern = Pattern.compile(typeFilterRegexp);

			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			if (!registry.resourceExists(resPath))
				return Response.status(Status.NOT_FOUND).build();

			Entry entry = FileUtils.treeWalk(filesPath, resPath, fileNamePattern, mediaTypePattern, registry, uriInfo.getBaseUri(), recursive);
			if (entry != null)
				return Response.ok(entry).build();
			else
				return Response.noContent().build();
		}
		catch (PatternSyntaxException e) {
			return Response.status(Status.BAD_REQUEST).entity("Regexp syntax error: " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("listContents", e);
			String errorMsg = String.format("Cannot list contents for: %s\n%s", path, e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@HEAD
	@Path("{path:.*}")
	public Response checkIfExists(
			@Context HttpServletRequest request,
			@PathParam("path") String path,
			@DefaultValue("false") @QueryParam("public") boolean checkPublic) {

		String userName = request.getRemoteUser();

		if(userName != null){
			userName = MultitenantUtils.getTenantAwareUsername(userName);
		}

		if (!path.startsWith("/")) path = "/" + path;
		Log.debug(String.format("checkIfExists: path=%s, public=%s, user=%s", path, checkPublic, userName));

		if (userName == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		String filesPath = checkPublic ? _config.getPublicFilesPath() : _config.getUserFilesPath(userName);
		String resPath = filesPath + path;

		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			if (registry.resourceExists(resPath)) {
				Resource resource = registry.get(resPath);
				String type = (resource instanceof Collection) ? "collection" : resource.getMediaType();
				return Response.ok().type(type).build();
			} else
				return Response.status(Status.NOT_FOUND).build();
		}
		catch (Exception e) {
			Log.error("checkIfExists", e);
			String errorMsg = String.format("Cannot verify existence of: %s\n%s", path, e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@GET
	@Path("{path:.*}")
	public Response getFileOrFolder(
			@Context HttpServletRequest request,
			@PathParam("path") String path,
			@DefaultValue(".*") @QueryParam("name") String nameFilterRegexp,
			@DefaultValue(".*") @QueryParam("type") String typeFilterRegexp,
			@DefaultValue("false") @QueryParam("public") boolean getPublic) {

		String userName = request.getRemoteUser();

		if(userName != null){
			userName = MultitenantUtils.getTenantAwareUsername(userName);
		}

		if (!path.startsWith("/")) path = "/" + path;

		Log.debug(String.format("getFileOrFolder: path=%s, nameFilter=%s, typeFilter=%s, public=%s, user=%s",
				path, nameFilterRegexp, typeFilterRegexp, getPublic, userName));

		if (userName == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		String filesPath = getPublic ? _config.getPublicFilesPath() : _config.getUserFilesPath(userName);
		String resPath = filesPath + path;

		try {
			Pattern fileNamePattern = Pattern.compile(nameFilterRegexp);
			Pattern mediaTypePattern = Pattern.compile(typeFilterRegexp);

			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			if (!registry.resourceExists(resPath))
				return Response.status(Status.NOT_FOUND).build();

			Resource resource = registry.get(resPath);
			String mediaType = resource.getMediaType();
			if (resource instanceof Collection) {
				Collection collection = (Collection) resource;
				List<Attachment> attachments = new ArrayList<Attachment>();
				for (String child : collection.getChildren()) {
					try {
						resource = registry.get(child);
					} catch (AuthorizationFailedException afe){
						Log.warn("getFileOrFolder: Registry authorization failure. This should not happen." +
								" But latest version of getChildren returns private resources too.");
						continue;
					}

					if (resource instanceof Collection)
						continue;
					String attachmentName = child.substring(child.lastIndexOf("/") + 1);
					String attachmentType = resource.getMediaType();
					if (FileUtils.filterMatches(attachmentName, attachmentType, fileNamePattern, mediaTypePattern)) {
						try {
							InputStream contentStream = resource.getContentStream();
							Attachment attachment = new Attachment(attachmentName, attachmentType, contentStream);
							attachments.add(attachment);
						}
						catch (RegistryException e) {
							Log.warn("getContentStream: " + e.getMessage() + " - " + child);
							continue;
						}
					}
				}

				if (!attachments.isEmpty()) {
					MultipartBody multipartBody = new MultipartBody(attachments, true);
					return Response.ok(multipartBody).type("multipart/mixed").build();
				} else
					return Response.noContent().build();
			}

			return Response.ok(resource.getContentStream(), mediaType).build();
		}
		catch (PatternSyntaxException e) {
			return Response.status(Status.BAD_REQUEST).entity("Regexp syntax error: " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getFileOrFolder", e);
			String errorMsg = String.format("Cannot retrieve entry: %s\n%s", path, e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@POST
	@Path("{path:.*}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadToFolder(
			@Context HttpServletRequest request,
			@PathParam("path") String path,
			@DefaultValue("false") @QueryParam("public") boolean isPublic,
            @QueryParam("user") String user,
			List<Attachment> attachments) {

		String userName;
		String authenticatedUser = request.getRemoteUser();

		if(authenticatedUser != null){
			authenticatedUser = MultitenantUtils.getTenantAwareUsername(authenticatedUser);
		}

		// Allowing uploads to someone else files directory. (Requirement for Agent)
		// TODO: Check permission of authenticated user.
        if(user != null && user.length() > 0){
            userName = user;
        }else{
            userName  = authenticatedUser;
        }

		if (!path.startsWith("/")) path = "/" + path;
		if (!path.endsWith("/")) path += "/";
		Log.debug(String.format("uploadToFolder: path=%s, public=%s, user=%s", path, isPublic, userName));

		if (authenticatedUser == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			String parentPath = _config.getUserFilesPath(userName) + path;
			String publicFilesPath = _config.getPublicFilesPath();

			registry.beginTransaction();
			try {
				for (Attachment attachment : attachments) {
					MediaType contentType = attachment.getContentType();
					String fileName = attachment.getContentDisposition().getParameter("filename");
					InputStream fileStream = attachment.getDataHandler().getInputStream();

					Resource fileResource = registry.newResource();
					fileResource.setMediaType(contentType.toString());
					fileResource.setContentStream(fileStream);

					String resPath = parentPath + fileName;
					resPath = registry.put(resPath, fileResource);
					Log.debug("Uploaded: " + resPath);

					if (isPublic) {
						_registryUtils.authorizeEveryone(resPath, registry, ActionConstants.GET);
						String symlinkPath = publicFilesPath + path + fileName;
						Log.debug("Creating symlink: " + symlinkPath + " -> " + resPath);
						registry.createLink(symlinkPath, resPath);
					}
				}

				registry.commitTransaction();
			}
			catch (Exception e) {
				registry.rollbackTransaction();
				throw e;
			}

			return Response.noContent().build();
		}
		catch (Exception e) {
			Log.error("uploadToFolder", e);
			String errorMsg = String.format("Cannot upload to folder: %s\n%s", path, e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@PUT
	@Path("{path:.*}")
	public Response uploadFile(
			@Context HttpServletRequest request,
			@HeaderParam("Content-Type") MediaType contentType,
			@PathParam("path") String path,
			@DefaultValue("false") @QueryParam("public") boolean isPublic,
            @QueryParam("user") String user,
			InputStream fileStream) {

		String userName;
		String authenticatedUser = request.getRemoteUser();

		if(authenticatedUser != null){
			authenticatedUser = MultitenantUtils.getTenantAwareUsername(authenticatedUser);
		}

		// Allowing uploads to someone else files directory. (Requirement for Agent)
		// TODO: Check permission of authenticated user.
		if(user != null && user.length() > 0){
			userName = user;
		}else{
			userName  = authenticatedUser;
		}

		if (!path.startsWith("/")) path = "/" + path;

		Log.debug(String.format("uploadFile: path=%s, contentType=%s, public=%s, user=%s", path, contentType, isPublic, userName));

		if (authenticatedUser == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		if (contentType == null)
			contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;

		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);

			String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userName);
			String resPath = _config.getUserFilesPath(tenantAwareUsername) + path;

			registry.beginTransaction();
			try {
				Resource fileResource = registry.newResource();
				fileResource.setMediaType(contentType.toString());
				fileResource.setContentStream(fileStream);

				resPath = registry.put(resPath, fileResource);
				Log.debug("Uploaded: " + resPath);

				if (isPublic) {
					_registryUtils.authorizeEveryone(resPath, registry, ActionConstants.GET);
					String symlinkPath = _config.getPublicFilesPath() + path;
					Log.debug("Creating symlink: " + symlinkPath + " -> " + resPath);
					registry.createLink(symlinkPath, resPath);
				}

				registry.commitTransaction();
			}
			catch (Exception e) {
				registry.rollbackTransaction();
				throw e;
			}

			return Response.noContent().build();
		}
		catch (Exception e) {
			Log.error("uploadFile", e);
			String errorMsg = String.format("Cannot upload file: %s\n%s", path, e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@DELETE
	@Path("{path:.*}")
	public Response deleteFileOrFolder(
			@Context HttpServletRequest request,
			@PathParam("path") String path,
			@DefaultValue("false") @QueryParam("public") boolean delPublic) {

		String userName = request.getRemoteUser();

		if(userName != null){
			userName = MultitenantUtils.getTenantAwareUsername(userName);
		}

		if (!path.startsWith("/")) path = "/" + path;
		Log.debug(String.format("deleteFileOrFolder: path=%s, public=%s, user=%s", path, delPublic, userName));

		if (userName == null)
			return Response.status(Status.UNAUTHORIZED).entity("Not authenticated").type(MediaType.TEXT_PLAIN).build();

		String publicResPath = _config.getPublicFilesPath() + path;
		String privateResPath = _config.getUserFilesPath(userName) + path;
		String resPath = delPublic ? publicResPath : privateResPath;

		try {
			UserRegistry registry = _registryUtils.getUserRegistry(userName);
			if (registry.resourceExists(resPath)) {
				if (delPublic) {
					Resource res = registry.get(resPath);
					boolean isLink = Boolean.parseBoolean(res.getProperty(RegistryConstants.REGISTRY_LINK));
					String author = res.getProperty(RegistryConstants.REGISTRY_AUTHOR);
					String targetPath = res.getProperty(RegistryConstants.REGISTRY_TARGET_POINT);
					if (isLink) {
						if (userName.equals(author)) {
							registry.beginTransaction();
							try {
								Log.debug("Removing symlink: " + resPath + " -> " + targetPath);
								registry.removeLink(resPath);
								_registryUtils.clearEveryone(targetPath, registry, ActionConstants.GET);
								registry.commitTransaction();
								return Response.noContent().build();
							}
							catch (Exception e) {
								registry.rollbackTransaction();
								throw e;
							}
						} else {
							Log.warn(String.format("User '%s' attempted to remove symlink '%s' " +
									"owned by '%s' - action denied.", userName, resPath, author));
							registry.rollbackTransaction();
							return Response.status(Status.UNAUTHORIZED)
									.entity("Cannot remove public entry for which you're not an owner")
									.type(MediaType.TEXT_PLAIN).build();
						}
					}
				}

				Log.debug("Removing resource: " + resPath);
				registry.delete(resPath);
				return Response.noContent().build();
			} else
				return Response.status(Status.NOT_FOUND).build();
		}
		catch (Exception e) {
			Log.error("deleteFileOrFolder", e);
			String errorMsg = String.format("Cannot delete entry: %s\n%s", path, e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

}
