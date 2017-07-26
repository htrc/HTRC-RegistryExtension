package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * JAX-RS Interface for accessing public Worksets
 *
 * @author capitanu
 */

@Path("/")
@Produces({
    HTRCMediaTypes.WORKSET_XML,
    HTRCMediaTypes.WORKSET_JSON,
    MediaType.APPLICATION_XML,
    MediaType.APPLICATION_JSON
})
public interface PublicWorksetsAPI {

    /**
     * GET: Retrieve list of public worksets
     *
     * @return The list of public worksets
     */
    @GET
    Response getPublicWorksets();

    /**
     * Get access to a {@link PublicWorksetAPI} instance used to manage individual public worksets
     *
     * @param id The workset id (name)
     * @return The {@link PublicWorksetAPI}
     */
    @Path("/{id}")
    PublicWorksetAPI getPublicWorksetAPI(@PathParam("id") String id);

}
