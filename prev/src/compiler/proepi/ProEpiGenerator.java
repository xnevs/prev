package compiler.proepi;

import java.util.*;

import compiler.imcode.*;
import compiler.asmcode.*;

public class ProEpiGenerator {
	
	private ImcCodeChunk chunk;
	
	public ProEpiGenerator(ImcCodeChunk chunk) {
		this.chunk = chunk;
	}
	
	public void generate() {
		
		String RAOffset = Integer.toString(chunk.frame.sizeLocs+16);
		String frameSize = Integer.toString(chunk.frame.size());
		String RV = chunk.registerAllocation.get(chunk.frame.RV);
		
		LinkedList<AsmInstr> prologue = new LinkedList<AsmInstr>();
		prologue.add(new AsmLABEL(chunk.frame.label.name(), chunk.frame.label));
		prologue.add(new  AsmOPER("\tSET\t$0,"+RAOffset, null, null));
		prologue.add(new  AsmOPER("\tSUB\t$0,$250,$0", null, null));
		prologue.add(new  AsmOPER("\tSTO\t$251,$0,8", null, null));
		prologue.add(new  AsmOPER("\tGET\t$1,rJ", null, null));
		prologue.add(new  AsmOPER("\tSTO\t$1,$0,0", null, null));
		prologue.add(new  AsmOPER("\tSET\t$251,$250", null, null));
		prologue.add(new  AsmOPER("\tSET\t$0,"+frameSize, null, null));
		prologue.add(new  AsmOPER("\tSUB\t$250,$250,$0", null, null));
		prologue.add(new  AsmOPER("% END prolog", null, null));
		
		LinkedList<AsmInstr> epilogue = new LinkedList<AsmInstr>();
		epilogue.add(new  AsmOPER("% START epilog", null, null));
		epilogue.add(new  AsmOPER("\tSTO\t"+RV+",$251,0", null, null));
		epilogue.add(new  AsmOPER("\tSET\t$0,"+RAOffset, null, null));
		epilogue.add(new  AsmOPER("\tSET\t$250,$251", null, null));
		epilogue.add(new  AsmOPER("\tSUB\t$0,$251,$0", null, null));
		epilogue.add(new  AsmOPER("\tLDO\t$251,$0,8", null, null));
		epilogue.add(new  AsmOPER("\tLDO\t$0,$0,0", null, null));
		epilogue.add(new  AsmOPER("\tPUT\trJ,$0", null, null));
		epilogue.add(new  AsmOPER("\tPOP\t0,0", null, null));
		
		
		ListIterator<AsmInstr> it = prologue.listIterator(prologue.size());
		while(it.hasPrevious())
			chunk.asmcode.addFirst(it.previous());
		
		it = epilogue.listIterator();
		while(it.hasNext())
			chunk.asmcode.addLast(it.next());
	}
}
