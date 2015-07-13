package org.xmlcml.cmine.args;

import nu.xom.Element;

import org.apache.log4j.Logger;

/** this isn't working properly.
 * The class hierarchy and containment hierarchy are muddled, so
 * only the immediate instance retains the correct version
 * 
 * @author pm286
 *
 */
public class VersionManager {

	private static final Logger LOG = Logger.getLogger(VersionManager.class);
	static {LOG.setLevel(org.apache.log4j.Level.DEBUG);}

	private static final String VERSION = "version";
	private static final String NAME    = "name";
	
	// these are private so each level can have its own version and name
	private static String name;
	private static String version;
	
	public VersionManager() {
	}

	private void setNameVersion(String name, String version) {
		// only set the first time
//		if (this.name == null) {
			this.name = name;
//		}
//		if (this.version == null) {
			this.version = version;
//		}
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public void readNameVersion(Element argListElement) {
		setNameVersion(
				argListElement.getAttributeValue(NAME), 
				argListElement.getAttributeValue(VERSION));
//		LOG.debug(this.hashCode()+" "+name+" "+version);
	}

	public void printVersion() {
		System.err.println(""+name + "("+version+")");
	}
}
