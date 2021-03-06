package compiler.imcode;

import compiler.*;
import compiler.asmcode.AsmCodeGenerator;
import compiler.frames.FrmTemp;

/**
 * Stavki v izrazu.
 * 
 */
public class ImcESEQ extends ImcExpr {

	/** Stavki.  */
	public ImcStmt stmt;

	/** Vrednost.  */
	public ImcExpr expr;

	/**
	 * Ustvari stavke v izrazu.
	 * 
	 * @param stmt Stavki.
	 * @param expr Izraz.
	 */
	public ImcESEQ(ImcStmt stmt, ImcExpr expr) {
		this.stmt = stmt;
		this.expr = expr;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "ESEQ");
		stmt.dump(indent + 2);
		expr.dump(indent + 2);
	}

	@Override
	public ImcESEQ linear() {
		ImcSEQ linStmt = stmt.linear();
		ImcESEQ linExpr = expr.linear();
		linStmt.stmts.addAll(((ImcSEQ)linExpr.stmt).stmts);
		linExpr.stmt = linStmt;
		return linExpr;
	}

	
	@Override
	public FrmTemp accept(AsmCodeGenerator codeGen) {
		return codeGen.munch(this);
	}
}
