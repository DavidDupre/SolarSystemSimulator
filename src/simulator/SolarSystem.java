package simulator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
	private EventComparator eComp;
	private ReverseEventComparator eCompRev;
	
	private ArrayList<ThreadRequest> requestQueue;
	
	public SolarSystem(Simulation sim) {
		renderer = new SolarSystemRenderer();
		objects = new ArrayList<SimObject>();
		this.sim = sim;
		events = new ArrayList<SimEvent>();
		eComp = new EventComparator();
		eCompRev = new ReverseEventComparator();
		
		requestQueue = new ArrayList<ThreadRequest>();
	}
	
	@Override
	public void run() {
		lastTime = System.currentTimeMillis();
		if (Double.isNaN(epochTAI)) {
			epochTAI = simStartTimeTAI;
		}

		bufferEvents();
		updateTo(simStartTimeTAI);

//		System.out.println("\nstarting live sim");

		displayLiveSimulation();
	}
	
	public void updateTo(double epoch) {
		// TODO This method of looking for events seems wasteful
		ArrayList<SimEvent> forwardEvents = new ArrayList<SimEvent>();
		ArrayList<SimEvent> reverseEvents = new ArrayList<SimEvent>();
		for (SimEvent e : events) {
			if (epoch >= e.getEpoch() && !e.isFinished()) {
				forwardEvents.add(e);
			} else if (epoch < e.getEpoch() && e.isFinished()) {
				reverseEvents.add(e);
			}
		}
		forwardEvents.sort(eComp);
		reverseEvents.sort(eCompRev);
		for (SimEvent e : forwardEvents) {
			for (SimObject o : objects) {
				o.updateTo(e.getEpoch());
			}
			e.execute();

//			if (e instanceof Burn) {
//				System.out.println("execute " + ((Burn) e).maneuver.getIndex() + " -- " + e.toString());
//			} else {
//				System.out.println("execute " + e.toString());
//			}
		}
		for (SimEvent e : reverseEvents) {
			for (SimObject o : objects) {
				o.updateTo(e.getEpoch());
			}
			e.reverse();

//			if (e instanceof Burn) {
//				System.out.println("reverse " + ((Burn) e).maneuver.getIndex() + " -- " + e.toString());
//			} else {
//				System.out.println("reverse " + e.toString());
//			}
		}

		for (SimObject o : objects) {
			o.updateTo(epoch);
		}
	}

	private void updateEpoch() {
		double delta = getDeltaTime() / 1000.0;
		delta *= sim.simSpeed;
		epochTAI += delta;
	}

	private void bufferEvents() {
		ArrayList<SimEvent> deadEvents = new ArrayList<SimEvent>();

		// Create list of ships
		ArrayList<Ship> ships = new ArrayList<Ship>();
		for (SimObject o : objects) {
			if (o instanceof Ship) {
				Ship s = (Ship) o;
				ships.add(s);
			}
		}

		// Create list of burns and initialize first maneuvers
		events = new ArrayList<SimEvent>();
		for (Ship s : ships) {
			if (!s.getManeuvers().isEmpty()) {
				Maneuver m = s.getManeuvers().get(0);
				m.burns.clear();
				m.init();
				events.addAll(m.burns);
			}
		}
		eComp = new EventComparator();
		events.sort(eComp);

		// Cycle through events
		while (!events.isEmpty()) {
			SimEvent e = events.get(0);
			for (SimObject o : objects) {
				o.updateTo(e.getEpoch());
			}

			e.execute();
			events.remove(e);
			deadEvents.add(e);

			checkForSOIChanges(e.getShip());

			if (e instanceof Burn) {
				Burn b = (Burn) e;

				// Handle maneuver initialization
				if (b.isLast()) {
					ArrayList<Maneuver> maneuvers = b.maneuver.getShip()
							.getManeuvers();
					int nextIndex = maneuvers.indexOf(b.maneuver) + 1;
					if (nextIndex < maneuvers.size()) {
						Maneuver nextM = maneuvers.get(nextIndex);
						nextM.init();
						events.addAll(nextM.burns);
						events.sort(eComp);
					}
				}
			}
		}

		// Resurrect the events for use in the live simulation
		events = deadEvents;
	}
	
	/**
	 * Must be called after each burn or SOI change
	 */
	private void checkForSOIChanges(Ship s) {
		// Check sibling case
		for (SimObject o : s.parent.getChildren()) {
			if (o instanceof Body) {
				Body body = (Body) o;
				double timeToIntercept = s.timeToIntercept(body);
				if (timeToIntercept > 0) {
					events.add(new SOIChange(s, body, s.lastUpdatedTime
							+ timeToIntercept));
					events.sort(eComp);
				}
			}
		}
		// Check parent case
		if (s.isEscapingSOI()) {
			double timeToEscape = Astrophysics.timeToEscape(s.pos, s.vel,
					s.parent.mu, s.parent.soiRadius);
			events.add(new SOIChange(s, s.parent.parent, s.lastUpdatedTime
					+ timeToEscape));
			events.sort(eComp);
		}
	}

	private void displayLiveSimulation() {
		while (true) {
			for(ThreadRequest r: requestQueue) {
				r.fulfill();
			}
			requestQueue.clear();
			
			updateEpoch();
			updateTo(epochTAI);
		}
	}

	/**
	 * Add a new even to the queue during the simulation. Make sure to call
	 * setShip() on the maneuver first.
	 * 
	 * @param m
	 */
	public void addManeuver(final Maneuver m) {
		if(Thread.currentThread().equals(thread)) {
			Ship s = m.getShip();
			double lastEpoch = 0;
			if (s.getManeuvers().isEmpty()) {
				lastEpoch = epochTAI;
			} else {
				Maneuver lastManeuver = s.getManeuvers().get(
						s.getManeuvers().size() - 1);
				lastEpoch = lastManeuver.getEndEpoch();
				lastEpoch = lastEpoch > epochTAI ? lastEpoch : epochTAI;
			}
			System.out.println("updating to lastEpoch");
			updateTo(lastEpoch);
			System.out.println("init new maneuver");
			m.init();
			s.getManeuvers().add(m);
			events.addAll(m.burns);
			for (Burn b : m.burns) {
				System.out.println("updating to new burn");
				updateTo(b.getEpoch());
				this.checkForSOIChanges(s);
			}
			System.out.println("updating to epochTAI");
			updateTo(epochTAI);
		} else {
			requestQueue.add(new ThreadRequest() {
				@Override
				public void fulfill() {
					addManeuver(m);
				}
			});
		}
	}

	public void addManeuver(Maneuver m, Ship s) {
		m.setShip(s);
		addManeuver(m);
	}
	
	private interface ThreadRequest {
		public void fulfill();
	}

	public void insertManeuver(final Maneuver m, final int index) {
		if(Thread.currentThread().equals(thread)) {
			Ship s = m.getShip();
			List<Maneuver> master = s.getManeuvers();
			List<Maneuver> before = new ArrayList<Maneuver>();
			for (int i = 0; i < index; i++) {
				before.add(master.get(i));
			}
			List<Maneuver> after = new ArrayList<Maneuver>();
			for (int i = index; i < s.getManeuvers().size(); i++) {
				after.add(master.get(i));
			}
			master.clear();
			master.addAll(before);
			master.add(m);
			master.addAll(after);
	
			double preEpoch = 0;
			if (index > 0) {
				preEpoch = master.get(index - 1).getEndEpoch();
			} else {
				preEpoch = simStartTimeTAI;
			}
	
	//		System.out.println("updating to preEpoch");
			updateTo(preEpoch);
	
			// Remove stale SOIChanges
			for (SimEvent e : events) {
				if (e instanceof SOIChange && e.getShip().equals(s)
						&& e.getEpoch() > preEpoch) {
					events.remove(e);
				}
			}
	
			m.init();
			events.addAll(m.burns);
	
			// Remove stale burns
			for (Maneuver a : after) {
				events.removeAll(a.burns);
			}
	
			// Add a sketchy offset so the events will still occur in sequence
			double sketchOffset = 1E-5;
			
	//		System.out.println("updating to new event");
			updateTo(m.getEndEpoch() + sketchOffset);
	
			// Re-initialize stale burns
			for (Maneuver a : after) {
	//			System.out.println("re-init " + a.getIndex());
				a.reInit();
				events.addAll(a.burns);
				events.sort(eComp);
				for (Burn b : a.burns) {
	//				System.out.println("updating to stale burn");
					updateTo(b.getEpoch() + sketchOffset);
					checkForSOIChanges(s);
				}
			}
	
	//		System.out.println("updating to current");
			updateTo(epochTAI);
		} else {
			requestQueue.add(new ThreadRequest() {
				@Override
				public void fulfill() {
					insertManeuver(m, index);
				}
			});
		}
	}

	public void insertManeuver(Maneuver m, int index, Ship s) {
		m.setShip(s);
		insertManeuver(m, index);
	}
	
	public void removeManeuver(final Ship s, final int index) {
		if(Thread.currentThread().equals(thread)) {
			Maneuver m = s.getManeuvers().get(index);
			
			List<Maneuver> after = new ArrayList<Maneuver>();
			for (int i = index+1; i < s.getManeuvers().size(); i++) {
				after.add(s.getManeuvers().get(i));
			}
			s.getManeuvers().remove(m);
	
			double preEpoch = 0;
			if (index > 0) {
				preEpoch = s.getManeuvers().get(index - 1).getEndEpoch();
			} else {
				preEpoch = simStartTimeTAI;
			}
	
	//		System.out.println("updating to preEpoch");
			updateTo(preEpoch);
			
			// Remove removed maneuver
			events.removeAll(m.burns);
	
			// Remove stale SOIChanges
			for (SimEvent e : events) {
				if (e instanceof SOIChange && e.getShip().equals(s)
						&& e.getEpoch() > preEpoch) {
					events.remove(e);
				}
			}
			
			// Remove stale burns
			for (Maneuver a : after) {
				events.removeAll(a.burns);
			}
			
			// Add a sketchy offset so the events will still occur in sequence
			double sketchOffset = 1E-5;
			
			// Re-initialize stale burns
			for (Maneuver a : after) {
	//			System.out.println("re-init " + a.getIndex());
				a.reInit();
				events.addAll(a.burns);
				events.sort(eComp);
				for (Burn b : a.burns) {
	//				System.out.println("updating to stale burn");
					updateTo(b.getEpoch() + sketchOffset);
					checkForSOIChanges(s);
				}
			}
	
	//		System.out.println("updating to current");
			updateTo(epochTAI);
		} else {
			requestQueue.add(new ThreadRequest() {
				@Override
				public void fulfill() {
					removeManeuver(s, index);
				}
			});
		}
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
			for (SimObject o : objects) {
				o.render(RenderDetail.MAX);
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

	private class ReverseEventComparator implements Comparator<SimEvent> {
		@Override
		public int compare(SimEvent burn1, SimEvent burn2) {
			return burn1.getEpoch() < burn2.getEpoch() ? 1
					: burn1.getEpoch() == burn2.getEpoch() ? 0 : -1;
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
