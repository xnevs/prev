package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 */
public class TypeChecker implements Visitor {

	private final SemType integerType = new SemAtomType(SemAtomType.INT);
	private final SemType logicalType = new SemAtomType(SemAtomType.LOG);
	private final SemType stringType = new SemAtomType(SemAtomType.STR);
	private final SemType voidType = new SemAtomType(SemAtomType.VOID);

	//za rekurzivne tipe
	private Map<SemTypeName,Set<SemTypeName>> dependingOn;
	private Map<SemTypeName, Set<SemTypeName>> unresolved;
	private void addUnresolved(SemTypeName type, SemType realType) {
		if(realType instanceof SemTypeName) {
			SemTypeName nameRealType = (SemTypeName)realType;
			if(!dependingOn.containsKey(nameRealType))
				return;
			dependingOn.get(nameRealType).add(type);
			unresolved.get(type).add(nameRealType);
		}
		else if(realType instanceof SemArrType) {
			SemArrType arrRealType = (SemArrType)realType;
			addUnresolved(type, arrRealType.type);
		}
		else if(realType instanceof SemRecType) {
			SemRecType recRealType = (SemRecType)realType;
			for(int i=0; i<recRealType.getNumComps(); i++) {
				SemType compType = recRealType.getCompType(i);
				addUnresolved(type, compType);
			}
		}
	}
	private void resolve(SemTypeName type) {
		if(unresolved.get(type).isEmpty()) {
			for(SemTypeName t : dependingOn.get(type)) {
				Set<SemTypeName> tUnresolved = unresolved.get(t);
				tUnresolved.remove(type);
				resolve(t);
			}
		}
	}
	private boolean checkCyclicTypes(Position position) { //ni ravno najlepsa, se mi pa zdi da dela
		for(SemTypeName t : unresolved.keySet())
			resolve(t);
		
		Set<String> problematicNames = new HashSet<String>();
		for(SemTypeName t : unresolved.keySet()) {
			Set<SemTypeName> tUnresolved = unresolved.get(t);
			if(!tUnresolved.isEmpty())
				problematicNames.add(t.name);
		}
		if(problematicNames.isEmpty())
			return true;
		else {
			StringBuilder sb = new StringBuilder();
			for(String s : problematicNames) {
				sb.append(s);
				sb.append(" ");
			}
			Report.error(position, "Incomplete (cyclic) type definitions detected: " + sb + ".");
			return false;
		}
	}
	
	public void visit(AbsArrType arrType) {
		arrType.type.accept(this);
		
		SemType elementType = SymbDesc.getType(arrType.type);
		
		SymbDesc.setType(arrType, new SemArrType(arrType.length, elementType));
	}
	
	public void visit(AbsAtomConst atomConst) {
		switch(atomConst.type) {
		case SemAtomType.INT: SymbDesc.setType(atomConst, integerType); break;
		case SemAtomType.LOG: SymbDesc.setType(atomConst, logicalType); break;
		case SemAtomType.STR: SymbDesc.setType(atomConst, stringType); break;
		}
	}
	
	public void visit(AbsAtomType atomType) {
		switch(atomType.type) {
		case SemAtomType.INT: SymbDesc.setType(atomType, integerType); break;
		case SemAtomType.LOG: SymbDesc.setType(atomType, logicalType); break;
		case SemAtomType.STR: SymbDesc.setType(atomType, stringType); break;
		}
	}
	
	private SemType checkComp(SemRecType recType, String name) {
		for(int i=0; i<recType.getNumComps(); i++)
			if(recType.getCompName(i).equals(name))
				return recType.getCompType(i);
		return null;
	}
	public void visit(AbsBinExpr binExpr) {
		binExpr.expr1.accept(this);
		binExpr.expr2.accept(this);
		
		SemType type1 = SymbDesc.getType(binExpr.expr1);
		SemType type2 = SymbDesc.getType(binExpr.expr2);
		
		switch(binExpr.oper) {
		case AbsBinExpr.IOR:
		case AbsBinExpr.AND:
			if(type1.sameStructureAs(logicalType) && type2.sameStructureAs(type1))
				SymbDesc.setType(binExpr, logicalType);
			else
				Report.error(binExpr.position, "Operands must be of type LOGICAL.");
			break;
			
		case AbsBinExpr.EQU:
		case AbsBinExpr.NEQ:
		case AbsBinExpr.LEQ:
		case AbsBinExpr.GEQ:
		case AbsBinExpr.LTH:
		case AbsBinExpr.GTH:
			if((   type1.sameStructureAs(logicalType)
			    || type1.sameStructureAs(integerType)
			    || type1.actualType() instanceof SemPtrType)
			   && type2.sameStructureAs(type1))
				SymbDesc.setType(binExpr, logicalType);
			else
				Report.error(binExpr.position, "Operands must be of type LOGICAL, INTEGER, or POINTER.");
			break;
			
		case AbsBinExpr.ADD:
		case AbsBinExpr.SUB:
		case AbsBinExpr.MUL:
		case AbsBinExpr.DIV:
		case AbsBinExpr.MOD:
			if(type1.sameStructureAs(integerType) && type1.sameStructureAs(type2))
				SymbDesc.setType(binExpr, integerType);
			else
				Report.error(binExpr.position, "Operands must be of type INTEGER.");
			break;
			
		case AbsBinExpr.DOT:
			if(type1.actualType() instanceof SemRecType) {
				SemRecType recType = (SemRecType)type1.actualType();
				AbsCompName compName = (AbsCompName)binExpr.expr2;
				SemType compType;
				if((compType = checkComp(recType, compName.name)) != null) {
					SymbDesc.setType(binExpr.expr2, compType);
					SymbDesc.setType(binExpr, compType);
				}
				else
					Report.error(binExpr.position, "Record has no member " + compName.name + ".");
			}
			else
				Report.error(binExpr.expr1.position, "Record expected.");
			break;
			
		case AbsBinExpr.ARR:
			if(type1.actualType() instanceof SemArrType) {
				SemArrType arrType = (SemArrType)type1.actualType();
				if(type2.sameStructureAs(integerType))
					SymbDesc.setType(binExpr, arrType.type);
				else
					Report.error(binExpr.expr2.position, "Array subscript must be an integer.");
			}
			else
				Report.error(binExpr.expr1.position, "Array expected.");
			break;
			
			
		case AbsBinExpr.ASSIGN:
			if((   type1.sameStructureAs(integerType)
			    || type1.sameStructureAs(logicalType)
			    || type1.sameStructureAs(stringType)
			    || type1.actualType() instanceof SemPtrType)
			   && type2.sameStructureAs(type1)) {
				SymbDesc.setType(binExpr, type1);}
			else
				Report.error(binExpr.position, "Cannot assign different type.");
			break;
		}
	}
	
	public void visit(AbsComp comp) {
		comp.type.accept(this);
		
		SemType type = SymbDesc.getType(comp.type);
		
		SymbDesc.setType(comp, type);
	}
	
	public void visit(AbsCompName compName) {
		//void
		//tip ji dam pri visit(AbsBinExpr): DOT
	}
	
	public void visit(AbsDefs defs) {
		dependingOn = new HashMap<SemTypeName,Set<SemTypeName>>();
		unresolved = new HashMap<SemTypeName,Set<SemTypeName>>();
		
		for(int i=0; i<defs.numDefs(); i++) {
			AbsDef def = defs.def(i);
			if(def instanceof AbsTypeDef) {
				SemTypeName type = new SemTypeName(((AbsTypeDef)def).name);
				SymbDesc.setType(def, type);
				
				dependingOn.put(type, new HashSet<SemTypeName>());
				unresolved.put(type, new HashSet<SemTypeName>());
			}
		}
		
		for(int i=0; i<defs.numDefs(); i++) {
			AbsDef def = defs.def(i);
			if(def instanceof AbsVarDef || def instanceof AbsTypeDef)
				def.accept(this);
		}
		for(int i=0; i<defs.numDefs(); i++) {
			AbsDef def = defs.def(i);
			if(def instanceof AbsFunDef) {
				AbsFunDef funDef = (AbsFunDef)def;

				Vector<SemType> parTypes = new Vector<SemType>();
				for(int j=0; j<funDef.numPars(); j++) {
					AbsPar par = funDef.par(j);
					par.accept(this);
					parTypes.add(SymbDesc.getType(par));
				}
				
				funDef.type.accept(this);
				SemType resultType = SymbDesc.getType(funDef.type);
				
				SymbDesc.setType(funDef, new SemFunType(parTypes, resultType));
			}
		}
		
		//preverimo rekurzivnost tipov
		checkCyclicTypes(defs.position);

		for(int i=0; i<defs.numDefs(); i++) {
			AbsDef def = defs.def(i);
			if(def instanceof AbsFunDef)
				def.accept(this);
		}
	}
	
	public void visit(AbsExprs exprs) {
		for(int i=0; i<exprs.numExprs(); i++)
			exprs.expr(i).accept(this);
		
		SemType type = SymbDesc.getType(exprs.expr(exprs.numExprs()-1));
		SymbDesc.setType(exprs, type);
	}
	
	public void visit(AbsFor forStmt) {
		forStmt.count.accept(this);
		forStmt.lo.accept(this);
		forStmt.hi.accept(this);
		forStmt.step.accept(this);
		forStmt.body.accept(this);

		if(   SymbDesc.getType(forStmt.count).sameStructureAs(integerType)
		   && SymbDesc.getType(forStmt.lo).sameStructureAs(integerType)
		   && SymbDesc.getType(forStmt.hi).sameStructureAs(integerType)
		   && SymbDesc.getType(forStmt.step).sameStructureAs(integerType))
			SymbDesc.setType(forStmt, voidType);
		else
			Report.error(forStmt.position, "All 'count', 'lo', 'hi', 'step' must be of type INTEGER.");
	}
	
	public void visit(AbsFunCall funCall) {
		AbsFunDef funDef = (AbsFunDef)SymbDesc.getNameDef(funCall);
		SemFunType funType = (SemFunType)SymbDesc.getType(funDef);
		
		if(funCall.numArgs() != funType.getNumPars())
			Report.error(funCall.position, "Incorrect number of arguments for '" + funCall.name + "'.");
		
		for(int i=0; i<funCall.numArgs(); i++) {
			AbsExpr arg = funCall.arg(i);
			arg.accept(this);
			if(! SymbDesc.getType(arg).sameStructureAs( funType.getParType(i) ))
				Report.error(arg.position, "Argument for '" + funCall.name + "' of incorrect type.");
		}
		
		SymbDesc.setType(funCall, funType.resultType);
	}
	
	public void visit(AbsFunDef funDef) { //je ze v SymbDesc.type
		SemFunType funType = (SemFunType)SymbDesc.getType(funDef);
		
		if(! (   funType.resultType.sameStructureAs(integerType)
		      || funType.resultType.sameStructureAs(logicalType)
		      || funType.resultType.sameStructureAs(stringType)
		      || funType.resultType.actualType() instanceof SemPtrType) )
			Report.error(funDef.position, "Function result type must be atomic or pointer.");
		
		funDef.expr.accept(this);
		
		if(!funType.resultType.sameStructureAs(SymbDesc.getType(funDef.expr)))
			Report.error(funDef.position, "Declared and actual return type of '" + funDef.name + "' do not match.");
	}

	public void visit(AbsIfThen ifThen) {
		ifThen.cond.accept(this);
		ifThen.thenBody.accept(this);
		
		if(SymbDesc.getType(ifThen.cond).sameStructureAs(logicalType))
			SymbDesc.setType(ifThen, voidType);
		else
			Report.error(ifThen.cond.position, "Condition must be of type LOGICAL.");
	}
	
	public void visit(AbsIfThenElse ifThenElse) {
		ifThenElse.cond.accept(this);
		ifThenElse.thenBody.accept(this);
		ifThenElse.elseBody.accept(this);
		
		if(SymbDesc.getType(ifThenElse.cond).sameStructureAs(logicalType))
			{}//SymbDesc.setType(ifThenElse, voidType);
		else
			Report.error(ifThenElse.cond.position, "Condition must be of type LOGICAL.");
		
		//TODO naloga1
		//naloga na vajah
		if(SymbDesc.getType(ifThenElse.thenBody).sameStructureAs(SymbDesc.getType(ifThenElse.elseBody))) {
			SymbDesc.setType(ifThenElse, SymbDesc.getType(ifThenElse.thenBody));
		}
		else {
			Report.error(ifThenElse.elseBody.position, "Then and else must be of the same type.");
		}
	}
	
	public void visit(AbsPar par) {
		par.type.accept(this);
		
		SemType type = SymbDesc.getType(par.type);
		
		if( ! (type.sameStructureAs(integerType) || type.sameStructureAs(logicalType) || type.sameStructureAs(stringType) || type.actualType() instanceof SemPtrType) )
			Report.error(par.position, "Function parameters must be of atomic or pointer type.");
		
		SymbDesc.setType(par, SymbDesc.getType(par.type));
	}
	
	public void visit(AbsPtrType ptrType) {
		ptrType.type.accept(this);
		SymbDesc.setType(ptrType, new SemPtrType(SymbDesc.getType(ptrType.type)));
	}
	
	public void visit(AbsRecType recType) {
		Vector<String> compNames = new Vector<String>();
		Vector<SemType> compTypes = new Vector<SemType>();
		for(int i=0; i<recType.numComps(); i++) {
			AbsComp comp = recType.comp(i);
			comp.accept(this);
			compNames.add(comp.name);
			compTypes.add(SymbDesc.getType(comp));
		}
		SymbDesc.setType(recType, new SemRecType(compNames, compTypes));
	}
	
	public void visit(AbsTypeDef typeDef) { //je ze v SymbDesc.type
		SemTypeName type = (SemTypeName)SymbDesc.getType(typeDef);
		
		typeDef.type.accept(this);
		SemType realType = SymbDesc.getType(typeDef.type);
		
		addUnresolved(type, realType);
		
		type.setType(realType);
	}
	
	public void visit(AbsTypeName typeName) {
		AbsTypeDef typeDef = (AbsTypeDef)SymbDesc.getNameDef(typeName);
		SymbDesc.setType(typeName, SymbDesc.getType(typeDef));
	}
	
	public void visit(AbsUnExpr unExpr) {
		unExpr.expr.accept(this);
		SemType type = SymbDesc.getType(unExpr.expr);
		
		switch(unExpr.oper) {
		case AbsUnExpr.ADD:
		case AbsUnExpr.SUB:
			if(type.sameStructureAs(integerType))
				SymbDesc.setType(unExpr, integerType);
			else
				Report.error(unExpr.expr.position, "Integer expected.");
			break;
		case AbsUnExpr.MEM:
			SymbDesc.setType(unExpr, new SemPtrType(type));
			break;
		case AbsUnExpr.VAL:
			if(type.actualType() instanceof SemPtrType)
				SymbDesc.setType(unExpr, ((SemPtrType)type.actualType()).type);
			else
				Report.error(unExpr.expr.position, "Pointer expected.");
			break;
		case AbsUnExpr.NOT:
			if(type.sameStructureAs(logicalType))
				SymbDesc.setType(unExpr, logicalType);
			else
				Report.error(unExpr.expr.position, "Logical value expected.");
			break;
		}
	}
	
	public void visit(AbsVarDef varDef) {
		varDef.type.accept(this);
		SymbDesc.setType(varDef, SymbDesc.getType(varDef.type));
	}
	
	public void visit(AbsVarName varName) {
		AbsDef def = SymbDesc.getNameDef(varName);
		SymbDesc.setType(varName, SymbDesc.getType(def));
	}
	
	public void visit(AbsWhere where) {
		where.defs.accept(this);
		where.expr.accept(this);
		
		SymbDesc.setType(where, SymbDesc.getType(where.expr));
	}
	
	public void visit(AbsWhile whileStmt) {
		whileStmt.cond.accept(this);
		whileStmt.body.accept(this);
		
		if(SymbDesc.getType(whileStmt.cond).sameStructureAs(logicalType))
			SymbDesc.setType(whileStmt, voidType);
		else
			Report.error(whileStmt.cond.position, "Condition must be of type LOGICAL.");

		SymbDesc.setType(whileStmt, voidType);
	}
}
