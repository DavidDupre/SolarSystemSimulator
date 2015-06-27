package simulator;

import java.io.File;
import java.net.URISyntaxException;

import simulator.astro.Time;
import simulator.screen.Screen;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;

public class Simulation {
	/*
	 * Simulation seconds per real seconds
	 */
	public static final double SIM_SPEED = 1.0;
	public static final boolean USE_INTERNET = false;

	/**
	 * Real time updates and syncs all objects to the same epoch. It should only
	 * be false for debugging
	 */
	public static final boolean REAL_TIME = true;

	private SimObject focus;

	public SolarSystem solarSystem;

	public void start() {
		solarSystem = new SolarSystem(this);
		SystemLoader loader = new SystemLoader();

		String filePath = "/res/solarSystem.csv";
		try {
			File jarFile = new File(getClass().getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath());
			String root = jarFile.getParent();
			filePath = root + filePath;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		solarSystem.addObjects(loader.getObjects(filePath));
		solarSystem.addObjects(loader.getShips("iss"));
//		solarSystem.addObjects(loader.getShips("stations"));
//		solarSystem.addObjects(loader.getShips("resource"));
//		solarSystem.addObjects(loader.getShips("geo"));

//		for(int i=0; i<TLELoader.categories.length; i++) {
//			solarSystem.addObjects(loader.getShips(TLELoader.categories[i])); 
//		}
		 
		Ship iss = (Ship) solarSystem.getObject("ISS (ZARYA)");
		setFocus(solarSystem.getObject("Earth"));
		solarSystem.start();
		solarSystem.setEpoch(2015, 6, 30, 0, 0, 0);
		System.out.println(Time.getJulianDate((long)(1000*solarSystem.getEpoch())));

		/*
		 * Add maneuvers after starting the solar system
		 */
		
//		iss.vel.multiply(2.0);
		
//		for(SimObject o : solarSystem.getObjects()) {
//			if(o instanceof Ship) {
//				Ship s = (Ship) o;
//				s.addManeuver(new WaitCommand(5000));
//				s.addManeuver(new Incline(0));
//				s.addManeuver(new Hohmann(1E7));
//				s.addManeuver(new Hohmann(1E7));
//				s.addManeuver(new Hohmann(1E7));
//			}
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
		this.focus = focus;
	}

	public SimObject getFocus() {
		return focus;
	}

	public static void main(String[] args) {
		Simulation sim = new Simulation();
		sim.start();
	}
}
