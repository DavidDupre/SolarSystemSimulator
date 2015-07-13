package simulator.plans;

import java.util.ArrayList;
import java.util.Comparator;

import simulator.simObject.Ship;

public class FlightPlan {
	private ArrayList<Maneuver> maneuvers;
	public Ship ship;
	private ManeuverComparator comp;

	public FlightPlan(Ship ship) {
		this.ship = ship;
		maneuvers = new ArrayList<Maneuver>();
		comp = new ManeuverComparator();
	}

	public void addManeuver(Maneuver m) {
		maneuvers.add(m);
//		maneuvers.sort(comp);
	}
	
	private class ManeuverComparator implements Comparator<Maneuver> {
		@Override
		public int compare(Maneuver o1, Maneuver o2) {
			Burn burn1 = o1.burns.get(0);
			Burn burn2 = o2.burns.get(0);
			return burn1.epoch < burn2.epoch ? -1 : burn1.epoch == burn2.epoch ? 0 : 1;
		}
		
	}

	public ArrayList<Maneuver> getManeuvers() {
		return maneuvers;
	}
}
