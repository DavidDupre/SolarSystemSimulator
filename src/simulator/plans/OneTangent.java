package simulator.plans;

import java.util.ArrayList;

public class OneTangent extends Maneuver {
	private double rFinal;
	private double anom;

	/**
	 * Gotta go fast. Page 321.
	 * 
	 * @param rFinal
	 *            target radius
	 * @param anom
	 *            true anomaly of the non-tangential burn
	 */
	public OneTangent(double rFinal, double anom) {
		this.rFinal = rFinal;
		this.anom = anom;
		burns = new ArrayList<Burn>();
	}

	@Override
	public void init() {
		double mu = ship.parent.mu;
		
		double rInitial = ship.pos.magnitude();
		double inverseR = rInitial / rFinal;
		
		/* Determine if the orbit is being lowered or raised. For use in eTrans and aTrans */
		boolean isApoapsis = rInitial > rFinal;
		double apoScale = isApoapsis ? 1.0 : -1.0;
		
		double eTrans = (inverseR - 1.0)/(Math.cos(anom) + apoScale*inverseR);
		double aTrans = rInitial/(1 + apoScale*eTrans);
		
		double vInitial = Math.sqrt(mu/rInitial);
		double vFinal = Math.sqrt(mu/rFinal);
		
		double vTransA = Math.sqrt((2.0*mu/rInitial)-(mu/aTrans));
		double vTransB = Math.sqrt((2.0*mu/rFinal)-(mu/aTrans));
		
		double deltaVA = vTransA - vInitial;
		
		double phiTransB = Math.atan2(eTrans*Math.sin(anom), 1.0+eTrans*Math.cos(anom));
		double deltaVB = Math.sqrt(vTransB*vTransB+vFinal*vFinal-2.0*vTransB*vFinal*Math.cos(phiTransB));
		
		deltaV = Math.abs(deltaVA) + Math.abs(deltaVB);
		
		double E = Math.acos((eTrans+Math.cos(anom))/(1.0+eTrans*Math.cos(anom)));
		
		// TODO fix tTrans. It doesn't match the book
		double tTrans = Math.sqrt((aTrans*aTrans*aTrans)/mu)*((E-eTrans*Math.sin(E)));
	}
}
