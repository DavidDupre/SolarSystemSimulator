package simulator.plans;

import simulator.plans.maneuvers.Maneuver;
import simulator.simObject.Ship;

import com.pi.math.vector.Vector;

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
	
	public boolean isFirst() {
		return maneuver.burns.indexOf(this) == 0;
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
	
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
