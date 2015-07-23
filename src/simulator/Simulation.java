package simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import simulator.scenario.ScenarioEditor;
import simulator.scenario.ScenarioFactory;
import simulator.scenario.ScenarioLoader;
import simulator.scenario.source.Source;
import simulator.screen.Window;
import simulator.simObject.Ship;
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
	private ScenarioLoader loader;
	public Window screen;

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
		screen = new Window(this);
		
		loader = new ScenarioLoader(this, getScenarioPath());
		loader.init();

		solarSystem.start();

		screen.setRenderer(solarSystem.getRenderer());
		screen.run();

//		System.out.println("exporting simulation");
//		export();
//		System.out.println("export complete");

		System.exit(0);
	}
	
	public String getScenarioPath() {
		PropertiesManager.load();
		String scenarioFilePath = PropertiesManager.getProperty("path");
		File file = new File(scenarioFilePath);
		if(!file.exists()){
			File res = new File(rootFilePath + "\\res");
			File[] files = res.listFiles();
			try {
				for(int i=0; i<files.length; i++) {
					String name = files[i].getName();
					if(name.endsWith("xml")) {
						scenarioFilePath = files[i].getAbsolutePath();
					}
				}
				PropertiesManager.setProperty("path", scenarioFilePath);
				System.out.println("Using " + scenarioFilePath + " as scenario filepath");
			} catch (NullPointerException e) {
				System.out.println("res path not found");
				e.printStackTrace();
				System.exit(1);
			}
		}
		return scenarioFilePath;
	}

	public void setFocus(SimObject focus) {
		if (focus != null) {
			this.focus = focus;
			this.screen.camera.updateFocus(focus.getAbsolutePos());
		}
	}
	
	public SimObject getFocus() {
		return focus;
	}

	/**
	 * Save all the simulation data and settings as a scenario file
	 */
	public void export() {
		String filePath = PropertiesManager.getProperty("save");

		// Remove contents of file
		try {
			PrintWriter pw = new PrintWriter(filePath);
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		ScenarioEditor editor = new ScenarioEditor(
				ScenarioFactory.newScenario(filePath));

		editor.setFocus(focus.name);
		editor.setSpeed(String.valueOf(simSpeed));
		editor.setEpoch(solarSystem.getEpoch());
		editor.setCamera(screen.camera.pitch, screen.camera.yaw,
				screen.camera.centerDistance);

		for (Source s : loader.sources) {
			editor.addGroup(s);
		}

		for (SimObject o : solarSystem.getObjects()) {
			if (o instanceof Ship) {
				if (((Ship) o).storeRaw) {
					editor.addObject(o);
				}
				editor.addPlan(((Ship) o).getManeuvers());
			}
		}

		editor.updateFile();
	}

	public static void main(String[] args) {
		Simulation sim = new Simulation();
		sim.start();
	}
}
