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
	private HashMap<Integer, TLE> tles;
	private HashMap<String, HashMap<Integer, TLE>> loadedCategories;
	public static String[] categories;

	public TLELoader() {
		tles = new HashMap<Integer, TLE>();
		loadedCategories = new HashMap<String, HashMap<Integer, TLE>>();
		categories = new String[] { "amateur", "argos", "beidou", "cubesat",
				"dmc", "education", "engineering", "galileo", "geo", "glo-ops",
				"globalstar", "goes", "gorizont", "gps-ops", "intelsat",
				"iridium", "military", "molniya", "musson", "nnss", "noaa",
				"orbcomm", "other", "other-comm", "radar", "raduga",
				"resource", "sarsat", "sbas", "science", "stations", "tdrss",
				"tle-new", "visual", "weather", "x-comm", "1999-025",
				"iridium-33-debris" };
	}

	private boolean isCategory(String category) {
		for (int i = 0; i < categories.length; i++) {
			if (categories[i].equals(categories[i])) {
				return true;
			}
		}
		return false;
	}

	private TLE getTLE(String name) {
		return getTLE(name, false);
	}
	
	private TLE getTLE(String name, boolean tryhard) {
		for (TLE tle : tles.values()) {
			if (tle.name.equals(name)) {
				return tle;
			}
		}
		if(tryhard) {
			for (int i = 0; i < categories.length; i++) {
				if (!loadedCategories.containsKey(categories[i])) {
					load(categories[i]);
					for (TLE tle : loadedCategories.get(categories[i]).values()) {
						if (tle.name.equals(name)) {
							return tle;
						}
					}
				}
			}
		}
		return null;
	}

	public TLE getTLE(String category, String name) {
		TLE firstGuess = getTLE(name);
		if (firstGuess != null) {
			return firstGuess;
		} else if (isCategory(category)) {
			load(category);
			for (TLE tle : loadedCategories.get(category).values()) {
				if (tle.name.equals(name)) {
					return tle;
				}
			}
			System.out.println(name + " not found!");
			return null;
		}
		System.out.println(category + " not found!");
		return null;
	}

	public ArrayList<TLE> getCategory(String category) {
		if (isCategory(category)) {
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
	 * Archive a category into </res/tle/category.txt>. Allows offline usage.
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
		if (!loadedCategories.containsKey(category) && isCategory(category)) {
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
			HashMap<Integer, TLE> newTles = new HashMap<Integer, TLE>();

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
					tles.put(newTLE.id, newTLE);
					newTles.put(newTLE.id, newTLE);
				}
			}
			s.close();

			loadedCategories.put(category, newTles);
		}
	}
}
