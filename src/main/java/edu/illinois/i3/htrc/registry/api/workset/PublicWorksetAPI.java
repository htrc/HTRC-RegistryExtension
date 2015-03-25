package edu.illinois.i3.htrc.registry.api.workset;

import javax.ws.rs.GET;
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
public interface PublicWorksetAPI {

	/**
	 * GET: Retrieve a public workset
	 *
	 * @param author The workset author
	 * @return The retrieved workset
	 */
	@GET
	public Response getPublicWorkset(@QueryParam("author") String author);

	/**
	 * Get access to the API for managing this workset's volumes
	 *
	 * @return The {@link VolumesAPI}
	 */
	@Path("/volumes")
	public PublicVolumesAPI getPublicVolumesAPI();

	/**
	 * Get access to the API for managing this workset's tags
	 *
	 * @return The {@link TagsAPI}
	 */
	@Path("/tags")
	public TagsAPI getTagsAPI();

}
