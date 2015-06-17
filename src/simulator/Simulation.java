package simulator;

import java.io.File;
import java.net.URISyntaxException;

import simulator.plans.Incline;
import simulator.plans.WaitCommand;
import simulator.screen.Screen;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;
import simulator.tle.TLELoader;

public class Simulation {
	/*
	 * Simulation seconds per real seconds
	 */
	public static final double SIM_SPEED = 1E3;
	public static final boolean USE_INTERNET = false;

	/**
	 * Real time updates and syncs all objects to the same epoch. It should only
	 * be false for debugging
	 */
	public static final boolean REAL_TIME = false;

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
		/*
		for(int i=0; i<TLELoader.categories.length; i++) {
			solarSystem.addObjects(loader.getShips(TLELoader.categories[i]));
		}
		*/
		setFocus(solarSystem.getObject("ISS (ZARYA)"));
		solarSystem.start();
		
		Ship iss = (Ship) solarSystem.getObject("ISS (ZARYA)");
		iss.addManeuver(new WaitCommand(3000));
		iss.addManeuver(new Incline(0));
		/*
		iss.addManeuver(new Circularize());
		iss.addManeuver(new WaitCommand(3000));
		iss.addManeuver(new Incline(0));
		iss.addManeuver(new WaitCommand(11000));
		iss.addManeuver(new Bielliptic(4E8, 1E7));
		*/

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
