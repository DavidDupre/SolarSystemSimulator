package simulator.astro;

public class Orbit {
	/**
	 * Parameter
	 */
	public double p;

	/**
	 * Semi-major axis
	 */
	public double a;

	/**
	 * Eccentricity
	 */
	public double e;

	/**
	 * Inclination
	 */
	public double i;

	/**
	 * Longitude of ascending node
	 */
	public double node;

	/**
	 * Argument of periapsis
	 */
	public double peri;

	/**
	 * Mean anomaly
	 */
	public double v;

	/**
	 * Create a set of orbital elements
	 * 
	 * @param p
	 *            - parameter
	 * @param a
	 *            - semi-major axis
	 * @param e
	 *            - eccentricity
	 * @param i
	 *            - inclination
	 * @param node
	 *            - longitude of ascending node
	 * @param peri
	 *            - argument of periapsis
	 * @param v
	 *            - true anomaly
	 */
	public Orbit(double p, double a, double e, double i, double node,
			double peri, double v) {
		this.p = p;
		this.a = a;
		this.e = e;
		this.i = i;
		this.node = node;
		this.peri = peri;
		this.v = v;
	}
	
	public Orbit(double a, double e, double i, double node,
			double peri, double v) {
		this.p = Astrophysics.getSemiParameter(a, e);
		this.a = a;
		this.e = e;
		this.i = i;
		this.node = node;
		this.peri = peri;
		this.v = v;
	}

	/**
	 * Create a set of orbital elements for a basic orbit where all elements
	 * other than the semi-major axis are zero
	 * 
	 * @param a
	 *            - semi-major axis
	 */
	public Orbit(double a) {
		this(a, a, 0, 0, 0, 0, 0);
	}
	
	public void toRadians() {
		i = Math.toRadians(i);
		node = Math.toRadians(node);
		peri = Math.toRadians(peri);
		v = Math.toRadians(v);
	}
}
