package simulator.simObject;

import java.util.concurrent.locks.ReentrantLock;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Time;
import simulator.plans.FlightPlan;
import simulator.plans.Maneuver;
import simulator.tle.TLE;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Ship extends SimObject {

	private FlightPlan plan;

	/*
	 * Regarding the source of the ship. Raw means storing its vectors. The data
	 * should be stored raw if it was like that originally. It should also be
	 * stored raw if the orbit has changed from its original TLE.
	 */
	public boolean storeRaw = false;

	public Ship(TLE tle, Body parent) {
		color = new float[] { 1.0f, .2f, .2f };
		lock = new ReentrantLock();
		plan = new FlightPlan(this);
		name = tle.name;
		setParent(parent);
		this.orb = tle.getOrbit();
		Vector[] state = Astrophysics.toRV(orb, parent.mu, false);
		pos = state[0];
		vel = state[1];
		double now = System.currentTimeMillis() / 1000.0;
		if (Simulation.REAL_TIME) {
			lastUpdatedTime = Time.jdToTAI(tle.getEpoch());
		} else {
			lastUpdatedTime = now;
		}
		updateTo(now);
	}

	/**
	 * 
	 * @param name
	 * @param state
	 * @param parent
	 * @param epoch
	 *            Epoch in julian date
	 */
	public Ship(String name, Vector[] state, Body parent, double epoch) {
		color = new float[] { 1.0f, .2f, .2f };
		lock = new ReentrantLock();
		plan = new FlightPlan(this);
		this.name = name;
		setParent(parent);
		this.orb = Astrophysics
				.toOrbitalElements(state[0], state[1], parent.mu);
		pos = state[0];
		vel = state[1];
		double now = System.currentTimeMillis() / 1000.0;
		if (Simulation.REAL_TIME && !Double.isNaN(epoch)) {
			lastUpdatedTime = Time.jdToTAI(epoch);
		} else {
			lastUpdatedTime = now;
		}
		updateTo(now);
	}

	public void addManeuver(Maneuver m) {
		m.setShip(this);
		if (plan.getManeuvers().isEmpty()) {
			m.init();
		}
		plan.addManeuver(m);
	}

	public FlightPlan getPlan() {
		return plan;
	}

	/**
	 * Update to the current epoch. Also updates the flight plan
	 * 
	 * @param epoch
	 */
	@Override
	public void updateTo(double epoch) {
		update(epoch - lastUpdatedTime);
		lastUpdatedTime = epoch;
		plan.updateTo(epoch);

		/* Check for sphere of influence changing */
		if (pos.magnitude() > parent.soiRadius) {
			System.out.println(name + " escaped " + parent.name);
			setParent(parent.parent);
		} else {
			for (int i = 0; i < parent.children.size(); i++) {
				SimObject o = parent.children.get(i);
				if (o instanceof Body) {
					Body b = (Body) o;
					if (((VectorND)pos).clone().subtract(b.pos).magnitude() < b.soiRadius) {
						System.out.println(name + " is in the SOI of " + b.name
								+ "!");
						setParent(b);
					}
				}
			}
		}
	}
}
