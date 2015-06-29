package simulator.plans;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Utility class to work with scenario files
 * 
 * #enterprise af #reflect hard in the paint
 * 
 * @author David
 *
 */
public class ManeuverFactory {
	public HashMap<String, Class> classMap;

	public ManeuverFactory() {
		classMap = new HashMap<String, Class>();
		classMap.put("Incline", Incline.class);
		classMap.put("Hohmann", Hohmann.class);
		classMap.put("Wait", WaitCommand.class);
		classMap.put("Bielliptic", Bielliptic.class);
		classMap.put("OneTangent", OneTangent.class);
		classMap.put("Circularize", Circularize.class);
		classMap.put("Direct", Direct.class);
	}

	/**
	 * Creates a new maneuver using reflection. Meant to be used with scenario
	 * files
	 * 
	 * @param type
	 *            the simple name of the sub-class of Maneuver (ie Hohmann,
	 *            Incline, OneTangent)
	 * @param args
	 *            the parameters for the sub-class constructor
	 * @return new maneuver
	 */
	public Maneuver createNewManeuver(String type, HashMap<String, String> args) {
		Maneuver m = null;
		try {
			Class c = classMap.get(type);
			// TODO fix this next line. InvocationTargetException
			Constructor constructor = c
					.getConstructor(new HashMap<String, String>().getClass());
			m = (Maneuver) constructor.newInstance((Object) args);
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return m;
	}
}
