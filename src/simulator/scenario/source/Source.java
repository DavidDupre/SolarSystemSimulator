package simulator.scenario.source;

import java.util.HashMap;

public interface Source {
	/**
	 * 
	 * @return a map which represents the attributes of a group element in a
	 *         scenario XML file
	 */
	public HashMap<String, String> getArgs();
}
