package simulator.screen.gui;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;

import simulator.Simulation;
import simulator.screen.Window;
import simulator.simObject.SimObject;

public class GUI extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private Canvas canvas;
	private Window window;
	
	private InfoPanel infoPanel;
	
	private ExplorerPanel explorerPanel;
	private PlanPanel planPanel; 
	private JTabbedPane tabbedPane;
	
	public GUI(Simulation sim, Window window, int width, int height) {
		this.window = window;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, width, height);
		contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new GridBagLayout());

		canvas = new Canvas();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		contentPane.add(canvas, gbc);

		infoPanel = new InfoPanel();
		gbc.gridx = 2;
		gbc.gridy = 0;
		contentPane.add(infoPanel, gbc);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		explorerPanel = new ExplorerPanel(sim);
		planPanel = new PlanPanel(sim);
		tabbedPane.addTab("Explorer", explorerPanel);
		tabbedPane.addTab("Plans", planPanel);
		gbc.gridx = 0;
		gbc.gridy = 0;
		contentPane.add(tabbedPane, gbc);
		
		this.setTitle("Solar System Simulator");

		contentPane.revalidate();
		
		UpdateThread updateThread = new UpdateThread();
		updateThread.start();
	}
	
	private class UpdateThread extends Thread {
		public UpdateThread() {
			this.setName("GUI");
		}
		public void run() {
			while (true) {
				if(explorerPanel.isShowing()) {
					explorerPanel.update();
				}
				if(planPanel.isShowing()) {
					planPanel.update();
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void setFocus(SimObject focus) {
		infoPanel.setFocus(focus);
	}

	public void addDisplayToCanvas() {
		try {
			Display.setParent(canvas);
			Display.setVSyncEnabled(true);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				// TODO pls no hardcoderino
				
				Dimension size = e.getComponent().getSize();
				int infoWidth = 200;
				int tabPaneWidth = 200;
				int padY = 80;
				int padX = 40;
				int canvasWidth = (int) (size.width - padX - infoWidth - tabPaneWidth);
				int canvasHeight = (int) (size.height - padY);
				canvas.setSize(canvasWidth, canvasHeight);
				window.setSize(canvasWidth, canvasHeight);
				infoPanel.setSize(infoWidth, canvasHeight); 
				
				int tabPadX = 10;
				int tabPadY = 30;
				tabbedPane.setSize(tabPaneWidth, canvasHeight);
				explorerPanel.setSize(tabPaneWidth-tabPadX, canvasHeight-tabPadY);
				planPanel.setSize(tabPaneWidth-tabPadX, canvasHeight-tabPadY);
				canvas.revalidate();
			}
		});
	}
}
