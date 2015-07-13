package simulator.plans;

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
		command.run();
		isFinished = true;
	}
	
	public interface Command {
		public void run();
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
}
