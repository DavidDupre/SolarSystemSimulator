package simulator.screen;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import java.awt.Canvas;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public class GUI extends JFrame {

	private JPanel contentPane;
	private Canvas canvas;

	/**
	 * Create the frame.
	 */
	public GUI(int width, int height) {
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
	}
	
	protected void addDisplayToCanvas() {
		try {
			Display.setParent(canvas);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

}
