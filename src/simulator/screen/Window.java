package simulator.screen;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import simulator.Simulation;
import simulator.simObject.SimObject;

public class Window {
	private int width = 960;
	private int height = 540;
	
	private Simulation sim;
	
	private Renderer renderer;
	private InputThread input;
	public Camera camera;
	
	private GUI gui;
	
	public Window(Simulation sim) {
		this.sim = sim;

		camera = new Camera();
		camera.setScale(1E-6);
		
		input = new InputThread(sim, camera);
		
		gui = new GUI(this, width, height);
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	public void run() {
		gui.addDisplayToCanvas();
		gui.setVisible(true);
		
		try {
			Display.setResizable(true);
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			Display.destroy();
			System.exit(1);
		}
		
		renderer.initGL();
		
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST); // Enables Depth Testing
		glDepthFunc(GL_LEQUAL); // The Type Of Depth Test To Do
		glPointSize(5); // Changes point size to 5 pixels
		// TODO pls no hardcoderino
		
		while (!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			updateGL();

			SimObject focus = sim.getFocus();
			if (focus != null) {
				focus.superLock(true);
				camera.lookAt(focus.getAbsolutePos());
				camera.apply();
				renderer.update();
				focus.superLock(false);
			}
			
			input.pollInput();
			
			Display.update();
			Display.sync(60);
		}
		
		renderer.dispose();

		Display.destroy();
	}
	
	/**
	 * Called in the update loop to keep 3D views working
	 */
	private void updateGL() {
		GL11.glViewport(0, 0, width, height);
		
		glMatrixMode(GL_PROJECTION); // Select The Projection Matrix
		glLoadIdentity(); // Reset The Projection Matrix

		float aspect = (float) height / (float) width;
		glFrustum(-.5, .5, aspect * .5, aspect * -.5, 1.0, 1000000000.0);
	}
	
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
}