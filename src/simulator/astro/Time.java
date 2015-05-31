package simulator.astro;

public class Time {
	public static double getJulianDate(int yr, int mo, int d, int h, int min, double s) {
		double t1 = 367.0*yr;
		int t2 = -(int) ((7.0*(yr+((int) ((mo+9)/12.0)))) / 4.0);
		int t3 = (int) ((275*mo) / 9.0);
		int t4 = d;
		double t5 = 1721013.5;
		double t6 = ((((s/60.0) + min)/60.0)+h)/24.0;
		
		return t1 + t2 + t3 + t4 + t5 + t6;
	}
	
	public static double getJulianDate(long millis) {
		return (millis / 86400000.0) + 2440587.5;
	}
	
	public static double getJulianDate() {
		return getJulianDate(System.currentTimeMillis());
	}
	
	/**
	 * Convert Julian day to TAI time, in seconds
	 * @return
	 */
	public static double jdToTAI(double jd) {
		return (jd - 2440587.5) * 86400.0;
	}
}
