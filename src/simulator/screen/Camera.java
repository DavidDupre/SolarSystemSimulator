package simulator.screen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import static org.lwjgl.opengl.GL11.*;
import simulator.Simulation;
import simulator.astro.Vector3D;

public class Camera {
	private float centerDistance, pitch, yaw;
	private double scale;
	private Vector3D pos;
	private InputThread input;
	private Simulation sim;
	
	private int focusIndex = 0;

	public Camera(Simulation sim) {
		input = new InputThread();
		this.sim = sim;

		centerDistance = 100f;
		pitch = -120f;
		yaw = 0f;
		scale = 1.0;
		pos = new Vector3D();
	}
	
	/**
	 * Start the input thread
	 */
	public void start() {
		input.start();
	}

	/**
	 * Set the focus of the camera to a position
	 * 
	 * @param pos
	 */
	public void lookAt(Vector3D pos) {
		this.pos = pos;
	}

	/**
	 * Apply the various translations and rotations
	 */
	public void apply() {
		glTranslated(0, 0, -centerDistance);
		glRotated(pitch, 1, 0, 0);
		glRotated(yaw, 0, 0, 1);
		glScaled(scale, scale, scale);

		glTranslated(-pos.x, -pos.y, -pos.z);
	}
	
	public void setScale(double scale) {
		this.scale = scale;
	}

	private class InputThread extends Thread {
		private Thread thread;

		@Override
		public void run() {
			while (Display.isCreated()) {
				try {
					if (Mouse.isButtonDown(1)) {
						int mouseDX = Mouse.getDX();
						int mouseDY = Mouse.getDY();
						if ((mouseDY > 0 && pitch < 0)
								|| (mouseDY < 0 && pitch > -180)) {
							pitch += mouseDY / 1.5;
						}
						yaw += mouseDX / 2;
					}
	
					double zoom = Mouse.getDWheel()
							* (Math.abs(centerDistance) / 1000);
					centerDistance -= zoom;
					if (centerDistance < 1) {
						centerDistance = 1;
					}
					
					while(Keyboard.next()) {
						if(Keyboard.getEventKeyState()) {
							switch(Keyboard.getEventKey()) {
							case Keyboard.KEY_LBRACKET:
								focusIndex--;
								if(focusIndex < 0) {
									focusIndex = sim.solarSystem.getObjects().size()-1;
								}
								sim.setFocus(sim.solarSystem.getObjects().get(focusIndex));
								break;
							case Keyboard.KEY_RBRACKET:
								focusIndex++;
								if(focusIndex >= sim.solarSystem.getObjects().size()) {
									focusIndex = 0;
								}
								sim.setFocus(sim.solarSystem.getObjects().get(focusIndex));
								break;
							}
						}
					}
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void start() {
			if (thread == null) {
				thread = new Thread(this);
				thread.setName("Camera input");
				thread.start();
			}
		}
	}
}
