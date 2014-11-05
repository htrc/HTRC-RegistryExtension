package edu.illinois.i3.htrc.registry.api.utils;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;

import edu.illinois.i3.htrc.registry.entities.workset.Comment;
import edu.illinois.i3.htrc.registry.entities.workset.Volume;
import edu.illinois.i3.htrc.registry.entities.workset.Workset;
import edu.illinois.i3.htrc.registry.entities.workset.WorksetMeta;

/**
 * Logging utility helper methods
 *
 * @author capitanu
 *
 */
public class LogUtils {

	/**
	 * Log only if not null
	 *
	 * @param log The logger to log to
	 * @param format The log message format
	 * @param data The data to log
	 */
	public static void logIfNotNull(Log log, String format, Object... data) {
		if (data[0] == null) return;

		log.debug(String.format(format, data));
	}

	/**
	 * Log workset details
	 *
	 * @param log The logger to log to
	 * @param workset The workset
	 */
	public static void logWorkset(Log log, Workset workset) {
		WorksetMeta metadata = workset.getMetadata();

		log.debug(String.format("=== Workset: %s ===", metadata.getName()));
		logIfNotNull(log, "description: %s", metadata.getDescription());
		logIfNotNull(log, "author: %s", metadata.getAuthor());
		logIfNotNull(log, "avgRating: %f", metadata.getAvgRating());
		logIfNotNull(log, "version: %d", metadata.getVersion());

		if (!metadata.getTags().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			for (String tag : metadata.getTags())
				sb.append(", ").append(tag);
			log.debug("tags: " + sb.substring(2));
		}

		if (!metadata.getComments().isEmpty()) {
			log.debug("comments:");
			for (Comment comment : metadata.getComments())
				log.debug(String.format("\tauthor: %s text: '%s'", comment.getAuthor(), comment.getText()));
		}

		if (workset.getContent() != null) {
			log.debug("volumes:");
			for (Volume volume : workset.getContent().getVolumes())
				log.debug("\tvolume: " + volume.getId());
		}
		log.debug("============================");
	}

	/**
	 * Log the metadata of a registry resource
	 *
	 * @param log The logger to log to
	 * @param resource The registry resource to log
	 */
	public static void logResource(Log log, Resource resource) {
		log.debug(String.format("=== Resource: %s ===", resource.getPath()));
		log.debug("isCollection: " + (resource instanceof Collection));
		log.debug("isSymLink: " + Boolean.parseBoolean(resource.getProperty("registry.link")));
		logIfNotNull(log, "author: %s", resource.getAuthorUserName());
		logIfNotNull(log, "description: %s", resource.getDescription());
		logIfNotNull(log, "id: %s", resource.getId());
		logIfNotNull(log, "lastUpdatedBy: %s", resource.getLastUpdaterUserName());
		logIfNotNull(log, "mediaType: %s", resource.getMediaType());
		logIfNotNull(log, "permanentPath: %s", resource.getPermanentPath());
		logIfNotNull(log, "parentPath: %s", resource.getParentPath());
		if (!resource.getProperties().isEmpty()) {
			log.debug("Properties:");
			for (Map.Entry<Object,Object> prop : resource.getProperties().entrySet())
				log.debug("\t" + prop.getKey() + ": " + prop.getValue());
		}
		log.debug("============================");
	}

}
