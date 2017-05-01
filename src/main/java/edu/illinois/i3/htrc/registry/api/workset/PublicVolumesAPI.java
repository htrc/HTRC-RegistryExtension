package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * JAX-RS API Interface for accessing workset volumes of public worksets
 *
 * @author capitanu
 */

@Produces({
        HTRCMediaTypes.VOLUME_XML,
        HTRCMediaTypes.VOLUME_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_JSON
})
public interface PublicVolumesAPI {

    /**
     * GET: Retrieve workset volumes as plain-text
     *
     * @param author The workset author
     * @return The workset volumes
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getVolumesAsPlainText(@QueryParam("author") String author);

    /**
     * GET: Retrieve workset volumes as CSV
     *
     * @param author The workset author
     * @return The workset volumes
     */
    @GET
    @Produces(HTRCMediaTypes.TEXT_CSV)
    public Response getVolumesAsCSV(@QueryParam("author") String author);

    /**
     * GET: Retrieve the workset volumes as XML or JSON
     *
     * @param author The workset author
     * @return The workset volumes
     */
    @GET
    public Response getVolumes(@QueryParam("author") String author);

}
