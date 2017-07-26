package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * JAX-RS Interface for accessing Worksets
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
public interface WorksetsAPI {

    /**
     * GET: Retrieve a user's list of worksets
     *
     * @param includePublic True to include "public" worksets (shared with everyone), False
     *                      otherwise
     * @return The list of worksets for the currently authorized user
     */
    @GET
    Response getWorksets(@DefaultValue("false") @QueryParam("public") boolean includePublic);

    /**
     * POST: Upload a new workset
     *
     * @param workset  The workset
     * @param isPublic True to make this workset "public" (shared with everyone), False otherwise
     * @return The workset
     */
    @POST
    @Consumes({
        HTRCMediaTypes.WORKSET_XML,
        HTRCMediaTypes.WORKSET_JSON
    })
    Response newWorkset(
        Workset workset,
        @DefaultValue("false") @QueryParam("public") boolean isPublic);

    /**
     * Get access to a {@link WorksetAPI} instance used to manage individual worksets
     *
     * @param id The workset id (name)
     * @return The {@link WorksetAPI}
     */
    @Path("/{id}")
    WorksetAPI getWorksetAPI(@PathParam("id") String id);

}
