package simulator.plans;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;
import simulator.plans.Burn.Command;
import simulator.simObject.SimObject;

public class Target extends Maneuver {
	private SimObject target;
	private boolean useLongWay;

	/**
	 * TODO automatically determine the optimal time
	 * 
	 * @param target
	 * @param deltaT
	 */
	public Target(SimObject target) {
		this.target = target;
		burns = new ArrayList<Burn>();

		inputs = new HashMap<String, String>();
		inputs.put("target", target.name);
	}

	public Target(Simulation sim, HashMap<String, String> args) {
		target = sim.solarSystem.getObject(args.get("target"));

		burns = new ArrayList<Burn>();
		inputs = args;
	}
	
	public double getTimeOfMin(double min, double max, int sampleSize) {
		// Plot the possible transfer times and look for a minimum
		Vector3D[] changes = new Vector3D[2];
		double minDeltaV = Double.MAX_VALUE;
		double timeOfMin = 0;
		for (int i = 0; i < sampleSize; i++) {
			double t = min + (max-min) * i / (double) sampleSize;
			changes = Astrophysics.target(ship, target, t, useLongWay);
			if (changes != null) {
				double deltaV = changes[0].magnitude() + changes[1].magnitude();
				if (deltaV < minDeltaV) {
					minDeltaV = deltaV;
					timeOfMin = t;
				}
//				System.out.println(t + "," + deltaV);
			}
		}
		return timeOfMin;
	}
	
	public double getTimeOfMin(double min, double max, int sampleSize, int iterations) {
		double timeOfMin = 0;
		for(int i=0; i<iterations; i++) {
			timeOfMin = getTimeOfMin(min, max, sampleSize);
			double stepSize = 5.0 * (max - min) / sampleSize;
			min = timeOfMin - stepSize;
			max = timeOfMin + stepSize;
		}
		return timeOfMin;
	}

	@Override
	public void init() {
		/*
		 * Determine whether to use the long or short way
		 */
		Vector3D tran_n = Vector3D.crossProduct(ship.pos, target.pos);
		Vector3D h_n = Vector3D.crossProduct(ship.pos, ship.vel);
		useLongWay = Vector3D.dotProduct(h_n, tran_n) < 0;

		/*
		 * Determine optimal time
		 */
		// As a first guess, find the transfer time for a hohmann transfer
		Orbit orb_int = Astrophysics.toOrbitalElements(ship.pos, ship.vel,
				ship.parent.mu);
		Orbit orb_tgt = Astrophysics.toOrbitalElements(target.pos, target.vel,
				target.parent.mu);
		double aTrans = (orb_int.a + orb_tgt.a) / 2.0;
		double tTrans = Math.PI
				* Math.sqrt((aTrans * aTrans * aTrans) / ship.parent.mu);
		double timeOfMin = getTimeOfMin(0, 4.0*tTrans, 100, 2);
		
		Vector3D[] changes = Astrophysics.target(ship, target, timeOfMin, useLongWay);
		final Vector3D deltaVA = changes[0];
		final Vector3D deltaVB = changes[1];

		this.deltaV = deltaVA.magnitude() + deltaVB.magnitude();

//		System.out.println("Transfer time (min): " + timeOfMin/60.0);
//		System.out.println("Delta v: " + deltaV);

		Burn burnA = new Burn(ship.lastUpdatedTime, new Command() {
			@Override
			public void run() {
				ship.vel.add(deltaVA);
			}
		});

		Burn burnB = new Burn(ship.lastUpdatedTime + timeOfMin, new Command() {
			@Override
			public void run() {
				ship.vel.add(deltaVB);
			}
		});

		burns.add(burnA);
		burns.add(burnB);
	}

}
