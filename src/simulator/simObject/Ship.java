package simulator.simObject;

import static org.lwjgl.opengl.GL11.*;
import simulator.Simulation;
import simulator.astro.Astrophysics;
import simulator.astro.Conic;
import simulator.astro.Orbit;
import simulator.astro.Time;
import simulator.astro.Vector3D;
import simulator.plans.FlightPlan;
import simulator.plans.Maneuver;
import simulator.tle.TLE;

public class Ship extends SimObject {

	private FlightPlan plan;

	public Ship(TLE tle, Body parent) {
		plan = new FlightPlan(this);
		name = tle.name;
		setParent(parent);
		this.orb = tle.getOrbit();
		Vector3D[] state = Astrophysics.toRV(orb, parent.mu, false);
		pos = state[0];
		vel = state[1];
		double now = System.currentTimeMillis() / 1000.0;
		if(Simulation.REAL_TIME) {
			lastUpdatedTime = Time.jdToTAI(tle.getEpoch());
		} else {
			lastUpdatedTime = now;
		}
		updateTo(now);
	}

	public void addManeuver(Maneuver m) {
		m.setShip(this);
		if(plan.getManeuvers().isEmpty()) {
			m.init();
		}
		plan.addManeuver(m);
	}

	/**
	 * Update to the current epoch. Also updates the flight plan
	 * 
	 * @param epoch
	 */
	@Override
	public void updateTo(double epoch) {
		update(epoch - lastUpdatedTime);
		lastUpdatedTime = epoch;
		plan.updateTo(epoch);
	}

	@Override
	public void render(RenderDetail detail) {
		glPushMatrix();

		// Position to body
		Vector3D absPos = getAbsolutePos();
		glTranslated(absPos.x, absPos.y, absPos.z);

		// Draw conic
		if (detail.ordinal() > RenderDetail.LOW.ordinal()) {
			glColor3f(1.0f, .4f, .4f);
			glBegin(GL_LINE_STRIP);
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

		// Draw point
		glColor3f(1.0f, .2f, .2f);
		glBegin(GL_POINTS);
		glVertex3d(0, 0, 0);
		glEnd();
		glColor3f(1.0f, 1.0f, 1.0f);

		glPopMatrix();
	}
}
