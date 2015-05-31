package simulator.astro;
import java.util.Arrays;

public class Matrix {
	private double[][] data;

	public Matrix(int width, int height) {
		data = new double[width][height];
	}

	public void set(int x, int y, double d) {
		data[x][y] = d;
	}

	public int getWidth() {
		return data.length;
	}

	public int getHeight() {
		return data[0].length;
	}

	public double get(int x, int y) {
		return data[x][y];
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Matrix) {
			Matrix m = (Matrix) o;
			if (m.getWidth() == getWidth() && m.getHeight() == getHeight()) {
				return Arrays.deepEquals(m.data, data);
			}
		}
		return false;
	}

	@Override
	public Object clone() {
		Matrix mat = new Matrix(data.length, data[0].length);
		for (int i = 0; i < data.length; i++) {
			System.arraycopy(data[i], 0, mat.data[i], 0, data[i].length);
		}
		return mat;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int y = 0; y < data[0].length; y++) {
			s.append("[");
			for (int x = 0; x < data.length; x++) {
				s.append(data[x][y]);
				if (x < data.length - 1)
					s.append(", ");
			}
			s.append("]");
			if (y < data.length - 1)
				s.append(", ");
		}
		return s.toString();
	}

	public void add(double scalar) {
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				data[x][y] += scalar;
			}
		}
	}

	public void subtract(double scalar) {
		add(-scalar);
	}

	public void multiply(double scalar) {
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				data[x][y] *= scalar;
			}
		}
	}

	public void divide(double scalar) {
		multiply(1 / scalar);
	}

	public Matrix add(Matrix m) {
		if (m.getWidth() != getWidth() || m.getHeight() != getHeight())
			throw new ArrayIndexOutOfBoundsException("Matrix size mismatch!");
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				data[x][y] += m.data[x][y];
			}
		}
		return this;
	}

	public Matrix subtract(Matrix m) {
		if (m.getWidth() != getWidth() || m.getHeight() != getHeight())
			throw new ArrayIndexOutOfBoundsException("Matrix size mismatch!");
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				data[x][y] -= m.data[x][y];
			}
		}
		return this;
	}

	public static Matrix add(Matrix a, Matrix b) {
		return ((Matrix) a.clone()).add(b);
	}

	public static Matrix subtract(Matrix a, Matrix b) {
		return ((Matrix) a.clone()).subtract(b);
	}

	public Matrix transpose() {
		Matrix mat = new Matrix(getHeight(), getWidth());
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				mat.data[y][x] = data[x][y];
			}
		}
		return mat;
	}

	public static Matrix createIdentityMatrix(int size) {
		Matrix mat = new Matrix(size, size);
		for (int i = 0; i < size; i++) {
			mat.data[i][i] = 1;
		}
		return mat;
	}

	public static Matrix multiply(Matrix a, Matrix b) {
		if (a.getWidth() != b.getHeight())
			throw new ArrayIndexOutOfBoundsException("Matrix size mismatch!");
		Matrix result = new Matrix(a.getHeight(), b.getWidth());
		for (int x = 0; x < result.getWidth(); x++) {
			for (int y = 0; y < result.getHeight(); y++) {
				for (int i = 0; i < a.getWidth(); i++) {
					result.data[x][y] += (a.data[i][x] * b.data[y][i]);
				}
			}
		}
		return result;
	}
}