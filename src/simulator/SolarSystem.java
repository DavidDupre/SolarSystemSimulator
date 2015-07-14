package simulator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import com.pi.math.vector.Vector;

import simulator.astro.Astrophysics;
import simulator.astro.Time;
import simulator.plans.Burn;
import simulator.plans.Maneuver;
import simulator.plans.SOIChange;
import simulator.plans.SimEvent;
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

	private ArrayList<SimEvent> events;
	private EventComparator bComp;

	public SolarSystem(Simulation sim) {
		renderer = new SolarSystemRenderer();
		objects = new ArrayList<SimObject>();
		this.sim = sim;
		events = new ArrayList<SimEvent>();
		bComp = new EventComparator();
	}

	@Override
	public void run() {
		lastTime = System.currentTimeMillis();
		if (Double.isNaN(epochTAI)) {
			epochTAI = simStartTimeTAI;
		}

		bufferEvents();
		
		System.out.println("\n\nStarting live simulation\n\n");
		
		displayLiveSimulation();
	}

	private void bufferEvents() {
		ArrayList<SimEvent> deadEvents = new ArrayList<SimEvent>();
		HashMap<Ship, Body> initParents = new HashMap<Ship, Body>();
		HashMap<Ship, Vector[]> initStates = new HashMap<Ship, Vector[]>();
		
		// Create list of ships
		ArrayList<Ship> ships = new ArrayList<Ship>();
		for (SimObject o : objects) {
			if (o instanceof Ship) {
				Ship s = (Ship) o;
				ships.add(s);
				initParents.put(s, s.parent);
				initStates.put(s, new Vector[]{s.pos, s.vel});
			}
		}

		// Create list of burns and initialize first maneuvers
		events = new ArrayList<SimEvent>();
		for (Ship s : ships) {
			if (!s.getPlan().getManeuvers().isEmpty()) {
				Maneuver m = s.getPlan().getManeuvers().get(0);
				m.init();
				events.addAll(m.burns);
			}
		}
		bComp = new EventComparator();
		events.sort(bComp);

		// Cycle through events
		while (!events.isEmpty()) {
			SimEvent e = events.get(0);
			for (SimObject o : objects) {
				o.updateTo(e.getEpoch());
			}
			
			e.execute();
			events.remove(e);
			deadEvents.add(e);

			refreshSimState(e.getShip());

			if (e instanceof Burn) {
				Burn b = (Burn) e;

				// Handle maneuver initialization
				if (b.isLast()) {
					ArrayList<Maneuver> maneuvers = b.maneuver.getShip()
							.getPlan().getManeuvers();
					int nextIndex = maneuvers.indexOf(b.maneuver) + 1;
					if (maneuvers.size() > nextIndex) {
						Maneuver nextM = maneuvers.get(nextIndex);
						nextM.init();
						events.addAll(nextM.burns);
						events.sort(bComp);
					}
				}
			} else if (e instanceof SOIChange) {
				// Check for other events with the same ship. After a SOIChange,
				// those changes are irrelevant
				for (SimEvent e2 : events) {
					if (e2.getShip().equals(e.getShip()) && e2 instanceof SOIChange) {
						events.remove(e2);
					}
				}
			}
		}
		
		// Resurrect the events for use in the live simulation
		events = deadEvents;
		
		// Reverse time and restore ships to initial states
		for(SimObject o: objects) {
			o.updateTo(simStartTimeTAI);
		}
		for(Ship s: ships) {
			s.setParent(initParents.get(s));
			Vector[] initState = initStates.get(s);
			s.pos = initState[0];
			s.vel = initState[1];
		}
	}

	/**
	 * Must be called after any change is made to the simulation (other than
	 * updating it). This checks for SOI changes.
	 * 
	 * TODO use some sort of listener to do this automatically?
	 */
	private void refreshSimState(Ship s) {
		// Check sibling case
		for (SimObject o : s.parent.getChildren()) {
			if (o instanceof Body) {
				Body body = (Body) o;
				double timeToIntercept = s.timeToIntercept(body);
				if (timeToIntercept > 0) {
					events.add(new SOIChange(s, body, s.lastUpdatedTime
							+ timeToIntercept));
					events.sort(bComp);
				}
			}
		}
		// Check parent case
		if (s.isEscapingSOI()) {
			double timeToEscape = Astrophysics.timeToEscape(s.pos, s.vel,
					s.parent.mu, s.parent.soiRadius);
			events.add(new SOIChange(s, s.parent.parent, s.lastUpdatedTime
					+ timeToEscape));
			events.sort(bComp);
		}
	}

	private void displayLiveSimulation() {
		// Live simulation
		int eventIndex = 0;
		SimEvent currentEvent = events.get(eventIndex);
		while (true) {
			/*
			 * Add objects to the render update list. Children of all direct
			 * ancestors are added. Children are also added.
			 */
			ArrayList<SimObject> updateObjects = sim.getFocus().getFamily();

			/* Update the simObjects */
			double lastEpoch = epochTAI;
			updateEpoch();
			while(epochTAI > currentEvent.getEpoch() && eventIndex < events.size()) {
				for (SimObject o : objects) {
					o.updateTo(currentEvent.getEpoch());
				}
				currentEvent.execute();
				eventIndex++;
				if(eventIndex < events.size()) {
					currentEvent = events.get(eventIndex);
				}
			}
			for (SimObject o : objects) {
				o.updateTo(epochTAI);
			}
			
			// System.out.println("physics fps: " +
			// (Simulation.SIM_SPEED/(epochTAI-lastEpoch)));
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
	
	private class SolarSystemRenderer implements Renderer {
		@Override
		public void initGL() {
			for (SimObject o : objects) {
				o.initGL();
			}
		}

		@Override
		public void update() {

			/*
			 * Add objects to the render update list. Children of all direct
			 * ancestors are added. Children are also added.
			 */
			ArrayList<SimObject> updateObjects = sim.getFocus().getFamily();

			/*
			 * TODO find a better way for object-specific settings
			 */
			for (SimObject o : objects) {
				if (o instanceof Ship) {
					o.render(RenderDetail.MAX);
				} else {
					o.render(RenderDetail.MAX);
				}
			}
		}

		@Override
		public void dispose() {
			for (SimObject o : objects) {
				o.dispose();
			}
		}
	}
	
	private class EventComparator implements Comparator<SimEvent> {
		@Override
		public int compare(SimEvent burn1, SimEvent burn2) {
			return burn1.getEpoch() < burn2.getEpoch() ? -1
					: burn1.getEpoch() == burn2.getEpoch() ? 0 : 1;
		}
	}
	
	/**
	 * @return the current epoch in TAI seconds for the solar system
	 */
	public double getEpoch() {
		return epochTAI;
	}

	/**
	 * @param epoch
	 *            The epoch in TAI time
	 */
	public void setEpoch(double epoch) {
		this.epochTAI = epoch;
	}

	/**
	 * @param jd
	 *            The epoch as a julian date
	 */
	public void setEpochJD(double jd) {
		setEpoch(Time.jdToTAI(jd));
	}

	/**
	 * Set the epoch as a date from the gregorian calendar
	 */
	public void setEpoch(int yr, int mo, int d, int h, int min, double s) {
		setEpoch(Time.jdToTAI(Time.getJulianDate(yr, mo, d, h, min, s)));
	}

	public void setObjects(ArrayList<SimObject> objects) {
		this.objects = objects;
	}

	public void addObject(SimObject object) {
		objects.add(object);
	}

	public void addObjects(ArrayList<SimObject> objects) {
		if (objects != null) {
			this.objects.addAll(objects);
		}
	}

	/**
	 * @param name
	 * @return The first object with the given name
	 */
	public SimObject getObject(String name) {
		for (SimObject o : objects) {
			if (o.name.equals(name)) {
				return o;
			}
		}
		return null;
	}

	/**
	 * Remove an object from the solar system list and from its parent's list of
	 * children. If the object has children, they will be removed too.
	 * 
	 * @param object
	 */
	public void removeObject(SimObject object) {
		objects.remove(object);
		if (object instanceof Body) {
			objects.removeAll(((Body) object).getChildren());
		}
		object.parent.getChildren().remove(object);
	}

	public ArrayList<SimObject> getObjects() {
		return objects;
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
