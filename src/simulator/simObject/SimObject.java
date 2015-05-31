package simulator.simObject;

import java.util.ArrayList;

import simulator.astro.Astrophysics;
import simulator.astro.Orbit;
import simulator.astro.Vector3D;

public abstract class SimObject {
	public Body parent;
	public Vector3D pos;
	public Vector3D vel;
	public String name;
	protected Orbit orb;
	public double lastUpdatedTime;

	public enum RenderDetail {
		LOW, MAX
	}

	public void render() {
		render(RenderDetail.MAX);
	}

	public abstract void render(RenderDetail detail);

	protected void setParent(Body b) {
		if (parent != null) {
			parent.getChildren().remove(this);
		}
		b.getChildren().add(this);
		parent = b;
	}

	public ArrayList<SimObject> getChildren() {
		return new ArrayList<SimObject>();
	}

	protected void update(double delta) {
		if (parent != null) {
			Vector3D[] state = Astrophysics
					.kepler(pos, vel, parent.mu, delta);
			pos = state[0];
			vel = state[1];
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
