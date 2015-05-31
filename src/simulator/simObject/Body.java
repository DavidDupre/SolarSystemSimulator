package simulator.simObject;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import org.lwjgl.util.glu.Sphere;

import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Conic;
import simulator.astro.Orbit;
import simulator.astro.Time;
import simulator.astro.Vector3D;

public class Body extends SimObject {
	public double radius;
	public double mu;

	private Sphere sphere;
	
	protected ArrayList<SimObject> children;

	public Body() {
		sphere = new Sphere();
		radius = 1.0;
		mu = Astrophysics.G;
		pos = new Vector3D();
		vel = new Vector3D();
		name = "null";
		children = new ArrayList<SimObject>();
	}

	public Body(String name, Body parent, double mass, double radius,
			Orbit orb, double epoch) {
		sphere = new Sphere();
		this.name = name;
		children = new ArrayList<SimObject>();
		if(parent != null) {
			setParent(parent);
		}
		mu = mass * Astrophysics.G;
		this.radius = radius;
		this.orb = orb;
		if (parent != null) {
			Vector3D[] state = Astrophysics.toRV(orb, parent.mu, true);
			pos = state[0];
			vel = state[1];
		} else {
			pos = new Vector3D();
			vel = new Vector3D();
		}
		double now = System.currentTimeMillis() / 1000.0;
		if(Simulation.REAL_TIME) {
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
	public void render(RenderDetail detail) {
		glPushMatrix();

		// Position to body
		Vector3D absPos = getAbsolutePos();
		glTranslated(absPos.x, absPos.y, absPos.z);

		// Draw conic
		if(detail.ordinal() > RenderDetail.LOW.ordinal()) {
			if (parent != null) {
				glBegin(GL_LINE_STRIP);
				// TODO use buffered orbits (that look pretty)
				Orbit orb = Astrophysics.toOrbitalElements(pos, vel, parent.mu);
				Conic c = new Conic(orb);
				int numOfPoints = 50;
				for (int i = 0; i < numOfPoints; i++) {
					Vector3D vertex = c.getPosition(i * 2.0 * Math.PI
							/ (numOfPoints - 1) + orb.v);
					vertex.subtract(pos);
					glVertex3d(vertex.x, vertex.y, vertex.z);
				}
				glEnd();
			}
		}

		// Draw point
		glBegin(GL_POINTS);
		glVertex3d(0, 0, 0);
		glEnd();

		// Draw sphere
		sphere.draw((float) radius, 16, 16);

		glPopMatrix();
	}
}
