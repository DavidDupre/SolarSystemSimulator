package simulator.astro;

public class Astrophysics {
	public static/* The one and only D O */double G = 6.67384E-11;

	/**
	 * Returns the delta-v needed to circularize an orbit given position,
	 * velocity, and gravity of the primary
	 * 
	 * @param r
	 *            - position
	 * @param v
	 *            - velocity
	 * @param mu
	 *            - gravitational parameter of primary
	 * @return
	 */
	public static Vector3D dVToCircularize(double r, Vector3D v, double mu) {
		double vCircMag = Math.sqrt(mu / r);
		Vector3D vCircVector = v.clone().normalize().multiply(vCircMag);
		Vector3D deltaV = vCircVector.subtract(v);
		return deltaV;
	}

	/**
	 * Get semi-parameter, as found in textbook
	 * 
	 * @param a
	 *            semi-major axis
	 * @param e
	 *            eccentricity
	 * @return semi-parameter
	 */
	public static double getSemiParameter(double a, double e) {
		return a * (1 - e * e);
	}

	/**
	 * Convert orbital elements to position and velocity relative to the parent
	 * body
	 * 
	 * @param orb
	 * @param mu
	 * @param useDegrees
	 * @return index 0 = position; index 1 = velocity
	 */
	public static Vector3D[] toRV(Orbit orb, double mu, boolean useDegrees) {
		return toRV(orb.a, orb.e, orb.i, orb.node, orb.peri, orb.v, mu,
				useDegrees);
	}

	/**
	 * Convert orbital elements to position and velocity relative to the parent
	 * body
	 * 
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
	 * @param mu
	 *            - gravitational parameter of primary
	 * @param useDegrees
	 * @return index 0 = position; index 1 = velocity
	 */
	public static Vector3D[] toRV(double a, double e, double i, double node,
			double peri, double v, double mu, boolean useDegrees) {
		double p = getSemiParameter(a, e);

		if (useDegrees) {
			i = Math.toRadians(i); // convert angles to radians
			node = Math.toRadians(node);
			peri = Math.toRadians(peri);
			v = Math.toRadians(v);
		}

		// store trig variables to optimize
		double cosI = Math.cos(i);
		double sinI = Math.sin(i);
		double cosNode = Math.cos(node);
		double sinNode = Math.sin(node);
		double cosPeri = Math.cos(peri);
		double sinPeri = Math.sin(peri);
		double cosV = Math.cos(v);
		double sinV = Math.sin(v);

		Matrix rPQW = new Matrix(1, 3);
		rPQW.set(0, 0, (p * cosV) / (1 + e * cosV));
		rPQW.set(0, 1, (p * sinV) / (1 + e * cosV));
		rPQW.set(0, 2, 0);

		Matrix vPQW = new Matrix(1, 3);
		vPQW.set(0, 0, -(Math.sqrt(mu / p) * sinV));
		vPQW.set(0, 1, Math.sqrt(mu / p) * (e + cosV));
		vPQW.set(0, 2, 0);

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

		Matrix rIJK = Matrix.multiply(trans, rPQW);
		Matrix vIJK = Matrix.multiply(trans, vPQW);

		Vector3D[] state = new Vector3D[2];
		state[0] = new Vector3D(rIJK.get(0, 0), rIJK.get(1, 0), rIJK.get(2, 0));
		state[1] = new Vector3D(vIJK.get(0, 0), vIJK.get(1, 0), vIJK.get(2, 0));
		return state;
	}

	/**
	 * Find orbital elements given relative position and velocity. Angles in
	 * radians.
	 * 
	 * @param r
	 *            - position
	 * @param v
	 *            - velocity
	 * @param mu
	 *            - gravitational parameter of primary
	 * @return
	 */
	public static Orbit toOrbitalElements(Vector3D r, Vector3D v, double mu) {
		/* Prevent sneaky divide by zero errors */
		if (r.z == 0) {
			r.z = 1E-10;
		}

		double rMag = r.magnitude();
		double vMag = v.magnitude();
		double rDotV = Vector3D.dotProduct(r, v);
		double twoPi = 2 * Math.PI;

		// Find specific angular momentum
		Vector3D h = Vector3D.crossProduct(r, v);

		// Find the node vector
		Vector3D k = new Vector3D(0, 0, 1.0);
		Vector3D n = Vector3D.crossProduct(k, h);
		double nMag = n.magnitude(); // nMag being 0 screws thing up

		Vector3D e = ((r.clone().multiply(vMag * vMag - (mu / rMag)))
				.subtract(v.clone().multiply(rDotV))).multiply(1.0 / mu);
		double eMag = e.magnitude();

		double sguig = vMag * vMag / 2.0 - mu / rMag;

		// Not true if e = 1
		double a = -mu / (2.0 * sguig);
		double p = a * (1.0 - eMag * eMag);

		double i = Math.acos(h.z / h.magnitude());

		double node = Math.acos(n.x / nMag);
		if (n.y < 0) {
			node = twoPi - node;
		}

		double peri = Math.acos(Vector3D.dotProduct(n, e) / (nMag * eMag));
		if (e.z < 0) {
			peri = twoPi - peri;
		}

		double anomaly = Math.acos(Vector3D.dotProduct(e, r) / (eMag * rMag));
		if (rDotV < 0) {
			anomaly = twoPi - anomaly;
		}

		Orbit orb = new Orbit(p, a, eMag, i, node, peri, anomaly);

		return orb;
	}

	/**
	 * Partial form of toOrbitalElements. Used to optimize parent checking.
	 * 
	 * @param r
	 *            - relative position
	 * @param v
	 *            - relative velocity
	 * @param mu
	 *            - gravitational parameter of parent body
	 * @return eccentricity
	 */
	public static double getEccentricity(Vector3D r, Vector3D v, double mu) {
		double rMag = r.magnitude();
		double vMag = v.magnitude();
		double rDotV = Vector3D.dotProduct(r, v);

		Vector3D e = ((r.clone().multiply(vMag * vMag - (mu / rMag)))
				.subtract(v.clone().multiply(rDotV))).multiply(1 / mu);

		return e.magnitude();
	}

	/**
	 * Propagate the orbit. From page 101 of Fundamentals of Astrodynamics and
	 * Applications
	 * 
	 * @param r_0
	 *            Initial (relative) position
	 * @param v_0
	 *            Initial (relative) velocity
	 * @param deltaT
	 *            Time difference
	 * @param mu
	 *            Parent's gravitational parameter
	 * @return Future state vectors
	 */
	public static Vector3D[] kepler(Vector3D r_0, Vector3D v_0, double mu,
			double deltaT) {
		double v_0mag2 = v_0.mag2();
		double r_0mag = r_0.magnitude();

		double alpha = (-v_0mag2 / mu) + (2.0 / r_0mag);

		double rDotV = Vector3D.dotProduct(r_0, v_0);
		double rootGrav = Math.sqrt(mu);

		double x_0;
		double small = 0.000001;
		if (alpha > small) {
			// Circle or ellipse
			x_0 = rootGrav * deltaT * alpha;
		} else if (Math.abs(alpha) < small) {
			// Parabola
			Vector3D h = Vector3D.crossProduct(r_0, v_0);
			double p = h.mag2() / mu;
			double s = Math.atan2(1.0, 3*Math.sqrt(mu/(p*p*p))*deltaT)/2.0;
			double w = Math.atan(Math.pow(Math.tan(s),1.0/3.0));
			x_0 = (Math.sqrt(p)*2.0)/Math.tan(2.0*w);
		} else {
			// Hyperbola
			double a = 1.0 / alpha;
			x_0 = Math.signum(deltaT)
					* Math.sqrt(-a)
					* Math.log((-2.0 * mu * alpha * deltaT)
							/ (rDotV + Math.signum(deltaT)
									* Math.sqrt(-mu * a)
									* (1.0 - r_0mag * alpha)));
		}

		double c2 = 0, c3 = 0, r = 0, psi = 0;

		double x_n = x_0;
		double x_n1; // x_{n-1}
		double dif = 1.0; // temp
		int attempts = 0;
		while (dif > small && attempts < 10) {
			psi = x_n * x_n * alpha;

			double[] c2c3 = findC2C3(psi);
			c2 = c2c3[0];
			c3 = c2c3[1];

			r = x_n * x_n * c2 + (rDotV / rootGrav) * x_n * (1 - psi * c3)
					+ r_0mag * (1 - psi * c2);
			x_n1 = x_n;
			x_n = x_n
					+ (Math.sqrt(mu) * deltaT - x_n * x_n * x_n * c3
							- (rDotV / rootGrav) * x_n * x_n * c2 - r_0mag
							* x_n * (1.0 - psi * c3)) / r;
			dif = Math.abs(x_n - x_n1);
			attempts++;
		}

		double f = 1.0 - ((x_n * x_n) / r_0mag) * c2;
		double g = deltaT - ((x_n * x_n * x_n) / rootGrav) * c3;
		double gDot = 1.0 - ((x_n * x_n) / r) * c2;
		double fDot = (rootGrav / (r * r_0mag)) * x_n * (psi * c3 - 1.0);

		Vector3D rVec = r_0.clone().multiply(f).add(v_0.clone().multiply(g));
		Vector3D vVec = r_0.clone().multiply(fDot)
				.add(v_0.clone().multiply(gDot));

		return new Vector3D[] { rVec, vVec };
	}

	/**
	 * Mysterious helper method from page 71 of Fundamentals of Astrodynamics
	 * and Applications
	 * 
	 * @param psi
	 * @return c2, c3
	 */
	public static double[] findC2C3(double psi) {
		double c2 = 0, c3 = 0;
		if (psi > 1E-6) {
			double rootPsi = Math.sqrt(psi);
			c2 = (1.0 - Math.cos(rootPsi)) / psi;
			c3 = (rootPsi - Math.sin(rootPsi)) / Math.sqrt(psi * psi * psi);
		} else if (psi < -1E-6) {
			double rootNegPsi = Math.sqrt(-psi);
			c2 = (1.0 - Math.cosh(rootNegPsi)) / psi;
			c3 = (Math.sinh(rootNegPsi) - rootNegPsi)
					/ Math.sqrt(rootNegPsi * rootNegPsi * rootNegPsi);
		} else {
			c2 = .5;
			c3 = 1.0 / 6.0;
		}
		return new double[] { c2, c3 };
	}

	/**
	 * "Less annoying" but probably slower way of propagating the orbit. From
	 * page 89.
	 * 
	 * @param r_0
	 *            Initial (relative) position
	 * @param v_0
	 *            Initial (relative) velocity
	 * @param deltaT
	 *            Delta time
	 * @return State vectors
	 */
	public static Vector3D[] keplerCOE(Vector3D r_0, Vector3D v_0, double mass,
			double deltaT) {
		Orbit orb = toOrbitalElements(r_0, v_0, mass);

		double E_0 = 0;
		if (orb.e != 0) {
			E_0 = vToAnomaly(orb.e, orb.v);
		} else {
			E_0 = orb.v;
		}

		double M_0 = 0;
		double E;
		if (orb.e < 1.0) {
			M_0 = E_0 - orb.e * Math.sin(E_0);
			double M = M_0 + deltaT;
			E = kepEqtnE(M, orb.e);
		} else {
			M_0 = orb.e * Math.sinh(E_0) - E_0;
			double M = M_0 + deltaT;
			E = kepEqtnH(M, orb.e);
		}

		double v;
		if (orb.e != 0) {
			v = anomalyToV(orb.e, E);
		} else {
			v = E;
		}

		orb.v = v;

		return toRV(orb, mass, false);
	}

	/**
	 * Convert true anomaly to eccentric, parabolic, or hyperbolic anomaly
	 * 
	 * @param e
	 *            eccentricity
	 * @param v
	 *            true anomaly
	 * @return
	 */
	public static double vToAnomaly(double e, double v) {
		if (e < 1.0) {
			return Math.acos((e + Math.cos(v)) / (1.0 + e * Math.cos(v)));
		} else if (e == 1.0) {
			return Math.tan(v / 2.0);
		} else {
			double x = (Math.sin(v) * Math.sqrt(e * e - 1.0))
					/ (1.0 + e * Math.cos(v));
			return Math.log(x + Math.sqrt(x * x + 1.0)); // same as asinh(x)
		}
	}

	/**
	 * Convert eccentric or hyperbolic anomaly to true anomaly
	 * 
	 * @param e
	 *            eccentricity
	 * @param E
	 *            eccentric (or hyperbolic) anomaly
	 * @return
	 */
	public static double anomalyToV(double e, double E) {
		if (e < 1.0) {
			return Math.asin((Math.sin(E) * Math.sqrt(1.0 - e * e)))
					/ (1.0 - e * Math.cos(E));
		} else {
			return Math.acos((Math.cosh(E) - e)) / (1.0 - e * Math.cosh(E));
		}
	}

	/**
	 * Convert mean anomaly and eccentricity to eccentric anomaly. Page 73.
	 * 
	 * @return
	 */
	public static double kepEqtnE(double M, double e) {
		double E;
		if ((M > -Math.PI && M < 0) || (M > Math.PI)) {
			E = M - e;
		} else {
			E = M + e;
		}

		double E_n = E;
		double E_n1;
		double dif = 1.0;
		while (dif > 1E-8) {
			E_n1 = E_n;
			E_n += (M - E_n + e * Math.sin(E_n)) / (1.0 - e * Math.cos(E_n));
			dif = Math.abs(E_n - E_n1);
		}

		return E_n;
	}

	/**
	 * Convert mean anomaly and eccentricity to hyperbolic anomaly. Page 79.
	 * 
	 * @return
	 */
	public static double kepEqtnH(double M, double e) {
		double H;
		if (e < 1.6) {
			if ((M > -Math.PI && M < 0) || M > Math.PI) {
				H = M - e;
			} else {
				H = M + e;
			}
		} else {
			if (e < 3.6 && Math.abs(M) > Math.PI) {
				H = M - Math.signum(M) * e;
			} else {
				H = M / (e - 1.0);
			}
		}

		double H_n = H;
		double H_n1;
		double dif = 1.0;
		while (dif > 1E-8) {
			H_n1 = H_n;
			H_n += (M - H_n + e * Math.sinh(H_n)) / (e * Math.cosh(H_n) - 1.0);
			dif = Math.abs(H_n - H_n1);
		}

		return H_n;
	}
}
