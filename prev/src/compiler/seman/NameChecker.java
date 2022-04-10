package compiler.seman;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 */
public class NameChecker implements Visitor {

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
	
	public void visit(AbsComp comp) {
		comp.type.accept(this);
	}
	
	public void visit(AbsCompName compName) {
		
	}
	
	private void castAndInsert(AbsDef def) {
		if(def instanceof AbsTypeDef) {
			AbsTypeDef typeDef = (AbsTypeDef) def;
			try {
				SymbTable.ins(typeDef.name, typeDef);
			} catch(SemIllegalInsertException e) {
				AbsDef prevDef = SymbTable.fnd(typeDef.name);
				Report.error(typeDef.position, "Duplicate definitions. Previous definition at [" + prevDef.position.toString() + "].");
			}
		}
		else if(def instanceof AbsFunDef) {
			AbsFunDef funDef = (AbsFunDef) def;
			try {
				SymbTable.ins(funDef.name, funDef);
			} catch(SemIllegalInsertException e) {
				AbsDef prevDef = SymbTable.fnd(funDef.name);
				if(prevDef.position != null)
					Report.error(funDef.position, "Duplicate definitions. Previous definition at [" + prevDef.position.toString() + "].");
				else
					Report.error(funDef.position, "Redefined standard library function.");
			}
		}
		else if(def instanceof AbsVarDef) {
			AbsVarDef varDef = (AbsVarDef) def;
			try {
				SymbTable.ins(varDef.name, varDef);
			} catch(SemIllegalInsertException e) {
				AbsDef prevDef = SymbTable.fnd(varDef.name);
				Report.error(varDef.position, "Duplicate definitions. Previous definition at [" + prevDef.position.toString() + "].");
			}
		}
	}
	public void visit(AbsDefs defs) {
		for(int i=0; i<defs.numDefs(); i++)
			castAndInsert(defs.def(i));
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
		AbsDef def = SymbTable.fnd(funCall.name);
		
		if(def == null)
			Report.error(funCall.position, "No definition for '" + funCall.name + "' found.");
		else if( !(def instanceof AbsFunDef) )
			Report.error(funCall.position, funCall.name + " is not a function");
		else {
			SymbDesc.setNameDef(funCall, def);
			for(int i=0; i<funCall.numArgs(); i++)
				funCall.arg(i).accept(this);
		}
	}
	
	public void visit(AbsFunDef funDef) { //je ze dodana v SymbTable
		funDef.type.accept(this);
		
		SymbTable.newScope();

		for(int i=0; i<funDef.numPars(); i++)
			funDef.par(i).accept(this);
		
		funDef.expr.accept(this);
		
		SymbTable.oldScope();
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
		try {
			SymbTable.ins(par.name, par);
		} catch(SemIllegalInsertException e) {
			AbsDef prevDef = SymbTable.fnd(par.name);
			Report.error(par.position, "Duplicate definitions. Previous definition at [" + prevDef.position.toString() + "].");
		}
		
		par.type.accept(this);
	}
	
	public void visit(AbsPtrType ptrType) {
		ptrType.type.accept(this);
	}
	
	public void visit(AbsRecType recType) {
		for(int i=0; i<recType.numComps(); i++)
			recType.comp(i).accept(this);
	}
	
	public void visit(AbsTypeDef typeDef) { //je ze dodan v SymbTable
		typeDef.type.accept(this);
	}
	
	public void visit(AbsTypeName typeName) {
		AbsDef def = SymbTable.fnd(typeName.name);
		if(def == null)
			Report.error(typeName.position, "No definition for '" + typeName.name + "' found.");
		else if( !(def instanceof AbsTypeDef) )
			Report.error(typeName.position, typeName.name + " is not a type.");
		else
			SymbDesc.setNameDef(typeName, def);
	}
	
	public void visit(AbsUnExpr unExpr) {
		unExpr.expr.accept(this);
	}
	
	public void visit(AbsVarDef varDef) { //ze dodana v SymbTable
		varDef.type.accept(this);
	}
	
	public void visit(AbsVarName varName) {
		AbsDef def = SymbTable.fnd(varName.name);
		if(def == null)
			Report.error(varName.position, "No definition for '" + varName.name + "' found.");
		else if(! (def instanceof AbsVarDef || def instanceof AbsPar))
			Report.error(varName.position, varName.name + " is not a variable or parameter.");
		else
			SymbDesc.setNameDef(varName, def);
	}
	
	public void visit(AbsWhere where) {
		SymbTable.newScope();
		where.defs.accept(this);
		where.expr.accept(this);
		SymbTable.oldScope();
	}
	
	public void visit(AbsWhile whileStmt) {
		whileStmt.cond.accept(this);
		whileStmt.body.accept(this);
	}
}
