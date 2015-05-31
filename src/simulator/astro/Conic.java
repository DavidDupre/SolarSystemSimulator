package simulator.astro;


/**
 * For optimized conic section illustration
 * 
 * @author s-2482153
 *
 */
public class Conic {
	Orbit orb;
	Matrix trans;

	/**
	 * Starts getting the position of the body, but waits on true anomaly
	 * 
	 * @param orb
	 */
	public Conic(Orbit orb) {
		this.orb = orb;

		double i = orb.i;
		double node = orb.node;
		double peri = orb.peri;

		double cosI = Math.cos(i);
		double sinI = Math.sin(i);
		double cosNode = Math.cos(node);
		double sinNode = Math.sin(node);
		double cosPeri = Math.cos(peri);
		double sinPeri = Math.sin(peri);

		Matrix trans = new Matrix(3, 3);
		trans.set(0, 0, cosNode * cosPeri - sinNode * sinPeri * cosI);
		trans.set(0, 1, sinNode * cosPeri + cosNode * sinPeri * cosI);
		trans.set(0, 2, sinPeri * sinI);
		trans.set(1, 0, -cosNode * sinPeri - sinNode * cosPeri * cosI);
		trans.set(1, 1, -sinNode * sinPeri + cosNode * cosPeri * cosI);
		trans.set(1, 2, cosPeri * sinI);
		trans.set(2, 0, sinNode * sinI);
		trans.set(2, 1, -cosNode * sinI);
		trans.set(2, 2, cosI);

		this.trans = trans;
	}

	/**
	 * Finishes position computation with a variable mean anomaly
	 * 
	 * @param v
	 *            - mean anomaly
	 * @return position vector
	 */
	public Vector3D getPosition(double v) {
		double p = orb.p;
		double e = orb.e;

		double cosV = Math.cos(v);
		double sinV = Math.sin(v);

		Matrix rPQW = new Matrix(1, 3);
		rPQW.set(0, 0, (p * cosV) / (1 + e * cosV));
		rPQW.set(0, 1, (p * sinV) / (1 + e * cosV));

		Matrix rIJK = Matrix.multiply(trans, rPQW);
		return new Vector3D(rIJK.get(0, 0), rIJK.get(1, 0), rIJK.get(2, 0));
	}
}
