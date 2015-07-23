package simulator.plans;

import com.pi.math.vector.Vector;

import simulator.simObject.Ship;

public class Burn implements SimEvent {
	private Command command;
	public double epoch;
	public boolean isFinished = false;
	public Maneuver maneuver;
	
	public Burn(Maneuver maneuver, double epoch, Command command) {
		this.maneuver = maneuver;
		this.command = command;
		this.epoch = epoch;
	}
	
	public void execute() {
		if(!isFinished) {
			maneuver.ship.vel.add(command.getDeltaV());
			isFinished = true;
		}
	}
	
	public void reverse() {
		if(isFinished) {
			maneuver.ship.vel.subtract(command.getDeltaV());
			isFinished = false;
		}
	}
	
	public interface Command {
		public Vector getDeltaV();
	}
	
	public double getEpoch() {
		return epoch;
	}
	
	public Ship getShip() {
		return maneuver.getShip();
	}
	
	public boolean isLast() {
		return maneuver.burns.indexOf(this) == maneuver.burns.size() - 1;
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
