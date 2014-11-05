package edu.illinois.i3.htrc.registry.entities.workset;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean representing a collection of worksets
 *
 * @author capitanu
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Worksets {

	@XmlElement(name = "workset")
	private List<Workset> _worksets;

	/**
	 * Default constructor - needed by the JAXB serializer
	 */
	public Worksets() { }

	/**
	 * Constructor
	 *
	 * @param worksets The list of worksets
	 */
	public Worksets(List<Workset> worksets) {
		_worksets = worksets;
	}

	/**
	 * Returns the list of worksets
	 *
	 * @return The list of worksets
	 */
	public List<Workset> getWorksets() {
		if (_worksets == null)
			_worksets = new ArrayList<Workset>();

		return _worksets;
	}
}
