package simulator.plans;

import java.util.ArrayList;

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

	private static double NULL_DOUBLE = -420.0;

	/**
	 * Raise the orbit to the desired radius, rFinal, using two tangent burns
	 * TODO add version where it waits until peripasis to minimize delta-v
	 * 
	 * @param executeEpoch
	 * @param rFinal
	 */
	public Hohmann(double executeEpoch, double rFinal) {
		this.executeEpoch = executeEpoch;
		this.rFinal = rFinal;
		burns = new ArrayList<Burn>();
	}

	public Hohmann(double rFinal) {
		executeEpoch = NULL_DOUBLE;
		this.rFinal = rFinal;
		burns = new ArrayList<Burn>();
	}

	public void init() {
		if (executeEpoch == NULL_DOUBLE) {
			executeEpoch = ship.lastUpdatedTime;
		}

		double grav = ship.parent.mu;
		double rInitial = ship.pos.magnitude();

		double aTrans = (rInitial + rFinal) / 2.0;

		double vInitial = Math.sqrt(grav / rInitial);
		double vFinal = Math.sqrt(grav / rFinal);
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
