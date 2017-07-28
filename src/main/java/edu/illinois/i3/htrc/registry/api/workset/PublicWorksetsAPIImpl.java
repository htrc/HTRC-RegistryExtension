package edu.illinois.i3.htrc.registry.api.workset;

import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;
import edu.illinois.i3.htrc.registry.entities.workset.Worksets;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 * JAX-RS Implementation for {@link PublicWorksetsAPI}
 *
 * @author capitanu
 */
public class PublicWorksetsAPIImpl implements PublicWorksetsAPI {

    private static final Log Log = LogFactory.getLog(PublicWorksetsAPIImpl.class);

    protected @Context
    HttpServletRequest _request;

    private RegistryExtensionConfig _config;
    private ServletContext _context;

    /**
     * Injected ServletContext
     *
     * @param context The servlet context
     */
    @javax.annotation.Resource
    public void setServletContext(ServletContext context) {
        _config = RegistryExtension.getConfig();
        _context = context;
    }

    @GET
    public Response getPublicWorksets() {
        Log.info("getPublicWorksets");

        Worksets worksets = new Worksets();
        List<Workset> worksetList = worksets.getWorksets();

        try {
            UserRegistry adminRegistry = RegistryUtils.getAdminRegistry();
            Collection publicWorksets = WorksetUtils.getPublicWorksetsCollection(_context);
            List<WorksetMeta> metas = WorksetUtils.getWorksetsMeta(publicWorksets, adminRegistry);
            for (WorksetMeta meta : metas) {
                Workset workset = new Workset();
                workset.setMetadata(meta);
                worksetList.add(workset);
            }
        }
        catch (Exception e) {
            Log.error("getPublicWorksets", e);
            String errorMsg = String.format("Cannot retrieve public worksets: %s", e.toString());
            return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
        }

        return Response.ok(worksets).build();
    }

    @Path("/{worksetId}")
    public PublicWorksetAPI getPublicWorksetAPI(@PathParam("worksetId") String worksetId) {
        try {
            UserRegistry registry = RegistryUtils.getAdminRegistry();
            return new PublicWorksetAPIImpl(worksetId, registry);
        }
        catch (RegistryException e) {
            Log.error("getPublicWorksetAPI", e);
            String errorMsg = String.format("Error processing workset API: %s", e.toString());
            throw new WebApplicationException(
                Response.serverError()
                        .entity(errorMsg)
                        .type(MediaType.TEXT_PLAIN)
                        .build());
        }
    }

}
