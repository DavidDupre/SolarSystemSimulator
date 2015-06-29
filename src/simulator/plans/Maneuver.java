package simulator.plans;

import java.util.ArrayList;
import java.util.HashMap;

import simulator.simObject.Ship;

public abstract class Maneuver {	
	protected ArrayList<Burn> burns;
	protected Ship ship;
	public double deltaV;
	public HashMap<String, String> inputs;
	
	public void setShip(Ship ship) {
		this.ship = ship;
	}
	
	public void updateTo(double epoch) {
		for(int i=0; i<burns.size(); i++) {
			Burn b = burns.get(i);
			if(!b.isFinished) {
				/* If the ship has missed the burn, reverse time */
				if(ship.lastUpdatedTime > b.epoch) {
					ship.storeRaw = true;
					ship.updateTo(b.epoch);
					b.execute();
				}
			}
		}
	}
	
	public double getStartEpoch() {
		return burns.get(0).epoch;
	}
	
	public abstract void init();
	
	public boolean isFinished() {
		for(Burn b : burns) {
			if(!b.isFinished) {
				return false;
			}
		}
		return burns.size() > 0;
	}
}
