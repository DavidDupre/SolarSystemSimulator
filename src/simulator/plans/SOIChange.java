package simulator.plans;

import simulator.simObject.Body;
import simulator.simObject.Ship;

public class SOIChange implements SimEvent {
	private Ship ship;
	private Body newParent;
	private Body oldParent;
	private double epoch;
	private boolean isFinished = false;
	
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
		if(!isFinished) {
			System.out.println(ship.name + " left " + ship.parent.name + " to " + newParent.name);
			oldParent = ship.parent;
			ship.setParent(newParent);
			isFinished = true;
		}
	}
	
	@Override
	public void reverse() {
		if(isFinished) {
			newParent = ship.parent;
			ship.setParent(oldParent);
			isFinished = false;
		}
	}
	
	@Override
	public Ship getShip() {
		return ship;
	}
	
	@Override
	public boolean isFinished() {
		return isFinished;
	}
	
	@Override
	public void setFinished(boolean finished) {
		isFinished = finished;
	}
}
