package compiler.frames;

import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.*;

public class FrmEvaluator implements Visitor {
	
	private int level = 1;
	private FrmFrame frame = null;
	
	public void visit(AbsArrType arrType) {
		arrType.type.accept(this);
	}
	
	public void visit(AbsAtomConst atomConst) {
		//void
	}
	
	public void visit(AbsAtomType atomType) {
		//void
	}
	
	public void visit(AbsBinExpr binExpr) {
		binExpr.expr1.accept(this);
		binExpr.expr2.accept(this);
	}
	
	public void visit(AbsComp comp) { //vse naredim v AbsRecType
		comp.type.accept(this);
	}
	
	public void visit(AbsCompName compName) {
		//void
	}
	
	public void visit(AbsDefs defs) {
		for(int i=0; i<defs.numDefs(); i++)
			defs.def(i).accept(this);
	}
	
	public void visit(AbsExprs exprs) {
		for(int i=0; i<exprs.numExprs(); i++)
			exprs.expr(i).accept(this);
	}
	
	public void visit(AbsFor forStmt) {
		forStmt.count.accept(this);
		forStmt.lo.accept(this);
		forStmt.hi.accept(this);
		forStmt.step.accept(this);
		forStmt.body.accept(this);
	}
	
	public void visit(AbsFunCall funCall) {
		AbsFunDef funDef = (AbsFunDef)SymbDesc.getNameDef(funCall);
		
		for(int i=0; i<funCall.numArgs(); i++)
			funCall.arg(i).accept(this);
		
		int sizeArgs = SymbDesc.getType(funDef).size();
		if(sizeArgs > frame.sizeArgs)
			frame.sizeArgs = sizeArgs;
	}
	
	public void visit(AbsFunDef funDef) {
		FrmFrame prevFrame = frame;
		frame = new FrmFrame(funDef, level);
		FrmDesc.setFrame(funDef, frame);
		level++;
		
		funDef.type.accept(this);
		for(int i=0; i<funDef.numPars(); i++)
			funDef.par(i).accept(this);
		funDef.expr.accept(this);
		
		level--;
		frame = prevFrame;
	}

	public void visit(AbsIfThen ifThen) {
		ifThen.cond.accept(this);
		ifThen.thenBody.accept(this);
	}
	
	public void visit(AbsIfThenElse ifThenElse) {
		ifThenElse.cond.accept(this);
		ifThenElse.thenBody.accept(this);
		ifThenElse.elseBody.accept(this);
	}
	
	public void visit(AbsPar par) {
		par.type.accept(this);
		FrmParAccess access = new FrmParAccess(par, frame);
		FrmDesc.setAccess(par, access);
	}
	
	public void visit(AbsPtrType ptrType) {
		ptrType.type.accept(this);
	}
	
	public void visit(AbsRecType recType) {
		int offset = 0;
		for(int i=0; i<recType.numComps(); i++) {
			AbsComp comp = recType.comp(i);
			comp.accept(this);
			FrmCmpAccess access = new FrmCmpAccess(comp, offset);
			FrmDesc.setAccess(comp, access);
			offset += SymbDesc.getType(comp).size();
		}
	}
	
	public void visit(AbsTypeDef typeDef) {
		typeDef.type.accept(this);
	}
	
	public void visit(AbsTypeName typeName) {
		//void
	}
	
	public void visit(AbsUnExpr unExpr) {
		unExpr.expr.accept(this);
	}
	
	public void visit(AbsVarDef varDef) {
		varDef.type.accept(this);
		if(frame == null) //globalna
			FrmDesc.setAccess(varDef, new FrmVarAccess(varDef));
		else              //lokalna
			if(varDef.name.startsWith("static"))
				FrmDesc.setAccess(varDef, new FrmVarAccess(varDef));
			else
				FrmDesc.setAccess(varDef, new FrmLocAccess(varDef, frame));
	}
	
	public void visit(AbsVarName varName) {
		//void
	}
	
	public void visit(AbsWhere where) {
		where.defs.accept(this);
		where.expr.accept(this);
	}
	
	public void visit(AbsWhile whileStmt) {
		whileStmt.cond.accept(this);
		whileStmt.body.accept(this);
	}
}
