package simulator.plans;

import java.util.ArrayList;

import simulator.plans.Burn.Command;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Circularize extends Maneuver {
	public Circularize() {
		burns = new ArrayList<Burn>();
	}

	@Override
	public void init() {
		double vCircMag = Math.sqrt(ship.parent.mu/ship.pos.magnitude());
		final Vector circVel = ((VectorND) ship.vel).clone().normalize().multiply(vCircMag);
		deltaV = circVel.dist(ship.vel);
		
		burns.add(new Burn(ship.lastUpdatedTime, new Command() {
			@Override
			public void run() {
				ship.vel.set(circVel);
			}
		}));
	}
}
