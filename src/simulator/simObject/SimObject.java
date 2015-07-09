package simulator.simObject;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

import simulator.astro.Astrophysics;
import simulator.astro.Conic;
import simulator.astro.Orbit;

public abstract class SimObject {
	public Body parent;
	public Vector pos;
	public Vector vel;
	public String name;
	protected Orbit orb;
	public double lastUpdatedTime;
	protected Vector[] orbitBuffer = new Vector[50];
	protected float[] color;

	/**
	 * Each object needs a lock for rendering. This is to prevent stutters while
	 * focusing on the object or while drawing its orbit
	 */
	public ReentrantLock lock;

	public RenderDetail renderDetail = RenderDetail.MAX;

	public enum RenderDetail {
		LOW, MAX
	}

	/**
	 * Render using the last render detail level used, or its default render
	 * level if no level is specified
	 */
	public void render() {
		render(renderDetail);
	}

	public void render(RenderDetail detail) {
		renderDetail = detail;

		glPushMatrix();

		// Position to body
		Vector absPos = getAbsolutePos();
		glTranslated(absPos.get(0), absPos.get(1), absPos.get(2));

		// Set object-specific color
		glColor3f(color[0], color[1], color[2]);
		
		// Draw conic
		if (detail.ordinal() > RenderDetail.LOW.ordinal()) {
			lock.lock();
			if (parent != null) {
				glBegin(GL_LINE_STRIP);
				for (int i = 0; i < orbitBuffer.length; i++) {
					glVertex3d(orbitBuffer[i].get(0), orbitBuffer[i].get(1),
							orbitBuffer[i].get(2));
				}
				glEnd();
			}
			lock.unlock();
		}

		// Draw point
		glBegin(GL_POINTS);
		glVertex3d(0, 0, 0);
		glEnd();

		renderPhysical();

		glPopMatrix();
		
		// Reset color to default
		glColor3f(1.0f, 1.0f, 1.0f);
	}

	/**
	 * For class-specific rendering, called during the render loop. Meant to be
	 * overridden
	 */
	protected void renderPhysical() {

	}

	protected void setParent(Body b) {
		if(b != null) {
			b.lock.lock();
			if (parent != null) {
				Vector absPos = getAbsolutePos();
				Vector absVel = getAbsoluteVel();
				parent.getChildren().remove(this);
				pos = absPos.subtract(b.getAbsolutePos());
				vel = absVel.subtract(b.getAbsoluteVel());
			}
			b.getChildren().add(this);
			parent = b;
			b.lock.unlock();
		}
	}

	public ArrayList<SimObject> getChildren() {
		return new ArrayList<SimObject>();
	}

	protected void update(double delta) {
		if (parent != null) {
			lock.lock(); // TODO does this ruin epoch synchronization? 
			Vector[] state = Astrophysics.kepler(pos, vel, parent.mu, delta);
			pos = state[0];
			vel = state[1];

			/*
			 * Buffer the orbit for the render thread
			 */
			if (renderDetail.ordinal() > RenderDetail.LOW.ordinal()) {
				Orbit orb = Astrophysics.toOrbitalElements(pos, vel, parent.mu);
				Conic c = new Conic(orb);
				if(orb.e < 1.0) {
					for (int i = 0; i < orbitBuffer.length; i++) {
						Vector vertex = c.getPosition(i * 2.0 * Math.PI
								/ (orbitBuffer.length - 1) + orb.v);
						vertex.subtract(pos);
						orbitBuffer[i] = vertex;
					}
				} else {
					// TODO this can be vastly optimized. Only update escapeV when the ship's orbit changes
					
					double timeToEscape = Astrophysics.timeToEscape(pos, vel, parent.mu, parent.soiRadius, false);
					double timeStep = timeToEscape / orbitBuffer.length;
					for (int i=0; i<orbitBuffer.length; i++) {
						Vector vertex = Astrophysics.kepler(pos, vel, parent.mu, timeStep*i)[0];
						vertex.subtract(pos);
						orbitBuffer[i] = vertex;
					}
					
//					double escapeV = Astrophysics.anomalyToEscape(pos, vel, parent.mu, parent.soiRadius);
//					System.out.println("escapeV: " + escapeV);
//					for (int i = 0; i < orbitBuffer.length; i++) {
//						VectorND vertex = c.getPosition((i-orbitBuffer.length/2) * escapeV * 2.0
//								/ (orbitBuffer.length - 1));
//						vertex.subtract(pos);
//						orbitBuffer[i] = vertex;
//					}
				}
			}
			lock.unlock();
		}
	}

	/**
	 * Update to the current TAI epoch (assuming you never used update()
	 * directly)
	 */
	public void updateTo(double timeTAI) {
		update(timeTAI - lastUpdatedTime);
		lastUpdatedTime = timeTAI;
	}

	public Vector getRelativePos() {
		if (parent == null) {
			return pos;
		}
		return ((VectorND) pos).clone().subtract(parent.pos);
	}

	public Vector getRelativeVel() {
		if (parent == null) {
			return vel;
		}
		return ((VectorND) vel).clone().subtract(parent.vel);
	}

	public Vector getAbsolutePos() {
		if (parent == null) {
			return pos;
		}
		return ((VectorND) pos).clone().add(parent.getAbsolutePos());
	}

	public Vector getAbsoluteVel() {
		if (parent == null) {
			return vel;
		}
		return ((VectorND) vel).clone().add(parent.getAbsoluteVel());
	}
	
	public void superLock(boolean doLock) {
		if(parent != null) {
			parent.superLock(doLock);
		}
		if(doLock) {
			lock.lock();
		} else {
			lock.unlock();
		}
	}
}
