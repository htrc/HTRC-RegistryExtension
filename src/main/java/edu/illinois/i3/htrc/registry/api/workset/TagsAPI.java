package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * JAX-RS API Interface for accessing workset tags
 *
 * @author capitanu
 */

@Produces({
        HTRCMediaTypes.TAG_XML,
        HTRCMediaTypes.TAG_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_JSON
})
public interface TagsAPI {

    /**
     * GET: Retrieve the list of tags for a workset
     *
     * @param author The workset author, or null to use current user
     * @return The tags
     */
    @GET
    public Response getTags(@QueryParam("author") String author);

}
