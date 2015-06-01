package simulator.plans;

import java.util.ArrayList;

import simulator.astro.Vector3D;
import simulator.plans.Burn.Command;

public class Circularize extends Maneuver {
	public Circularize() {
		burns = new ArrayList<Burn>();
	}

	@Override
	public void init() {
		double vCircMag = Math.sqrt(ship.parent.mu/ship.pos.magnitude());
		final Vector3D circVel = ship.vel.clone().normalize().multiply(vCircMag);
		deltaV = circVel.dist(ship.vel);
		
		burns.add(new Burn(ship.lastUpdatedTime, new Command() {
			@Override
			public void run() {
				ship.vel.set(circVel);
			}
		}));
	}
}
