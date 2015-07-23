package simulator.plans.maneuvers;

import java.util.ArrayList;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

import simulator.plans.Burn;
import simulator.plans.Maneuver;
import simulator.plans.Burn.Command;

public class Bielliptic extends Maneuver {
	private double rFinal;
	private double rB;

	/**
	 * Perform three tangent burns to achieve a circular orbit of radius rFinal.
	 * Note that the orbit should be circular to begin with, as this is a
	 * circular implementation of BiElliptic.
	 * 
	 * @param rB
	 *            The first radius to extend to
	 * @param rFinal
	 *            The final, target radius
	 */
	public Bielliptic(double rB, double rFinal) {
		this.rFinal = rFinal;
		this.rB = rB;
		burns = new ArrayList<Burn>();
	}

	@Override
	public void init() {
		double rInitial = ship.pos.magnitude();
		double mu = ship.parent.mu;

		double aTrans1 = (rInitial + rB) / 2.0;
		double aTrans2 = (rB + rFinal) / 2.0;

		double vInitial = Math.sqrt(mu / rInitial);
		double vTrans1B = Math.sqrt((2.0 * mu / rB) - (mu / aTrans1));
		double vTrans2C = Math.sqrt((2.0 * mu / rFinal) - (mu / aTrans2));

		double vTrans1A = Math.sqrt((2.0 * mu / rInitial) - (mu / aTrans1));
		double vTrans2B = Math.sqrt((2.0 * mu / rB) - (mu / aTrans2));
		double vFinal = Math.sqrt(mu / rFinal);

		final double deltaVA = vTrans1A - vInitial;
		final double deltaVB = vTrans2B - vTrans1B;
		final double deltaVC = vFinal - vTrans2C;
		deltaV = Math.abs(deltaVA) + Math.abs(deltaVB) + Math.abs(deltaVC);

		double tTrans1 = Math.PI
				* Math.sqrt((aTrans1 * aTrans1 * aTrans1) / mu);
		double tTrans2 = Math.PI
				* Math.sqrt((aTrans2 * aTrans2 * aTrans2) / mu);

		burns.add(new Burn(this, ship.lastUpdatedTime, new Command() {
			@Override
			public Vector getDeltaV() {
				Vector delta = ((VectorND) ship.vel).clone().normalize().multiply(deltaVA);
				return delta;
			}
		}));

		burns.add(new Burn(this, ship.lastUpdatedTime + tTrans1, new Command() {
			@Override
			public Vector getDeltaV() {
				Vector delta = ((VectorND) ship.vel).clone().normalize().multiply(deltaVB);
				return delta;
			}
		}));

		burns.add(new Burn(this, ship.lastUpdatedTime + tTrans1 + tTrans2,
				new Command() {
					@Override
					public Vector getDeltaV() {
						Vector delta = ((VectorND) ship.vel).clone().normalize()
								.multiply(deltaVC);
						return delta;
					}
				}));
	}
}
