package simulator.screen;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import simulator.Simulation;
import simulator.simObject.SimObject;

public class Window {
	private static final int WIDTH = 1920 / 2;
	private static final int HEIGHT = 1080 / 2;
	
	private Simulation sim;
	
	private Renderer renderer;
	private InputThread input;
	public Camera camera;

	public Window(Simulation sim) {
		this.sim = sim;

		camera = new Camera();
		camera.setScale(1E-6);
		
		input = new InputThread(sim, camera);
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	public void run() {
		try {
			Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
			Display.setTitle("Solar System Simulator");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			Display.destroy();
			System.exit(1);
		}

		renderer.initGL();
		
		input.start();
		
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
		int width = Display.getDisplayMode().getWidth();
		int height = Display.getDisplayMode().getHeight();

		glMatrixMode(GL_PROJECTION); // Select The Projection Matrix
		glLoadIdentity(); // Reset The Projection Matrix

		float aspect = (float) height / (float) width;
		glFrustum(-.5, .5, aspect * .5, aspect * -.5, 1.0, 1000000000.0);
	}
}