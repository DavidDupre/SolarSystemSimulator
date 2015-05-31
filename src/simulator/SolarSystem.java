package simulator;

import java.util.ArrayList;

import simulator.screen.Renderer;
import simulator.simObject.Body;
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
	private double epochTAI;

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

	@Override
	public void run() {
		lastTime = System.currentTimeMillis();
		epochTAI = simStartTimeTAI;
		while (true) {
			double delta = getDeltaTime() / 1000.0;
			if (delta != 0) {
				/* Update the epoch */
				delta *= Simulation.SIM_SPEED;
				epochTAI += delta;

				/*
				 * Add objects to the update list based on focus body. Children
				 * and siblings are updated
				 */
				ArrayList<SimObject> updateObjects = new ArrayList<SimObject>();
				if (sim.getFocus().parent != null) {
					updateObjects.addAll(sim.getFocus().parent.getChildren());
				}
				updateObjects.addAll(sim.getFocus().getChildren());

				/* Update the simObjects */
				for (SimObject o : updateObjects) {
					if (o.equals(sim.getFocus())) {
						Simulation.physicsLock.lock();
						o.updateTo(epochTAI);
						Simulation.physicsLock.unlock();
					} else {
						o.updateTo(epochTAI);
					}
				}
			}
		}
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
		this.objects.addAll(objects);
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
				o.render(RenderDetail.MAX);
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
