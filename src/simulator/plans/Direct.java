package simulator.plans;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.plans.Burn.Command;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Direct extends Maneuver {
	private Vector vel;
	
	/**
	 * Add velocity directly to the craft
	 * @param vel
	 */
	public Direct(Vector vel) {
		this.vel = vel;
		burns = new ArrayList<Burn>();
		
		inputs = new HashMap<String, String>();
		inputs.put("x", String.valueOf(vel.get(0)));
		inputs.put("y", String.valueOf(vel.get(1)));
		inputs.put("z", String.valueOf(vel.get(2)));
	}
	
	public Direct(HashMap<String, String> args) {
		double x = Double.parseDouble(args.get("x"));
		double y = Double.parseDouble(args.get("y"));
		double z = Double.parseDouble(args.get("z"));
		vel = new VectorND(x,y,z);
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
