package compiler.tmpan;

import java.util.*;

import compiler.frames.*;
import compiler.imcode.*;
import compiler.asmcode.*;
import compiler.tmpan.graph.*;

public class TmpAnalyser {

	public ImcCodeChunk chunk;

	private Map<AsmInstr, Set<AsmInstr>> succ;
	
	private Map<AsmInstr, Set<FrmTemp>> in;
	private Map<AsmInstr, Set<FrmTemp>> out;
	
	public TmpAnalyser(ImcCodeChunk chunk) {
		this.chunk = chunk;
		
		this.succ = new HashMap<AsmInstr, Set<AsmInstr>>();
		
		this.in = new HashMap<AsmInstr, Set<FrmTemp>>();
		this.out = new HashMap<AsmInstr, Set<FrmTemp>>();
		
		for(AsmInstr instr : chunk.asmcode) {
			this.succ.put(instr, new HashSet<AsmInstr>());
			
			this.in.put(instr, new HashSet<FrmTemp>());
			this.out.put(instr, new HashSet<FrmTemp>());
		}
	}
	
	
	private void buildFlowGraph() {
		Map<FrmLabel, AsmInstr> labels = new HashMap<FrmLabel, AsmInstr>();
		
		for(AsmInstr instr : chunk.asmcode)
			if(instr instanceof AsmLABEL)
				labels.put(instr.labels.get(0), instr);
		
		ListIterator<AsmInstr> it = chunk.asmcode.listIterator();
		AsmInstr instr = it.next();
		while(instr != null) {
			AsmInstr next = it.hasNext() ? it.next() : null;
			
			if(instr instanceof AsmLABEL || instr.labels.size() == 0) {
				if(next != null)
					succ.get(instr).add(next);
			}
			else {
				for(FrmLabel label : instr.labels)
					succ.get(instr).add(labels.get(label));
			}
			
			instr = next;
		}
	}
	
	
	private Set<FrmTemp> difference(Collection<FrmTemp> a, Collection<FrmTemp> b) {
		Set<FrmTemp> r = new HashSet<FrmTemp>();
		r.addAll(a);
		r.removeAll(b);
		return r;
	}
	private void solveInOut() {
		boolean change;
		do {
			change = false;
			ListIterator<AsmInstr> it = chunk.asmcode.listIterator(chunk.asmcode.size());
			while(it.hasPrevious()) {
				AsmInstr instr = it.previous();
				Set<FrmTemp> tmp;
				
				tmp = new HashSet<FrmTemp>();
				tmp.addAll(instr.uses);
				tmp.addAll(difference( out.get(instr) , instr.defs ));
				if( !in.get(instr).containsAll(tmp) ) {
					in.get(instr).addAll(tmp);
					change = true;
				}
				
				tmp = new HashSet<FrmTemp>();
				for(AsmInstr s : succ.get(instr))
					tmp.addAll(in.get(s));
				if( !out.get(instr).containsAll(tmp) ) {
					out.put(instr, tmp);
					change = true;
				}
			}
			
		} while(change);
	}
	
	private InterferenceGraph buildInterferenceGraph() {
		InterferenceGraph g = new InterferenceGraph();
		
		g.addNode(chunk.frame.FP);
		
		for(AsmInstr instr : chunk.asmcode) {
			for(FrmTemp a : instr.defs) {
				g.addNode(a);
				for(FrmTemp b : out.get(instr)) {
					if( !(instr instanceof AsmMOVE && b == instr.uses.get(0)) )
						g.addEdge(a, b);
				}
			}
		}
		
		/* TOLE DODA VSE
		for(AsmInstr instr : chunk.asmcode) {
			for(FrmTemp a : instr.defs) {
				g.addNode(a);
			}
		}
		
		for(AsmInstr instr : chunk.asmcode) {
			for(FrmTemp a : instr.defs) {
				for(InterferenceGraph.Node b : g.nodes) {
					if(a != b.val)
						g.addEdge(a, b.val);
				}
			}
		}*/
		
		return g;
	}
	
	public InterferenceGraph analyse() {
		buildFlowGraph();
		
		
		out.get(chunk.asmcode.get(chunk.asmcode.size()-1)).add(chunk.frame.RV);
		solveInOut();
		
		return buildInterferenceGraph();
	}
}
