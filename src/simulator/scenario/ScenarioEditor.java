package simulator.scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import simulator.astro.Time;
import simulator.plans.FlightPlan;
import simulator.plans.Maneuver;
import simulator.scenario.source.Source;
import simulator.simObject.Body;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;

/**
 * 
 * Used for reading and writing scenario XML files.
 * 
 * @author David
 *
 */
public class ScenarioEditor {
	protected Document doc;
	private File xmlFile;
	private ArrayList<Element> bodyElements;
	private ArrayList<Element> shipElements;
	private ArrayList<Element> objectElements;

	/**
	 * @param scenarioFilePath
	 *            the file path of a scenario XML file
	 */
	public ScenarioEditor(String scenarioFilePath) {
		try {
			xmlFile = new File(scenarioFilePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Element getElement(String key) {
		return (Element) doc.getElementsByTagName(key).item(0);
	}

	protected Element getCameraElement() {
		return getElement("camera");
	}

	public void setCamera(float pitch, float yaw, float zoom) {
		Element e = getCameraElement();
		if (e == null) {
			e = doc.createElement("camera");
			doc.getDocumentElement().appendChild(e);
		}
		e.setAttribute("pitch", String.valueOf(pitch));
		e.setAttribute("yaw", String.valueOf(yaw));
		e.setAttribute("zoom", String.valueOf(zoom));
	}

	protected Element getFocusElement() {
		return getElement("focus");
	}

	public String getFocus() {
		return getFocusElement().getTextContent();
	}

	public void setFocus(String name) {
		Element e = getFocusElement();
		if (e == null) {
			e = doc.createElement("focus");
			doc.getDocumentElement().appendChild(e);
		}
		e.setTextContent(name);
	}

	public void setFocus(SimObject o) {
		setFocus(o.name);
	}

	protected Element getSpeedElement() {
		return getElement("speed");
	}

	public double getSpeed() {
		return Double.parseDouble(getSpeedElement().getTextContent());
	}

	public void setSpeed(String speed) {
		Element e = getSpeedElement();
		if (e == null) {
			e = doc.createElement("speed");
			doc.getDocumentElement().appendChild(e);
		}
		e.setTextContent(speed);
	}

	protected Element getEpochElement() {
		return getElement("epoch");
	}

	/**
	 * Get the epoch as stated in the XML file
	 * 
	 * @return a map of attributes from the epoch element. Each epoch element
	 *         has a "type" attribute with other attributes depending on the
	 *         type
	 */
	public HashMap<String, String> getEpoch() {
		HashMap<String, String> values = new HashMap<String, String>();
		Element e = getEpochElement();
		NamedNodeMap attr = e.getAttributes();
		for (int i = 0; i < attr.getLength(); i++) {
			Node n = attr.item(i);
			values.put(n.getNodeName(), n.getNodeValue());
		}
		return values;
	}

	/**
	 * Set the attributes of the epoch element
	 * 
	 * @param values
	 *            a map of the epoch element's attributes
	 */
	public void setEpoch(HashMap<String, String> values) {
		Element e = getEpochElement();
		for (String key : values.keySet()) {
			e.setAttribute(key, values.get(key));
		}
	}

	public void setEpoch(String yr, String mo, String day, String hr,
			String min, String s) {
		Element e = getEpochElement();
		e.setAttribute("yr", yr);
		e.setAttribute("mo", mo);
		e.setAttribute("d", day);
		e.setAttribute("h", hr);
		e.setAttribute("min", min);
		e.setAttribute("s", s);
	}

	public void setEpoch(int yr, int mo, int day, int hr, int min, double s) {
		setEpoch(String.valueOf(yr), String.valueOf(mo), String.valueOf(day),
				String.valueOf(hr), String.valueOf(min), String.valueOf(s));
	}

	public void setEpoch(double epochTAI) {
		Element e = getEpochElement();
		if (e == null) {
			e = doc.createElement("epoch");
			doc.getDocumentElement().appendChild(e);
		}
		if (e.hasAttributes()) {
			NamedNodeMap attr = e.getAttributes();
			int length = attr.getLength();
			Node[] toRemove = new Node[length];
			for (int i = 0; i < length; i++) {
				toRemove[i] = attr.item(i);
			}
			for (int i = 0; i < length; i++) {
				e.removeAttributeNode((Attr) toRemove[i]);
			}
		}
		e.setAttribute("time", String.valueOf(epochTAI));
		e.setAttribute("type", "tai");
	}

	private void loadObjects() {
		objectElements = new ArrayList<Element>();
		NodeList nList = doc.getElementsByTagName("object");
		for (int i = 0; i < nList.getLength(); i++) {
			Element e = (Element) nList.item(i);
			objectElements.add(e);
		}
	}

	private void loadGroups() {
		bodyElements = new ArrayList<Element>();
		shipElements = new ArrayList<Element>();
		NodeList nList = doc.getElementsByTagName("group");
		for (int i = 0; i < nList.getLength(); i++) {
			Element e = (Element) nList.item(i);
			String type = e.getAttribute("type");
			switch (type) {
			case "body":
				bodyElements.add(e);
				break;
			case "ship":
				shipElements.add(e);
				break;
			}
		}
	}

	protected ArrayList<Element> getObjectElements() {
		if (objectElements == null) {
			loadObjects();
		}
		return objectElements;
	}

	protected ArrayList<Element> getBodyElements() {
		if (bodyElements == null) {
			loadGroups();
		}
		return bodyElements;
	}

	protected ArrayList<Element> getShipElements() {
		if (shipElements == null) {
			loadGroups();
		}
		return shipElements;
	}

	protected NodeList getPlanNodes() {
		return doc.getElementsByTagName("plan");
	}

	/**
	 * Add a group of simulation elements
	 * 
	 * @param source
	 */
	public void addGroup(Source source) {
		Element group = doc.createElement("group");
		for (String key : source.getArgs().keySet()) {
			group.setAttribute(key, source.getArgs().get(key));
		}
		doc.getDocumentElement().appendChild(group);
	}

	/**
	 * Add an individual simulation object. Doing so overrides any group objects
	 * by the same name. The state of the object is stored as two vectors. It's
	 * epoch is its last updated time.
	 * 
	 * @param object
	 */
	public void addObject(SimObject object) {
		Element eObject = doc.createElement("object");
		eObject.setAttribute("name", object.name);
		String type = "";
		if (object instanceof Ship) {
			type = "ship";
		} else {
			type = "body";
			eObject.setAttribute("radius",
					String.valueOf(((Body) object).radius));
			eObject.setAttribute("mass", String.valueOf(((Body) object).mass));
		}
		eObject.setAttribute("type", type);
		if (object.parent != null) {
			eObject.setAttribute("parent", object.parent.name);
		} else {
			eObject.setAttribute("parent", "null");
		}

		Element eState = doc.createElement("state");
		eObject.appendChild(eState);
		eState.setAttribute("type", "vector");

		// TODO less copy-paste. See vectorFromElement method in ScenarioLoader
		Element ePos = doc.createElement("pos");
		ePos.setAttribute("x", String.valueOf(object.pos.x));
		ePos.setAttribute("y", String.valueOf(object.pos.y));
		ePos.setAttribute("z", String.valueOf(object.pos.z));

		Element eVel = doc.createElement("vel");
		eVel.setAttribute("x", String.valueOf(object.vel.x));
		eVel.setAttribute("y", String.valueOf(object.vel.y));
		eVel.setAttribute("z", String.valueOf(object.vel.z));

		eState.appendChild(ePos);
		eState.appendChild(eVel);

		Element eEpoch = doc.createElement("epoch");
		eObject.appendChild(eEpoch);
		eEpoch.setAttribute("type", "jd");
		eEpoch.setAttribute("day", String.valueOf(Time
				.getJulianDate((long) (1000 * object.lastUpdatedTime))));

		doc.getDocumentElement().appendChild(eObject);
	}

	/**
	 * Add a plan to the scenario file. If a plan for the same ship already
	 * exists, this will replace it
	 * 
	 * @param plan
	 */
	public void addPlan(FlightPlan plan) {
		if (!plan.getManeuvers().isEmpty()) {
			Element e = getPlanElement(plan.ship.name);
			if (e == null) {
				// New element
				e = doc.createElement("plan");
				e.setAttribute("name", plan.ship.name);
				doc.getDocumentElement().appendChild(e);
			} else {
				// Overwrite element
				NodeList nList = e.getElementsByTagName("command");
				int length = nList.getLength();
				Node[] toRemove = new Node[length];
				for (int i = 0; i < length; i++) {
					toRemove[i] = nList.item(i);
				}
				for (int i = 0; i < length; i++) {
					e.removeChild(toRemove[i]);
				}
			}
			for (Maneuver m : plan.getManeuvers()) {
				Element command = doc.createElement("command");
				command.setAttribute("type", m.getClass().getSimpleName());
				for (String key : m.inputs.keySet()) {
					Element param = doc.createElement("param");
					param.setAttribute(key, m.inputs.get(key));
					command.appendChild(param);
				}
				e.appendChild(command);
			}
		}
	}

	/**
	 * Utility method for addPlan()
	 * 
	 * @param shipName
	 *            The name of the ship which owns the plan
	 * @return The element of the plan
	 */
	private Element getPlanElement(String shipName) {
		NodeList plans = getPlanNodes();
		for (int i = 0; i < plans.getLength(); i++) {
			Element plan = (Element) plans.item(i);
			String name = plan.getAttribute("name");
			if (name.equals(shipName)) {
				return plan;
			}
		}
		return null;
	}

	/**
	 * Write to the file with the updated info
	 */
	public void updateFile() {
		try {
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(xmlFile);
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
}
