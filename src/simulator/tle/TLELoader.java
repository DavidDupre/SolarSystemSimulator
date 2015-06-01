package simulator.tle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import simulator.Simulation;

public class TLELoader {
	private HashMap<String, TLE> tles;
	private HashMap<String, HashMap<String, TLE>> loadedCategories;
	public String[] categories;

	public TLELoader() {
		tles = new HashMap<String, TLE>();
		loadedCategories = new HashMap<String, HashMap<String, TLE>>();
		categories = new String[] { "amateur", "argos", "beidou", "cubesat",
				"dmc", "education", "engineering", "galileo", "geo", "glo-ops",
				"globalstar", "goes", "gorizont", "gps-ops", "intelsat",
				"iridium", "military", "molniya", "musson", "nnss", "noaa",
				"orbcomm", "other", "other-comm", "radar", "raduga",
				"resource", "sarsat", "sbas", "science", "stations", "tdrss",
				"tle-new", "visual", "weather", "x-comm", "1999-025",
				"iridium-33-debris" };
	}

	private boolean isReal(String category) {
		for (int i = 0; i < categories.length; i++) {
			if (categories[i].equals(categories[i])) {
				return true;
			}
		}
		return false;
	}

	public TLE getTLE(String name) {
		if (tles.containsKey(name)) {
			return tles.get(name);
		} else {
			for (int i = 0; i < categories.length; i++) {
				if (!loadedCategories.containsKey(categories[i])) {
					load(categories[i]);
					if (tles.containsKey(name)) {
						return tles.get(name);
					}
				}
			}
			System.out.println(name + " not found!");
			return null;
		}
	}

	public TLE getTLE(String category, String name) {
		if (tles.containsKey(name)) {
			return tles.get(name);
		} else if (isReal(category)) {
			load(category);
			if (tles.containsKey(name)) {
				return tles.get(name);
			}
			System.out.println(name + " not found!");
			return null;
		}
		System.out.println(category + " not found!");
		return null;
	}

	public ArrayList<TLE> getCategory(String category) {
		if (isReal(category)) {
			if (!loadedCategories.containsKey(category)) {
				load(category);
			}
			return new ArrayList<TLE>(loadedCategories.get(category).values());
		} else {
			System.out.println(category + " not found");
			return new ArrayList<TLE>();
		}
	}

	/**
	 * Archive a category into </res/tle/category.txt>. Allows offline ussage.
	 * 
	 * @param category
	 */
	public void archiveCategory(String category) {
		try {
			/* Get URL */
			URL url = new URL("http://www.celestrak.com/NORAD/elements/"
					+ category + ".txt");

			/* Create new file */
			File jarFile = new File(getClass().getProtectionDomain()
					.getCodeSource().getLocation().toURI().getPath());
			String root = jarFile.getParent();
			String tlePath = (root + "/res/tle/" + category + ".txt");
			Path archivePath = new File(tlePath).toPath();

			/* Copy the file from celestrak */
			Files.copy(url.openStream(), archivePath,
					StandardCopyOption.REPLACE_EXISTING);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load(String category) {
		if (!loadedCategories.containsKey(category) && isReal(category)) {
			Scanner s;
			InputStream is = null;

			if (Simulation.USE_INTERNET) {
				try {
					URL url = new URL(
							"http://www.celestrak.com/NORAD/elements/"
									+ category + ".txt");
					is = url.openStream();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					File jarFile = new File(getClass().getProtectionDomain()
							.getCodeSource().getLocation().toURI().getPath());
					String root = jarFile.getParent();
					String tlePath = (root + "/res/tle/" + category + ".txt");
					is = new FileInputStream(tlePath);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			s = new Scanner(is);
			HashMap<String, TLE> newTles = new HashMap<String, TLE>();

			while (s.hasNextLine()) {
				for (int i = 0; i < 3; i++) {
					if (!s.hasNextLine()) {
						break;
					}
					String name = s.nextLine();
					name = name.trim();
					String line1 = s.nextLine();
					String line2 = s.nextLine();
					TLE newTLE = new TLE(name, line1, line2);
					tles.put(name, newTLE);
					newTles.put(name, newTLE);
				}
			}
			s.close();

			loadedCategories.put(category, newTles);
		}
	}
}
