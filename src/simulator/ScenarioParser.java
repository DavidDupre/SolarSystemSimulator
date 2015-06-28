package simulator;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import simulator.plans.ManeuverFactory;
import simulator.simObject.Ship;

public class ScenarioParser {
	private Simulation sim;
	private SystemLoader loader;
	private Document doc;
	
	public ScenarioParser(Simulation sim, String scenerioFilePath) {
		this.sim = sim;
		loader = new SystemLoader();
		
		/*
		 * Load scenario file
		 */
		try {
			/*
			 * Initialize document
			 */
			File xmlFile = new File(sim.rootFilePath + scenerioFilePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			loadObjects();
			setFocus();
			setEpoch();
			setSpeed();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadObjects() {
		NodeList nList = doc.getElementsByTagName("group");
		for(int i=0; i<nList.getLength(); i++) {
			Element e = (Element) nList.item(i);
			String type = e.getAttribute("type");
			switch(type) {
			case "body":
				String filePath = e.getAttribute("path");
				sim.solarSystem.addObjects(loader.getObjects(sim.rootFilePath + filePath));
				break;
			case "ship":
				String category = e.getAttribute("category");
				String name = e.getAttribute("name");
				if(!name.isEmpty()) {
					sim.solarSystem.addObject(loader.getShip(category, name));
				} else {
					sim.solarSystem.addObjects(loader.getShips(category));
				}
				break;
			}
		}
	}
	
	private void setFocus() {
		String focusName = doc.getElementsByTagName("focus").item(0).getTextContent();
		sim.setFocus(sim.solarSystem.getObject(focusName));
	}
	
	private void setEpoch() {
		Element eEpoch = (Element) doc.getElementsByTagName("epoch").item(0);
		if(eEpoch != null) {
			int yr = Integer.parseInt(eEpoch.getAttribute("yr"));
			int mo = Integer.parseInt(eEpoch.getAttribute("mo"));
			int day = Integer.parseInt(eEpoch.getAttribute("d"));
			int hr = Integer.parseInt(eEpoch.getAttribute("h"));
			int min = Integer.parseInt(eEpoch.getAttribute("min"));
			double sec = Double.parseDouble(eEpoch.getAttribute("s"));
			sim.solarSystem.setEpoch(yr, mo, day, hr, min, sec);
		}
	}
	
	private void setSpeed() {
		Element eSpeed = (Element) doc.getElementsByTagName("speed").item(0);
		if(eSpeed != null) {
			double speed = Double.parseDouble(eSpeed.getTextContent());
			sim.simSpeed = speed;
		}
	}
	
	public void loadPlans() {
		NodeList plans = doc.getElementsByTagName("plan");
		ManeuverFactory factory = new ManeuverFactory();
		for(int i=0; i<plans.getLength(); i++) {
			Element plan = (Element) plans.item(i);
			String name = plan.getAttribute("name");
			Ship ship = (Ship) sim.solarSystem.getObject(name);
			NodeList commands = plan.getElementsByTagName("command");
			for(int j=0; j<commands.getLength(); j++) {
				Element command = (Element) commands.item(j);
				String[] args = new String[command.getAttributes().getLength()];
				int length = command.getAttributes().getLength();
				for(int k=0; k<length; k++) {
					args[k] = command.getAttributes().item(length-k-1).getNodeValue();
				}
				ship.addManeuver(factory.createNewManeuver(args));
			}
		}
	}
}
