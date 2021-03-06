package compiler.imcode;

import compiler.*;
import compiler.asmcode.AsmCodeGenerator;
import compiler.frames.FrmTemp;

/**
 * Izraz kot stavek.
 * 
 */
public class ImcEXP extends ImcStmt {

	/** Izraz.  */
	public ImcExpr expr;

	/**
	 * Ustvari izraz kot stavek.
	 * 
	 * @param expr
	 */
	public ImcEXP(ImcExpr expr) {
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "EXP");
		expr.dump(indent + 2);
	}

	@Override
	public ImcSEQ linear() {
		ImcSEQ lin = new ImcSEQ();
		ImcESEQ linExpr = expr.linear();
		lin.stmts.addAll(((ImcSEQ)linExpr.stmt).stmts);
		lin.stmts.add(new ImcEXP(linExpr.expr));
		return lin;
	}

	
	@Override
	public FrmTemp accept(AsmCodeGenerator codeGen) {
		return codeGen.munch(this);
	}

}

