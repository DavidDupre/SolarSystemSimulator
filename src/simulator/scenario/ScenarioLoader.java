package simulator.scenario;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import simulator.Simulation;
import simulator.SystemLoader;
import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;
import simulator.plans.ManeuverFactory;
import simulator.scenario.source.CSVSource;
import simulator.scenario.source.Source;
import simulator.scenario.source.TLESource;
import simulator.simObject.Body;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;
import simulator.tle.TLE;

public class ScenarioLoader {
	private Simulation sim;
	private SystemLoader loader;
	private ScenarioEditor editor;

	public ArrayList<Source> sources;

	public ScenarioLoader(Simulation sim, String filePath) {
		this.sim = sim;
		loader = new SystemLoader();
		editor = new ScenarioEditor(filePath);
		sources = new ArrayList<Source>();
	}

	public void init() {
		/*
		 * Load groups
		 */
		for (Element e : editor.getBodyElements()) {
			String filePath = e.getAttribute("path");
			sources.add(new CSVSource(filePath));
			sim.solarSystem.addObjects(loader.getObjects(sim.rootFilePath
					+ filePath));
		}
		for (Element e : editor.getShipElements()) {
			String category = e.getAttribute("category");
			String name = e.getAttribute("name");
			if (!name.isEmpty()) {
				sources.add(new TLESource(category, name));
				sim.solarSystem.addObject(loader.getShip(category, name));
			} else {
				sources.add(new TLESource(category));
				sim.solarSystem.addObjects(loader.getShips(category));
			}
		}

		/*
		 * Load individual objects
		 */
		loadObjects();

		/*
		 * Load focus
		 */
		String focusName = editor.getFocusElement().getTextContent();
		sim.setFocus(sim.solarSystem.getObject(focusName));

		/*
		 * Load epoch
		 */
		loadEpoch();

		/*
		 * Load speed
		 */
		Element eSpeed = editor.getSpeedElement();
		if (eSpeed != null) {
			double speed = Double.parseDouble(eSpeed.getTextContent());
			sim.simSpeed = speed;
		}
	}

	private void loadEpoch() {
		Element eEpoch = editor.getEpochElement();
		if (eEpoch != null && eEpoch.hasAttributes()) {
			switch (eEpoch.getAttribute("type")) {
			case "gregorian":
				int yr = Integer.parseInt(eEpoch.getAttribute("yr"));
				int mo = Integer.parseInt(eEpoch.getAttribute("mo"));
				int day = Integer.parseInt(eEpoch.getAttribute("d"));
				int hr = Integer.parseInt(eEpoch.getAttribute("h"));
				int min = Integer.parseInt(eEpoch.getAttribute("min"));
				double sec = Double.parseDouble(eEpoch.getAttribute("s"));
				sim.solarSystem.setEpoch(yr, mo, day, hr, min, sec);
				break;
			case "jd":
				double jd = Double.parseDouble(eEpoch.getAttribute("day"));
				sim.solarSystem.setEpochJD(jd);
				break;
			case "tai":
				double tai = Double.parseDouble(eEpoch.getAttribute("time"));
				sim.solarSystem.setEpoch(tai);
				break;
			}
		}
		// Will start with system time if no epoch is found
	}

	/**
	 * Load individual objects
	 */
	private void loadObjects() {
		for (Element e : editor.getObjectElements()) {
			String name = e.getAttribute("name");
			String parentName = e.getAttribute("parent");
			Body parent = (Body) sim.solarSystem.getObject(parentName);

			Vector3D[] state = new Vector3D[2];
			Element eState = (Element) e.getElementsByTagName("state").item(0);
			switch (eState.getAttribute("type")) {
			case "orbit":
				double a = Double.parseDouble(eState.getAttribute("a"));
				double i = Double.parseDouble(eState.getAttribute("i"));
				double ecc = Double.parseDouble(eState.getAttribute("e"));
				double peri = Double.parseDouble(eState.getAttribute("peri"));
				double node = Double.parseDouble(eState.getAttribute("node"));
				double v = Double.parseDouble(eState.getAttribute("v"));
				Orbit orb = new Orbit(a, ecc, i, node, peri, v);
				state = Astrophysics.toRV(orb, parent.mu, true);
				break;
			case "vector":
				Element posElement = (Element) eState.getElementsByTagName(
						"pos").item(0);
				Vector3D pos = getVectorFromElement(posElement);
				Element velElement = (Element) eState.getElementsByTagName(
						"vel").item(0);
				Vector3D vel = getVectorFromElement(velElement);
				state[0] = pos;
				state[1] = vel;
				break;
			case "tle":
				Element eLine1 = (Element) eState.getElementsByTagName("line1")
						.item(0);
				Element eLine2 = (Element) eState.getElementsByTagName("line2")
						.item(0);
				TLE tle = new TLE(name, eLine1.getTextContent(),
						eLine2.getTextContent());
				orb = tle.getOrbit();
				state = Astrophysics.toRV(orb, parent.mu, true);
				break;
			}

			double epoch = 0; // in julian date
			Element eEpoch = (Element) e.getElementsByTagName("epoch").item(0);
			switch (eEpoch.getAttribute("type")) {
			case "jd":
				epoch = Double.parseDouble(eEpoch.getAttribute("day"));
				break;
			case "gregorian":
				// TODO make a decent Epoch class
				break;
			}

			SimObject object = null;
			String type = e.getAttribute("type");
			switch (type) {
			case "ship":
				object = new Ship(name, state, parent, epoch);
				((Ship) object).storeRaw = true;
				break;
			case "body":
				double mass = Double.parseDouble(e.getAttribute("mass"));
				double radius = Double.parseDouble(e.getAttribute("radius"));
				object = new Body(name, parent, mass, radius, state, epoch);
				break;
			}

			/*
			 * If there is already an object by the same name, remove it and
			 * replace it with the individual object
			 */
			SimObject duplicate = sim.solarSystem.getObject(object.name);
			if(duplicate != null) {
				sim.solarSystem.removeObject(duplicate);
			}
			
			sim.solarSystem.addObject(object);
		}
	}

	private Vector3D getVectorFromElement(Element e) {
		double x = Double.parseDouble(e.getAttribute("x"));
		double y = Double.parseDouble(e.getAttribute("y"));
		double z = Double.parseDouble(e.getAttribute("z"));
		return new Vector3D(x, y, z);
	}

	public void loadPlans() {
		NodeList plans = editor.getPlanNodes();
		ManeuverFactory factory = new ManeuverFactory();
		for (int i = 0; i < plans.getLength(); i++) {
			Element plan = (Element) plans.item(i);
			String name = plan.getAttribute("name");
			Ship ship = (Ship) sim.solarSystem.getObject(name);
			NodeList commands = plan.getElementsByTagName("command");
			for (int j = 0; j < commands.getLength(); j++) {
				Element command = (Element) commands.item(j);
				String type = command.getAttribute("type");
				HashMap<String, String> args = new HashMap<String, String>();
				NodeList argNodes = command.getElementsByTagName("param");
				for (int k = 0; k < argNodes.getLength(); k++) {
					Node n = argNodes.item(k).getAttributes().item(0);
					args.put(n.getNodeName(), n.getNodeValue());
				}
				ship.addManeuver(factory.createNewManeuver(type, args));
			}
		}
	}
}
