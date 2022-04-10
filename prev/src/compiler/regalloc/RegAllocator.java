package compiler.regalloc;

import java.util.*;

import compiler.Report;
import compiler.frames.*;
import compiler.imcode.*;
import compiler.asmcode.*;
import compiler.tmpan.*;
import compiler.tmpan.graph.*;

public class RegAllocator {
	
	public ImcCodeChunk chunk;

	public RegAllocator(ImcCodeChunk chunk) {
		this.chunk = chunk;
	}
	
	private InterferenceGraph graph;
	
	private Deque<InterferenceGraph.Node> stack;
	
	private List<InterferenceGraph.Node> actualSpills;
	
	private Set<FrmTemp> spilled;
	
	private void build() {
		TmpAnalyser tmpAnalyser = new TmpAnalyser(chunk);
		graph = tmpAnalyser.analyse();
	}
	
	private void simplify() {
		if(graph.isEmpty()) return;
		
		Queue<InterferenceGraph.Node> q = new LinkedList<InterferenceGraph.Node>();
		
		for(InterferenceGraph.Node node : graph.nodes) {
			if(node.degree < AsmCode.NUM_LOCAL_REGS)
				q.add(node);
		}
		
		while(!q.isEmpty()) {
			InterferenceGraph.Node node = q.poll();
			if(!node.exists)
				continue;
			
			graph.remove(node);
			stack.addFirst(node);
			
			for(InterferenceGraph.Node neigh : node.neigh) {
				if(neigh.exists && neigh.degree < AsmCode.NUM_LOCAL_REGS && !q.contains(neigh))
					q.add(neigh);
			}
		}
	}
	
	
	private InterferenceGraph.Node choose() {
		InterferenceGraph.Node node = null;
		for(InterferenceGraph.Node n : graph.nodes) {
			if(n.exists) {
				if((node == null || n.degree < node.degree) && !spilled.contains(n.val))
					node = n;
			}
		}
		return node;
	}
	private void spill() {
		if(graph.isEmpty()) return;
		
		InterferenceGraph.Node node = choose();
		if(node != null) {
			graph.remove(node);
			stack.addFirst(node);
		}
		else
			Report.error("Could not allocate registers");
	}
	
	private boolean colour(InterferenceGraph.Node node) {
		if(node.val == chunk.frame.FP) {
			node.colour = 251;
			return true;
		}
		
		Set<Integer> colours = new HashSet<Integer>();
		for(int i=0; i<AsmCode.NUM_LOCAL_REGS; i++)
			colours.add(i);
		
		for(InterferenceGraph.Node neigh : node.neigh)
			colours.remove(neigh.colour);
		
		if(colours.isEmpty()) {
			return false;
		}
		else {
			node.colour = colours.iterator().next();
			return true;
		}
	}
	private void select() {
		while(!stack.isEmpty()) {
			InterferenceGraph.Node node = stack.pollFirst();
			if(!colour(node))
				actualSpills.add(node);
		}
	}
	
	
	private LinkedList<FrmTemp> L(FrmTemp... temps) {
		LinkedList<FrmTemp> l = new LinkedList<FrmTemp>();
		for(FrmTemp temp : temps)
			l.add(temp);
		return l;
	}
	private void listReplaceAll(LinkedList<FrmTemp> l, FrmTemp oldTemp, FrmTemp newTemp) {
		ListIterator<FrmTemp> it = l.listIterator();
		while(it.hasNext())
			if(it.next() == oldTemp)
				it.set(newTemp);
	}
	private void generateSpillCode(FrmTemp spill) {
		Map<FrmTemp, Integer> spillOffsets = new HashMap<FrmTemp, Integer>();
		
		int numInstrs = chunk.asmcode.size();
		for(int i=0; i<numInstrs; i++) {
			AsmInstr instr = chunk.asmcode.get(i);
			
			
			if(instr.uses.contains(spill)) {
				FrmTemp newTemp = new FrmTemp();
				
				int offset = spillOffsets.get(spill);
				chunk.asmcode.add(i, new AsmOPER(
				        "	SETL	`d0," + (offset & 0xFFFF) + "\n" +
				        "	ORML	`d0," + ((offset >> 16) & 0xFFFF) + "\n" +
				        "	ORMH	`d0," + ((offset >> 32) & 0xFFFF) + "\n" +
				        "	ORH	`d0," + ((offset >> 48) & 0xFFFF) + "\n" +
				        "	ADD	`d0,`s0,`d0" + "\n" +
				        "	LDO	`d0,`d0,0",
						L(newTemp), L(chunk.frame.FP)));
				numInstrs++;
				i++;
				
				spilled.add(newTemp);
				
				listReplaceAll(instr.uses, spill, newTemp);
			}
			
			if(instr.defs.contains(spill)) {
				FrmTemp newTemp = new FrmTemp();
				FrmTemp offsetTemp = new FrmTemp();
				spilled.add(offsetTemp);
				
				int offset = -1 * (chunk.frame.sizeLocs + 16 + chunk.frame.sizeTmps);
				chunk.frame.sizeTmps += 8;
				
				chunk.asmcode.add(i+1, new AsmOPER(
				        "	SETL	`d0," + (offset & 0xFFFF) + "\n" +
				        "	ORML	`d0," + ((offset >> 16) & 0xFFFF) + "\n" +
				        "	ORMH	`d0," + ((offset >> 32) & 0xFFFF) + "\n" +
				        "	ORH	`d0," + ((offset >> 48) & 0xFFFF) + "\n" +
				        "	ADD	`d0,`s0,`d0",
						L(offsetTemp), L(chunk.frame.FP)));
				chunk.asmcode.add(i+2, new AsmOPER(
				        "	STO	`s0,`s1,0",
						null, L(newTemp, offsetTemp)));
				spillOffsets.put(spill, offset);
				numInstrs++;
				i++;
				
				spilled.add(newTemp);
				
				listReplaceAll(instr.defs, spill, newTemp);
			}
		}
	}
	
	public HashMap<FrmTemp, String> allocate() {
		spilled = new HashSet<FrmTemp>();
		do {
			stack = new LinkedList<InterferenceGraph.Node>();
			actualSpills = new LinkedList<InterferenceGraph.Node>();
			
			build();

			while(!graph.isEmpty()) {
				simplify();
				spill();
			}

			select();
			
			for(InterferenceGraph.Node node : actualSpills) {
				generateSpillCode(node.val);
			}
			
		} while(!actualSpills.isEmpty());
		
		HashMap<FrmTemp, String> allocation = new HashMap<FrmTemp, String>();
		for(InterferenceGraph.Node node : graph.nodes) {
			allocation.put(node.val, String.format("$%d", node.colour));
		}
		return allocation;
	}
}
