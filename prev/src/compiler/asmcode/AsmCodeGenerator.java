package compiler.asmcode;

import java.util.*;

import compiler.Report;
import compiler.frames.*;
import compiler.imcode.*;


public class AsmCodeGenerator {
	
	public ImcCodeChunk chunk;
	
	public AsmCodeGenerator(ImcCodeChunk chunk) {
		this.chunk = chunk;
	}
	
	public void generate() {
		chunk.lincode.accept(this);
	}
	
	private LinkedList<FrmTemp> L(FrmTemp... temps) {
		LinkedList<FrmTemp> l = new LinkedList<FrmTemp>();
		for(FrmTemp temp : temps)
			l.add(temp);
		return l;
	}
	private LinkedList<FrmLabel> L(FrmLabel... labels) {
		LinkedList<FrmLabel> l = new LinkedList<FrmLabel>();
		for(FrmLabel label : labels)
			l.add(label);
		return l;
	}
	
	public FrmTemp munch(ImcBINOP imcode) {
		FrmTemp result = new FrmTemp();
		
		switch(imcode.op) {
		case ImcBINOP.ADD:
			chunk.asmcode.add(new AsmOPER("\tADD\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			break;
		case ImcBINOP.AND:
			chunk.asmcode.add(new AsmOPER("\tSET\t`d0,`s0", L(result), L(imcode.limc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tCSZ\t`d0,`s0,0", L(result), L(imcode.rimc.accept(this))));
			break;
		case ImcBINOP.DIV:
			chunk.asmcode.add(new AsmOPER("\tDIV\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			break;
		case ImcBINOP.EQU:
			chunk.asmcode.add(new AsmOPER("\tXOR\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSZ\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.GEQ:
			chunk.asmcode.add(new AsmOPER("\tCMP\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSNN\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.GTH:
			chunk.asmcode.add(new AsmOPER("\tCMP\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSP\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.LEQ:
			chunk.asmcode.add(new AsmOPER("\tCMP\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSNP\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.LTH:
			chunk.asmcode.add(new AsmOPER("\tCMP\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSN\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.MUL:
			chunk.asmcode.add(new AsmOPER("\tMUL\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			break;
		case ImcBINOP.NEQ:
			chunk.asmcode.add(new AsmOPER("\tXOR\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSNZ\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.OR:
			chunk.asmcode.add(new AsmOPER("\tOR\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			chunk.asmcode.add(new AsmOPER("\tZSNZ\t`d0,`s0,1", L(result), L(result)));
			break;
		case ImcBINOP.SUB:
			chunk.asmcode.add(new AsmOPER("\tSUB\t`d0,`s0,`s1", L(result), L(imcode.limc.accept(this), imcode.rimc.accept(this))));
			break;
		}
		return result;
	}
	public FrmTemp munch(ImcCALL imcode) {  //odlocil sem se, da SP kaze na polno mesto
		FrmTemp result = new FrmTemp();
		
		ListIterator<ImcExpr> it = imcode.args.listIterator(imcode.args.size());
		while(it.hasPrevious()) {
			chunk.asmcode.add(new AsmOPER("\tSUB\t$250,$250,8", null, null));
			chunk.asmcode.add(new AsmOPER("\tSTO\t`s0,$250,0", null, L(it.previous().accept(this))));
		}
		
		chunk.asmcode.add(new AsmOPER("\tPUSHJ\t$"+AsmCode.NUM_LOCAL_REGS+","+imcode.label.name(), null, null));
		
		//result
		chunk.asmcode.add(new AsmOPER("\tLDO\t`d0,$250,0", L(result), null));
		
		chunk.asmcode.add(new AsmOPER("\tADD\t$250,$250,"+Integer.toString(imcode.args.size()*8), null, null));
		
		return result;
	}
	public FrmTemp munch(ImcCJUMP imcode) {
		chunk.asmcode.add(new AsmOPER("\tBNZ\t`s0,`l0", null, L(imcode.cond.accept(this)), L(imcode.trueLabel, imcode.falseLabel)));
		return null;
	}
	public FrmTemp munch(ImcCONST imcode) {
		FrmTemp result = new FrmTemp();
		Long val = imcode.value & 0xFFFF;
		chunk.asmcode.add(new AsmOPER("\tSETL\t`d0,"+val.toString(), L(result), null));
		val = (imcode.value >> 16) & 0xFFFF;
		if(val != 0) chunk.asmcode.add(new AsmOPER("\tORML\t`d0,"+val.toString(), L(result), null));
		val = (imcode.value >> 32) & 0xFFFF;
		if(val != 0) chunk.asmcode.add(new AsmOPER("\tORMH\t`d0,"+val.toString(), L(result), null));
		val = (imcode.value >> 48) & 0xFFFF;
		if(val != 0) chunk.asmcode.add(new AsmOPER("\tORH\t`d0,"+val.toString(), L(result), null));
		return result;
	}
	public FrmTemp munch(ImcESEQ imcode) {
		Report.error("Internal error: ImcESEQ.");
		return null;
	}
	public FrmTemp munch(ImcEXP imcode) {
		imcode.expr.accept(this);
		return null;
	}
	public FrmTemp munch(ImcJUMP imcode) {
		chunk.asmcode.add(new AsmOPER("\tJMP\t`l0", null, null, L(imcode.label)));
		return null;
	}
	public FrmTemp munch(ImcLABEL imcode) {
		chunk.asmcode.add(new AsmLABEL(imcode.label.name(), imcode.label));
		return null;
	}
	public FrmTemp munch(ImcMEM imcode) { //predvidevam, da ni levo od MOVE
		FrmTemp result = new FrmTemp();
		chunk.asmcode.add(new AsmOPER("\tLDO\t`d0,`s0,0", L(result), L(imcode.expr.accept(this))));
		return result;
	}
	public FrmTemp munch(ImcMOVE imcode) {
		if(imcode.dst instanceof ImcMEM) {
			ImcMEM dst = (ImcMEM)imcode.dst;
			chunk.asmcode.add(new AsmOPER("\tSTO\t`s0,`s1,0", null, L(imcode.src.accept(this), dst.expr.accept(this))));
		}
		else {
			chunk.asmcode.add(new AsmMOVE("\tSET\t`d0,`s0", imcode.src.accept(this), imcode.dst.accept(this)));
		}
		return null;
	}
	public FrmTemp munch(ImcNAME imcode) {
		FrmTemp result = new FrmTemp();
		chunk.asmcode.add(new AsmOPER("\tLDA\t`d0,"+imcode.label.name(), L(result), null));
		return result;
	}
	public FrmTemp munch(ImcSEQ imcode) {
		if(chunk.asmcode != null)
			Report.error("Internal error: ImcSEQ.");
		chunk.asmcode = new LinkedList<AsmInstr>();
		for(ImcStmt stmt : imcode.stmts)
			stmt.accept(this);
		return null;
	}
	public FrmTemp munch(ImcTEMP imcode) {
		return imcode.temp;
	}
}
