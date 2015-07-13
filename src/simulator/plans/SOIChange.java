package simulator.plans;

import simulator.simObject.Body;
import simulator.simObject.Ship;

public class SOIChange implements SimEvent {
	private Ship ship;
	private Body newParent;
	private double epoch;
	
	public SOIChange(Ship ship, Body newParent, double epoch) {
		this.ship = ship;
		this.newParent = newParent;
		this.epoch = epoch;
	}
	
	@Override
	public double getEpoch() {
		return epoch;
	}

	@Override
	public void execute() {
		System.out.println(ship.name + " left " + ship.parent.name + " to " + newParent.name);
		ship.setParent(newParent);
	}
	
	public Ship getShip() {
		return ship;
	}
}
