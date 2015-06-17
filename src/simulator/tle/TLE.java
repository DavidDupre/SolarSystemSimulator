package simulator.tle;

import java.util.Calendar;
import java.util.GregorianCalendar;

import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Time;

public class TLE {
	public String name;
	private String line1;
	private String line2;
	public int id;

	public TLE(String name, String line1, String line2) {
		// TODO use String.trim on name?
		this.name = name;
		this.line1 = line1;
		this.line2 = line2;
		id = Integer.parseInt(line1.substring(2, 7));
	}

	/**
	 * @return the epoch as TAI time in seconds
	 */
	public double getEpoch() {
		// Calculate epoch
		int epochYear = Integer.parseInt("20" + line1.substring(18, 20));
		double epochDays = Double.parseDouble(line1.substring(20, 32));
		double hour = (epochDays - (int) (epochDays)) / 24.0;
		double min = (hour - (int) hour) / 60.0;
		double second = (min - (int) min) / 60.0;
		Calendar cal = new GregorianCalendar();
		cal.set(epochYear, 0, 0, 0, 0, 0);
		cal.add(Calendar.DAY_OF_YEAR, (int) epochDays);
		cal.add(Calendar.HOUR_OF_DAY, (int) hour);
		cal.add(Calendar.MINUTE, (int) min);
		cal.add(Calendar.SECOND, (int) second);

		double epochJD = Time.getJulianDate(cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE),
				cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), second);

		return epochJD;
	}

	public Orbit getOrbit() {
		// Store earth's gravitational parameter
		double mu = 3.986E14;

		// Get orbital elements
		double i = Double.parseDouble(line2.substring(9, 16));
		double node = Double.parseDouble(line2.substring(17, 25));
		double e = Double.parseDouble("." + line2.substring(27, 33));
		double peri = Double.parseDouble(line2.substring(34, 42));
		double meanAnom = Double.parseDouble(line2.substring(43, 51));
		double meanMotion = Double.parseDouble(line2.substring(52, 63));

		// Calculate true anomaly
		double a = Math.pow(
				mu
						/ Math.pow(
								meanMotion * 2.0 * Math.PI / (24.0 * 3600.0),
								2.0), 1.0 / 3.0);

		// Convert mean anomaly to true anomaly
		double E = Astrophysics.kepEqtnE(meanAnom, e);
		double v = Astrophysics.anomalyToV(e, E);

		Orbit orb = new Orbit(a, e, i, node, peri, v);
		orb.toRadians();

		return orb;
	}
}
