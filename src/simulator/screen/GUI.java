package simulator.screen;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

public class GUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private Canvas canvas;
	private Window window;
	
	/**
	 * Create the frame.
	 */
	public GUI(Window window, int width, int height) {
		this.window = window;
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, width, height);
		contentPane = new JPanel();
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{1, 0};
		gbl_contentPane.rowHeights = new int[]{1, 0};
		gbl_contentPane.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);
		
		canvas = new Canvas();
		canvas.setSize(width, height);
		GridBagConstraints gbc_canvas = new GridBagConstraints();
		gbc_canvas.gridx = 0;
		gbc_canvas.gridy = 0;
		contentPane.add(canvas, gbc_canvas);
		
		this.setTitle("Solar System Simulator");
	}
	
	protected void addDisplayToCanvas() {
		try {
			Display.setParent(canvas);
			Display.setVSyncEnabled(true);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Dimension size = e.getComponent().getSize();
				canvas.setSize(size.width, size.height);
				window.setSize(size.width, size.height);
				canvas.revalidate();
			}
		});
	}
}
