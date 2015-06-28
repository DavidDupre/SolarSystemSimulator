package simulator;

import java.util.ArrayList;

import simulator.astro.Time;
import simulator.screen.Renderer;
import simulator.simObject.Body;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;
import simulator.simObject.SimObject.RenderDetail;

/**
 * Manages the rendering and updating of simObjects
 * 
 * @author S-2482153
 *
 */
public class SolarSystem extends Thread {
	private ArrayList<SimObject> objects;
	private Renderer renderer;
	private Simulation sim;

	private Thread thread;
	private long lastTime;
	private double simStartTimeTAI;
	private double epochTAI = Double.NaN;
	
	public SolarSystem(Simulation sim) {
		renderer = new SolarSystemRenderer();
		objects = new ArrayList<SimObject>();
		this.sim = sim;
	}

	/**
	 * Get the current epoch in TAI seconds for the solar system. Don't use this
	 * before calling start()!
	 * 
	 * @return
	 */
	public double getEpoch() {
		return epochTAI;
	}
	
	public void setEpoch(double epoch) {
		this.epochTAI = epoch;
	}
	
	/*
	 * TODO fix this
	 */
	public void setEpoch(int yr, int mo, int d, int h, int min, double s) {
		setEpoch(Time.jdToTAI(Time.getJulianDate(yr, mo, d, h, min, s)));
	}

	@Override
	public void run() {
		lastTime = System.currentTimeMillis();
		if(Double.isNaN(epochTAI)) {
			epochTAI = simStartTimeTAI;
		}
		while (true) {
			/*
			 * Add objects to the render update list. Children of all direct
			 * ancestors are added. Children are also added.
			 */
			ArrayList<SimObject> updateObjects = new ArrayList<SimObject>();
			updateObjects.addAll(sim.getFocus().getChildren());
			updateObjects.add(sim.getFocus());
			Body parent = sim.getFocus().parent;
			while (parent != null) {
				updateObjects.addAll(parent.getChildren());
				updateObjects.add(parent);
				parent = parent.parent;
			}

			/* Update the simObjects */
			double lastEpoch = epochTAI;
			for (SimObject o : updateObjects) {
				updateEpoch();
				o.updateTo(epochTAI);
			}
			//System.out.println("physics fps: " + (Simulation.SIM_SPEED/(epochTAI-lastEpoch)));
			//System.out.println("Current epoch: " + Time.getJulianDate((long) (1000 * epochTAI)));
		}
	}
	
	private void updateEpoch() {
		double delta = getDeltaTime() / 1000.0;
		delta *= sim.simSpeed;
		epochTAI += delta;
	}

	private double getDeltaTime() {
		long time = System.currentTimeMillis();
		long delta = time - lastTime;
		lastTime = time;
		return delta;
	}

	public void setObjects(ArrayList<SimObject> objects) {
		this.objects = objects;
	}

	public void addObject(SimObject object) {
		objects.add(object);
	}

	public void addObjects(ArrayList<SimObject> objects) {
		if(objects != null) {
			this.objects.addAll(objects);
		}
	}

	public SimObject getObject(String name) {
		for (SimObject o : objects) {
			if (o.name.equals(name)) {
				return o;
			}
		}
		return null;
	}

	public ArrayList<SimObject> getObjects() {
		return objects;
	}

	private class SolarSystemRenderer implements Renderer {
		@Override
		public void update() {

			/*
			 * Add objects to the render update list. Children of all direct
			 * ancestors are added. Children are also added.
			 */
			ArrayList<SimObject> updateObjects = new ArrayList<SimObject>();
			updateObjects.addAll(sim.getFocus().getChildren());
			updateObjects.add(sim.getFocus());
			Body parent = sim.getFocus().parent;
			while (parent != null) {
				updateObjects.addAll(parent.getChildren());
				updateObjects.add(parent);
				parent = parent.parent;
			}

			for (SimObject o : updateObjects) {
				if (o instanceof Ship) {
					o.render(RenderDetail.MAX);
				} else {
					o.render(RenderDetail.MAX);
				}
			}
		}
	}

	public Renderer getRenderer() {
		return renderer;
	}

	@Override
	public void start() {
		if (thread == null) {
			simStartTimeTAI = System.currentTimeMillis() / 1000.0;
			thread = new Thread(this);
			thread.setName("Solar system");
			thread.start();
		}
	}
}
