package simulator.plans;

import java.util.ArrayList;

import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;
import simulator.plans.Burn.Command;

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
	}

	public void init() {
		/* Get current orbit */
		Orbit orb = Astrophysics.toOrbitalElements(ship.pos, ship.vel,
				ship.parent.mu);
		double grav = ship.parent.mu;

		/* Calculate change in inclination */
		double i_initial = orb.i;
		double deltaI = i_final - i_initial;

		/*
		 * Determine true anomaly of ascending / descending nodes. TODO
		 * determine which node comes first TODO determine which node is less
		 * expensive
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
		if(vAscend - orb.v < 0) {
			vNext = vDescend;
		} else {
			vNext = vAscend;
		}
		
		/*
		 * Perform binary search to find time to next ascending node
		 */
		double period = 2.0 * Math.PI
				* Math.sqrt((orb.a * orb.a * orb.a) / grav);
		double low = 0;
		double high = period; // use period / 2 when finding next node in
								// general
		double mid = (low + high) / 2.0;
		double tolerance = Math.toRadians(1.0);
		double anomDif = tolerance * 10.0;
		int iterations = 0;
		while (Math.abs(anomDif) > tolerance && iterations < 10) {
			Vector3D[] futureState = Astrophysics.kepler(ship.pos, ship.vel,
					ship.parent.mu, mid);
			Orbit futureOrb = Astrophysics.toOrbitalElements(futureState[0],
					futureState[1], ship.parent.mu);
			anomDif = futureOrb.v - vNext;
			if (anomDif < 0) {
				low = mid;
			} else {
				high = mid;
			}
			mid = (low + high) / 2.0;
			iterations++;
		}
		double timeToNode = mid;

		/*
		 * Determine deltaV costs This could also be done by subtracting the two
		 * velocities
		 */
		double r = ship.pos.magnitude();
		double v = ship.vel.magnitude();

		/* Calculate flight path angle fpa */
		Vector3D h = Vector3D.crossProduct(ship.pos, ship.vel);
		double fpa = Math.acos(h.magnitude() / (r * v));

		// TODO cos(acos()) is dumb
		deltaV = Math.abs(2.0 * v * Math.cos(fpa) * Math.sin(deltaI / 2.0));

		/*
		 * Create the burn TODO use actual epoch instead of lastUpdatedTime
		 */
		burns.add(new Burn(ship.lastUpdatedTime + timeToNode, new Command() {
			@Override
			public void run() {
				Orbit orb = Astrophysics.toOrbitalElements(ship.pos, ship.vel,
						ship.parent.mu);
				orb.i = i_final;
				Vector3D[] newState = Astrophysics.toRV(orb, ship.parent.mu,
						false);
				// TODO ideally it shouldn't have to change position
				ship.pos = newState[0];
				ship.vel = newState[1];
			}
		}));
	}
}
