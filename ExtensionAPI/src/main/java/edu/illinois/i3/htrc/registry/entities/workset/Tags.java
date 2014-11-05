package edu.illinois.i3.htrc.registry.entities.workset;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean representing a collection of tags
 *
 * @author capitanu
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Tags {

	@XmlElement(name = "tag")
	private List<String> _tags;

	/**
	 * Default constructor - needed by the JAXB serializer
	 */
	public Tags() { }

	/**
	 * Constructor
	 *
	 * @param tags The list of tags
	 */
	public Tags(List<String> tags) {
		_tags = tags;
	}

	/**
	 * Returns the list of tags
	 *
	 * @return The list of tags
	 */
	public List<String> getTags() {
		if (_tags == null)
			_tags = new ArrayList<String>();

		return _tags;
	}

}
