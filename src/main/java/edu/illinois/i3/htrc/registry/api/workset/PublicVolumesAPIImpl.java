package edu.illinois.i3.htrc.registry.api.workset;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.encoder.CsvEncoder;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;
import org.wso2.carbon.registry.core.ActionConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.secure.AuthorizationFailedException;
import org.wso2.carbon.registry.core.session.UserRegistry;

import edu.illinois.i3.htrc.registry.api.HTRCMediaTypes;
import edu.illinois.i3.htrc.registry.api.RegistryExtension;
import edu.illinois.i3.htrc.registry.api.RegistryExtensionConfig;
import edu.illinois.i3.htrc.registry.api.utils.RegistryUtils;
import edu.illinois.i3.htrc.registry.api.utils.WorksetUtils;
import edu.illinois.i3.htrc.registry.entities.workset.Property;
import edu.illinois.i3.htrc.registry.entities.workset.Volume;
import edu.illinois.i3.htrc.registry.entities.workset.Volumes;

/**
 * JAX-RS Implementation for {@link PublicVolumesAPI}
 *
 * @author capitanu
 *
 */
public class PublicVolumesAPIImpl implements PublicVolumesAPI {

	private static final Log Log = LogFactory.getLog(PublicVolumesAPIImpl.class);
	private static final String VOLUME_ID_HEADER = "volume_id";

	private final String _worksetId;
	private final UserRegistry _registry;
	private final RegistryExtensionConfig _config;

	public PublicVolumesAPIImpl(String worksetId, UserRegistry registry, RegistryExtension registryExtension) {
		_worksetId = worksetId;
		_registry = registry;
		_config = registryExtension.getConfig();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVolumesAsPlainText(@QueryParam("author") String author) {
		Log.debug(String.format("getVolumesAsPlainText(public): id=%s, author=%s", _worksetId, author));

		if (author == null)
			return Response.status(Status.BAD_REQUEST)
					.entity("author parameter is mandatory")
					.type(MediaType.TEXT_PLAIN)
					.build();

		try {
			String resPath = _config.getWorksetPath(_worksetId, author);

			// check if public workset
			if (!RegistryUtils.isEveryoneAuthorized(resPath, _registry, ActionConstants.GET))
				throw new AuthorizationFailedException(String.format("%s is not a public workset", _worksetId));

			Resource resource = _registry.get(resPath);
			Volumes volumes = WorksetUtils.getWorksetVolumesFromResource(resource);

			StringBuilder sb = new StringBuilder();
			if (volumes != null)
				for (Volume volume : volumes.getVolumes())
					sb.append('\n').append(volume.getId());

			String response = sb.length() > 0 ? sb.substring(1) : sb.toString();
			return Response.ok(response).build();
		}
		catch (AuthorizationFailedException e) {
			return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions").type(MediaType.TEXT_PLAIN).build();
		}
		catch (ResourceNotFoundException e) {
			String errorMsg = "Unable to locate workset: " + _worksetId;
			return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getVolumesAsPlainText(public)", e);
			String errorMsg = String.format("Cannot retrieve volumes: %s", e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@GET
	@Produces(HTRCMediaTypes.TEXT_CSV)
	public Response getVolumesAsCSV(@QueryParam("author") String author) {
		Log.debug(String.format("getVolumesAsCSV(public): id=%s, author=%s", _worksetId, author));

		if (author == null)
			return Response.status(Status.BAD_REQUEST)
					.entity("author parameter is mandatory")
					.type(MediaType.TEXT_PLAIN)
					.build();

		try {
			String resPath = _config.getWorksetPath(_worksetId, author);

			// check if public workset
			if (!RegistryUtils.isEveryoneAuthorized(resPath, _registry, ActionConstants.GET))
				throw new AuthorizationFailedException(String.format("%s is not a public workset", _worksetId));

			Resource resource = _registry.get(resPath);
			Volumes volumes = WorksetUtils.getWorksetVolumesFromResource(resource);
	        StringWriter csvData = new StringWriter();

			if (volumes != null) {
				// compute the CSV header names
				LinkedHashSet<String> propertyNames = new LinkedHashSet<String>();
				for (Volume volume : volumes.getVolumes())
					for (Property property : volume.getProperties())
						propertyNames.add(property.getName());

		        ICsvMapWriter csvWriter = null;
		        try {
		            final CsvEncoder csvEncoder = new DefaultCsvEncoder();
		            final CsvPreference csvPreference =
		                    new CsvPreference.Builder(CsvPreference.EXCEL_PREFERENCE)
		                            .useEncoder(csvEncoder).build();
		            csvWriter = new CsvMapWriter(csvData, csvPreference);

		            int size = propertyNames.size() + 1; // +1 for volume_id
		            String[] header = new String[size];
		            CellProcessor[] processors = new CellProcessor[size];

		            // set up the header
		            header[0] = VOLUME_ID_HEADER;
		            processors[0] = new NotNull();
		            Iterator<String> propNameIter = propertyNames.iterator();
		            for (int i = 1; i < size; i++) {
		                header[i] = propNameIter.next();
		                processors[i] = new Optional();
		            }

		            csvWriter.writeHeader(header);

		            Map<String, Object> rowData = new HashMap<String, Object>();
		            for (Volume volume : volumes.getVolumes()) {
		            	rowData.clear();

		            	// add the volume id
		                rowData.put(VOLUME_ID_HEADER, volume.getId());

		                // add the volume properties
		                for (Property property : volume.getProperties()) {
		                	Log.debug(String.format("getVolumesAsCSV(public): vol: %s propName: %s propValue: %s",
		                			volume.getId(), property.getName(), property.getValue().toString()));
		                	rowData.put(property.getName(), property.getValue().toString());
		                }

		                csvWriter.write(rowData, header, processors);
		            }
		        }
		        finally {
		            if (csvWriter != null)
		                csvWriter.close();
		        }
			}

			String response = csvData.toString();
			return Response.ok(response).build();
		}
		catch (AuthorizationFailedException e) {
			return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions").type(MediaType.TEXT_PLAIN).build();
		}
		catch (ResourceNotFoundException e) {
			String errorMsg = "Unable to locate workset: " + _worksetId;
			return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getVolumesAsCSV(public)", e);
			String errorMsg = String.format("Cannot retrieve volumes: %s", e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@GET
	public Response getVolumes(@QueryParam("author") String author) {
		Log.debug(String.format("getVolumes(public): id=%s, author=%s", _worksetId, author));

		if (author == null)
			return Response.status(Status.BAD_REQUEST)
					.entity("author parameter is mandatory")
					.type(MediaType.TEXT_PLAIN)
					.build();

		try {
			String resPath = _config.getWorksetPath(_worksetId, author);

			// check if public workset
			if (!RegistryUtils.isEveryoneAuthorized(resPath, _registry, ActionConstants.GET))
				throw new AuthorizationFailedException(String.format("%s is not a public workset", _worksetId));

			Resource resource = _registry.get(resPath);
			Volumes volumes = WorksetUtils.getWorksetVolumesFromResource(resource);
			if (volumes == null) volumes = new Volumes();

			return Response.ok(volumes).build();
		}
		catch (AuthorizationFailedException e) {
			return Response.status(Status.UNAUTHORIZED).entity("Insufficient permissions").type(MediaType.TEXT_PLAIN).build();
		}
		catch (ResourceNotFoundException e) {
			String errorMsg = "Unable to locate workset: " + _worksetId;
			return Response.status(Status.NOT_FOUND).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
		catch (Exception e) {
			Log.error("getVolumes(public)", e);
			String errorMsg = String.format("Cannot retrieve volumes: %s", e.toString());
			return Response.serverError().entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
		}
	}

}
