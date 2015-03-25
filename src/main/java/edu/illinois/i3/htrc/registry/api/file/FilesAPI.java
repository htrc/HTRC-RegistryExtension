package edu.illinois.i3.htrc.registry.api.file;

import java.io.InputStream;
import java.util.List;

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
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;

/**
 * JAX-RS API Interface for file storage and access
 *
 * @author capitanu
 *
 */

@Path("/")
public interface FilesAPI {

	/**
	 * OPTIONS: Retrieve the metadata information of the entries found in the user's file space
	 *
	 * @param request Injected HttpServletRequest
	 * @param uriInfo Injected UriInfo
	 * @param path The path in the user's file space to query for entries
	 * @param recursive True to recurse subdirectories, False otherwise
	 * @param nameFilterRegexp Only returns entries whose name matches this regular expression (default: .*)
	 * @param typeFilterRegexp Only returns entries whose media type matches this regular expression (default: .*)
	 * @param listPublic True to retrieve the public entries only (the shared files), False otherwise (default: false)
	 * @return The metadata for the entries found
	 */
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
			@DefaultValue("false") @QueryParam("public") boolean listPublic);

	/**
	 * HEAD: Check whether a file/folder exists
	 *
	 * @param request Injected HttpServletRequest
	 * @param filePath The path to check
	 * @param checkPublic True to check for the path in the public file space, False otherwise
	 * @return 200 if found, 404 if not
	 */
	@HEAD
	@Path("{path:.*}")
	public Response checkIfExists(
			@Context HttpServletRequest request,
			@PathParam("path") String filePath,
			@DefaultValue("false") @QueryParam("public") boolean checkPublic);

	/**
	 * GET: Retrieve a file or folder
	 *
	 * @param request Injected HttpServletRequest
	 * @param filePath The path to retrieve
	 * @param nameFilterRegexp If retrieving a folder, include only entries whose name matches this regexp
	 * @param typeFilterRegexp If retrieving a folder, include only entries whose media type matches this regexp
	 * @param getPublic True to retrieve entries from the public file space, False otherwise
	 * @return The file or folder requested
	 */
	@GET
	@Path("{path:.*}")
	public Response getFileOrFolder(
			@Context HttpServletRequest request,
			@PathParam("path") String filePath,
			@DefaultValue(".*") @QueryParam("name") String nameFilterRegexp,
			@DefaultValue(".*") @QueryParam("type") String typeFilterRegexp,
			@DefaultValue("false") @QueryParam("public") boolean getPublic);

	/**
	 * POST: Upload file(s) to folder
	 *
	 * @param request Injected HttpServletRequest
	 * @param folderPath The path to the folder to upload to
	 * @param isPublic True to upload to the public file space, False otherwise
     * @param user The user on behalf of whom to upload the files
	 * @param attachments The list of files to upload
	 * @return 204 if successful, error otherwise
	 */
	@POST
	@Path("{path:.*}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadToFolder(
			@Context HttpServletRequest request,
			@PathParam("path") String folderPath,
			@DefaultValue("false") @QueryParam("public") boolean isPublic,
            @QueryParam("user") String user,
            List<Attachment> attachments);

	/**
	 * PUT: Upload a file
	 *
	 * @param request Injected HttpServletRequest
	 * @param contentType The file's media type
	 * @param filePath The path to upload to
	 * @param isPublic True to upload to the public file space, False otherwise
     * @param user The user on behalf of whom to upload the files
	 * @param fileStream The file content stream
	 * @return 204 if successful, error otherwise
	 */
	@PUT
	@Path("{path:.*}")
	public Response uploadFile(
			@Context HttpServletRequest request,
			@HeaderParam("Content-Type") MediaType contentType,
			@PathParam("path") String filePath,
			@DefaultValue("false") @QueryParam("public") boolean isPublic,
            @QueryParam("user") String user,
			InputStream fileStream);

	/**
	 * DELETE: Delete a file/folder
	 *
	 * @param request Injected HttpServletRequest
	 * @param path The path to delete
	 * @param delPublic True to delete from the public file space, False otherwise
	 * @return 204 if successful, error otherwise
	 */
	@DELETE
	@Path("{path:.*}")
	public Response deleteFileOrFolder(
			@Context HttpServletRequest request,
			@PathParam("path") String path,
			@DefaultValue("false") @QueryParam("public") boolean delPublic);

}
