package simulator.plans;

import java.util.ArrayList;

import simulator.plans.Burn.Command;

public class WaitCommand extends Maneuver {
	private double waitTime;

	/**
	 * Wait before initializing the next maneuver. waitTime in seconds
	 * 
	 * @param waitTime
	 */
	public WaitCommand(double waitTime) {
		burns = new ArrayList<Burn>();
		this.waitTime = waitTime;
	}

	@Override
	public void init() {
		burns.add(new Burn(ship.lastUpdatedTime + waitTime, new Command() {
			@Override
			public void run() {
				// ayy lmao
			}
		}));
	}

}
