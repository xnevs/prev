package compiler.tmpan.graph;

import java.util.*;

import compiler.frames.*;

public class InterferenceGraph {
	
	public class Node {
		public FrmTemp val;
		public Set<Node> neigh;
	
		public boolean exists;
		public int degree;
		
		public int colour;
		
		public Node(FrmTemp val) {
			this.val = val;
			this.neigh = new HashSet<Node>();
			
			this.exists = true;
			this.degree = 0;
			
			this.colour = -1;
		}
		
		public void addNeigh(Node node) {
			neigh.add(node);
			degree++;
		}
	}

	public Set<Node> nodes;
	
	public Map<FrmTemp, Node> tempsToNodes;
	
	public int numExisting;
	
	public InterferenceGraph() {
		this.nodes = new HashSet<Node>();
		this.tempsToNodes = new HashMap<FrmTemp, Node>();
		this.numExisting = 0;
	}
	
	public Node addNode(FrmTemp a) {
		Node node = tempsToNodes.get(a);
		
		if(node == null) {
			node = new Node(a);
			nodes.add(node);
			tempsToNodes.put(a, node);
			numExisting++;
		}
		
		return node;
	}
	
	public void addEdge(FrmTemp a, FrmTemp b) {
		if(a == b)
			return;
		Node node1 = addNode(a);
		Node node2 = addNode(b);
		
		node1.addNeigh(node2);
		node2.addNeigh(node1);
	}
	
	public boolean exists(Node node) {
		return node.exists;
	}
	
	public void remove(Node node) {
		node.exists = false;
		for(Node neigh : node.neigh)
			neigh.degree--;
		numExisting--;
	}
	
	public boolean isEmpty() {
		return numExisting == 0;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for(Node node : nodes) {
			sb.append(String.format("%5s:", node.val.name()));
			for(Node neigh : node.neigh)
				sb.append(String.format(" %s", neigh.val.name()));
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
