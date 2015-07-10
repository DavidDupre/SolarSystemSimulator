package simulator.screen;

import static org.lwjgl.opengl.GL11.*;

/**
 * Basic renderer example
 * 
 * @author S-2482153
 *
 */
public class SphereRenderer implements Renderer {

	public SphereRenderer() {
	}

	@Override
	public void update() {
		glPushMatrix();

		glColor3f(1.0f, 1.0f, 1.0f);

		glPopMatrix();
	}
}
