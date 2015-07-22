package edu.illinois.i3.htrc.registry.api.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.UniqueHashCode;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourcePath;
import org.wso2.carbon.registry.core.Tag;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import edu.illinois.i3.htrc.registry.api.Constants;
import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.entities.workset.Comment;
import edu.illinois.i3.htrc.registry.entities.workset.ExternalWorkset;
import edu.illinois.i3.htrc.registry.entities.workset.Property;
import edu.illinois.i3.htrc.registry.entities.workset.Volume;
import edu.illinois.i3.htrc.registry.entities.workset.Volumes;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetContent;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;

/**
 * Workset utility helper methods
 *
 * @author capitanu
 *
 */
public class WorksetUtils {

    private static final Log Log = LogFactory.getLog(WorksetUtils.class);
    private static final JAXBContext jaxbContext;
    private static final Pattern IllegalWorksetCharactersPattern;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(Volumes.class, Volume.class, ExternalWorkset.class);
            IllegalWorksetCharactersPattern = Pattern.compile(Constants.ILLEGAL_CHARACTERS_FOR_PATH);
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
     * Construct a {@link Workset} from a registry resource
     *
     * @param resource The resource
     * @param registry The {@link UserRegistry} instance
     * @return The {@link Workset}
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static Workset getWorksetFromResource(Resource resource, UserRegistry registry) throws RegistryException {
        String sWorksetClass = resource.getProperty(Constants.HTRC_PROP_WORKSETCLASS);
        if (sWorksetClass == null) sWorksetClass = "1";

        WorksetMeta worksetMeta = getWorksetMetaFromResource(resource, registry);
        WorksetContent worksetContent = getWorksetContentFromResource(resource);

        Workset workset = new Workset();
        workset.setMetadata(worksetMeta);
        workset.setContent(worksetContent);
        workset.setWorksetClass(Integer.parseInt(sWorksetClass));

        return workset;
    }

    /**
     * Construct a {@link WorksetContent} from a registry resource
     *
     * @param resource The resource
     * @return The {@link WorksetContent}
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static WorksetContent getWorksetContentFromResource(Resource resource) throws RegistryException {
        WorksetContent worksetContent = new WorksetContent();
        try {
            Volumes volumes = getWorksetVolumesFromResource(resource);
            if (volumes != null)
                worksetContent.setVolumes(volumes.getVolumes());
            else
                worksetContent = null;
        }
        catch (JAXBException e) {
            throw new RegistryException("Cannot unmarshal workset volumes", e);
        }

        return worksetContent;
    }

    /**
     * Construct a {@link WorksetMeta} from a registry resource
     *
     * @param resource The resource
     * @param registry The {@link UserRegistry} instance
     * @return The {@link WorksetMeta}
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static WorksetMeta getWorksetMetaFromResource(Resource resource, UserRegistry registry) throws RegistryException {
        String resPath = resource.getPath();
        String[] versions = registry.getVersions(resPath);
        Tag[] tags = registry.getTags(resPath);
        org.wso2.carbon.registry.core.Comment[] comments = registry.getComments(resPath);
        String name = resPath.substring(resPath.lastIndexOf("/") + 1);
        Long version = versions.length > 0 ?
                org.wso2.carbon.registry.core.utils.RegistryUtils.getVersionedPath(new ResourcePath(versions[0])).getVersion() : null;

        WorksetMeta worksetMeta = new WorksetMeta();
        worksetMeta.setName(name);
        worksetMeta.setDescription(resource.getDescription());
        worksetMeta.setAuthor(resource.getAuthorUserName());
        worksetMeta.setVersion(version);
        worksetMeta.setRating(registry.getRating(resPath, registry.getUserName()));
        worksetMeta.setAvgRating(registry.getAverageRating(resPath));
        String sVolCount = resource.getProperty(Constants.HTRC_PROP_VOLCOUNT);
        if (sVolCount == null)
            Log.warn(String.format("Missing property: '%s' for workset: %s", Constants.HTRC_PROP_VOLCOUNT, resPath));
        int volumeCount = (sVolCount != null) ? Integer.parseInt(sVolCount) : -1;
        worksetMeta.setVolumeCount(volumeCount);

        try {
            worksetMeta.setPublic(RegistryUtils.isEveryoneAuthorized(resPath, registry, ActionConstants.GET));
        }
        catch (Exception e) {
            throw new RegistryException("Error getting resource permissions", e);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(resource.getLastModified());
        worksetMeta.setLastModified(calendar);
        worksetMeta.setLastModifiedBy(resource.getLastUpdaterUserName());

        for (Tag tag : tags)
            worksetMeta.getTags().add(tag.getTagName());

        for (org.wso2.carbon.registry.core.Comment comment : comments) {
            Comment c = new Comment();
            c.setAuthor(comment.getAuthorUserName());
            c.setText(comment.getText());
            calendar.setTime(comment.getCreatedTime());
            c.setCreated(calendar);
            calendar.setTime(comment.getLastModified());
            c.setLastModified(calendar);
            worksetMeta.getComments().add(c);
        }

        return worksetMeta;
    }

    /**
     * Create a registry resource from a {@link Workset}
     *
     * @param workset The workset
     * @param registry The {@link UserRegistry} instance
     * @return The registry {@link Resource} created
     * @throws RegistryException Thrown if a registry error occurs
     * @throws JAXBException Thrown if a serialization error occurs
     */
    public static Resource createResourceFromWorkset(Workset workset, UserRegistry registry) throws RegistryException, JAXBException {
        WorksetMeta worksetMeta = workset.getMetadata();
        WorksetContent worksetContent = workset.getContent();

        Resource resource = registry.newResource();
        resource.setDescription(worksetMeta.getDescription());
        resource.setMediaType(HTRCMediaTypes.WORKSET_XML);
        Integer volumeCount = -1;

        Integer worksetClass = workset.getWorksetClass();
        if (worksetClass == null) worksetClass = 1;

        if (worksetContent != null) {
            switch (worksetClass) {
                case 1:
                    if (worksetContent.getVolumes() != null) {
                        List<Volume> volumes = worksetContent.getVolumes();
                        volumeCount = volumes.size();
                        resource.setContentStream(createWorksetContentStream(volumes));
                    } else
                        throw new RegistryException("Malformed workset - missing <volumes>");
                    break;

                case 2:
                    ExternalWorkset externalWorkset = worksetContent.getExternalWorkset();
                    if (externalWorkset != null) {
                        if (worksetMeta.getVolumeCount() != null)
                            volumeCount = worksetMeta.getVolumeCount();
                        resource.setContentStream(createWorksetContentStream(externalWorkset));
                    } else
                        throw new RegistryException("Malformed workset - missing <externalWorkset>");
                    break;

                default:
                    Log.error("Unknown workset class: " + worksetClass);
                    throw new RegistryException("Unknown workset class: " + worksetClass);
            }
        }

        resource.setProperty(Constants.HTRC_PROP_VOLCOUNT, Integer.toString(volumeCount));
        resource.setProperty(Constants.HTRC_PROP_WORKSETCLASS, Integer.toString(worksetClass));

        return resource;
    }

    /**
     * Update community metadata for a registry resource representing a workset
     *
     * @param resource The resource
     * @param meta The {@link WorksetMeta} instance holding the community metadata
     * @param registry The {@link UserRegistry} instance
     * @param registryUtils The {@link RegistryUtils} instance
     * @return The updated metadata
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static WorksetMeta updateResourceCommunityMeta(Resource resource, WorksetMeta meta, UserRegistry registry, RegistryUtils registryUtils) throws RegistryException {
        String resPath = resource.getPath();
        for (String tag : meta.getTags())
            registry.applyTag(resPath, tag);
        for (Comment c : meta.getComments()) {
            Registry commentAuthorRegistry = registryUtils.getUserRegistry(c.getAuthor());
            commentAuthorRegistry.addComment(resPath, new org.wso2.carbon.registry.core.Comment(c.getText()));
        }
        if (meta.getRating() != null)
            registry.rateResource(resPath, meta.getRating());

        return getWorksetMetaFromResource(resource, registry);
    }

    /**
     * Serialize a list of volumes to a stream
     *
     * @param volumes The list of volumes
     * @return The {@link InputStream} containing the serialized volumes
     * @throws JAXBException Thrown if a serialization error occurs
     */
    public static InputStream createWorksetContentStream(List<Volume> volumesList) throws JAXBException {
        Volumes volumes = new Volumes();
        volumes.getVolumes().addAll(volumesList);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        createMarshaller().marshal(volumes, baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Serialize an externalWorkset to a stream
     *
     * @param externalWorkset The externalWorkset
     * @return The {@link InputStream} containing the serialized externalWorkset
     * @throws JAXBException Thrown if a serialization error occurs
     */
    public static InputStream createWorksetContentStream(ExternalWorkset externalWorkset) throws JAXBException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        createMarshaller().marshal(externalWorkset, baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static ExternalWorkset getExternalWorkset(Resource resource) throws JAXBException, RegistryException {
        return resource.getContent() != null ?
                (ExternalWorkset)createUnmarshaller().unmarshal(resource.getContentStream()) : null;
    }

    /**
     * Construct a {@link Volumes} instance containing volumes stored in a registry resource
     *
     * @param resource The resource
     * @return The volumes
     * @throws JAXBException Thrown if a de-serialization error occurs
     * @throws RegistryException Thrown if a registry error occurs
     */
    public static Volumes getWorksetVolumesFromResource(Resource resource) throws JAXBException, RegistryException {
        if (resource.getContent() == null)
            return null;

        String sWorksetClass = resource.getProperty(Constants.HTRC_PROP_WORKSETCLASS);
        if (sWorksetClass == null) sWorksetClass = "1";

        Volumes volumes = null;

        try {
            switch (Integer.parseInt(sWorksetClass)) {
                case 1:
                    volumes = (Volumes)createUnmarshaller().unmarshal(resource.getContentStream());
                    break;

                case 2:
                    ExternalWorkset externalWorkset = getExternalWorkset(resource);
                    volumes = getWorksetVolumesFromExternalWorkset(externalWorkset);
                    break;

                default:
                    Log.error("Unknown workset class: " + sWorksetClass);
                    throw new RegistryException("Unknown workset class: " + sWorksetClass);
            }
        }
        catch (JAXBException e) {
            Log.error("Cannot unmarshal volumes for resource: " + resource.getPath(), e);
            throw new RegistryException("JAXBException", e);
        }

        return volumes;
    }

    /**
     * Retrieves the volumes and associated API service endpoint for an external workset
     *
     * @param externalWorkset The external workset
     * @return The volumes
     * @throws RegistryException Thrown if unable to retrieve the volumes of the external workset
     */
    public static Volumes getWorksetVolumesFromExternalWorkset(ExternalWorkset externalWorkset) throws RegistryException {
        String ep = externalWorkset.getSparqlEndPoint();
        String query = externalWorkset.getGetVolsQuery();
        String reqUrl = String.format("%s%s", ep, query);

        Reader csvReader = null;
        try {
            URI uri = new URI(reqUrl);
            csvReader = IOUtils.getReaderForResource(uri, 1000, 3000);
        }
        catch(IOException | URISyntaxException e) {
            throw new RegistryException("Cannot retrieve response from SPARQL endpoint", e);
        }

        Volumes volumes = new Volumes();
        List<Volume> volumesList = volumes.getVolumes();

        try {
            ICsvMapReader mapReader = null;
            try {
                mapReader = new CsvMapReader(csvReader, CsvPreference.STANDARD_PREFERENCE);
                final String[] header = mapReader.getHeader(true);
                final CellProcessor[] processors = getProcessors();
                Map<String, Object> volumesServiceMap = null;
                while ((volumesServiceMap = mapReader.read(header, processors)) != null) {
                    String volId = (String) volumesServiceMap.get(header[0]);
                    String volEpr = (String) volumesServiceMap.get(header[1]);

                    Volume volume = new Volume();
                    volume.setId(volId);

                    Property propVolEpr = new Property();
                    propVolEpr.setName(Constants.HTRC_VOL_PROP_EPR);
                    propVolEpr.setValue(volEpr);

                    List<Property> volProperties = volume.getProperties();
                    volProperties.add(propVolEpr);

                    volumesList.add(volume);
                }
            }
            finally {
                if (mapReader != null)
                    mapReader.close();
            }
        }
        catch (Exception e) {
            throw new RegistryException("Error while parsing SPARQL response", e);
        }

        return volumes;
    }

    private static CellProcessor[] getProcessors() {
        final CellProcessor[] processors = new CellProcessor[] {
                new UniqueHashCode(), // volume id
                new Optional()        // service endpoint
        };

        return processors;
    }

    /**
     * Return the URI for a workset path
     *
     * @param worksetResPath The workset path
     * @return The URI
     */
    public static URI getWorksetUri(String worksetResPath) {
        try {
            return new URI(worksetResPath);
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    private static Marshaller createMarshaller(){
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            return marshaller;
        }
        catch (JAXBException e) {
            throw  new RuntimeException("Cannot create marshaller.", e);
        }
    }

    private static Unmarshaller createUnmarshaller(){
        try {
            return jaxbContext.createUnmarshaller();
        }
        catch (JAXBException e) {
            throw  new RuntimeException("Cannot create unmarshaller.", e);
        }
    }
}
