package simulator.screen;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import simulator.Simulation;
import simulator.simObject.SimObject;

public class Screen {
	private static int WIDTH = 640;
	private static int HEIGHT = 480;
	
	private Renderer renderer;
	private Simulation sim;

	private Camera camera;

	public Screen(Simulation sim) {
		this.renderer = new SphereRenderer();
		this.sim = sim;
		
		camera = new Camera(sim);
		camera.setScale(1E-6);
	}
	
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}
	
	private void init() {
		try {
			Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
			Display.setTitle("Solar System Simulator");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			Display.destroy();
			System.exit(1);
		}
		
		camera.start();
		
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST); // Enables Depth Testing
		glDepthFunc(GL_LEQUAL); // The Type Of Depth Test To Do
	}

	public void start() {
		init();
		run();
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
	
	private void run() {
		while(!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			updateGL();

			Simulation.physicsLock.lock();
			SimObject focus = sim.getFocus();
			if(focus != null) {
				camera.lookAt(focus.getAbsolutePos());
			}
			
			camera.apply();
			renderer.update();
			Simulation.physicsLock.unlock();
						
			Display.update();
			Display.sync(60);
		}
		
		Display.destroy();
		System.exit(0);
	}
}