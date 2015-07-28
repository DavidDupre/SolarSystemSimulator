package simulator.plans.maneuvers;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.plans.Burn;
import simulator.simObject.Ship;

public abstract class Maneuver {	
	public ArrayList<Burn> burns;
	public Ship ship;
	public double deltaV;
	public HashMap<String, String> inputs;
	
	public void setShip(Ship ship) {
		this.ship = ship;
	}
	
	public Ship getShip() {
		return ship;
	}
	
	public double getStartEpoch() {
		return burns.get(0).epoch;
	}
	
	public double getEndEpoch() {
		return burns.get(burns.size()-1).epoch;
	}
	
	public abstract void init();
	
	public void reInit() {
		burns.clear();
		init();
	}
	
	public int getIndex() {
		return getShip().getManeuvers().indexOf(this);
	}
	
	public boolean isFinished() {
		for(Burn b : burns) {
			if(!b.isFinished) {
				return false;
			}
		}
		return burns.size() > 0;
	}
	
	public String toString() {
		return this.getClass().getSimpleName();
	}
}
