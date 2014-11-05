package edu.illinois.i3.htrc.registry.entities.workset;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean representing a collection of volumes
 *
 * @author capitanu
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Volumes {

	@XmlElement(name = "volume")
	private List<Volume> _volumes;

	/**
	 * Default constructor - needed by the JAXB serializer
	 */
	public Volumes() { }

	/**
	 * Constructor
	 *
	 * @param volumes The list of volumes
	 */
	public Volumes(List<Volume> volumes) {
		_volumes = volumes;
	}

	/**
	 * Returns the list of volumes
	 *
	 * @return The list of volumes
	 */
	public List<Volume> getVolumes() {
		if (_volumes == null)
			_volumes = new ArrayList<Volume>();

		return _volumes;
	}
}
