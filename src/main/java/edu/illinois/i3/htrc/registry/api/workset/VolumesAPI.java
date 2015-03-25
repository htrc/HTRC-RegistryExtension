package edu.illinois.i3.htrc.registry.api.workset;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.entities.workset.Volumes;

/**
 * JAX-RS API Interface for accessing workset volumes
 *
 * @author capitanu
 *
 */

@Produces({
	HTRCMediaTypes.VOLUME_XML,
	HTRCMediaTypes.VOLUME_JSON,
	MediaType.APPLICATION_XML,
	MediaType.APPLICATION_JSON
})
public interface VolumesAPI {

	/**
	 * GET: Retrieve workset volumes as plain-text
	 *
	 * @param author The workset author, or null to use current user
	 * @return The workset volumes
	 */
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVolumesAsPlainText(@QueryParam("author") String author);

	/**
	 * GET: Retrieve workset volumes as CSV
	 *
	 * @param author The workset author, or null to use current user
	 * @return The workset volumes
	 */
	@GET
	@Produces(HTRCMediaTypes.TEXT_CSV)
	public Response getVolumesAsCSV(@QueryParam("author") String author);

	/**
	 * GET: Retrieve the workset volumes as XML or JSON
	 *
	 * @param author The workset author, or null to use current user
	 * @return The workset volumes
	 */
	@GET
	public Response getVolumes(@QueryParam("author") String author);

	/**
	 * PUT: Replace the volumes of a workset
	 *
	 * @param volumes The new volumes
	 * @return 200 if successful, error otherwise
	 */
	@PUT
	@Consumes({
		HTRCMediaTypes.VOLUME_XML,
		HTRCMediaTypes.VOLUME_JSON
	})
	public Response replaceVolumes(Volumes volumes);

}
