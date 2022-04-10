package compiler.regalloc;

import java.util.*;

import compiler.*;
import compiler.imcode.*;
import compiler.asmcode.*;

public class RegAlloc {
	private boolean dump;
	
	public RegAlloc(boolean dump) {
		this.dump = dump;
	}
	
	public void allocate(LinkedList<ImcChunk> chunks) {
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				ImcCodeChunk codeChunk = (ImcCodeChunk)chunk;
				
				RegAllocator regAllocator = new RegAllocator(codeChunk);
				
				codeChunk.registerAllocation = regAllocator.allocate();
				
			}
		}
	}
	
	public void dump(LinkedList<ImcChunk> chunks) {
		if(!this.dump)
			return;
		if(Report.dumpFile() == null)
			return;
		
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				ImcCodeChunk codeChunk = (ImcCodeChunk)chunk;
				for(AsmInstr instr : (codeChunk).asmcode) {
					Report.dump(0, instr.format(codeChunk.registerAllocation));
				}
			}
		}
	}
	
}
