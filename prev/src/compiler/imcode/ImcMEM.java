package compiler.imcode;

import compiler.*;
import compiler.asmcode.AsmCodeGenerator;
import compiler.frames.FrmTemp;

/**
 * Dostop do pomnilnika.
 * 
 */
public class ImcMEM extends ImcExpr {

	/** Opis dostopa do pomnilnika.  */
	public ImcExpr expr;

	/**
	 * Ustvari nov dostop do pomnilnika.
	 * 
	 * @param expr Naslov.
	 */
	public ImcMEM(ImcExpr expr) {
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "MEM");
		expr.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcESEQ lin = expr.linear();
		lin.expr = new ImcMEM(lin.expr);
		return lin;
	}

	
	@Override
	public FrmTemp accept(AsmCodeGenerator codeGen) {
		return codeGen.munch(this);
	}

}
