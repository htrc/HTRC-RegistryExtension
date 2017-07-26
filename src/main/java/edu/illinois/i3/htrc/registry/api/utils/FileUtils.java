package edu.illinois.i3.htrc.registry.api.utils;

import edu.illinois.i3.htrc.registry.entities.file.Entry;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 * File access utility helper methods
 *
 * @author capitanu
 */
public class FileUtils {

    private static final Log Log = LogFactory.getLog(FileUtils.class);

    /**
     * Return a URI for a resource path
     *
     * @param resPath The resource path
     * @return The URI
     */
    public static URI getFileUri(String resPath) {
        try {
            return new URI(resPath);
        }
        catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Return an {@link Entry} representing file/folder metadata for a resource
     *
     * @param resource  The file/folder resource
     * @param filesPath The base path for files
     * @param baseUri   The request base URI
     * @return The entry bean representing the file/folder
     * @throws RegistryException Thrown if an error occurs when accessing the registry
     */
    public static Entry getEntryForResource(Resource resource, String filesPath, URI baseUri)
        throws RegistryException {
        String resPath = resource.getPath();
        assert (resPath.startsWith(filesPath));
        String relPath = resPath.substring(filesPath.length());
        Entry entry = new Entry();
        if (resource instanceof Collection) {
            entry.setContentType("collection");
        }
        else {
            entry.setContentType(resource.getMediaType());
        }
        String parentPath =
            relPath.isEmpty() ? null : relPath.substring(0, relPath.lastIndexOf("/") + 1);
        entry.setParentPath(parentPath);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(resource.getCreatedTime());
        entry.setCreatedTime(calendar);

        calendar = Calendar.getInstance();
        calendar.setTime(resource.getLastModified());
        entry.setLastModified(calendar);

        entry.setAuthor(resource.getAuthorUserName());
        entry.setLastModifiedBy(resource.getLastUpdaterUserName());
        entry.setDescription(resource.getDescription());
        String name = parentPath == null ? "/" : relPath.substring(parentPath.length());
        entry.setName(name);
        URI fileRelUri = getFileUri(relPath);
        entry.setUrl(baseUri.toString() + fileRelUri.toString());

        return entry;
    }

    /**
     * Construct metadata for a hierarchical file/folder tree
     *
     * @param filesPath The path to the file space
     * @param path      The root path to start at
     * @param registry  The registry instance
     * @param baseUri   The request base URI
     * @param recursive True to recurse into subdirectories, False otherwise
     * @return The hierarchical metadata
     * @throws RegistryException Thrown if an error occurs when accessing the registry
     */
    public static Entry treeWalk(
        String filesPath, String path, UserRegistry registry, URI baseUri,
        boolean recursive) throws RegistryException {
        return treeWalk(
            filesPath, path, null, null, registry, baseUri, recursive
        );
    }

    /**
     * Construct metadata for a hierarchical file/folder tree
     *
     * @param filesPath        The path to the file space
     * @param path             The root path to start at
     * @param fileNamePattern  Only include entries whose file name matches this regexp
     * @param mediaTypePattern Only include entries whose media type matches this regexp
     * @param registry         The registry instance
     * @param baseUri          The request base URI
     * @param recursive        True to recurse into subdirectories, False otherwise
     * @return The hierarchical metadata
     * @throws RegistryException Thrown if an error occurs when accessing the registry
     */
    public static Entry treeWalk(
        String filesPath, String path, Pattern fileNamePattern,
        Pattern mediaTypePattern, UserRegistry registry, URI baseUri, boolean recursive)
        throws RegistryException {
        Resource resource = registry.get(path);
        Entry entry = getEntryForResource(resource, filesPath, baseUri);
        if (resource instanceof Collection) {
            Collection folder = (Collection) resource;
            List<Entry> folderEntries = new ArrayList<Entry>(folder.getChildCount());
            for (String child : folder.getChildren()) {
                Entry childEntry = null;
                if (recursive) {
                    childEntry = treeWalk(
                        filesPath, child, fileNamePattern, mediaTypePattern,
                        registry, baseUri, recursive
                    );
                }
                else {
                    Resource childResource = registry.get(child);
                    childEntry = getEntryForResource(childResource, filesPath, baseUri);
                    if (!(childResource instanceof Collection) &&
                        !filterMatches(childEntry, fileNamePattern, mediaTypePattern)) {
                        childEntry = null;
                    }
                }

                if (childEntry != null) {
                    folderEntries.add(childEntry);
                }
            }

            entry.setEntries(folderEntries);
        }
        else if (!filterMatches(entry, fileNamePattern, mediaTypePattern)) {
            entry = null;
        }

        return entry;
    }

    /**
     * Check whether an entry matches the given filters
     *
     * @param entry            The entry metadata
     * @param fileNamePattern  The file name regexp
     * @param mediaTypePattern The media type regexp
     * @return True if the entry matches, False otherwise
     */
    public static boolean filterMatches(
        Entry entry, Pattern fileNamePattern,
        Pattern mediaTypePattern) {
        return filterMatches(
            entry.getName(), entry.getContentType(), fileNamePattern, mediaTypePattern
        );
    }

    /**
     * Check whether the given fileName and mediaType match the given filters
     *
     * @param fileName         The file name
     * @param mediaType        The file media type
     * @param fileNamePattern  The file name regexp
     * @param mediaTypePattern The media type regexp
     * @return True if match successful, False otherwise
     */
    public static boolean filterMatches(
        String fileName, String mediaType, Pattern fileNamePattern,
        Pattern mediaTypePattern) {
        if (fileNamePattern != null && !fileNamePattern.matcher(fileName).matches()) {
            return false;
        }

        if (mediaTypePattern != null && !mediaTypePattern.matcher(mediaType).matches()) {
            return false;
        }

        return true;
    }
}
