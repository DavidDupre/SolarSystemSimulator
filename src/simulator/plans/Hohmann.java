package simulator.plans;

import java.util.ArrayList;

import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;
import simulator.plans.Burn.Command;

/**
 * From page 313
 * 
 * @author s-2482153
 *
 */
public class Hohmann extends Maneuver {
	private double executeEpoch;
	private double rFinal;

	/**
	 * Raise or lower the orbit to the desired radius, rFinal, using two tangent
	 * burns.
	 * 
	 * @param rFinal
	 *            the target radius
	 */
	public Hohmann(double rFinal) {
		this.rFinal = rFinal;
		burns = new ArrayList<Burn>();
	}

	public void init() {
		double grav = ship.parent.mu;

		/*
		 * Calculate time to first burn
		 */
		Orbit orb = Astrophysics.toOrbitalElements(ship.pos, ship.vel, grav);
		double rPeri = orb.a * (1.0 - orb.e);
		double rApo = orb.a * (1.0 + orb.e);
		double rInitial = 0;
		double targetAnomaly = 0;
		if (rFinal > rApo) {
			rInitial = rPeri;
			targetAnomaly = 2.0 * Math.PI;
		} else {
			rInitial = rApo;
			targetAnomaly = Math.PI;
		}
		executeEpoch = ship.lastUpdatedTime
				+ Astrophysics.timeToAnomaly(ship.pos, ship.vel, orb, grav,
						targetAnomaly);

		double aTrans = (rInitial + rFinal) / 2.0;
		double aInitial = (rApo + rPeri) / 2.0;

		double vInitial = Math.sqrt(((2.0 * grav) / rInitial)
				- (grav / aInitial));
		double vFinal = Math.sqrt(grav / rFinal); // TODO use aFinal for
													// elliptical final orbits
		double vTransA = Math.sqrt(((2.0 * grav) / rInitial) - (grav / aTrans));
		double vTransB = Math.sqrt(((2.0 * grav) / rFinal) - (grav / aTrans));

		final double deltaVAMag = vTransA - vInitial;
		final double deltaVBMag = vFinal - vTransB;

		double tTrans = Math.PI * Math.sqrt((aTrans * aTrans * aTrans) / grav);

		deltaV = Math.abs(deltaVAMag) + Math.abs(deltaVBMag);

		Burn burnA = new Burn(executeEpoch, new Command() {
			@Override
			public void run() {
				Vector3D deltaVA = ship.vel.clone().normalize()
						.multiply(deltaVAMag);
				ship.vel.add(deltaVA);
			}
		});

		Burn burnB = new Burn(executeEpoch + tTrans, new Command() {
			@Override
			public void run() {
				Vector3D deltaVB = ship.vel.clone().normalize()
						.multiply(deltaVBMag);
				ship.vel.add(deltaVB);
			}
		});

		burns.add(burnA);
		burns.add(burnB);
	}
}
