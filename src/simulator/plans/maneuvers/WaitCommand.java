package simulator.plans.maneuvers;

import java.util.ArrayList;
import java.util.HashMap;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

import simulator.Simulation;
import simulator.plans.Burn;
import simulator.plans.Maneuver;
import simulator.plans.Burn.Command;

public class WaitCommand extends Maneuver {
	private double waitTime;

	/**
	 * Wait before initializing the next maneuver. waitTime in seconds
	 * 
	 * @param waitTime
	 */
	public WaitCommand(double waitTime) {
		this.waitTime = waitTime;
		
		burns = new ArrayList<Burn>();
		inputs = new HashMap<String, String>();
		inputs.put("time", String.valueOf(waitTime));
	}
	
	public WaitCommand(Simulation sim, HashMap<String, String> args) {
		waitTime = Double.parseDouble(args.get("time"));
		
		burns = new ArrayList<Burn>();
		inputs = args;
	}

	@Override
	public void init() {
		burns.add(new Burn(this, ship.lastUpdatedTime + waitTime, new Command() {
			@Override
			public Vector getDeltaV() {
				// TODO this is kinda dumb
				return new VectorND(0,0,0);
			}
		}));
	}

}
