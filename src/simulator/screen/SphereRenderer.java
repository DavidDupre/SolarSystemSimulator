package simulator.screen;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.util.glu.Sphere;

/**
 * Basic renderer example
 * 
 * @author S-2482153
 *
 */
public class SphereRenderer implements Renderer {
	private Sphere sphere;

	public SphereRenderer() {
		sphere = new Sphere();
	}

	@Override
	public void update() {
		glPushMatrix();

		glColor3f(1.0f, 1.0f, 1.0f);
		sphere.draw(1.5f, 16, 16);

		glPopMatrix();
	}
}
