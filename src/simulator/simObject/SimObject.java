package simulator.simObject;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import simulator.astro.Astrophysics;
import simulator.astro.Conic;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;

public abstract class SimObject {
	public Body parent;
	public Vector3D pos;
	public Vector3D vel;
	public String name;
	protected Orbit orb;
	public double lastUpdatedTime;
	protected Vector3D[] orbitBuffer = new Vector3D[50];
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
		Vector3D absPos = getAbsolutePos();
		glTranslated(absPos.x, absPos.y, absPos.z);

		// Set object-specific color
		glColor3f(color[0], color[1], color[2]);
		
		// Draw conic
		lock.lock();
		if (detail.ordinal() > RenderDetail.LOW.ordinal()) {
			if (parent != null) {
				glBegin(GL_LINE_STRIP);
				for (int i = 0; i < orbitBuffer.length; i++) {
					glVertex3d(orbitBuffer[i].x, orbitBuffer[i].y,
							orbitBuffer[i].z);
				}
				glEnd();
			}
		}
		lock.unlock();

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
				Vector3D absPos = getAbsolutePos();
				Vector3D absVel = getAbsoluteVel();
				parent.getChildren().remove(this);
				pos = absPos.clone().subtract(b.getAbsolutePos());
				vel = absVel.clone().subtract(b.getAbsoluteVel());
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
			lock.lock();
			Vector3D[] state = Astrophysics.kepler(pos, vel, parent.mu, delta);
			pos = state[0];
			vel = state[1];

			/*
			 * Buffer the orbit for the render thread
			 */
			if (renderDetail.ordinal() > RenderDetail.LOW.ordinal()) {
				Orbit orb = Astrophysics.toOrbitalElements(pos, vel, parent.mu);
				Conic c = new Conic(orb);
				for (int i = 0; i < orbitBuffer.length; i++) {
					Vector3D vertex = c.getPosition(i * 2.0 * Math.PI
							/ (orbitBuffer.length - 1) + orb.v);
					vertex.subtract(pos);
					orbitBuffer[i] = vertex;
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

	public Vector3D getRelativePos() {
		if (parent == null) {
			return pos;
		}
		return pos.clone().subtract(parent.pos);
	}

	public Vector3D getRelativeVel() {
		if (parent == null) {
			return vel;
		}
		return vel.clone().subtract(parent.vel);
	}

	public Vector3D getAbsolutePos() {
		if (parent == null) {
			return pos;
		}
		return pos.clone().add(parent.getAbsolutePos());
	}

	public Vector3D getAbsoluteVel() {
		if (parent == null) {
			return vel;
		}
		return vel.clone().add(parent.getAbsoluteVel());
	}
}
