package simulator.simObject;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.util.glu.Sphere;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Time;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Body extends SimObject {
	/**
	 * The physical radius (meters)
	 */
	public double radius;

	/**
	 * The radius of the sphere of influence (meters)
	 */
	public double soiRadius;

	/**
	 * The gravitational parameter
	 */
	public double mu;
	public double mass;

	private Sphere sphere;

	protected ArrayList<SimObject> children;

	/**
	 * Root body (ie Sun) constructor
	 */
	public Body() {
		color = new float[] { 1.0f, 1.0f, 1.0f };
		lock = new ReentrantLock();
		sphere = new Sphere();
		radius = 1.0;
		mu = Astrophysics.G;
		mass = 1.0;
		pos = new VectorND(0,0,0);
		vel = new VectorND(0,0,0);
		name = "null";
		children = new ArrayList<SimObject>();
	}

	/**
	 * Primary constructor
	 * 
	 * @param name
	 * @param parent
	 * @param mass
	 * @param radius
	 * @param orb
	 * @param epoch
	 */
	public Body(String name, Body parent, double mass, double radius,
			Orbit orb, double epoch) {
		color = new float[] { 1.0f, 1.0f, 1.0f };
		lock = new ReentrantLock();
		sphere = new Sphere();
		this.name = name;
		children = new ArrayList<SimObject>();
		this.mass = mass;
		mu = mass * Astrophysics.G;
		this.radius = radius;
		this.orb = orb;
		if (parent != null) {
			setParent(parent);
			Vector[] state = Astrophysics.toRV(orb, parent.mu, true);
			pos = state[0];
			vel = state[1];
			soiRadius = orb.a * Math.pow(mass / parent.mass, 2.0 / 5.0);
		} else {
			pos = new VectorND(0,0,0);
			vel = new VectorND(0,0,0);
			soiRadius = Double.MAX_VALUE;
		}

		double now = System.currentTimeMillis() / 1000.0;
		if (Simulation.REAL_TIME) {
			lastUpdatedTime = Time.jdToTAI(epoch);
		} else {
			lastUpdatedTime = now;
		}
		updateTo(now);
	}
	
	public Body(String name, Body parent, double mass, double radius,
			Vector[] state, double epoch) {
		color = new float[] { 1.0f, 1.0f, 1.0f };
		lock = new ReentrantLock();
		sphere = new Sphere();
		this.name = name;
		children = new ArrayList<SimObject>();
		this.mass = mass;
		mu = mass * Astrophysics.G;
		this.radius = radius;
		pos = state[0];
		vel = state[1];
		if (parent != null) {
			setParent(parent);
			this.orb = Astrophysics.toOrbitalElements(state[0], state[1], parent.mu);
			soiRadius = orb.a * Math.pow(mass / parent.mass, 2.0 / 5.0);
		} else {
			soiRadius = Double.MAX_VALUE;
		}

		double now = System.currentTimeMillis() / 1000.0;
		if (Simulation.REAL_TIME) {
			lastUpdatedTime = Time.jdToTAI(epoch);
		} else {
			lastUpdatedTime = now;
		}
		updateTo(now);
	}

	public ArrayList<SimObject> getChildren() {
		return children;
	}

	@Override
	protected void renderPhysical() {
		// Draw physical sphere
		sphere.draw((float) radius, 16, 16);
	}
}
