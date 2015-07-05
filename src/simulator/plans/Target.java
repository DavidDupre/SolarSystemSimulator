package simulator.plans;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;
import simulator.plans.Burn.Command;
import simulator.simObject.SimObject;

/**
 * 
 * @author David
 *
 */
public class Target extends Maneuver {
	private SimObject target;
	private Vector3D[] minChanges;
	private DelayType delayType;

	public enum DelayType {
		UNLIMITED, ONE_ORBIT, IMMEDIATE
	}

	/**
	 * @param target
	 */
	public Target(SimObject target) {
		this.target = target;
		burns = new ArrayList<Burn>();
		delayType = DelayType.UNLIMITED;

		inputs = new HashMap<String, String>();
		inputs.put("target", target.name);
		inputs.put("delay", delayType.toString().toLowerCase());
	}

	/**
	 * @param target
	 * @param delayType
	 *            Waiting a few orbits may lead to more optimized maneuvers, but
	 *            takes longer. Set to ONE_ORBIT to limit the delay time, or
	 *            IMMEDIATE to remove the delay
	 */
	public Target(SimObject target, DelayType delayType) {
		this.target = target;
		burns = new ArrayList<Burn>();
		this.delayType = delayType;

		inputs = new HashMap<String, String>();
		inputs.put("target", target.name);
		inputs.put("delay", delayType.toString().toLowerCase());
	}

	public Target(Simulation sim, HashMap<String, String> args) {
		target = sim.solarSystem.getObject(args.get("target"));
		if (args.containsKey("delay")) {
			switch (args.get("delay")) {
			case "immediate":
				delayType = DelayType.IMMEDIATE;
				break;
			case "one_orbit":
				delayType = DelayType.ONE_ORBIT;
				break;
			default:
				delayType = DelayType.UNLIMITED;
				break;
			}
		} else {
			delayType = DelayType.UNLIMITED;
		}

		burns = new ArrayList<Burn>();
		inputs = args;
	}

	public double getTimeOfMin(double delay, double min, double max,
			int sampleSize) {
		/*
		 * Calculate future state vectors after delay
		 */
		Vector3D[] fState_int;
		Vector3D[] fState_tgt;
		if (delay > 0) {
			fState_int = Astrophysics.kepler(ship.pos, ship.vel,
					ship.parent.mu, delay);
			fState_tgt = Astrophysics.kepler(target.pos, target.vel,
					target.parent.mu, delay);
		} else {
			fState_int = new Vector3D[] { ship.pos, ship.vel };
			fState_tgt = new Vector3D[] { target.pos, target.vel };
		}

		/*
		 * Determine if using long way
		 */
		Vector3D tran_n = Vector3D.crossProduct(fState_int[0], fState_tgt[0]);
		Vector3D h_n = Vector3D.crossProduct(fState_int[0], fState_int[1]);
		boolean useLongWay = Vector3D.dotProduct(h_n, tran_n) < 0;

		/*
		 * Plot the possible transfer times and look for a minimum
		 */
		Vector3D[] changes = new Vector3D[2];
		double minDeltaV = Double.MAX_VALUE;
		double timeOfMin = 0;
		for (int i = 0; i < sampleSize; i++) {
			double t = min + (max - min) * i / (double) sampleSize;

			changes = Astrophysics.target(fState_int, fState_tgt,
					ship.parent.mu, t, useLongWay);

			if (changes != null) {
				double deltaV = changes[0].magnitude() + changes[1].magnitude();
				if (deltaV < minDeltaV) {
					minDeltaV = deltaV;
					minChanges = changes;
					timeOfMin = t;
				}
				// System.out.println(t + "," + delay + "," + deltaV);
			}
		}
		return timeOfMin;
	}

	public double getTimeOfMin(double delay, double min, double max,
			int sampleSize, int iterations) {
		double timeOfMin = 0;
		for (int i = 0; i < iterations; i++) {
			timeOfMin = getTimeOfMin(delay, min, max, sampleSize);
			double stepSize = 5.0 * (max - min) / sampleSize;
			min = timeOfMin - stepSize;
			max = timeOfMin + stepSize;
		}
		return timeOfMin;
	}

	@Override
	public void init() {
		/*
		 * Determine maximum transfer time, based on time for hohmann transfer
		 */
		Orbit orb_int = Astrophysics.toOrbitalElements(ship.pos, ship.vel,
				ship.parent.mu);
		Orbit orb_tgt = Astrophysics.toOrbitalElements(target.pos, target.vel,
				target.parent.mu);
		double aTrans = (orb_int.a + orb_tgt.a) / 2.0;
		double tTrans = Math.PI
				* Math.sqrt((aTrans * aTrans * aTrans) / ship.parent.mu);

		/*
		 * Determine delay time. Max delay based on the longer period of the two
		 * satellites
		 */
		double tInt = 2.0
				* Math.PI
				* Math.sqrt((orb_int.a * orb_int.a * orb_int.a)
						/ ship.parent.mu);
		double tTgt = 2.0
				* Math.PI
				* Math.sqrt((orb_tgt.a * orb_tgt.a * orb_tgt.a)
						/ target.parent.mu);
		double maxDelay = 0;
		switch (delayType) {
		case UNLIMITED:
			maxDelay = Math.max(tInt, tTgt);
			break;
		case ONE_ORBIT:
			maxDelay = Math.min(tInt, tTgt);
			break;
		case IMMEDIATE:
			maxDelay = 0;
			break;
		}

		double minDeltaV = Double.MAX_VALUE;
		Vector3D[] realMinChanges = new Vector3D[2];
		double timeOfMinOfMin = 0;
		double minDelay = 0;
		int sampleSize = maxDelay > 0 ? 100 : 1;
		for (int i = 0; i < sampleSize; i++) {
			double delay = maxDelay * i / (double) sampleSize;
			double timeOfMin = getTimeOfMin(delay, 0, 4.0 * tTrans, 100, 1);
			double deltaV = minChanges[0].magnitude()
					+ minChanges[1].magnitude();
			if (deltaV < minDeltaV) {
				minDeltaV = deltaV;
				minDelay = delay;
				realMinChanges = minChanges;
				timeOfMinOfMin = timeOfMin;
			}
		}

		final Vector3D deltaVA = realMinChanges[0];
		final Vector3D deltaVB = realMinChanges[1];

		this.deltaV = deltaVA.magnitude() + deltaVB.magnitude();

		System.out.println("Delay (min): " + minDelay / 60.0);
		System.out.println("Transfer time (min): " + timeOfMinOfMin / 60.0);
		System.out.println("Delta v: " + deltaV);

		Burn burnA = new Burn(ship.lastUpdatedTime + minDelay, new Command() {
			@Override
			public void run() {
				ship.vel.add(deltaVA);
			}
		});

		Burn burnB = new Burn(ship.lastUpdatedTime + minDelay + timeOfMinOfMin,
				new Command() {
					@Override
					public void run() {
						ship.vel.add(deltaVB);
					}
				});

		burns.add(burnA);
		burns.add(burnB);
	}

}
