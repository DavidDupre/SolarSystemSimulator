package simulator.plans;

import java.util.ArrayList;
import java.util.HashMap;

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
		
		inputs = new HashMap<String, String>();
		inputs.put("x", String.valueOf(vel.x));
		inputs.put("y", String.valueOf(vel.y));
		inputs.put("z", String.valueOf(vel.z));
	}
	
	public Direct(HashMap<String, String> args) {
		double x = Double.parseDouble(args.get("x"));
		double y = Double.parseDouble(args.get("y"));
		double z = Double.parseDouble(args.get("z"));
		vel = new Vector3D(x,y,z);
		burns = new ArrayList<Burn>();
		
		inputs = args;
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
