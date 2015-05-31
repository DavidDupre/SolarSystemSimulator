package simulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import simulator.astro.Orbit;
import simulator.simObject.Body;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;
import simulator.tle.TLE;
import simulator.tle.TLELoader;

/**
 * Load solar system data from CSV files
 * 
 * @author S-2482153
 *
 */
public class SystemLoader {
	public ArrayList<SimObject> objects;
	private TLELoader tleLoader;

	public SystemLoader() {
		objects = new ArrayList<SimObject>();
		tleLoader = new TLELoader();
	}

	/**
	 * Get a list of satellites loaded from the Celestrak database
	 * 
	 * @param category
	 *            - the category to load from
	 * @return
	 */
	public ArrayList<SimObject> getShips(String category) {
		ArrayList<TLE> tles = tleLoader.getCategory(category);
		ArrayList<SimObject> ships = new ArrayList<SimObject>();
		Body earth = (Body) getObject("Earth");
		if(earth == null) {
			if(objects.isEmpty()) {
				earth = new Body();
			} else {
				earth = (Body) objects.get(0);
			}
		}
		for (TLE tle : tles) {
			ships.add(new Ship(tle, earth));
		}
		return ships;
	}
	
	public SimObject getShip(String category, String name) {
		TLE tle = tleLoader.getTLE(category, name);
		Body earth = (Body) getObject("Earth");
		if(earth == null) {
			if(objects.isEmpty()) {
				earth = new Body();
			} else {
				earth = (Body) objects.get(0);
			}
		}
		Ship ship = new Ship(tle, earth);
		return ship;
	}
	
	public SimObject getObject(String name) {
		for(SimObject o : objects) {
			if(o.name.equals(name)){
				return o;
			}
		}
		return null;
	}

	public ArrayList<SimObject> getObjects(String filePath) {
		BufferedReader brBodies = null;

		String line = "";
		String cvsSplitBy = ",";

		int n = 0;
		try {
			InputStream is = new FileInputStream(filePath);
			brBodies = new BufferedReader(new InputStreamReader(is));
			while (((line = brBodies.readLine()) != null)) {
				String[] column = line.split(cvsSplitBy);
				if (n > 0) { // skip header row
					loadBody(column);
				}
				n++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (brBodies != null) {
				try {
					brBodies.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return objects;
	}

	private void loadBody(String[] column) {
		//int id = Integer.parseInt(column[0]);
		String name = column[1];
		double mass = Double.parseDouble(column[3]);
		double radius = Double.parseDouble(column[4]);
		double a = Double.parseDouble(column[5]);
		double e = Double.parseDouble(column[6]);
		double i = Double.parseDouble(column[7]);
		double node = Double.parseDouble(column[8]);
		double peri = Double.parseDouble(column[9]);
		double anom = Double.parseDouble(column[10]);
		double epoch = Double.parseDouble(column[11]);

		Body parent = null;
		int parentID = Integer.parseInt(column[2]);
		if (parentID != -1) {
			parent = (Body) objects.get(parentID);
		}

		Orbit orb = new Orbit(a, e, i, node, peri, anom);
		Body newBody = new Body(name, parent, mass, radius, orb, epoch);

		objects.add(newBody);
	}
}
