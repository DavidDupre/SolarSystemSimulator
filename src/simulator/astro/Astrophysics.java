package simulator.astro;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

import simulator.simObject.SimObject;

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
	public static Vector dVToCircularize(double r, Vector v, double mu) {
		double vCircMag = Math.sqrt(mu / r);
		Vector vCircVector = ((VectorND) v).clone().normalize()
				.multiply(vCircMag);
		Vector deltaV = vCircVector.subtract(v);
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
	public static Vector[] toRV(Orbit orb, double mu, boolean useDegrees) {
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
	public static Vector[] toRV(double a, double e, double i, double node,
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

		Vector[] state = new Vector[2];
		state[0] = new VectorND(rIJK.get(0, 0), rIJK.get(1, 0), rIJK.get(2, 0));
		state[1] = new VectorND(vIJK.get(0, 0), vIJK.get(1, 0), vIJK.get(2, 0));
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
	public static Orbit toOrbitalElements(Vector r, Vector v, double mu) {
		/* Prevent sneaky divide by zero errors */
		if (r.get(2) == 0) {
			r.set(2, 1E-10);
		}

		double rMag = r.magnitude();
		double vMag = v.magnitude();
		double rDotV = Vector.dotProduct(r, v);
		double twoPi = 2 * Math.PI;

		// Find specific angular momentum
		Vector h = new VectorND(0, 0, 0);
		h = Vector.crossProduct(h, r, v);

		// Find the node vector
		Vector k = new VectorND(0, 0, 1.0);
		Vector n = new VectorND(0, 0, 0);
		n = Vector.crossProduct(n, k, h);
		double nMag = n.magnitude(); // nMag being 0 screws thing up

		Vector e = ((((VectorND) r).clone().multiply(vMag * vMag - (mu / rMag)))
				.subtract(((VectorND) v).clone().multiply(rDotV)))
				.multiply(1.0 / mu);
		double eMag = e.magnitude();

		double sguig = vMag * vMag / 2.0 - mu / rMag;

		// Not true if e = 1
		double a = -mu / (2.0 * sguig);
		double p = a * (1.0 - eMag * eMag);

		double i = Math.acos(h.get(2) / h.magnitude());

		double node = Math.acos(n.get(0) / nMag);
		if (n.get(1) < 0) {
			node = twoPi - node;
		}

		double peri = Math.acos(Vector.dotProduct(n, e) / (nMag * eMag));
		if (e.get(2) < 0) {
			peri = twoPi - peri;
		}

		double anomaly = Math.acos(Vector.dotProduct(e, r) / (eMag * rMag));
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
	public static double getEccentricity(Vector r, Vector v, double mu) {
		double rMag = r.magnitude();
		double vMag = v.magnitude();
		double rDotV = Vector.dotProduct(r, v);

		Vector e = ((((VectorND) r).clone().multiply(vMag * vMag - (mu / rMag)))
				.subtract(((VectorND) v).clone().multiply(rDotV)))
				.multiply(1 / mu);

		return e.magnitude();
	}

	/**
	 * Propagate the orbit. From page 101 of Fundamentals of Astrodynamics and
	 * Applications
	 * 
	 * TODO check up on this method
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
	public static Vector[] kepler(Vector r_0, Vector v_0, double mu,
			double deltaT) {
		double v_0mag2 = v_0.mag2();
		double r_0mag = r_0.magnitude();

		double alpha = (-v_0mag2 / mu) + (2.0 / r_0mag);

		double rDotV = Vector.dotProduct(r_0, v_0);
		double rootGrav = Math.sqrt(mu);

		double x_0;
		double parabThresh = 0.000001;
		if (alpha > parabThresh) {
			// Circle or ellipse
			x_0 = rootGrav * deltaT * alpha;
		} else if (Math.abs(alpha) < parabThresh) {
			// Parabola
			Vector h = new VectorND(0, 0, 0);
			h = Vector.crossProduct(h, r_0, v_0);
			double p = h.mag2() / mu;
			double s = Math
					.atan2(1.0, 3 * Math.sqrt(mu / (p * p * p)) * deltaT) / 2.0;
			double w = Math.atan(Math.pow(Math.tan(s), 1.0 / 3.0));
			x_0 = (Math.sqrt(p) * 2.0) / Math.tan(2.0 * w);
		} else {
			// Hyperbola
			double a = 1.0 / alpha;
			x_0 = Math.signum(deltaT)
					* Math.sqrt(-a)
					* Math.log((-2.0 * mu * alpha * deltaT)
							/ (rDotV + Math.signum(deltaT) * Math.sqrt(-mu * a)
									* (1.0 - r_0mag * alpha)));
		}

		double c2 = 0, c3 = 0, r = 0, psi = 0;

		double x_n = x_0;
		double x_n1; // x_{n-1}
		double dif = 1.0; // temp
		int attempts = 0;
		// attempts max is 10 in the book. Increase for more stability
		// threshold is 1E-6 in book
		while (dif > 1E-6 && attempts < 100) {
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

		Vector rVec = ((VectorND) r_0).clone().multiply(f)
				.add(((VectorND) v_0).clone().multiply(g));
		Vector vVec = ((VectorND) r_0).clone().multiply(fDot)
				.add(((VectorND) v_0).clone().multiply(gDot));

		return new Vector[] { rVec, vVec };
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
	public static Vector[] keplerCOE(Vector r_0, Vector v_0, double mass,
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

		double v = 0;
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
			double E = Math.acos( (e + Math.cos(v)) / (1.0 + e * Math.cos(v)) );
			if(v > Math.PI) {
				E = 2.0 * Math.PI - E;
			}
			return E;
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
			double v = Math.acos( (Math.cos(E) - e) / (1.0 - e * Math.cos(E)) );
			if(E > Math.PI) {
				v = -v;
			}
			return v;
		} else {
			return Math.acos( (Math.cosh(E) - e) / (1.0 - e * Math.cosh(E)) );
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

	/**
	 * Calculate the time to the next time the body with pass through the
	 * specified true anomaly
	 * 
	 * 
	 * @param orb
	 *            - the current orbit
	 * @param mu
	 *            - the gravitational constant for the parent body
	 * @param targetV
	 *            - target true anomaly, in radians
	 * @return the time, in seconds, to that true anomaly
	 */
	public static double timeToAnomaly(Vector pos, Vector vel, Orbit orb,
			double mu, double targetV) {
		double initSum = orb.peri + orb.v;

		/*
		 * Perform binary search to find time to next node
		 */
		double period = 2.0 * Math.PI * Math.sqrt((orb.a * orb.a * orb.a) / mu);
		double low = 0;
		double high = period;
		double mid = (low + high) / 2.0;
		double tolerance = Math.toRadians(.001);
		double anomDif = tolerance * 10.0;
		int iterations = 0;
		while (Math.abs(anomDif) > tolerance && iterations < 20) {
			Vector[] futureState = Astrophysics.kepler(pos, vel, mu, mid);
			Orbit futureOrb = Astrophysics.toOrbitalElements(futureState[0],
					futureState[1], mu);
			double sum = futureOrb.peri + futureOrb.v;
			anomDif = futureOrb.v - targetV;
			if (anomDif > 0 || sum < initSum) {
				high = mid;
			} else {
				low = mid;
			}
			mid = (low + high) / 2.0;
			iterations++;
		}
		double timeToNode = mid;
		return timeToNode;
	}

	/*------------------------------------------------------------------------------
	 *
	 *                           procedure lambertuniv
	 *
	 *  this procedure solves the lambert problem for orbit determination and returns
	 *    the velocity vectors at each of two given position vectors.  the solution
	 *    uses universal variables for calculation and a bissection technique for
	 *    updating psi.
	 *
	 *  algorithm     : setting the initial bounds:
	 *                  using -8pi and 4pi2 will allow single rev solutions
	 *                  using -4pi2 and 8pi2 will allow multi-rev solutions
	 *                  the farther apart the initial guess, the more iterations
	 *                    because of the iteration
	 *                  inner loop is for special cases. must be sure to exit both!
	 *
	 *  author        : david vallado                  719-573-2600   22 jun 2002
	 *
	 *  inputs          description                    range / units
	 *    r1          - ijk position vector 1          er
	 *    r2          - ijk position vector 2          er
	 *    dm          - direction of motion            'l','s'
	 *    dttu        - time between r1 and r2         tu
	 *
	 *  outputs       :
	 *    v1          - ijk velocity vector            er / tu
	 *    v2          - ijk velocity vector            er / tu
	 *    error       - error flag                     'ok', ...
	 *
	 *  locals        :
	 *    vara        - variable of the iteration,
	 *                  not the semi or axis!
	 *    y           - area between position vectors
	 *    upper       - upper bound for z
	 *    lower       - lower bound for z
	 *    cosdeltanu  - cosine of true anomaly change  rad
	 *    f           - f expression
	 *    g           - g expression
	 *    gdot        - g dot expression
	 *    xold        - old universal variable x
	 *    xoldcubed   - xold cubed
	 *    zold        - old value of z
	 *    znew        - new value of z
	 *    c2new       - c2(z) function
	 *    c3new       - c3(z) function
	 *    timenew     - new time                       tu
	 *    small       - tolerance for roundoff errors
	 *    i, j        - index
	 *
	 *  coupling      
	 *    mag         - magnitude of a vector
	 *    dot         - dot product of two vectors
	 *    findc2c3    - find c2 and c3 functions
	 *
	 *  references    :
	 *    vallado       2001, 459-464, alg 55, ex 7-5
	 *
	 -----------------------------------------------------------------------------*/

	public static Vector[] lambert(Vector ro, Vector r, boolean useLongWay,
			boolean overrev, double dttu, double mu) {
		double twopi = 2.0 * Math.PI;
		double small = 1E-6;
		int numiter = 40;

		int loops, ynegktr;
		double vara, y, upper, lower, cosdeltanu, f, g, gdot, xold, xoldcubed, psiold, psinew, c2new, c3new, dtnew = 0;
		double rMag = r.magnitude();
		double roMag = ro.magnitude();

		/* -------------------- initialize values -------------------- */
		psinew = 0.0;

		cosdeltanu = Vector.dotProduct(ro, r) / (ro.magnitude() * rMag);
		if (useLongWay) {
			vara = -Math.sqrt(roMag * rMag * (1.0 + cosdeltanu));
		} else {
			vara = Math.sqrt(roMag * rMag * (1.0 + cosdeltanu));
		}

		/* ---------------- form initial guesses --------------------- */
		psiold = 0.0;
		psinew = 0.0;
		xold = 0.0;
		c2new = 0.5;
		c3new = 1.0 / 6.0;

		/* -------- set up initial bounds for the bissection ------------ */
		if (overrev) {
			upper = twopi * twopi;
			lower = -4.0 * twopi;
		} else {
			upper = -0.001 + 4.0 * twopi * twopi; // at 4, not alw work, 2.0,
													// makes
			lower = 0.001 + twopi * twopi; // orbit bigger, how about 2 revs??xx
		}

		/* -------- determine if the orbit is possible at all ---------- */
		if (Math.abs(vara) > small) {
			loops = 0;
			ynegktr = 1; // y neg ktr
			while (true) {
				if (Math.abs(c2new) > small) {
					y = roMag
							+ rMag
							- (vara * (1.0 - psiold * c3new) / Math.sqrt(c2new));
				} else {
					y = roMag + rMag;
				}
				/* ------- check for negative values of y ------- */
				if ((vara > 0.0) && (y < 0.0)) {
					ynegktr = 1;
					while (true) {
						psinew = 0.8
								* (1.0 / c3new)
								* (1.0 - (roMag + rMag) * Math.sqrt(c2new)
										/ vara);

						/* ------ find c2 and c3 functions ------ */
						double[] c2c3 = findC2C3(psinew);
						c2new = c2c3[0];
						c3new = c2c3[1];
						psiold = psinew;
						lower = psiold;
						if (Math.abs(c2new) > small) {
							y = roMag
									+ rMag
									- (vara * (1.0 - psiold * c3new) / Math
											.sqrt(c2new));
						} else {
							y = roMag + rMag;
						}
						ynegktr++;
						if ((y >= 0.0) || (ynegktr >= 10)) {
							break;
						}
					}
				}

				if (ynegktr < 10) {
					if (Math.abs(c2new) > small) {
						xold = Math.sqrt(y / c2new);
					} else {
						xold = 0.0;
					}
					xoldcubed = xold * xold * xold;
					dtnew = (xoldcubed * c3new + vara * Math.sqrt(y))
							/ Math.sqrt(mu);

					/* ---- readjust upper and lower bounds ---- */
					if (dtnew < dttu) {
						lower = psiold;
					}
					if (dtnew > dttu) {
						upper = psiold;
					}
					psinew = (upper + lower) * 0.5;

					/* -------------- find c2 and c3 functions ---------- */
					double[] c2c3 = findC2C3(psinew);
					c2new = c2c3[0];
					c3new = c2c3[1];
					psiold = psinew;
					loops++;

					/* ---- make sure the first guess isn't too close --- */
					if ((Math.abs(dtnew - dttu) < small) && (loops == 1)) {
						dtnew = dttu - 1.0;
					}
				}

				if ((Math.abs(dtnew - dttu) < small) || (loops > numiter)
						|| (ynegktr > 10)) {
					break;
				}
			}

			if ((loops >= numiter) || (ynegktr >= 10)) {
				// System.out.println("loops: " + loops);
				// System.out.println("gnotconv");
				// if (ynegktr >= 10) {
				// System.out.println("y negative");
				// }
				// Exception e = new Exception();
				// e.printStackTrace();
			} else {
				/* ---- use f and g series to find velocity vectors ----- */
				f = 1.0 - y / roMag;
				gdot = 1.0 - y / rMag;
				g = 1.0 / (vara * Math.sqrt(y / mu)); // 1 over g
				Vector v_0 = ((VectorND) r).clone()
						.subtract(((VectorND) ro).clone().multiply(f))
						.multiply(g);
				Vector v = ((VectorND) r).clone().multiply(gdot).subtract(ro)
						.multiply(g);

				return new Vector[] { v_0, v };
			}
		} else {
			System.out.println("impos 180�");
		}
		return null;
	}

	/**
	 * Determine if there is a collision
	 * 
	 * @param r_int
	 *            the position of the intercepter
	 * @param r_tgt
	 *            the position of the target
	 * @param v_transA
	 *            the velocity along the transfer orbit at the initial time
	 * @param v_transB
	 *            the velocity along the transfer orbit at the time of intercept
	 * @param a
	 *            the semi-major axis of the transfer orbit
	 * @param mu
	 *            the gravitational constant for the system
	 * @return the radius of periapsis. There is a collision if r_p < r_earth.
	 *         Note that a low r_p means the body is subject to aerodynamic
	 *         perturbaterinos, which are not modeled with "kepler()"
	 */
	public static double hitEarth(Vector r_int, Vector r_tgt, Vector v_transA,
			Vector v_transB, double a, double mu) {
		// if(Vector3D.dotProduct(r_int, v_transA)<0 &&
		// Vector3D.dotProduct(r_tgt, v_transB)>0) {
		Vector h = new VectorND(0, 0, 0);
		double h_t = Vector.crossProduct(h, r_int, v_transB).magnitude();
		double p = (h_t * h_t) / mu;
		double e = Math.sqrt((a - p) / a);
		double r_p = a * (1 - e);
		// }
		return r_p;
	}

	/**
	 * @param r_int
	 *            position of intercepter
	 * @param r_tgt
	 *            position of target
	 * @param v_int
	 *            velocity of intercepter
	 * @param v_tgt
	 *            velocity of target
	 * @param deltaT
	 *            the time the transfer should take, in seconds
	 * @param mu
	 *            gravitational constant of the primary body
	 * @return the delta-v vectors for two burns, one to be completed
	 *         immediately and the other after time deltaT
	 */
	public static Vector[] target(Vector r_int, Vector r_tgt, Vector v_int,
			Vector v_tgt, double deltaT, double mu, boolean useLongWay) {
		// Propagate target
		Vector[] state = kepler(r_tgt, v_tgt, mu, deltaT);
		Vector r_tgtB = state[0];
		Vector v_tgtB = state[1];

		// Determine tranfer orbit
		Vector[] trans = lambert(r_int, r_tgtB, useLongWay, true, deltaT, mu);
		if (trans == null) {
			return null;
		}
		Vector v_transA = trans[0];
		Vector v_transB = trans[1];

		Vector deltaVA = ((VectorND) v_transA).clone().subtract(v_int);
		Vector deltaVB = ((VectorND) v_tgtB).clone().subtract(v_transB);

		// Check for Earth impact TODO

		return new Vector[] { deltaVA, deltaVB };
	}

	public static Vector[] target(SimObject inter, SimObject target,
			double deltaT, boolean useLongWay) {
		return target(inter.pos, target.pos, inter.vel, target.vel, deltaT,
				inter.parent.mu, useLongWay);
	}

	public static Vector[] target(Vector[] state_int, Vector[] state_tgt,
			double mu, double deltaT, boolean useLongWay) {
		return target(state_int[0], state_tgt[0], state_int[1], state_tgt[1],
				deltaT, mu, useLongWay);
	}

	public static double timeToEscape(Vector pos, Vector vel, double mu,
			double soiRadius) {
		return timeToEscape(pos, vel, mu, soiRadius, false);
	}

	/**
	 * TODO doesn't work through perigee
	 * 
	 * @param pos
	 * @param vel
	 * @param mu
	 * @param soiRadius
	 * @param reverse
	 * @return
	 */
	public static double timeToEscape(Vector pos, Vector vel, double mu,
			double soiRadius, boolean reverse) {
		Orbit orb = Astrophysics.toOrbitalElements(pos, vel, mu);
		orb.v = Math.PI; // TODO not sure if this works for hyperbolas
		double apoVel = Astrophysics.toRV(orb, mu, false)[1].magnitude();
		// max time to escape
		double high = (soiRadius / apoVel) * (reverse ? -1.0 : 1.0);
		double low = 0;
		double mid = (low + high) / 2.0;

		double dif = soiRadius - pos.magnitude();
		double tolerance = soiRadius / 1E9;
		int iterations = 0;
		while (Math.abs(dif) > tolerance && iterations < 100) {
			Vector[] futureState = Astrophysics.kepler(pos, vel, mu, mid);
			double r = futureState[0].magnitude();
			// System.out.println("r: " + r);

			dif = r - soiRadius;

			if (r > soiRadius) {
				high = mid;
			} else {
				low = mid;
			}
			mid = (low + high) / 2.0;

			iterations++;

		}
		// System.out.println();

		return mid;
	}

	/**
	 * Use a funky logarithmic search to brute force the time to escape. Don't
	 * use if the orbit isn't hyperbolic. I'm not checking that again.
	 */
	public static double shittyTimeToEscape(Vector pos, Vector vel, double mu,
			double soiRadius) {
		double t = 1.0;
		double r = 0;
		while (r < soiRadius) {
			t *= 10.0;
			r = kepler(pos, vel, mu, t)[0].magnitude();
		}
		double min = t / 10.0;
		double max = t;
		int samples = 100;
		for (int i = 0; i < samples; i++) {
			t = min + max * i / (double) samples;
			r = kepler(pos, vel, mu, t)[0].magnitude();
			if (r > soiRadius) {
				return t;
			}
		}
		return max;
	}

	public static double anomalyToEscape(Vector pos, Vector vel, double mu,
			double soiRadius) {
		Orbit orb = Astrophysics.toOrbitalElements(pos, vel, mu);
		orb.v = Math.PI; // TODO not sure if this works for hyperbolas
		double apoVel = Astrophysics.toRV(orb, mu, false)[1].magnitude();
		// max time to escape
		double high = (soiRadius / apoVel);
		double low = 0;
		double mid = (low + high) / 2.0;

		Vector futurePos = new VectorND(0, 0, 0);
		Vector futureVel = new VectorND(0, 0, 0);
		double dif = soiRadius - pos.magnitude();
		double tolerance = soiRadius / 1E9;
		int iterations = 0;
		while (Math.abs(dif) > tolerance && iterations < 100) {
			Vector[] futureState = Astrophysics.kepler(pos, vel, mu, mid);
			futurePos = futureState[0];
			futureVel = futureState[1];

			double r = futurePos.magnitude();
			dif = r - soiRadius;

			if (r > soiRadius) {
				high = mid;
			} else {
				low = mid;
			}
			mid = (low + high) / 2.0;

			iterations++;
		}
		Orbit futureOrb = toOrbitalElements(futurePos, futureVel, mu);

		return futureOrb.v;
	}

}
