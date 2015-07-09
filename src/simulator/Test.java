package simulator;

import simulator.astro.Astrophysics;

import com.pi.math.vector.Vector;
import com.pi.math.vector.VectorND;

public class Test {
	private double mu;

	public Test() {
		mu = 3.98574405E14;
	}

	public void lambert() {
		Vector[] lambert = Astrophysics.lambert(new VectorND(15945340, 0, 0),
				new VectorND(12214838.99, 10249467.31, 0), false, false,
				76 * 60, mu);
		System.out.println(lambert[0]);
		System.out.println(lambert[1]);
	}

	public void target() {
		// Fixed
		// Vector3D[] target = Astrophysics.target(new Vector3D(-6518108.3,
		// -2403847.9, -22172.2), new Vector3D(6697475.6, 1794583.2, 0.0),
		// new Vector3D(2604.057, -7105.717, -263.218), new Vector3D(
		// -1962.373, 7323.674, 0.0), 100.0 * 60.0, mu);
		// System.out.println(target[0].magnitude());

		// Moving
		for(int i=0; i<=250; i+=1){
			Vector[] target = Astrophysics.target(new VectorND(5328786.2,
					4436127.3, 101472.0), new VectorND(6697475.6, 1794583.1, 0),
					new VectorND(-4864.779, 5816.486, 240.163), new VectorND(
							-1962.372, 7323.674, 0), i * 60.0, mu, true);
			if(target != null) {
				System.out.println(i + ", " + target[0].magnitude());
			}
		}
	}

	public static void main(String[] args) {
		Test test = new Test();
		test.target();
	}
}
