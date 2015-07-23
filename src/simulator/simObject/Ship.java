package simulator.simObject;

import java.util.ArrayList;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Time;
import simulator.plans.Maneuver;
import simulator.tle.TLE;

import com.pi.math.vector.Vector;

public class Ship extends SimObject {

	/*
	 * Regarding the source of the ship. Raw means storing its vectors. The data
	 * should be stored raw if it was like that originally. It should also be
	 * stored raw if the orbit has changed from its original TLE.
	 */
	public boolean storeRaw = false;
	
	private ArrayList<Maneuver> maneuvers;

	public Ship(TLE tle, Body parent) {
		super();
		color = new float[] { 1.0f, .2f, .2f };
		maneuvers = new ArrayList<Maneuver>();
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
		super();
		color = new float[] { 1.0f, .2f, .2f };
		maneuvers = new ArrayList<Maneuver>();
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
		maneuvers.add(m);
	}

	public ArrayList<Maneuver> getManeuvers() {
		return maneuvers;
	}

	/**
	 * For use in patched conics.
	 * 
	 * @return true if the orbit is hyperbolic and the ship is moving away OR if
	 *         its apoapsis is beyond the parent's SOI radius
	 */
	public boolean isEscapingSOI() {
		Orbit orb = Astrophysics.toOrbitalElements(pos, vel, parent.mu);
		if (orb.v > Math.PI) {
			return false;
		}
		if (orb.e > 1) {
			return true;
		} else {
			orb.v = Math.PI;
			double apoapsis = Astrophysics.toRV(orb, parent.mu, false)[0]
					.magnitude();
			if (apoapsis > parent.soiRadius) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the shapes of the two orbits allow the ship to pass within the
	 * sphere of influence of Body b
	 * 
	 * TODO more accurate check involving other orbital elements
	 * 
	 * @param b
	 *            a sibling of this ship
	 * @return
	 */
	public boolean canIntercept(Body b) {
		Orbit orb_tgt = Astrophysics.toOrbitalElements(b.pos, b.vel, b.parent.mu);
		Orbit orb_int = Astrophysics.toOrbitalElements(pos, vel, parent.mu);
		
		if(Math.abs(orb_tgt.a - orb_int.a) < b.soiRadius) {
			return true;
		}
		
		double peri2, apo1;
		if(orb_tgt.a > orb_int.a) {
			peri2 = orb_tgt.a * (1-orb_tgt.e);
			apo1 = orb_int.a * (1+orb_int.e);
		} else {
			peri2 = orb_int.a * (1-orb_int.e);
			apo1 = orb_tgt.a * (1+orb_tgt.e);
		}
		
		if(peri2 < apo1) {
			return true;
		} else {
			return peri2 - apo1 < b.soiRadius;
		}
	}
	
	public double timeToIntercept(Body b) {
		if(!canIntercept(b)) {
			return -1;
		}
		
		double a = Astrophysics.toOrbitalElements(pos, vel, parent.mu).a;
		double period = 2.0*Math.PI*Math.sqrt((a*a*a)/parent.mu);
		int samples = 1000;
		for(int j=0; j<samples; j++) {
			double t = period * j / (double) samples;
			Vector[] state_ship = Astrophysics.kepler(pos, vel, parent.mu, t);
			Vector[] state_body = Astrophysics.kepler(b.pos, b.vel, b.parent.mu, t);
			double r = state_ship[0].subtract(state_body[0]).magnitude();
			if(r < b.soiRadius) {
				return t;
			}
		}
		
		return -1;
	}
}
