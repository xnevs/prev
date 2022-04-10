package compiler;

import java.util.*;

import compiler.abstr.tree.*;

public class StandardLibrary {
	
	private static LinkedList<AbsType> L(AbsType... types) {
		LinkedList<AbsType> l = new LinkedList<AbsType>();
		for(AbsType type : types)
			l.add(type);
		return l;
	}
	
	private static AbsFunDef function(String name, int returnType, LinkedList<AbsType> parTypes) {
				
		Vector<AbsPar> pars = new Vector<AbsPar>();
		
		int i = 0;
		for(AbsType parType : parTypes) {
			pars.add(new AbsPar(null, "p"+i, parType));
			i++;
		}

		AbsType type = new AbsAtomType(null, returnType);
		
		AbsExpr expr;
		if(returnType == AbsAtomType.INT)
			expr = new AbsAtomConst(null, AbsAtomConst.INT, "0");
		else
			expr = new AbsAtomConst(null, AbsAtomConst.STR, "0");
		
		return new AbsFunDef(null, name, pars, type, expr);
	}
	
	public static AbsDefs standardDefs() {
		Vector<AbsDef> defs = new Vector<AbsDef>();
		
		AbsType integerType = new AbsAtomType(null, AbsAtomType.INT);
		AbsType stringType = new AbsAtomType(null, AbsAtomType.STR);
		
		defs.add( function("get_int", AbsAtomType.INT, L(integerType)) );
		defs.add( function("put_int", AbsAtomType.INT, L(integerType)) );
		defs.add( function("put_nl", AbsAtomType.INT, L(integerType)) );
		

		defs.add( function("get_str", AbsAtomType.STR, L(new AbsAtomType(null, AbsAtomType.STR))) );
		defs.add( function("put_str", AbsAtomType.INT, L(stringType)) );
		
		defs.add( function("malloc", AbsAtomType.STR, L(integerType)) );
		defs.add( function("free", AbsAtomType.STR, L(integerType)) );

		defs.add( function("get_char_at", AbsAtomType.INT, L(stringType, integerType)) );
		defs.add( function("put_char_at", AbsAtomType.INT, L(stringType, integerType, integerType)) );
		
		return new AbsDefs(null, defs);	
	}
}
