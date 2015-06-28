package simulator;

import java.io.File;
import java.net.URISyntaxException;

import simulator.screen.Screen;
import simulator.simObject.SimObject;

public class Simulation {
	public static final boolean USE_INTERNET = false;

	/**
	 * Real time updates and syncs all objects to the same epoch. It should only
	 * be false for debugging
	 */
	public static final boolean REAL_TIME = true;

	private SimObject focus;
	public SolarSystem solarSystem;
	
	public String rootFilePath;
	public double simSpeed;
	
	public Simulation() {
		try {
			File jarFile = new File(getClass().getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath());
			rootFilePath = jarFile.getParent();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		solarSystem = new SolarSystem(this);
		ScenarioParser parser = new ScenarioParser(this, "/res/scenario.xml");
		solarSystem.start();
		parser.loadPlans();
		
//		for(int i=0; i<TLELoader.categories.length; i++) {
//			solarSystem.addObjects(loader.getShips(TLELoader.categories[i])); 
//		}

//		Vector3D[] target = Astrophysics.target(
//				new Vector3D(-6518108.3, -2403847.9, -22172.2),
//				new Vector3D(6697475.6, 1794583.2, 0.0),
//				new Vector3D(2604.057, -7105.717, -263.218),
//				new Vector3D(-1962.373, 7323.674, 0.0),
//				100.0*60.0, 3.986E14);
//		System.out.println(target[0].magnitude());

		Screen screen = new Screen(this);
		screen.setRenderer(solarSystem.getRenderer());
		screen.start();
	}

	public void setFocus(SimObject focus) {
		if(focus != null) {
			this.focus = focus;
		}
	}

	public SimObject getFocus() {
		return focus;
	}

	public static void main(String[] args) {
		Simulation sim = new Simulation();
		sim.start();
	}
}
