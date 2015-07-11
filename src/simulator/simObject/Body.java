package simulator.simObject;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL15.*;
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

	public Body() {
		super();
		color = new float[] { 1.0f, 1.0f, 1.0f };
		sphere = new Sphere(radius);
		radius = 1.0;
		mu = Astrophysics.G;
		mass = 1.0;
		pos = new VectorND(0, 0, 0);
		vel = new VectorND(0, 0, 0);
		name = "null";
		children = new ArrayList<SimObject>();
	}

	public Body(String name, Body parent, double mass, double radius,
			Orbit orb, double epoch) {
		this(name, parent, mass, radius, parent == null ? new VectorND[] {
				new VectorND(0, 0, 0), new VectorND(0, 0, 0) } : Astrophysics
				.toRV(orb, parent.mu, true), epoch);
	}

	public Body(String name, Body parent, double mass, double radius,
			Vector[] state, double epoch) {
		super();
		color = new float[] { 1.0f, 1.0f, 1.0f };
		lock = new ReentrantLock();
		sphere = new Sphere(radius);
		this.name = name;
		children = new ArrayList<SimObject>();
		this.mass = mass;
		mu = mass * Astrophysics.G;
		this.radius = radius;
		pos = state[0];
		vel = state[1];
		if (parent != null) {
			setParent(parent);
			this.orb = Astrophysics.toOrbitalElements(state[0], state[1],
					parent.mu);
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
	
	@Override
	public void initGL() {
		vHandle = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vHandle);
		glBufferData(GL_ARRAY_BUFFER, orbitBuffer,
				GL_STATIC_DRAW);
		sphere.initGL();
	}
	
	@Override
	public void dispose() {
		glDeleteBuffers(vHandle);
		sphere.dispose();
	}

	public ArrayList<SimObject> getChildren() {
		return children;
	}

	@Override
	protected void renderPhysical() {
		sphere.draw();
	}
}
