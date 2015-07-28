package simulator.screen;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import simulator.Simulation;
import simulator.simObject.SimObject;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class InputThread {
	private Simulation sim;
	private Camera cam;

	private FloatBuffer model;
	private FloatBuffer projection;
	private IntBuffer viewport;
	
	private static final float SENSITIVITY = 4f;
	
	/*
	 * Within this many pixels = valid selection
	 */
	private static final int SELECTION_THRESHOLD = 20;

	/**
	 * This isn't a real thread anymore. It needs to make OpenGL calls.
	 * 
	 * @param sim
	 * @param cam
	 */
	public InputThread(Simulation sim, Camera cam) {
		this.sim = sim;
		this.cam = cam;

		model = BufferUtils.createFloatBuffer(16);
		projection = BufferUtils.createFloatBuffer(16);
		viewport = BufferUtils.createIntBuffer(16);
	}

	public void pollInput() {
		if (Mouse.isButtonDown(1)) {
			int mouseDX = Mouse.getDX();
			int mouseDY = Mouse.getDY();
			if ((mouseDY > 0 && cam.pitch < 0)
					|| (mouseDY < 0 && cam.pitch > -180)) {
				cam.pitch += mouseDY / 1.5;
			}
			cam.yaw += mouseDX / 2.0;
		}

		double zoom = Mouse.getDWheel() * (Math.abs(cam.centerDistance) / 1000);
		cam.centerDistance -= zoom;
		
		if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
			cam.pitch -= SENSITIVITY;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			cam.pitch += SENSITIVITY;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
			cam.yaw += SENSITIVITY;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			cam.yaw -= SENSITIVITY;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_EQUALS)) {
			cam.centerDistance -= SENSITIVITY * (Math.abs(cam.centerDistance) / 50);
		} else if(Keyboard.isKeyDown(Keyboard.KEY_MINUS)) {
			cam.centerDistance += SENSITIVITY * (Math.abs(cam.centerDistance) / 50);
		}
		
		if (cam.centerDistance < 1) {
			cam.centerDistance = 1;
		}
		if(cam.pitch < -180) {
			cam.pitch = -180;
		} else if (cam.pitch > 0) {
			cam.pitch = 0;
		}

		while (Mouse.next()) {
			if (Mouse.getEventButton() > -1) {
				if (!Mouse.getEventButtonState()) {
					switch (Mouse.getEventButton()) {
					case 0:
						Vector screenPos = new VectorND(Mouse.getX(), Mouse.getY());
						SimObject selection = getClosestObject(screenPos);
						sim.setFocus(selection);
						break;
					}
				}
			}
		}

		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				switch (Keyboard.getEventKey()) {
				case Keyboard.KEY_LBRACKET:
					int focusIndex = sim.solarSystem.getObjects().indexOf(sim.getFocus());
					focusIndex--;
					if (focusIndex < 0) {
						focusIndex = sim.solarSystem.getObjects().size() - 1;
					}
					sim.setFocus(sim.solarSystem.getObjects().get(focusIndex));
					break;
				case Keyboard.KEY_RBRACKET:
					focusIndex = sim.solarSystem.getObjects().indexOf(sim.getFocus());
					focusIndex++;
					if (focusIndex >= sim.solarSystem.getObjects().size()) {
						focusIndex = 0;
					}
					sim.setFocus(sim.solarSystem.getObjects().get(focusIndex));
					break;
				case Keyboard.KEY_COMMA:
					sim.simSpeed *= .5;
					break;
				case Keyboard.KEY_PERIOD:
					sim.simSpeed *= 2.0;
					break;
				case Keyboard.KEY_SPACE:
					sim.setPaused(!sim.isPaused());
					break;
				}
			}
		}
	}

	private void recalculateMatrices() {
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
	}
	
	public SimObject getClosestObject(Vector cursorPos) {
		ArrayList<SimObject> candidates = sim.getFocus().getFamily();
		
		double min = Double.MAX_VALUE;
		SimObject closest = sim.getFocus();
		for(SimObject o: candidates) {
			Vector pos = getScreenPos(o.getAbsolutePos());
			double dist = pos.subtract(cursorPos).magnitude();
			if(dist < min) {
				min = dist;
				closest = o;
			}
		}
		if(min > SELECTION_THRESHOLD) {
			return null;
		}
		
		return closest;
	}

	/**
	 * 
	 * @param worldPos
	 * @return two-dimensional vector of screen position
	 */
	public Vector getScreenPos(Vector worldPos) {
		recalculateMatrices();

		FloatBuffer winPos = BufferUtils.createFloatBuffer(3);
		GLU.gluProject((float) worldPos.get(0), (float) worldPos.get(1),
				(float) worldPos.get(2), model, projection, viewport, winPos);
		
		return new VectorND(winPos.get(0), winPos.get(1));
	}

	public Vector getWorldPos(int mouseX, int mouseY) {
		recalculateMatrices();

		FloatBuffer winZ = BufferUtils.createFloatBuffer(1);
		GL11.glReadPixels(mouseX, mouseY, 1, 1, GL11.GL_DEPTH_COMPONENT,
				GL11.GL_FLOAT, winZ);
		FloatBuffer pos = BufferUtils.createFloatBuffer(3);
		GLU.gluUnProject(mouseX, mouseY, winZ.get(0), model, projection,
				viewport, pos);

		// Here's where I would use VectorBuff3... if I weren't using
		// DoubleBuffers
		return new VectorND(pos.get(0), pos.get(1), pos.get(2));
	}
}
