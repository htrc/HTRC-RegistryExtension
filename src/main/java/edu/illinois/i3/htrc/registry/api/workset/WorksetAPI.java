package edu.illinois.i3.htrc.registry.api.workset;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;

/**
 * JAX-RS Interface for managing {@link Workset}
 *
 * @author capitanu
 *
 */

@Produces({
	HTRCMediaTypes.WORKSET_XML,
	HTRCMediaTypes.WORKSET_JSON,
	MediaType.APPLICATION_XML,
	MediaType.APPLICATION_JSON
})
public interface WorksetAPI {

	/**
	 * GET: Retrieve a workset
	 *
	 * @param author The workset author, or null to use current user
	 * @return The retrieved workset
	 */
	@GET
	public Response getWorkset(@QueryParam("author") String author);

	/**
	 * PUT: Update/replace a workset
	 *
	 * @param workset The new workset
	 * @param isPublic True to mark this workset as "public" (shared with everyone), False otherwise
	 * @return 200 if successful, error otherwise
	 */
	@PUT
	@Consumes({
		HTRCMediaTypes.WORKSET_XML,
		HTRCMediaTypes.WORKSET_JSON
	})
	public Response updateWorkset(Workset workset, @DefaultValue("false") @QueryParam("public") boolean isPublic);

	/**
	 * DELETE: Delete a workset
	 *
	 * @return 204 if successful, error otherwise
	 */
	@DELETE
	@Produces(MediaType.WILDCARD)
	public Response deleteWorkset();

	/**
	 * Get access to the API for managing this workset's volumes
	 *
	 * @return The {@link VolumesAPI}
	 */
	@Path("/volumes")
	public VolumesAPI getVolumesAPI();

	/**
	 * GET: Retrieve a workset's metadata
	 *
	 * @param author The workset author, or null to use current user
	 * @return The retrieved workset metadata
	 */
	@GET
	@Path("/metadata")
	public Response getWorksetMeta(@QueryParam("author") String author);

	/**
	 * Get access to the API for managing this workset's tags
	 *
	 * @return The {@link TagsAPI}
	 */
	@Path("/tags")
	public TagsAPI getTagsAPI();

}
