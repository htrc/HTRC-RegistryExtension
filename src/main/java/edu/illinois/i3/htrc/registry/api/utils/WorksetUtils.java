package edu.illinois.i3.htrc.registry.api.utils;

import edu.illinois.i3.htrc.registry.api.Constants;
import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.entities.workset.Volume;
import edu.illinois.i3.htrc.registry.entities.workset.Volumes;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetContent;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.CollectionImpl;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.Tag;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDataAccessManager;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 * Workset utility helper methods
 *
 * @author capitanu
 */
public class WorksetUtils {

    private static final Log Log = LogFactory.getLog(WorksetUtils.class);
    private static final JAXBContext jaxbContext;
    private static final Pattern IllegalWorksetCharactersPattern;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(Volumes.class, Volume.class);
            IllegalWorksetCharactersPattern = Pattern
                .compile(Constants.ILLEGAL_CHARACTERS_FOR_PATH);
        }
        catch (Exception e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks workset name validity
     *
     * @param name The name to check
     * @return True if valid, False otherwise
     */
    public static boolean isLegalWorksetName(String name) {
        return !IllegalWorksetCharactersPattern.matcher(name).matches();
    }

    /**
     * Create a registry resource from a {@link Workset}
     *
     * @param workset  The workset
     * @param registry The {@link UserRegistry} instance
     * @return The registry {@link Resource} created
     * @throws RegistryException Thrown if a registry error occurs
     * @throws JAXBException     Thrown if a serialization error occurs
     */
    public static Resource createResourceFromWorkset(Workset workset, UserRegistry registry)
        throws RegistryException, JAXBException {
        WorksetMeta worksetMeta = workset.getMetadata();
        WorksetContent worksetContent = workset.getContent();

        Resource resource = registry.newResource();
        resource.setDescription(worksetMeta.getDescription());
        resource.setMediaType(HTRCMediaTypes.WORKSET_XML);
        int volumeCount = 0;

        if (worksetContent != null) {
            List<Volume> volumes = worksetContent.getVolumes();
            volumeCount = volumes.size();
            resource.setContentStream(createWorksetContentStream(volumes));
        }

        resource.setProperty(Constants.HTRC_PROP_VOLCOUNT, Integer.toString(volumeCount));

        return resource;
    }

    /**
     * Serialize a list of volumes to a stream
     *
     * @param volumesList The list of volumes
     * @return The {@link InputStream} containing the serialized volumes
     * @throws JAXBException Thrown if a serialization error occurs
     */
    public static InputStream createWorksetContentStream(List<Volume> volumesList)
        throws JAXBException {
        Volumes volumes = new Volumes();
        volumes.getVolumes().addAll(volumesList);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        createMarshaller().marshal(volumes, baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    private static Marshaller createMarshaller() {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            return marshaller;
        }
        catch (JAXBException e) {
            throw new RuntimeException("Cannot create marshaller.", e);
        }
    }

    /**
     * Update community metadata for a registry resource representing a workset
     *
     * @param resource The resource
     * @param meta     The {@link WorksetMeta} instance holding the community metadata
     * @param registry The {@link UserRegistry} instance
     * @return The updated metadata
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static WorksetMeta updateResourceCommunityMeta(
        Resource resource, WorksetMeta meta,
        UserRegistry registry) throws RegistryException {
        String resPath = resource.getPath();
        for (String tag : meta.getTags()) {
            registry.applyTag(resPath, tag);
        }

        return getWorksetMetaFromResource(resource, registry);
    }

    /**
     * Construct a {@link WorksetMeta} from a registry resource
     *
     * @param resource The resource
     * @param registry The {@link UserRegistry} instance
     * @return The {@link WorksetMeta}
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static WorksetMeta getWorksetMetaFromResource(Resource resource, UserRegistry registry)
        throws RegistryException {
        String resPath = resource.getPath();
        Tag[] tags = registry.getTags(resPath);
        String name = resPath.substring(resPath.lastIndexOf("/") + 1);

        WorksetMeta worksetMeta = new WorksetMeta();
        worksetMeta.setName(name);
        worksetMeta.setDescription(resource.getDescription());
        String authorUserName = resource.getAuthorUserName();
        String authorGuid = RegistryUtils.getUserGuid(authorUserName);
        String authorAlias = RegistryUtils.getUserAlias(authorUserName);
        worksetMeta.setAuthorId(authorGuid);
        worksetMeta.setAuthorAlias(authorAlias);
        String sVolCount = resource.getProperty(Constants.HTRC_PROP_VOLCOUNT);
        if (sVolCount == null) {
            Log.warn(
                String.format(
                    "Missing property: '%s' for workset: %s",
                    Constants.HTRC_PROP_VOLCOUNT,
                    resPath
                ));
        }
        int volumeCount = (sVolCount != null) ? Integer.parseInt(sVolCount) : -1;
        worksetMeta.setVolumeCount(volumeCount);

        try {
            boolean isPublic = RegistryUtils.isPublicResource(resPath, registry);
            worksetMeta.setPublic(isPublic);
        }
        catch (Exception e) {
            throw new RegistryException("Error getting resource permissions", e);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(resource.getLastModified());
        worksetMeta.setLastModified(calendar);

        calendar = Calendar.getInstance();
        calendar.setTime(resource.getCreatedTime());
        worksetMeta.setCreated(calendar);

        for (Tag tag : tags) {
            worksetMeta.getTags().add(tag.getTagName());
        }

        return worksetMeta;
    }

    /**
     * Return the URI component for a workset name
     *
     * @param worksetName The workset name
     * @return The URI
     */
    public static URI getWorksetNameUri(String worksetName) {
        try {
            return new URI(null, null, null, -1, "./" + worksetName, null, null);
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Creates a collection of all public worksets resources
     *
     * @param context The servlet context to retrieve the query info from
     * @return The collection of all public worksets resources
     * @throws SQLException Thrown if an error occurs during communication with the database
     */
    public static Collection getPublicWorksetsCollection(ServletContext context)
        throws SQLException {
        Set<String> pathList = WorksetUtils.getPublicWorksetsPathsViaSQL(context);
        return getCollectionFromPaths(pathList);
    }

    /**
     * Retrieves the registry paths of all the public worksets
     *
     * @param context The servlet context used to retrieve the query configuration
     * @return The set of paths of all public worksets
     * @throws SQLException Thrown if an error occurs when communicating with the database
     */
    public static Set<String> getPublicWorksetsPathsViaSQL(ServletContext context)
        throws SQLException {
        RegistryContext registryContext = RegistryExtension.getRegistryContext();
        DataSource dataSource = ((JDBCDataAccessManager)
            registryContext.getDataAccessManager()).getDataSource();
        String sqlPublicWorksets =
            context.getInitParameter(Constants.WEBXML_CONFIG_PUBLIC_WORKSETS_QUERY);
        String allUsersWorksetsPathsSql =
            RegistryExtension.getConfig().getUserWorksetsPath("%");

        Set<String> pathList = new LinkedHashSet<>();

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sqlPublicWorksets);
            stmt.setString(1, allUsersWorksetsPathsSql);
            stmt.setString(2, "everyone");
            stmt.setString(3, ActionConstants.GET);
            stmt.closeOnCompletion();

            try (ResultSet results = stmt.executeQuery()) {
                while (results.next()) {
                    pathList.add(results.getString("WS_PATH"));
                }
            }
        }

        return pathList;
    }

    /**
     * Converts a set of registry paths into a collection
     *
     * @param paths The paths
     * @return The collection
     */
    public static Collection getCollectionFromPaths(java.util.Collection<String> paths) {
        return new CollectionImpl(paths.toArray(new String[paths.size()]));
    }

    /**
     * Retrieves the worksets paths from a given collection
     *
     * @param worksetCollection The collection
     * @return The worksets paths
     * @throws RegistryException Thrown if an error occurs while accessing the registry
     */
    public static Set<String> getWorksetsPaths(Collection worksetCollection)
        throws RegistryException {
        String[] children = worksetCollection.getChildren();
        return new HashSet<>(Arrays.asList(children));
    }

    /**
     * Get the list of workset metadata for worksets in a registry collection
     *
     * @param collection The registry collection
     * @param registry   The {@link UserRegistry} instance
     * @return The list of workset metadata
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static List<WorksetMeta> getWorksetsMeta(Collection collection, UserRegistry registry)
        throws RegistryException {

        String[] children = collection.getChildren();
        List<WorksetMeta> worksetsMeta = new ArrayList<>(children.length);

        for (String child : children) {
            try {
                Resource resource = registry.get(child);

                if (Log.isDebugEnabled()) {
                    LogUtils.logResource(Log, resource);
                }

                WorksetMeta worksetMeta =
                    WorksetUtils.getWorksetMetaFromResource(resource, registry);
                worksetsMeta.add(worksetMeta);
            }
            catch (AuthorizationFailedException afe) {
                Log.warn(String.format(
                    "getWorksets: Registry authorization failure for '%s' (Message: %s)",
                    child, afe.getMessage()
                ));
            }
        }

        return worksetsMeta;
    }

    /**
     * Get the list of worksets in a registry collection
     *
     * @param collection The registry collection
     * @param registry   The {@link UserRegistry} instance
     * @return The list of worksets
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static List<Workset> getWorksets(Collection collection, UserRegistry registry)
        throws RegistryException {

        List<Workset> worksets = new ArrayList<>();
        for (String child : collection.getChildren()) {
            try {
                Resource resource = registry.get(child);

                if (Log.isDebugEnabled()) {
                    LogUtils.logResource(Log, resource);
                }

                Workset workset = WorksetUtils.getWorksetFromResource(resource, registry);
                worksets.add(workset);
            }
            catch (AuthorizationFailedException afe) {
                Log.warn(String.format(
                    "getWorksets: Registry authorization failure for '%s' (Message: %s)",
                    child, afe.getMessage()
                ));
            }
        }

        return worksets;
    }

    /**
     * Construct a {@link Workset} from a registry resource
     *
     * @param resource The resource
     * @param registry The {@link UserRegistry} instance
     * @return The {@link Workset}
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static Workset getWorksetFromResource(Resource resource, UserRegistry registry)
        throws RegistryException {
        WorksetMeta worksetMeta = getWorksetMetaFromResource(resource, registry);
        WorksetContent worksetContent = getWorksetContentFromResource(resource);

        Workset workset = new Workset();
        workset.setMetadata(worksetMeta);
        workset.setContent(worksetContent);

        return workset;
    }

    /**
     * Construct a {@link WorksetContent} from a registry resource
     *
     * @param resource The resource
     * @return The {@link WorksetContent}
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static WorksetContent getWorksetContentFromResource(Resource resource)
        throws RegistryException {
        WorksetContent worksetContent = new WorksetContent();

        try {
            Volumes volumes = getWorksetVolumesFromResource(resource);
            if (volumes != null) {
                worksetContent.setVolumes(volumes.getVolumes());
            }
            else {
                worksetContent = null;
            }
        }
        catch (JAXBException e) {
            Log.error("Cannot unmarshal volumes for resource: " + resource.getPath(), e);
            throw new RegistryException("JAXBException", e);
        }

        return worksetContent;
    }

    /**
     * Construct a {@link Volumes} instance containing volumes stored in a registry resource
     *
     * @param resource The resource
     * @return The volumes
     * @throws JAXBException     Thrown if a de-serialization error occurs
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static Volumes getWorksetVolumesFromResource(Resource resource)
        throws JAXBException, RegistryException {
        try (InputStream contentStream = resource.getContentStream()) {
            return resource.getContent() != null ?
                (Volumes) createUnmarshaller().unmarshal(contentStream) : null;
        }
        catch (IOException e) {
            throw new RegistryException("Error closing resource stream for: "
                                            + resource.getPath(), e);
        }
    }

    private static Unmarshaller createUnmarshaller() {
        try {
            return jaxbContext.createUnmarshaller();
        }
        catch (JAXBException e) {
            throw new RuntimeException("Cannot create unmarshaller.", e);
        }
    }
}
