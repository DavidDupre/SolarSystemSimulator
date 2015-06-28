package simulator.plans;

import java.util.ArrayList;

import simulator.astro.Vector3D;
import simulator.plans.Burn.Command;

public class Direct extends Maneuver {
	private Vector3D vel;
	
	/**
	 * Add velocity directly to the craft
	 * @param vel
	 */
	public Direct(Vector3D vel) {
		this.vel = vel;
		burns = new ArrayList<Burn>();
	}
	
	public Direct(String[] args) {
		double x = Double.parseDouble(args[1]);
		double y = Double.parseDouble(args[2]);
		double z = Double.parseDouble(args[3]);
		vel = new Vector3D(x,y,z);
		burns = new ArrayList<Burn>();
	}
	
	@Override
	public void init() {
		deltaV = vel.magnitude();
		
		Burn burn = new Burn(ship.lastUpdatedTime, new Command() {
			@Override
			public void run() {
				ship.vel.add(vel);
			}
		});
		
		burns.add(burn);
	}
	
}
