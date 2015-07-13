package simulator.plans;

import simulator.simObject.Ship;

public interface SimEvent {
	public double getEpoch();
	public void execute();
	public Ship getShip();
}
