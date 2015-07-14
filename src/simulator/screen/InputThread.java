package simulator.screen;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import simulator.Simulation;

public class InputThread extends Thread {
	private Thread thread;
	private Simulation sim;
	private Camera cam;

	private int focusIndex;

	public InputThread(Simulation sim, Camera cam) {
		this.sim = sim;
		this.cam = cam;
	}

	@Override
	public void run() {
		try {
			while (!Display.isCloseRequested()) {
				if (Mouse.isButtonDown(1)) {
					int mouseDX = Mouse.getDX();
					int mouseDY = Mouse.getDY();
					if ((mouseDY > 0 && cam.pitch < 0)
							|| (mouseDY < 0 && cam.pitch > -180)) {
						cam.pitch += mouseDY / 1.5;
					}
					cam.yaw += mouseDX / 2.0;
				}

				double zoom = Mouse.getDWheel()
						* (Math.abs(cam.centerDistance) / 1000);
				cam.centerDistance -= zoom;
				if (cam.centerDistance < 1) {
					cam.centerDistance = 1;
				}

				while (Keyboard.next()) {
					if (Keyboard.getEventKeyState()) {
						switch (Keyboard.getEventKey()) {
						case Keyboard.KEY_LBRACKET:
							focusIndex--;
							if (focusIndex < 0) {
								focusIndex = sim.solarSystem.getObjects()
										.size() - 1;
							}
							sim.setFocus(sim.solarSystem.getObjects().get(
									focusIndex));
							break;
						case Keyboard.KEY_RBRACKET:
							focusIndex++;
							if (focusIndex >= sim.solarSystem.getObjects()
									.size()) {
								focusIndex = 0;
							}
							sim.setFocus(sim.solarSystem.getObjects().get(
									focusIndex));
							break;
						case Keyboard.KEY_COMMA:
							sim.simSpeed *= .5;
							break;
						case Keyboard.KEY_PERIOD:
							sim.simSpeed *= 2.0;
							break;
						}
					}
				}
			}
		} catch (IllegalStateException e) {
			// ayy lmao
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.setName("Input");
			thread.start();
		}
	}
}
