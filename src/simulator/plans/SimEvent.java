package simulator.plans;

import simulator.simObject.Ship;

public interface SimEvent {
	public double getEpoch();
	public void execute();
	public void reverse();
	public Ship getShip();
	public boolean isFinished();
	public void setFinished(boolean finished);
}
