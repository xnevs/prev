package compiler.asmcode;

import java.util.LinkedList;

import compiler.Report;
import compiler.imcode.*;

public class AsmCode{
	
	public static final int NUM_LOCAL_REGS = 6;
	
	private boolean dump;
	
	public AsmCode(boolean dump) {
		this.dump = dump;
	}
	
	public void generate(LinkedList<ImcChunk> chunks) {
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				AsmCodeGenerator codeGen = new AsmCodeGenerator((ImcCodeChunk)chunk);
				codeGen.generate();
			}
		}
	}

	public void dump(LinkedList<ImcChunk> chunks) {
		if(!dump)
			return;
		if(Report.dumpFile() == null)
			return;
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				Report.dump(0,"% CHUNK %%%%%%%%%%%%%%%%%%%%%%%%%");
				chunk.dump();
				Report.dump(0,"% ASM %%%%%%%%%%%%%%%%%%%%%%%%%%%");
				ImcCodeChunk codeChunk = (ImcCodeChunk)chunk;
				for(AsmInstr instr : codeChunk.asmcode) {
					Report.dump(0, instr.format(null));
				}
				Report.dump(0,"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
				Report.dump(0, "");
				Report.dump(0, "");
			}
		}
	}
}
