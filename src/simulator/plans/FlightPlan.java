package simulator.plans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import simulator.simObject.Ship;

public class FlightPlan {
	private ArrayList<Maneuver> maneuvers;
	private int index;
	public Ship ship;

	public FlightPlan(Ship ship) {
		this.ship = ship;
		index = 0;
		maneuvers = new ArrayList<Maneuver>();
	}

	public void addManeuver(Maneuver m) {
		maneuvers.add(m);
		// sort();
	}

	public ArrayList<Maneuver> getManeuvers() {
		return maneuvers;
	}

	public void start() {
		maneuvers.get(0).init();
	}

	/**
	 * Sort the maneuvers based on epoch. Kind of broken and pointless...
	 */
	private void sort() {
		HashMap<Double, Maneuver> sorted = new HashMap<Double, Maneuver>();
		Double[] sortedEpochs = new Double[maneuvers.size()];
		for (int i = 0; i < maneuvers.size(); i++) {
			double startEpoch = maneuvers.get(i).getStartEpoch();
			sorted.put(startEpoch, maneuvers.get(i));
			sortedEpochs[i] = startEpoch;
		}
		Arrays.sort(sortedEpochs);
		maneuvers.clear();
		for (int i = 0; i < sorted.size(); i++) {
			maneuvers.add(sorted.get(sortedEpochs[i]));
		}
	}

	public void updateTo(double epoch) {
		if (!maneuvers.isEmpty()) {
			Maneuver current = maneuvers.get(index);
			current.updateTo(epoch);
			if (current.isFinished() && index < maneuvers.size() - 1) {
				index++;
				current = maneuvers.get(index);
				current.init();
			}
		}
	}
}
