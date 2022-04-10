package compiler.imcode;

import java.util.*;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.AbsArrType;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsComp;
import compiler.abstr.tree.AbsCompName;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsFor;
import compiler.abstr.tree.AbsFunCall;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsIfThen;
import compiler.abstr.tree.AbsIfThenElse;
import compiler.abstr.tree.AbsPar;
import compiler.abstr.tree.AbsPtrType;
import compiler.abstr.tree.AbsRecType;
import compiler.abstr.tree.AbsTypeDef;
import compiler.abstr.tree.AbsTypeName;
import compiler.abstr.tree.AbsUnExpr;
import compiler.abstr.tree.AbsVarDef;
import compiler.abstr.tree.AbsVarName;
import compiler.abstr.tree.AbsWhere;
import compiler.abstr.tree.AbsWhile;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.frames.FrmAccess;
import compiler.frames.FrmLocAccess;
import compiler.frames.FrmParAccess;
import compiler.frames.FrmVarAccess;
import compiler.frames.FrmTemp;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemArrType;
import compiler.seman.type.SemRecType;

public class ImcCodeGen implements Visitor {

	public LinkedList<ImcChunk> chunks;
	
	private FrmFrame frame;
	private ImcCode returnCode;
	
	public ImcCodeGen() {
		chunks = new LinkedList<ImcChunk>();
	}
	
	
	private ImcStmt makeStatement(ImcCode code) {
		if(code instanceof ImcStmt)
			return (ImcStmt)code;
		else
			return new ImcEXP((ImcExpr)code);
	}
	
	private ImcExpr reachLevel(int diff) {
		if(diff == 0)
			return new ImcTEMP(frame.FP);
		else
			return new ImcMEM(reachLevel(diff - 1));
	}
	
	private ImcExpr ESEQremoveMEM(ImcExpr expr) {
		if(expr instanceof ImcESEQ) {
			ImcESEQ eseq = (ImcESEQ)expr;
			return new ImcESEQ(eseq.stmt, ESEQremoveMEM(eseq.expr));
		}
		else {
			return ((ImcMEM)expr).expr;   //velja, ker je preveril TypeChecker
		}
	}
	
	public void visit(AbsArrType arrType) {
		//void
	}
	
	public void visit(AbsAtomConst atomConst) {
		String strValue = atomConst.value;
		if(atomConst.type == AbsAtomConst.INT) {
			returnCode = new ImcCONST(Long.parseLong(strValue));
			return;
		}
		else if(atomConst.type == AbsAtomConst.LOG) {
			returnCode = new ImcCONST((strValue.equals("true") ? 1L : 0L));
			return;
		}
		else {  //string
			FrmLabel stringLabel = FrmLabel.newLabel();
			ImcDataChunk stringChunk = new ImcDataChunk(stringLabel, strValue.length()-1);
			
			if(atomConst.value.equals("''"))
				stringChunk.value = "0";
			else
				stringChunk.value = "\""+atomConst.value.substring(1,atomConst.value.length()-1) + "\",0";
			chunks.add(stringChunk);
			returnCode = new ImcNAME(stringLabel);
			return;
		}
	}
	
	public void visit(AbsAtomType atomType) {
		//void
	}
	
	private int calcOffset(SemRecType recType, String compName) {
		int offset = 0;
		for(int i=0; i<recType.getNumComps(); i++) {
			if(recType.getCompName(i).equals(compName))
				return offset;
			else
				offset += recType.getCompType(i).size();
		}
		return -1;
	}
	private boolean checkLvalue(ImcExpr expr) {
		if(expr instanceof ImcESEQ)
			return checkLvalue(((ImcESEQ)expr).expr);
		else
			return expr instanceof ImcMEM; 
	}
	public void visit(AbsBinExpr binExpr) {
		binExpr.expr1.accept(this);
		ImcExpr expr1Code = (ImcExpr)returnCode;
		binExpr.expr2.accept(this);
		ImcExpr expr2Code = (ImcExpr)returnCode;
		
		switch(binExpr.oper) {
		case AbsBinExpr.IOR:
			returnCode = new ImcBINOP(ImcBINOP.OR, expr1Code, expr2Code);
			break;
		case AbsBinExpr.AND:
			returnCode = new ImcBINOP(ImcBINOP.AND, expr1Code, expr2Code);
			break;
		case AbsBinExpr.EQU:
			returnCode = new ImcBINOP(ImcBINOP.EQU, expr1Code, expr2Code);
			break;
		case AbsBinExpr.NEQ:
			returnCode = new ImcBINOP(ImcBINOP.NEQ, expr1Code, expr2Code);
			break;
		case AbsBinExpr.LEQ:
			returnCode = new ImcBINOP(ImcBINOP.LEQ, expr1Code, expr2Code);
			break;
		case AbsBinExpr.GEQ:
			returnCode = new ImcBINOP(ImcBINOP.GEQ, expr1Code, expr2Code);
			break;
		case AbsBinExpr.LTH:
			returnCode = new ImcBINOP(ImcBINOP.LTH, expr1Code, expr2Code);
			break;
		case AbsBinExpr.GTH:
			returnCode = new ImcBINOP(ImcBINOP.GTH, expr1Code, expr2Code);
			break;
		case AbsBinExpr.ADD:
			returnCode = new ImcBINOP(ImcBINOP.ADD, expr1Code, expr2Code);
			break;
		case AbsBinExpr.SUB:
			returnCode = new ImcBINOP(ImcBINOP.SUB, expr1Code, expr2Code);
			break;
		case AbsBinExpr.MUL:
			returnCode = new ImcBINOP(ImcBINOP.MUL, expr1Code, expr2Code);
			break;
		case AbsBinExpr.DIV:
			returnCode = new ImcBINOP(ImcBINOP.DIV, expr1Code, expr2Code);
			break;
		case AbsBinExpr.MOD: {
			ImcTEMP temp1Code = new ImcTEMP(new FrmTemp());
			ImcTEMP temp2Code = new ImcTEMP(new FrmTemp());
			
			ImcSEQ mod = new ImcSEQ();
			mod.stmts.add(new ImcMOVE(temp1Code, expr1Code));
			mod.stmts.add(new ImcMOVE(temp2Code, expr2Code));
			
			returnCode = new ImcESEQ(mod, new ImcBINOP(ImcBINOP.SUB,
					                              temp1Code,
					                              new ImcBINOP(ImcBINOP.MUL,
					                            		  new ImcBINOP(ImcBINOP.DIV, temp1Code, temp2Code),
					                            		  temp2Code)));
			
			break;
		}
		case AbsBinExpr.DOT: {
			ImcExpr recCode = ESEQremoveMEM(expr1Code);
			
			long compOffset = calcOffset((SemRecType)SymbDesc.getType(binExpr.expr1).actualType(), ((AbsCompName)binExpr.expr2).name);
			
			returnCode = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, recCode, new ImcCONST(compOffset)));
			
			 break;
		}
		case AbsBinExpr.ARR: {
			ImcExpr arrCode = ESEQremoveMEM(expr1Code);
			long elSize = ((SemArrType)SymbDesc.getType(binExpr.expr1).actualType()).type.size();
			returnCode = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, arrCode, new ImcBINOP(ImcBINOP.MUL, new ImcCONST(elSize), expr2Code)));
			break;
		}
		case AbsBinExpr.ASSIGN:{
			if(!checkLvalue(expr1Code))
				Report.error(binExpr.expr1.position, "lvalue required");
			
			ImcExpr tempCode = new ImcTEMP(new FrmTemp());
			
			ImcSEQ assign = new ImcSEQ();
			assign.stmts.add( new ImcMOVE(tempCode, ESEQremoveMEM(expr1Code)) );
			assign.stmts.add( new ImcMOVE(new ImcMEM(tempCode), expr2Code) );
			
			returnCode = new ImcESEQ(assign, new ImcMEM(tempCode));
			break;
		}
		}
	}
	
	public void visit(AbsComp comp) {
		//void
	}
	
	public void visit(AbsCompName compName) {
		//void
	}
	
	public void visit(AbsDefs defs) {
		for(int i=0; i<defs.numDefs(); i++)
			defs.def(i).accept(this);
	}
	
	public void visit(AbsExprs exprs) {
		ImcSEQ exprsCode = new ImcSEQ();
		for(int i=0; i<exprs.numExprs()-1; i++) {
			exprs.expr(i).accept(this);
			ImcStmt exprCode = makeStatement(returnCode);
			exprsCode.stmts.add(exprCode);
		}
		
		exprs.expr(exprs.numExprs()-1).accept(this);
		if(returnCode instanceof ImcExpr) {
			ImcExpr lastExprCode = (ImcExpr)returnCode;
			returnCode = new ImcESEQ(exprsCode, lastExprCode);
		}
		else {
			exprsCode.stmts.add(makeStatement(returnCode));
			returnCode = exprsCode;
		}
	}
	
	public void visit(AbsFor forStmt) { //TODO
		forStmt.count.accept(this);
		ImcExpr countCode = (ImcExpr)returnCode;
		forStmt.lo.accept(this);
		ImcExpr loCode = (ImcExpr)returnCode;
		forStmt.hi.accept(this);
		ImcExpr hiCode = (ImcExpr)returnCode;
		forStmt.step.accept(this);
		ImcExpr stepCode = (ImcExpr)returnCode;
		
		forStmt.body.accept(this);
		ImcStmt bodyCode = makeStatement(returnCode);
		
		FrmLabel loopLabel = FrmLabel.newLabel();
		FrmLabel trueLabel = FrmLabel.newLabel();
		FrmLabel falseLabel = FrmLabel.newLabel();
		
		ImcExpr tempHi = new ImcTEMP(new FrmTemp());
		ImcExpr tempStep = new ImcTEMP(new FrmTemp());
		
		ImcSEQ forCode = new ImcSEQ();
		forCode.stmts.add(new ImcMOVE(countCode, loCode));
		forCode.stmts.add(new ImcMOVE(tempHi, hiCode));
		forCode.stmts.add(new ImcMOVE(tempStep, stepCode));
		forCode.stmts.add(new ImcLABEL(loopLabel));
		forCode.stmts.add(new ImcCJUMP(
				new ImcBINOP(ImcBINOP.LTH, countCode, tempHi),
				trueLabel,
				falseLabel)
				);
		forCode.stmts.add(new ImcLABEL(trueLabel));
		forCode.stmts.add(bodyCode);
		forCode.stmts.add(new ImcMOVE(countCode, new ImcBINOP(ImcBINOP.ADD, countCode, tempStep)));
		forCode.stmts.add(new ImcJUMP(loopLabel));
		forCode.stmts.add(new ImcLABEL(falseLabel));
		
		returnCode = forCode;
	}
	
	public void visit(AbsFunCall funCall) {
		AbsFunDef funDef = (AbsFunDef)SymbDesc.getNameDef(funCall);
		
		FrmFrame funFrame = FrmDesc.getFrame(funDef);
		
		ImcCALL funCallCode = new ImcCALL(funFrame.label);
		
		funCallCode.args.add(reachLevel(frame.level - funFrame.level + 1));
		
		for(int i=0; i<funCall.numArgs(); i++) {
			funCall.arg(i).accept(this);
			ImcExpr argCode = (ImcExpr)returnCode;
			funCallCode.args.add(argCode);
		}
		
		returnCode = funCallCode;
	}
	
	public void visit(AbsFunDef funDef) {
		FrmFrame prevFrame = frame;
		frame = FrmDesc.getFrame(funDef);
		
		funDef.expr.accept(this);
		ImcExpr exprCode = (ImcExpr)returnCode;
		
		chunks.add(new ImcCodeChunk(frame, new ImcMOVE(new ImcTEMP(frame.RV), exprCode)));
		
		frame = prevFrame;
	}

	public void visit(AbsIfThen ifThen) {
		ifThen.cond.accept(this);
		ImcExpr condCode = (ImcExpr)returnCode;
		ifThen.thenBody.accept(this);
		ImcStmt thenBodyCode = makeStatement(returnCode);
		
		FrmLabel trueLabel = FrmLabel.newLabel();
		FrmLabel falseLabel = FrmLabel.newLabel();
		
		ImcSEQ ifThenCode = new ImcSEQ();
		ifThenCode.stmts.add(new ImcCJUMP(condCode, trueLabel, falseLabel));
		ifThenCode.stmts.add(new ImcLABEL(trueLabel));
		ifThenCode.stmts.add(thenBodyCode);
		ifThenCode.stmts.add(new ImcLABEL(falseLabel));
		
		returnCode = ifThenCode;
	}
	
	public void visit(AbsIfThenElse ifThenElse) {
		/*
		ifThenElse.cond.accept(this);
		ImcExpr condCode = (ImcExpr)returnCode;
		ifThenElse.thenBody.accept(this);
		ImcStmt thenBodyCode = makeStatement(returnCode);
		ifThenElse.elseBody.accept(this);
		ImcStmt elseBodyCode = makeStatement(returnCode);
		*/
		
		//TODO naloga1
		ifThenElse.cond.accept(this);
		ImcExpr condCode = (ImcExpr)returnCode;
		ifThenElse.thenBody.accept(this);
		ImcExpr thenBodyCode = (ImcExpr)returnCode;
		ifThenElse.elseBody.accept(this);
		ImcExpr elseBodyCode = (ImcExpr)returnCode;
		
		FrmLabel trueLabel = FrmLabel.newLabel();
		FrmLabel falseLabel = FrmLabel.newLabel();
		FrmLabel endLabel = FrmLabel.newLabel();
		
		/*
		ImcSEQ ifThenElseCode = new ImcSEQ();
		ifThenElseCode.stmts.add(new ImcCJUMP(condCode, trueLabel, falseLabel));
		ifThenElseCode.stmts.add(new ImcLABEL(trueLabel));
		ifThenElseCode.stmts.add(thenBodyCode);
		ifThenElseCode.stmts.add(new ImcJUMP(endLabel));
		ifThenElseCode.stmts.add(new ImcLABEL(falseLabel));
		ifThenElseCode.stmts.add(elseBodyCode);
		ifThenElseCode.stmts.add(new ImcLABEL(endLabel));
		*/
		
		//TODO naloga1
		
		ImcTEMP temp = new ImcTEMP(new FrmTemp());
		
		ImcSEQ stmts = new ImcSEQ();
		stmts.stmts.add(new ImcCJUMP(condCode, trueLabel, falseLabel));
		stmts.stmts.add(new ImcLABEL(trueLabel));
		stmts.stmts.add(new ImcMOVE(temp, thenBodyCode));
		stmts.stmts.add(new ImcJUMP(endLabel));
		stmts.stmts.add(new ImcLABEL(falseLabel));
		stmts.stmts.add(new ImcMOVE(temp, elseBodyCode));
		stmts.stmts.add(new ImcLABEL(endLabel));
		

		ImcESEQ ifThenElseCode = new ImcESEQ(stmts, temp);
		
		returnCode = ifThenElseCode;
	}
	
	public void visit(AbsPar par) {
		//void
	}
	
	public void visit(AbsPtrType ptrType) {
		//void
	}
	
	public void visit(AbsRecType recType) {
		//void
	}
	
	public void visit(AbsTypeDef typeDef) {
		//void
	}
	
	public void visit(AbsTypeName typeName) {
		//void
	}
	
	public void visit(AbsUnExpr unExpr) {
		unExpr.expr.accept(this);
		ImcExpr exprCode = (ImcExpr)returnCode;
		
		switch(unExpr.oper) {
		case AbsUnExpr.ADD:
			returnCode = exprCode;
			break;
		case AbsUnExpr.SUB:
			returnCode = new ImcBINOP(ImcBINOP.SUB, new ImcCONST(0L), exprCode);
			break;
		case AbsUnExpr.MEM: {
			if(!checkLvalue(exprCode)) {
				Report.error(unExpr.position, "lvalue required");
			}
			else {
				returnCode = ESEQremoveMEM(exprCode);
			}
			break;
		}
		case AbsUnExpr.VAL:
			returnCode = new ImcMEM(exprCode);
			break;
		case AbsUnExpr.NOT:
			returnCode = new ImcBINOP(ImcBINOP.EQU, new ImcCONST(0L), exprCode);
			break;
		}
	}
	
	public void visit(AbsVarDef varDef) {
		FrmAccess access = FrmDesc.getAccess(varDef);
		if(access instanceof FrmVarAccess) {
			FrmVarAccess varAccess = (FrmVarAccess)access;
			int size = SymbDesc.getType(varDef).size();
			
			boolean ze = false;
			for(ImcChunk chunk : chunks) {
				if(chunk instanceof ImcDataChunk) {
					if(((ImcDataChunk) chunk).label.name().equals(varAccess.label.name()))
						ze = true;
				}
			}
			
			if(!ze)
				chunks.add(new ImcDataChunk(varAccess.label, size));
		}
	}
	
	public void visit(AbsVarName varName) {
		FrmAccess access = FrmDesc.getAccess(SymbDesc.getNameDef(varName));
		if(access instanceof FrmVarAccess) {
			returnCode = new ImcMEM(new ImcNAME(((FrmVarAccess)access).label));
		}
		else if(access instanceof FrmLocAccess) {
			FrmLocAccess varAccess = (FrmLocAccess)access;
			int level = varAccess.frame.level;
			returnCode = new ImcMEM( new ImcBINOP(ImcBINOP.ADD, reachLevel(frame.level - level), new ImcCONST((long) varAccess.offset)) );
		}
		else if(access instanceof FrmParAccess) {
			FrmParAccess varAccess = (FrmParAccess)access;
			int level = varAccess.frame.level;
			returnCode = new ImcMEM( new ImcBINOP(ImcBINOP.ADD, reachLevel(frame.level - level), new ImcCONST((long) varAccess.offset)) );
		}
	}
	
	public void visit(AbsWhere where) {
		where.defs.accept(this);
		where.expr.accept(this);
	}
	
	public void visit(AbsWhile whileStmt) {
		whileStmt.cond.accept(this);
		ImcExpr condCode = (ImcExpr)returnCode;
		whileStmt.body.accept(this);
		ImcStmt bodyCode = makeStatement(returnCode);
		
		FrmLabel loopLabel = FrmLabel.newLabel();
		FrmLabel trueLabel = FrmLabel.newLabel();
		FrmLabel falseLabel = FrmLabel.newLabel();
		
		ImcSEQ whileCode = new ImcSEQ();
		whileCode.stmts.add(new ImcLABEL(loopLabel));
		whileCode.stmts.add(new ImcCJUMP(condCode, trueLabel, falseLabel));
		whileCode.stmts.add(new ImcLABEL(trueLabel));
		whileCode.stmts.add(bodyCode);
		whileCode.stmts.add(new ImcJUMP(loopLabel));
		whileCode.stmts.add(new ImcLABEL(falseLabel));
		
		returnCode = whileCode;
	}
}
