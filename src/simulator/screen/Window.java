package simulator.screen;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import simulator.Simulation;
import simulator.simObject.SimObject;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window {

	// We need to strongly reference callback instances.
	private GLFWErrorCallback errorCallback;
	private GLFWKeyCallback keyCallback;
	private GLFWScrollCallback scrollCallback;

	// The window handle
	private long window;

	private static final int WIDTH = 1920 / 2;
	private static final int HEIGHT = 1080 / 2;
	
	DoubleBuffer b1, b2;

	private Renderer renderer;
	private Simulation sim;
	private int focusIndex = 3;

	public Camera camera;

	public Window(Simulation sim) {
		this.renderer = new SphereRenderer();
		this.sim = sim;

		camera = new Camera();
		camera.setScale(1E-6);
		
		b1 = BufferUtils.createDoubleBuffer(1);
		b2 = BufferUtils.createDoubleBuffer(1);
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	public void run() {
		try {
			init();
			loop();

			// Release window and window callbacks
			glfwDestroyWindow(window);
			keyCallback.release();
			scrollCallback.release();
		} finally {
			// Terminate GLFW and release the GLFWerrorfun
			glfwTerminate();
			errorCallback.release();
		}
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (glfwInit() != GL11.GL_TRUE)
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure our window
		glfwDefaultWindowHints(); // optional, the current window hints are
									// already the default
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // the window will stay hidden
												// after creation
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(WIDTH, HEIGHT, "Simulator", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed,
		// repeated or released.
		glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action,
					int mods) {
				if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
					glfwSetWindowShouldClose(window, GL_TRUE);
				} else if (key == GLFW_KEY_LEFT_BRACKET && action == GLFW_RELEASE) {
					focusIndex--;
					if(focusIndex < 0) {
						focusIndex = sim.solarSystem.getObjects().size()-1;
					}
					sim.setFocus(sim.solarSystem.getObjects().get(focusIndex));
				} else if (key == GLFW_KEY_RIGHT_BRACKET && action == GLFW_RELEASE) {
					focusIndex++;
					if(focusIndex >= sim.solarSystem.getObjects().size()) {
						focusIndex = 0;
					}
					sim.setFocus(sim.solarSystem.getObjects().get(focusIndex));
				} else if (key == GLFW_KEY_COMMA && action == GLFW_RELEASE) {
					sim.simSpeed *= .5;
				} else if (key == GLFW_KEY_PERIOD && action == GLFW_RELEASE) {
					sim.simSpeed *= 2.0;
				}
			}
		});
		
		glfwSetScrollCallback(window, scrollCallback = new GLFWScrollCallback() {
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				double zoom = yoffset
						* (Math.abs(camera.centerDistance) / 5);
				camera.centerDistance -= zoom;
				if (camera.centerDistance < 1) {
					camera.centerDistance = 1;
				}
			}
		});

		// Get the resolution of the primary monitor
		ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		// Center our window
		glfwSetWindowPos(window, (GLFWvidmode.width(vidmode) - WIDTH) / 2,
				(GLFWvidmode.height(vidmode) - HEIGHT) / 2);

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}

	private void loop() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the ContextCapabilities instance and makes the OpenGL
		// bindings available for use.
		GLContext.createFromCurrent();

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		glDisable(GL_BLEND);
		glEnable(GL_DEPTH_TEST); // Enables Depth Testing
		glDepthFunc(GL_LEQUAL); // The Type Of Depth Test To Do
		glPointSize(5); // Changes point size to 5 pixels
		// TODO pls no hardcoderino

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (glfwWindowShouldClose(window) == GL_FALSE) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the
																// framebuffer

			// TODO Bad fixed-pipeline stuff that should go away (?)
			glMatrixMode(GL_PROJECTION); // Select The Projection Matrix
			glLoadIdentity(); // Reset The Projection Matrix

			float aspect = (float) HEIGHT / (float) WIDTH;
			glFrustum(-.5, .5, aspect * .5, aspect * -.5, 1.0, 1000000000.0);

			SimObject focus = sim.getFocus();
			if (focus != null) {
				focus.superLock(true);
				camera.lookAt(focus.getAbsolutePos());
				camera.apply();
				renderer.update();
				focus.superLock(false);
			}
			
			pollMouseInput();
			
			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}
	
	private double lastPosX;
	private double lastPosY;
	private boolean mouseLocked = false;
	
	private void pollMouseInput() {
		if (GLFW.glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GL11.GL_TRUE) {
			glfwGetCursorPos(window, b1, b2);
			
			double mouseDX;
			double mouseDY;
			if(!mouseLocked) {
				mouseDX = 0;
				mouseDY = 0;
				mouseLocked = true;
			} else {
				mouseDX = b1.get(0) - lastPosX;
				mouseDY = b2.get(0) - lastPosY;
			}
			lastPosX = b1.get(0);
			lastPosY = b2.get(0);
			if ((mouseDY < 0 && camera.pitch < 0)
					|| (mouseDY > 0 && camera.pitch > -180)) {
				camera.pitch -= mouseDY / 1.5;
			}
			camera.yaw += mouseDX / 2;
		} else {
			mouseLocked = false;
		}
	}
}