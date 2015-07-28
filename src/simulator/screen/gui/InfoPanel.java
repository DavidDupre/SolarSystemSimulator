package simulator.screen.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import simulator.astro.Orbit;
import simulator.simObject.Body;
import simulator.simObject.SimObject;

/**
 * Displays information on the sim object in focus
 * 
 * @author David
 *
 */
public class InfoPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private GridBagConstraints gbc;
	private int row = 0;
	private JScrollPane scrollPanel;
	private JPanel tablePanel;

	private SimObject focus;
	
	public InfoPanel() {
		this.setLayout(new FlowLayout());
		
		tablePanel = new JPanel();
		tablePanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();

		scrollPanel = new JScrollPane(tablePanel);
		scrollPanel
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPanel
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		this.add(scrollPanel);
	}
	
	public void setSize(int width, int height) {
		scrollPanel.setBounds(new Rectangle(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}
	
	private void addNewValue(String key, String value) {
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 5;
		JLabel keyLabel = new JLabel(key);
		keyLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		gbc.gridx = 0;
		tablePanel.add(keyLabel, gbc);
		JLabel valueLabel = new JLabel(value);
		valueLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		gbc.gridx = 1;
		tablePanel.add(valueLabel, gbc);
		row++;
	}

	private void addNewValue(String key, Double d) {
		if (d < 360 && d > 0.001) {
			addNewValue(key, String.format("%.5g%n", d));
		} else {
			addNewValue(key, String.format("%6.3e", d));
		}
	}

	public void setFocus(SimObject so) {
		this.focus = so;
		update();
	}

	private void update() {
		// TODO removing all elements and adding them back is probably bad
		
		row = 0;
		tablePanel.removeAll();
		addNewValue("Name", focus.name);
		if (focus.parent != null) {
			Orbit orb = focus.getOrbit();
			addNewValue("Anomaly", Math.toDegrees(orb.v));
			addNewValue("Eccentricity", orb.e);
			addNewValue("Inclination", Math.toDegrees(orb.i));
			addNewValue("Semi-major (m)", orb.a);
			if (focus instanceof Body) {
				addNewValue("Mass (kg)", ((Body) focus).mass);
			}
		} else {
			addNewValue("Mass (kg)", ((Body) focus).mass);
		}
		this.revalidate();
		this.repaint();
	}
}
