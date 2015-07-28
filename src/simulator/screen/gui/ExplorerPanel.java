package simulator.screen.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import simulator.Simulation;
import simulator.SolarSystem.SOIChangeListener;
import simulator.plans.SOIChange;
import simulator.simObject.Ship;
import simulator.simObject.SimObject;

/**
 * Project explorer which displays all sim objects in a tree layout
 * 
 * @author David
 *
 */
public class ExplorerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private Simulation sim;

	private JTree tree;
	private DefaultMutableTreeNode root;
	private DefaultTreeModel model;
	private JScrollPane scrollPane;
	private TreeSelectionListener listener;

	private boolean isPopulated = false;

	public ExplorerPanel(final Simulation sim) {
		this.sim = sim;

		this.setLayout(new FlowLayout());

		// Initialize variables
		root = new DefaultMutableTreeNode("Plan");
		tree = new JTree(root);
		model = (DefaultTreeModel) tree.getModel();
		scrollPane = new JScrollPane(tree);

		this.add(scrollPane);

		// Selection listener
		listener = new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree
						.getLastSelectedPathComponent();
				if (selectedNode != null) {
					sim.setFocus((SimObject) selectedNode.getUserObject());
				}
			}
		};
		tree.getSelectionModel().addTreeSelectionListener(listener);

		sim.solarSystem.addNewSOIChangeListener(new SOIChangeListener() {
			@Override
			public void soiChange(SOIChange e) {
				Ship s = e.getShip();
				SimObject newParent = e.getParent();
				MutableTreeNode parentNode = getNode(newParent);
				MutableTreeNode childNode = getNode(s);
				model.removeNodeFromParent(childNode);
				model.insertNodeInto(childNode, parentNode, 0);
			}
		});

		// Populate tree
		populate();

		tree.setShowsRootHandles(true);
	}

	private DefaultMutableTreeNode getNode(SimObject o) {
		Enumeration e = root.preorderEnumeration();
		while(e.hasMoreElements()) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
			if(n.getUserObject().equals(o)) {
				return n;
			}
		}
		return null;
	}

	public void update() {
		if (!isPopulated) {
			populate();
		}
	}

	private void populate() {
		if (!sim.solarSystem.getObjects().isEmpty()) {
			SimObject sun = sim.solarSystem.getObjects().get(0);
			root.add(populate(sun));

			tree.expandRow(0);
			tree.expandRow(1);
			tree.setRootVisible(false);
		}
	}

	private DefaultMutableTreeNode populate(SimObject o) {
		isPopulated = true;
		DefaultMutableTreeNode n = new DefaultMutableTreeNode(o);

		for (SimObject c : o.getChildren()) {
			n.add(populate(c));
		}

		return n;
	}

	public void setSize(int width, int height) {
		scrollPane.setPreferredSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}
}
