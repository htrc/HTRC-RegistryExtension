package edu.illinois.i3.htrc.registry.api.utils;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import edu.illinois.i3.htrc.registry.entities.workset.Tags;
import edu.illinois.i3.htrc.registry.entities.workset.Volumes;
import edu.illinois.i3.htrc.registry.entities.workset.Worksets;

@Provider
@Produces ({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public class JaxbContextResolver implements ContextResolver<JAXBContext> {

  @Override
  public JAXBContext getContext(Class<?> type) {
    Class<?>[] bindTypes = new Class<?>[] { Worksets.class, Volumes.class, Tags.class };

    try {
      return JAXBContext.newInstance(bindTypes);
    }
    catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }
}