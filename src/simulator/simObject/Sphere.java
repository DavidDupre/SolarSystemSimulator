package simulator.simObject;

import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.GL11.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Sphere {
	private ArrayList<Vector> vData;
	private ArrayList<Triangle> iData;
	
	private FloatBuffer vBuffer;
	private ShortBuffer iBuffer;
	private IntBuffer ib;
	
	private static final int ITERATIONS = 3;

	public Sphere(double radius) {
		// Create initial octahedron
		vData = new ArrayList<Vector>();
		vData.add(new VectorND(0, -1, 0)); 
		vData.add(new VectorND(1, 0, 0));
		vData.add(new VectorND(0, 0, 1));
		vData.add(new VectorND(-1, 0, 0));
		vData.add(new VectorND(0, 0, -1)); 
		vData.add(new VectorND(0, 1, 0));
		
		iData = new ArrayList<Triangle>();
		iData.add(new Triangle(0, 1, 2));
		iData.add(new Triangle(0, 2, 3));
		iData.add(new Triangle(0, 3, 4));
		iData.add(new Triangle(0, 4, 1));
		iData.add(new Triangle(1, 5, 2));
		iData.add(new Triangle(2, 5, 3));
		iData.add(new Triangle(3, 5, 4));
		iData.add(new Triangle(4, 5, 1));
		
		// Divide triangles
		// TODO prevent duplicate vectors
		for(int j=0; j<ITERATIONS; j++) {
			int iSize = iData.size();
			int vSize = vData.size();
			ArrayList<Triangle> newTriangles = new ArrayList<Triangle>();
			for(int i=0; i<iSize; i++) {
				Triangle t = iData.get(i);
				vData.add(midpoint(vData.get(t.a), vData.get(t.b)));
				vData.add(midpoint(vData.get(t.a), vData.get(t.c)));
				vData.add(midpoint(vData.get(t.c), vData.get(t.b)));
				int offset = i*3+vSize;
				newTriangles.add(new Triangle(offset, offset+1, offset+2));
				newTriangles.add(new Triangle(t.a, offset, offset+1));
				newTriangles.add(new Triangle(t.b, offset, offset+2));
				newTriangles.add(new Triangle(t.c, offset+1, offset+2));
			}
			iData = newTriangles;
		}
		
		// Normalize all vectors
		for(Vector v: vData) {
			v.normalize();
		}
		
		// Convert bs classes into buffers
		vBuffer = BufferUtils.createFloatBuffer(vData.size() * 3);
		for (int i = 0; i < vData.size(); i++) {
			Vector v = vData.get(i);
			v.multiply(radius);
			vBuffer.put((float) v.get(0))
			.put((float) v.get(1))
			.put((float) v.get(2));
		}
		vBuffer.flip();
		
		iBuffer = BufferUtils.createShortBuffer(iData.size() * 3);
		for(int i=0; i < iData.size(); i++) {
			Triangle t = iData.get(i);
			iBuffer.put((short) t.a).put((short) t.b).put((short) t.c);
		}
		iBuffer.flip();
		
		ib = BufferUtils.createIntBuffer(2);
	}
	
	private class Triangle {
		public int a, b, c;
		
		public Triangle(int a, int b, int c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}
	
	public Vector midpoint(Vector v1, Vector v2) {
		return ((VectorND) v1).clone().add(v2).multiply(.5);
	}

	public void draw() {
		glGenBuffersARB(ib); // only needs to be done once
		
		int vHandle = ib.get(0);
		int iHandle = ib.get(1);
		
		glEnableClientState(GL_VERTEX_ARRAY);
		
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vBuffer, GL_STATIC_DRAW_ARB);
		glVertexPointer(3, GL_FLOAT, 0, 0L);
		
		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, iHandle);
		glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, iBuffer, GL_STATIC_DRAW_ARB);
		
//		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

		glDrawElements(GL_TRIANGLES, vData.size()*3, GL_UNSIGNED_SHORT, 0L);
		
//		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

		glDisableClientState(GL_VERTEX_ARRAY);
		
		// cleanup VBO handles
		// TODO This was in the guide, but it seems to ruin the orbit buffers
//		ib.put(0, vHandle);
//		ib.put(1, iHandle);
//		glDeleteBuffersARB(ib);
	}
}
