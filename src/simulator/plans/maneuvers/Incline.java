package simulator.plans.maneuvers;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.plans.Burn;
import simulator.plans.Maneuver;
import simulator.plans.Burn.Command;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Incline extends Maneuver {
	private double i_final;

	/**
	 * Change the inclination at the next orbit node. Currently only works at
	 * ascending node
	 * 
	 * @param i_final
	 *            desired inclination in radians
	 */
	public Incline(double i_final) {
		this.i_final = i_final;
		burns = new ArrayList<Burn>();

		inputs = new HashMap<String, String>();
		inputs.put("i", String.valueOf(i_final));
	}

	public Incline(Simulation sim, HashMap<String, String> args) {
		this.i_final = Math.toRadians(Double.parseDouble((String) args.values()
				.toArray()[0]));
		burns = new ArrayList<Burn>();

		inputs = args;
	}

	public void init() {
		// TODO Doesn't work when 2+ other maneuvers come before it
		
		/* Get current orbit */
		Orbit orb = Astrophysics.toOrbitalElements(ship.pos, ship.vel,
				ship.parent.mu);

		/* Calculate change in inclination */
		double i_initial = orb.i;
		double deltaI = i_final - i_initial;

		/*
		 * Determine true anomaly of ascending / descending nodes. TODO
		 * determine which node is less expensive
		 */
		double vAscend;
		double vDescend;
		if (orb.peri < Math.PI) {
			vAscend = Math.PI - orb.peri;
			vDescend = vAscend + Math.PI;
		} else {
			vDescend = 2.0 * Math.PI - orb.peri;
			vAscend = vDescend + Math.PI;
		}
		double vNext;
		if (vAscend - orb.v < 0) {
			vNext = vDescend;
		} else {
			vNext = vAscend;
		}

		double timeToNode = Astrophysics.timeToAnomaly(ship.pos, ship.vel, orb,
				ship.parent.mu, vNext);

		/*
		 * Determine deltaV costs This could also be done by subtracting the two
		 * velocities
		 */
		double r = ship.pos.magnitude();
		double v = ship.vel.magnitude();

		/* Calculate flight path angle fpa */
		Vector h = new VectorND(0,0,0);
		h = Vector.crossProduct(h, ship.pos, ship.vel);
		double cos_fpa = h.magnitude() / (r * v);

		deltaV = Math.abs(2.0 * v * cos_fpa * Math.sin(deltaI / 2.0));
		
		Vector[] initState = Astrophysics.kepler(ship.pos, ship.vel, ship.parent.mu, timeToNode);
		Orbit initOrb = Astrophysics.toOrbitalElements(initState[0], initState[1],
				ship.parent.mu);
		initOrb.i = i_final;
		Vector[] finalState = Astrophysics.toRV(initOrb, ship.parent.mu, false);
		final Vector delta = ((VectorND) finalState[1]).clone().subtract(initState[1]);

		/*
		 * Create the burn
		 */
		burns.add(new Burn(this, ship.lastUpdatedTime + timeToNode, new Command() {
			@Override
			public Vector getDeltaV() {
				return delta;
			}
		}));
	}
}
