package simulator.screen;

import static org.lwjgl.opengl.GL11.glRotated;
import static org.lwjgl.opengl.GL11.glScaled;
import static org.lwjgl.opengl.GL11.glTranslated;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Camera {
	public float centerDistance;
	public float pitch;
	public float yaw;
	private double scale;
	private Vector pos;
	private Vector target;
	private Vector lastTarget;
	
	private boolean locked = true;
	
	private static final float MOVE_P = .15f;

	public Camera() {
		centerDistance = 100f;
		pitch = -120f;
		yaw = 0f;
		scale = 1.0;
		pos = new VectorND(0,0,0);
		target = new VectorND(0,0,0);
		lastTarget = new VectorND(0,0,0);
	}

	/**
	 * Set the focus of the camera to a position
	 * 
	 * @param pos
	 */
	public void lookAt(Vector pos) {
		if(locked) {
			this.pos = pos;
		} else {
			lastTarget = target;
			this.target = pos;
			update();
		}
	}
	
	private void update() {
		pos.add(((VectorND) target).clone().subtract(lastTarget));
		Vector dif = ((VectorND) target).clone().subtract(pos);
		pos.add(dif.multiply(MOVE_P));
		if(dif.magnitude() < 1E4) {
			// TODO pls no hardcoderino
			locked = true;
		}
	}

	/**
	 * Apply the various translations and rotations
	 */
	public void apply() {
		glTranslated(0, 0, -centerDistance);
		glRotated(pitch, 1, 0, 0);
		glRotated(yaw, 0, 0, 1);
		glScaled(scale, scale, scale);

		glTranslated(-pos.get(0), -pos.get(1), -pos.get(2));
	}
	
	public void setScale(double scale) {
		this.scale = scale;
	}
	
	public void set(float pitch, float yaw, float zoom) {
		this.pitch = pitch;
		this.yaw = yaw;
		this.centerDistance = zoom;
	}
	
	public void updateFocus(Vector newTarget) {
		locked = false;
		target = newTarget;
	}
}
