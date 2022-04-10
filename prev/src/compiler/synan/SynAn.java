package compiler.synan;

import java.util.Vector;

import compiler.*;
import compiler.Report;
import compiler.lexan.*;
import compiler.abstr.tree.*;

/**
 * Sintaksni analizator.
 * 
 */
public class SynAn {

	/** Leksikalni analizator. */
	private LexAn lexAn;

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	private Symbol symbol;
	
	private Position endPosition;
	
	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param lexAn
	 *            Leksikalni analizator.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public SynAn(LexAn lexAn, boolean dump) {
		this.lexAn = lexAn;
		this.dump = dump;
		
		this.symbol = lexAn.lexAn();
		this.endPosition = null;
	}

	private void nextSymbol() {
		endPosition = symbol.position;
		symbol = lexAn.lexAn();
	}
	private void check(int token) {
		if(symbol.token == token)
			nextSymbol();
		else {
			String token_str = "";
			switch(token) {
			case Token.EOF: token_str = "EOF"; break;
			case Token.IDENTIFIER: token_str = "IDENTIFIER"; break;
			case Token.LOG_CONST: token_str = "LOG_CONST"; break;
			case Token.INT_CONST: token_str = "INT_CONST"; break;
			case Token.STR_CONST: token_str = "STR_CONST"; break;
			case Token.AND: token_str = "&"; break;
			case Token.IOR: token_str = "|"; break;
			case Token.NOT: token_str = "!"; break;
			case Token.EQU: token_str = "=="; break;
			case Token.NEQ: token_str = "!="; break;
			case Token.LTH: token_str = "<"; break;
			case Token.GTH: token_str = ">"; break;
			case Token.LEQ: token_str = "<="; break;
			case Token.GEQ: token_str = ">="; break;
			case Token.MUL: token_str = "*"; break;
			case Token.DIV: token_str = "/"; break;
			case Token.MOD: token_str = "%"; break;
			case Token.ADD: token_str = "+"; break;
			case Token.SUB: token_str = "-"; break;
			case Token.PTR: token_str = "^"; break;
			case Token.LPARENT: token_str = "("; break;
			case Token.RPARENT: token_str = ")"; break;
			case Token.LBRACKET: token_str = "["; break;
			case Token.RBRACKET: token_str = "]"; break;
			case Token.LBRACE: token_str = "{"; break;
			case Token.RBRACE: token_str = "}"; break;
			case Token.DOT: token_str = "."; break;
			case Token.COLON: token_str = ":"; break;
			case Token.SEMIC: token_str = ";"; break;
			case Token.COMMA: token_str = ","; break;
			case Token.ASSIGN: token_str = "="; break;
			case Token.LOGICAL: token_str = "logical"; break;
			case Token.INTEGER: token_str = "integer"; break;
			case Token.STRING: token_str = "string"; break;
			case Token.KW_ARR: token_str = "arr"; break;
			case Token.KW_ELSE: token_str = "else"; break;
			case Token.KW_FOR: token_str = "for"; break;
			case Token.KW_FUN: token_str = "fun"; break;
			case Token.KW_IF: token_str = "if"; break;
			case Token.KW_REC: token_str = "rec"; break;
			case Token.KW_THEN: token_str = "then"; break;
			case Token.KW_TYP: token_str = "typ"; break;
			case Token.KW_VAR: token_str = "var"; break;
			case Token.KW_WHERE: token_str = "where"; break;
			case Token.KW_WHILE: token_str = "while"; break;
			}
			Report.warning(symbol.position, "'" + token_str + "' expected, ignoring and moving on ...");
		}
	}
	private String check_identifier() {
		String name = symbol.lexeme;
		if(symbol.token == Token.IDENTIFIER)
			nextSymbol();
		else
			Report.error(symbol.position, "IDENTIFIER expected, '" + symbol.lexeme + "' found.");
		return name;
	}
	private String check_atom_const(int token) {
		String value = symbol.lexeme;
		if(symbol.token == token)
			nextSymbol();
		else
			Report.error(symbol.position, "ATOM_CONST expexted '" + symbol.lexeme + "' found.");
		return value;
	}
	private int check_int_const_arr() {
		int value = 0;
		if(symbol.token == Token.INT_CONST) {
			try {
				value = Integer.parseInt(symbol.lexeme);
			}
			catch(NumberFormatException e) {
				Report.error(symbol.position, "Not a valid INT_CONST");
			}
			if(value < 0)
				Report.error(symbol.position, "Array length must be non-negative.");
				
			nextSymbol();
		}
		else
			Report.error(symbol.position, "INT_CONST expexted '" + symbol.lexeme + "' found.");
		
		
		return value;
	}
	
	/**
	 * Opravi sintaksno analizo.
	 */
	public AbsTree parse() {
		dump("source --> definitions");
		
		AbsTree source = parse_definitions();
		check(Token.EOF);
		
		return source;
	}
	
	private AbsDefs parse_definitions() {
		dump("definitions --> definition definitions'");
		
		Position begPosition = symbol.position;
		
		Vector<AbsDef> definitions = new Vector<AbsDef>();
		
		definitions.add(parse_definition());
		parse_definitions_(definitions);
		
		return new AbsDefs(new Position(begPosition, endPosition), definitions); 
	}
	private void parse_definitions_(Vector<AbsDef> definitions) {
		switch(symbol.token) {
		case Token.SEMIC:
			dump("definitions' --> ; definition definitions'");
			check(Token.SEMIC);
			definitions.add(parse_definition());
			parse_definitions_(definitions);
			return;
		default:
			dump("definitions' --> EMPTY");
			return;
		}
	}
	
	private AbsDef parse_definition() {
		switch(symbol.token) {
		case Token.KW_TYP:
			dump("definition --> type_definition");
			return parse_type_definition();
		case Token.KW_FUN:
			dump("definition --> function_definition");
			return parse_function_definition();
		case Token.KW_VAR:
			dump("definition --> variable_definition");
			return parse_variable_definition();
		default:
			Report.error(symbol.position, "Definition expected, " + symbol.lexeme + " found.");
			return null;
		}
	}
	
	private AbsTypeDef parse_type_definition() {
		dump("type_definition --> TYP IDENTIFIER : type");
		
		Position begPosition = symbol.position;
		
		check(Token.KW_TYP);
		String name = check_identifier();
		check(Token.COLON);
		AbsType type = parse_type();
		return new AbsTypeDef(new Position(begPosition, endPosition), name, type);
	}
	
	private AbsType parse_type() {
		Position begPosition = symbol.position;
		switch(symbol.token) {
		case Token.IDENTIFIER: {
			dump("type --> IDENTIFIER");
			return new AbsTypeName(begPosition, check_identifier());
		}
		case Token.LOGICAL: {
			dump("type --> LOGICAL");
			check(Token.LOGICAL);
			return new AbsAtomType(begPosition, AbsAtomType.LOG);
		}
		case Token.INTEGER: {
			dump("type --> INTEGER");
			check(Token.INTEGER);
			return new AbsAtomType(begPosition, AbsAtomType.INT);
		}
		case Token.STRING: {
			dump("type --> STRING");
			check(Token.STRING);
			return new AbsAtomType(begPosition, AbsAtomType.STR);
		}
		case Token.KW_ARR: {
			dump("type --> ARR [ INT_CONST ] type");
			check(Token.KW_ARR);
			check(Token.LBRACKET);
			int length = check_int_const_arr();
			check(Token.RBRACKET);
			AbsType type = parse_type();
			return new AbsArrType(new Position(begPosition, endPosition), length, type);
		}
		case Token.KW_REC: {
			dump("type --> REC { components }");
			check(Token.KW_REC);
			check(Token.LBRACE);
			Vector<AbsComp> components = parse_components();
			check(Token.RBRACE);
			return new AbsRecType(new Position(begPosition, endPosition), components);
		}
		case Token.PTR: {
			dump("type --> ^ type");
			check(Token.PTR);
			AbsType type = parse_type();
			return new AbsPtrType(new Position(begPosition, endPosition), type);
		}
		default: {
			Report.error(symbol.position, "Not a valid type");
		}
		}
		return null;
	}
	
	private Vector<AbsComp> parse_components() {
		dump("components --> component components'");
		Vector<AbsComp> components = new Vector<AbsComp>();
		components.add(parse_component());
		parse_components_(components);
		return components;
	}
	private void parse_components_(Vector<AbsComp> components) {
		switch(symbol.token) {
		case Token.COMMA:
			dump("components' --> , component components'");
			check(Token.COMMA);
			components.add(parse_component());
			parse_components_(components);
			return;
		default:
			dump("components' --> EMPTY");
			return;
		}
	}
	
	private AbsComp parse_component() {
		dump("component --> IDENTIFIER : type");
		Position begPosition = symbol.position;
		String name = check_identifier();
		check(Token.COLON);
		AbsType type = parse_type();
		
		return new AbsComp(new Position(begPosition, endPosition), name, type);
	}
	
	private AbsFunDef parse_function_definition() {
		dump("function_definition --> FUN IDENTIFIER ( parameters ) : type = expression");
		Position begPosition = symbol.position;
		
		check(Token.KW_FUN);
		String name = check_identifier();
		check(Token.LPARENT);
		Vector<AbsPar> parameters = parse_parameters();
		check(Token.RPARENT);
		check(Token.COLON);
		AbsType type = parse_type();
		check(Token.ASSIGN);
		AbsExpr expression = parse_expression();
		return new AbsFunDef(new Position(begPosition, endPosition), name, parameters, type, expression);
	}
	
	private Vector<AbsPar> parse_parameters() {
		dump("parameters --> parameter parameters'");
		Vector<AbsPar> parameters = new Vector<AbsPar>();
		parameters.add(parse_parameter());
		parse_parameters_(parameters);
		return parameters;
	}
	private void parse_parameters_(Vector<AbsPar> parameters) {
		switch(symbol.token) {
		case Token.COMMA:
			dump("parameters' --> , parameter parameters'");
			check(Token.COMMA);
			parameters.add(parse_parameter());
			parse_parameters_(parameters);
			return;
		default:
			dump("parameters' --> EMPTY");
			return;
		}
	}
	
	private AbsPar parse_parameter() {
		dump("parameter --> IDENTIFIER : type");
		Position begPosition = symbol.position;
		
		String name = check_identifier();
		check(Token.COLON);
		AbsType type = parse_type();
		return new AbsPar(new Position(begPosition, endPosition), name, type);
	}
	
	private AbsExpr parse_expression() {
		dump("expression --> logical_ior_expression expression'");
		AbsExpr expression = parse_logical_ior_expression();
		return parse_expression_(expression);
	}
	private AbsExpr parse_expression_(AbsExpr expression) {
		switch(symbol.token) {
		case Token.LBRACE:
			dump("expression' --> { WHERE definitions }");
			Position begPosition = expression.position;
			check(Token.LBRACE);
			check(Token.KW_WHERE);
			AbsDefs definitions = parse_definitions();
			check(Token.RBRACE);
			return new AbsWhere(new Position(begPosition, endPosition), expression, definitions);
		default:
			dump("expression' --> EMPTY");
			return expression;
		}
	}
	
	private AbsExpr parse_logical_ior_expression() {
		dump("logical_ior_expression --> logical_and_expression logical_ior_expression'");
		AbsExpr logical_and_expression = parse_logical_and_expression();
		return parse_logical_ior_expression_(logical_and_expression);
	}
	private AbsExpr parse_logical_ior_expression_(AbsExpr expression1) {
		switch(symbol.token) {
		case Token.IOR:
			dump("logical_ior_expression' --> | logical_and_expression logical_ior_expression'");
			Position begPosition = expression1.position;
			check(Token.IOR);
			AbsExpr expression2 = parse_logical_and_expression();
			AbsBinExpr expression = new AbsBinExpr(new Position(begPosition, endPosition), AbsBinExpr.IOR, expression1, expression2);
			return parse_logical_ior_expression_(expression);
		default:
			dump("logical_ior_expression' --> EMPTY");
			return expression1;
		}
	}
	
	private AbsExpr parse_logical_and_expression() {
		dump("logical_and_expression --> compare_expression logical_and_expression'");
		AbsExpr compare_expression = parse_compare_expression();
		return parse_logical_and_expression_(compare_expression);
	}
	private AbsExpr parse_logical_and_expression_(AbsExpr expression1) {
		switch(symbol.token) {
		case Token.AND:
			dump("logical_and_expression' --> & compare_expression logical_and_expression'");
			Position begPosition = expression1.position;
			check(Token.AND);
			AbsExpr expression2 = parse_compare_expression();
			AbsBinExpr expression = new AbsBinExpr(new Position(begPosition, endPosition), AbsBinExpr.AND, expression1, expression2);
			return parse_logical_and_expression_(expression);
		default:
			dump("logical_and_expression' --> EMPTY");
			return expression1;
		}
	}
	
	private AbsExpr parse_compare_expression() {
		dump("compare_expression --> additive_expression compare_expression'");
		AbsExpr additive_expression = parse_additive_expression();
		return parse_compare_expression_(additive_expression);
	}
	private AbsExpr parse_compare_expression_(AbsExpr expression1) { //ta rest ni porteben za LL, pac pa samo za lep izpis
		Position begPosition = expression1.position;
		int operator = -1;
		
		switch(symbol.token) {
		case Token.EQU:
			dump("compare_expression' --> == additive_expression");
			check(Token.EQU);
			operator = AbsBinExpr.EQU;
			break;
		case Token.NEQ:
			dump("compare_expression' --> != additive_expression");
			check(Token.NEQ);
			operator = AbsBinExpr.NEQ;
			break;
		case Token.LEQ:
			dump("compare_expression' --> <= additive_expression");
			check(Token.LEQ);
			operator = AbsBinExpr.LEQ;
			break;
		case Token.GEQ:
			dump("compare_expression' --> >= additive_expression");
			check(Token.GEQ);
			operator = AbsBinExpr.GEQ;
			break;
		case Token.LTH:
			dump("compare_expression' --> < additive_expression");
			check(Token.LTH);
			operator = AbsBinExpr.LTH;
			break;
		case Token.GTH:
			dump("compare_expression' --> > additive_expression");
			check(Token.GTH);
			operator = AbsBinExpr.GTH;
			break;
		default:
			dump("compare_expression' --> EMPTY");
			return expression1;
		}
		
		AbsExpr expression2 = parse_additive_expression();
		
		return new AbsBinExpr(new Position(begPosition, endPosition), operator, expression1, expression2);
	}

	private AbsExpr parse_additive_expression() {
		dump("additive_expression --> multiplicative_expression additive_expression'");
		AbsExpr multiplicative_expression = parse_multiplicative_expression();
		return parse_additive_expression_(multiplicative_expression);
	}
	private AbsExpr parse_additive_expression_(AbsExpr expression1) {
		Position begPosition = expression1.position;
		int operator = -1;
		
		switch(symbol.token) {
		case Token.ADD:
			dump("additive_expression' --> + multiplicative_expression additive_expression'");
			check(Token.ADD);
			operator = AbsBinExpr.ADD;
			break;
		case Token.SUB:
			dump("additive_expression' --> - multiplicative_expression additive_expression'");
			check(Token.SUB);
			operator = AbsBinExpr.SUB;
			break;
		default:
			dump("additive_expression' --> EMPTY");
			return expression1;
		}
		AbsExpr expression2 = parse_multiplicative_expression();
		
		AbsBinExpr expression = new AbsBinExpr(new Position(begPosition, endPosition), operator, expression1, expression2);
		return parse_additive_expression_(expression);
	}
	
	private AbsExpr parse_multiplicative_expression() {
		dump("multiplicative_expression --> prefix_expression multiplicative_expression'");
		AbsExpr prefix_expression = parse_prefix_expression();
		return parse_multiplicative_expression_(prefix_expression);
	}
	private AbsExpr parse_multiplicative_expression_(AbsExpr expression1) {
		Position begPosition = expression1.position;
		int operator = -1;
		
		switch(symbol.token) {
		case Token.MUL:
			dump("multiplicative_expression' --> * prefix_expression multiplicative_expression'");
			check(Token.MUL);
			operator = AbsBinExpr.MUL;
			break;
		case Token.DIV:
			dump("multiplicative_expression' --> / prefix_expression multiplicative_expression'");
			check(Token.DIV);
			operator = AbsBinExpr.DIV;
			break;
		case Token.MOD:
			dump("multiplicative_expression' --> % prefix_expression multiplicative_expression'");
			check(Token.MOD);
			operator = AbsBinExpr.MOD;
			break;
		default:
			dump("multiplicative_expression' --> EMPTY");
			return expression1;
		}
		
		AbsExpr expression2 = parse_prefix_expression();
		AbsBinExpr expression = new AbsBinExpr(new Position(begPosition, endPosition), operator, expression1, expression2);
		return parse_multiplicative_expression_(expression);
	}
	
	private AbsExpr parse_prefix_expression() {
		Position begPosition = symbol.position;
		int operator = -1;
		
		switch(symbol.token) {
		case Token.ADD:
			dump("prefix_expression --> + prefix_expression");
			check(Token.ADD);
			operator = AbsUnExpr.ADD;
			break;
		case Token.SUB:
			dump("prefix_expression --> - prefix_expression");
			check(Token.SUB);
			operator = AbsUnExpr.SUB;
			break;
		case Token.PTR:
			dump("prefix_expression --> ^ prefix_expression");
			check(Token.PTR);
			operator = AbsUnExpr.MEM;
			break;
		case Token.NOT:
			dump("prefix_expression --> ! prefix_expression");
			check(Token.NOT);
			operator = AbsUnExpr.NOT;
			break;
		default:
			dump("prefix_expression --> postfix_expression");
			return parse_postfix_expression();
		}
		AbsExpr prefix_expression = parse_prefix_expression();
		return new AbsUnExpr(new Position(begPosition, endPosition), operator, prefix_expression);
	}
	
	private AbsExpr parse_postfix_expression() {
		dump("postfix_expression --> atom_expression postfix_expression'");
		AbsExpr atom_expression = parse_atom_expression();
		return parse_postfix_expression_(atom_expression);
	}
	private AbsExpr parse_postfix_expression_(AbsExpr expression1) {
		Position begPosition = expression1.position;
		
		switch(symbol.token) {
		case Token.PTR: {
			dump("postfix_expression' --> ^ postfix_expression'");
			check(Token.PTR);
			AbsUnExpr expression = new AbsUnExpr(new Position(begPosition, endPosition), AbsUnExpr.VAL, expression1);
			return parse_postfix_expression_(expression);
		}
		case Token.DOT: {
			dump("postfix_expression' --> . IDENTIFIER postfix_expression'");
			check(Token.DOT);
			Position begPosition_CompName = symbol.position;
			String name = check_identifier();
			AbsCompName component_name = new AbsCompName(new Position(begPosition_CompName, endPosition), name);
			AbsBinExpr expression = new AbsBinExpr(new Position(begPosition, endPosition), AbsBinExpr.DOT, expression1, component_name);
			return parse_postfix_expression_(expression);
		}
		case Token.LBRACKET: {
			dump("postfix_expression' --> [ expression ] postfix_expression'");
			check(Token.LBRACKET);
			AbsExpr expression2 = parse_expression();
			check(Token.RBRACKET);
			AbsBinExpr expression = new AbsBinExpr(new Position(begPosition, endPosition), AbsBinExpr.ARR, expression1, expression2);
			return parse_postfix_expression_(expression);
		}
		default:
			dump("postfix_expression' --> EMPTY");
			return expression1;
		}
	}
	
	private AbsExpr parse_atom_expression() {
		Position begPosition = symbol.position;
		
		switch(symbol.token) {
		case Token.LOG_CONST: {
			dump("atom_expression --> LOG_CONST");
			String value = check_atom_const(Token.LOG_CONST);
			return new AbsAtomConst(begPosition, AbsAtomConst.LOG, value);
		}
		case Token.INT_CONST: {
			dump("atom_expression --> INT_CONST");
			String value = check_atom_const(Token.INT_CONST);
			return new AbsAtomConst(begPosition, AbsAtomConst.INT, value);
		}
		case Token.STR_CONST: {
			dump("atom_expression --> STR_CONST");
			String value = check_atom_const(Token.STR_CONST);
			return new AbsAtomConst(begPosition, AbsAtomConst.STR, value);
		}
		case Token.IDENTIFIER: {
			dump("atom_expression --> atom_expression_identifier");
			return parse_atom_expression_identifier();
		}
		case Token.LBRACE: {
			dump("atom_expression --> atom_expression_brace");
			AbsExpr expression = parse_atom_expression_brace();
			return expression;
		}
		case Token.LPARENT: {  //tukaj sem dal oklepaje v parse_epressions, ker tko dobim lepso pozicijo
			dump("atom_expression --> expressions");
			AbsExprs expressions = parse_expressions();
			return expressions;
		}
		default: {
			Report.error(symbol.position, "Not a valid expression, exiting");
		}
		}
		return null;
	}
	private AbsExpr parse_atom_expression_identifier() {
		Position begPosition = symbol.position;
		String name = check_identifier();
		
		switch(symbol.token) {
		case Token.LPARENT:
			dump("atom_expression_identifier --> IDENTIFIER ( expression expressions' )");
			check(Token.LPARENT);
			//AbsExprs arguments = parse_expressions(); // moram dobit Vector<AbsExpr> za konstruktor AbsFunCall
			Vector<AbsExpr> arguments = new Vector<AbsExpr>();
			arguments.add(parse_expression());
			parse_expressions_(arguments);
			check(Token.RPARENT);
			return new AbsFunCall(new Position(begPosition, endPosition), name, arguments);
		default:
			dump("atom_expression_identifier --> IDENTIFIER");
			return new AbsVarName(begPosition, name);
		}
	}
	private AbsExpr parse_atom_expression_brace() {
		Position begPosition = symbol.position;

		check(Token.LBRACE);
		switch(symbol.token) {
		case Token.KW_IF: {
			dump("atom_expression_brace --> { IF expression THEN expression atom_expression_brace_if");
			check(Token.KW_IF);
			AbsExpr condition = parse_expression();
			check(Token.KW_THEN);
			AbsExpr thenBody = parse_expression();
			return parse_atom_expression_brace_if(begPosition, condition, thenBody);
		}
		case Token.KW_WHILE: {
			dump("atom_expression_brace --> { WHILE expression : expression }");
			check(Token.KW_WHILE);
			AbsExpr condition = parse_expression();
			check(Token.COLON);
			AbsExpr body = parse_expression();
			check(Token.RBRACE);
			return new AbsWhile(new Position(begPosition, endPosition), condition, body);
		}
		case Token.KW_FOR: {
			dump("atom_expression_brace --> { FOR IDENTIFIER = expression , expression , expression : expression }");
			check(Token.KW_FOR);
			String name = check_identifier();
			AbsVarName count = new AbsVarName(endPosition, name);
			check(Token.ASSIGN);
			AbsExpr lo = parse_expression();
			check(Token.COMMA);
			AbsExpr hi = parse_expression();
			check(Token.COMMA);
			AbsExpr step = parse_expression();
			check(Token.COLON);
			AbsExpr body = parse_expression();
			check(Token.RBRACE);
			return new AbsFor(new Position(begPosition, endPosition), count, lo, hi, step, body);
		}
		default: {
			dump("atom_expression_brace --> { expression = expression }");
			AbsExpr expression1 = parse_expression();
			check(Token.ASSIGN);
			AbsExpr expression2 = parse_expression();
			check(Token.RBRACE);
			return new AbsBinExpr(new Position(begPosition, endPosition), AbsBinExpr.ASSIGN, expression1, expression2);
		}
		}
	}
	private AbsExpr parse_atom_expression_brace_if(Position begPosition, AbsExpr condition, AbsExpr thenBody) {
		switch(symbol.token) {
		case Token.KW_ELSE:
			dump("atom_expression_brace_if --> ELSE expression }");
			check(Token.KW_ELSE);
			AbsExpr elseBody = parse_expression();
			check(Token.RBRACE);
			return new AbsIfThenElse(new Position(begPosition, endPosition), condition, thenBody, elseBody);
		default:
			dump("atom_expression_brace_if --> }");
			check(Token.RBRACE);
			return new AbsIfThen(new Position(begPosition, endPosition), condition, thenBody);
		}
	}
	
	private AbsExprs parse_expressions() {
		dump("expressions --> ( expression expressions' )");
		Position begPosition = symbol.position;
		check(Token.LPARENT);
		Vector<AbsExpr> expressions = new Vector<AbsExpr>();
		expressions.add(parse_expression());
		parse_expressions_(expressions);
		check(Token.RPARENT);
		return new AbsExprs(new Position(begPosition, endPosition), expressions);
	}
	private void parse_expressions_(Vector<AbsExpr> expressions) {
		switch(symbol.token) {
		case Token.COMMA:
			dump("expressions' --> , expression expressions'");
			check(Token.COMMA);
			expressions.add(parse_expression());
			parse_expressions_(expressions);
			return;
		default:
			dump("expressions' --> EMPTY");
			return;
		}
	}
	
	private AbsVarDef parse_variable_definition() {
		dump("variable_definition --> VAR IDENTIFIER : type");
		Position begPosition = symbol.position;
		check(Token.KW_VAR);
		String name = check_identifier();
		check(Token.COLON);
		AbsType type = parse_type();
		return new AbsVarDef(new Position(begPosition, endPosition), name, type);
	}
	
	/**
	 * Izpise produkcijo v datoteko z vmesnimi rezultati.
	 * 
	 * @param production
	 *            Produkcija, ki naj bo izpisana.
	 */
	private void dump(String production) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		Report.dumpFile().println(production);
	}

}
