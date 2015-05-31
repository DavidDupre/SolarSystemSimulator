package simulator;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.locks.ReentrantLock;

import simulator.plans.Hohmann;
import simulator.plans.Incline;
import simulator.plans.WaitCommand;
import simulator.screen.Screen;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;

public class Simulation {
	public static ReentrantLock physicsLock;

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

	public Simulation() {
		physicsLock = new ReentrantLock();
	}

	public void start() {
		solarSystem = new SolarSystem(this);
		SystemLoader loader = new SystemLoader();

		String filePath = "/res/test.csv";
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
		setFocus(solarSystem.getObject("ISS (ZARYA)"));
		solarSystem.start();
		Ship iss = ((Ship) solarSystem.getObject("ISS (ZARYA)"));
		iss.addManeuver(new WaitCommand(2E3));
		iss.addManeuver(new Hohmann(1E7));
		iss.addManeuver(new Incline(0));

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
