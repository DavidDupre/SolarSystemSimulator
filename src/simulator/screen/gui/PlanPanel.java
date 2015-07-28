package simulator.screen.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import simulator.Simulation;
import simulator.plans.ManeuverFactory;
import simulator.plans.maneuvers.Maneuver;
import simulator.simObject.Ship;

/**
 * Lists all plans in a ship-maneuver-burn hierarchy
 * 
 * @author David
 *
 */
@SuppressWarnings("serial")
public class PlanPanel extends JPanel {
	private Simulation sim;

	private JTree tree;
	private DefaultMutableTreeNode root;
	private DefaultTreeModel model;
	private JScrollPane scrollPane;
	private MouseAdapter mouseListener;
	private ManeuverFactory factory;

	private boolean isPopulated = false;

	public PlanPanel(final Simulation sim) {
		this.sim = sim;

		this.setLayout(new FlowLayout());

		// Initialize variables
		root = new DefaultMutableTreeNode("Plan");
		tree = new JTree(root);
		model = (DefaultTreeModel) tree.getModel();
		scrollPane = new JScrollPane(tree);
		factory = new ManeuverFactory();

		mouseListener = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)
						&& e.getClickCount() >= 1) {
					TreePath path = tree.getPathForLocation(e.getX(), e.getY());
					Rectangle pathBounds = tree.getUI().getPathBounds(tree,
							path);
					if (pathBounds != null
							&& pathBounds.contains(e.getX(), e.getY())) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
								.getLastPathComponent();
						if (node.getUserObject() instanceof String) {
							Maneuver m = (Maneuver) ((DefaultMutableTreeNode) node
									.getParent()).getUserObject();
							String input = (String) node.getUserObject();

							EditMenu menu = new EditMenu(m, input, node);
							menu.show(tree, pathBounds.x, pathBounds.y
									+ pathBounds.height);
							menu.selectAll();
						}
					}
				}
			}
		};
		tree.addMouseListener(mouseListener);

		this.add(scrollPane);

		// Populate tree
		populate();

		// Hide root node
		tree.setShowsRootHandles(true);
	}

	private class EditMenu extends JPopupMenu {
		private JTextField textField;
		private Maneuver m;

		public EditMenu(final Maneuver maneuver, final String inputKey,
				final DefaultMutableTreeNode node) {
			this.m = maneuver;
			String value = m.inputs.get(inputKey);
			textField = new JTextField(value);

			textField.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					String newInput = textField.getText();
					m.inputs.put(inputKey, newInput);
					Maneuver newManeuver = factory.createNewManeuver(sim, m
							.getClass().getSimpleName(), m.inputs);
					int index = m.getShip().getManeuvers().indexOf(m);

					sim.solarSystem.removeManeuver(m.getShip(), index);
					sim.solarSystem.insertManeuver(newManeuver, index,
							m.getShip());

					((DefaultMutableTreeNode) node.getParent())
							.setUserObject(newManeuver);
					model.nodeChanged(node.getParent());
					model.nodeChanged(node);

					m = newManeuver;
				}
			});

			textField.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent e) {
					if (!e.isTemporary()) {
						textField.selectAll();
					}
				}
			});

			add(textField);
		}

		public void selectAll() {
			textField.requestFocusInWindow();
			textField.selectAll();
		}
	}

	public void update() {
		if (!isPopulated) {
			populate();
		}
	}

	private void populate() {
		ArrayList<Ship> ships = sim.solarSystem.getShips();
		for (Ship s : ships) {
			isPopulated = true;
			if (!s.getManeuvers().isEmpty()) {
				DefaultMutableTreeNode n = new DefaultMutableTreeNode(s);
				root.add(n);
				for (Maneuver m : s.getManeuvers()) {
					DefaultMutableTreeNode mNode = new DefaultMutableTreeNode(m);
					n.add(mNode);
					for (String input : m.inputs.keySet()) {
						DefaultMutableTreeNode iNode = new DefaultMutableTreeNode(
								input);
						mNode.add(iNode);
					}
				}
			}
		}
		if (isPopulated) {
			tree.expandRow(0);
			tree.setRootVisible(false);
		}
	}

	public void setSize(int width, int height) {
		scrollPane.setPreferredSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}
}
