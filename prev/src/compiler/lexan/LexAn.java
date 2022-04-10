package compiler.lexan;

import java.io.*;

import compiler.*;

/**
 * Leksikalni analizator.
 * 
 */
public class LexAn {
	
	private FileReader sourceFile;
	
	private int character;
	private int line;
	private int column;
	
	private StringBuilder lexeme;
	private int beginLine;
	private int beginColumn;
	private int endLine;
	private int endColumn;
	
	private int token;
	
	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param sourceFileName
	 *			Ime izvorne datoteke.
	 * @param dump
	 *			Ali se izpisujejo vmesni rezultati.
	 */
	public LexAn(String sourceFileName, boolean dump) {
		try {
			sourceFile = new FileReader(sourceFileName);
		}
		catch (FileNotFoundException e) {
			Report.error("Source file " + sourceFileName + " not found.");
		}
		
		line = 1;
		column = 0;
		character = -1;
		readCharacter();
		
		this.dump = dump;
		if(this.dump)
			Report.openDumpFile(sourceFileName);
	}
	
	/** Prebere naslednji znak */
	private void readCharacter() {
		int prev = character;
		
		try {
			character = sourceFile.read();
		}
		catch(IOException e) {
			Report.error("Source file read error occured.");
		}
		
		//column++;
		if(prev == '\n') {
			line++;
			column = 1;
		}
		else if(prev == '\t') {
			column += 8 - ((column-1) % 8);
		}
		else
			column++;
	}
	
	/**
	 * Vrne naslednji simbol iz izvorne datoteke. Preden vrne simbol, ga izpise
	 * na datoteko z vmesnimi rezultati.
	 * 
	 * @return Naslednji simbol iz izvorne datoteke.
	 */
	public Symbol lexAn() {
		token = -1;
		lexeme = new StringBuilder();
		
		while(token == -1) {
			switch(character) {
			case -1: token = Token.EOF; break;
			
			case ' ':
			case '\t':
			case '\n':
			case '\r':
				readCharacter();
				break;
				
			case '#': readComment(); break;
			
			case '+': token = Token.ADD; firstCharacter(); break;
			case '-': token = Token.SUB; firstCharacter(); break;
			case '*': token = Token.MUL; firstCharacter(); break;
			case '/': token = Token.DIV ; firstCharacter(); break;
			case '%': token = Token.MOD; firstCharacter(); break;
			case '&': token = Token.AND; firstCharacter(); break;
			case '|': token = Token.IOR; firstCharacter(); break;
			case '^': token = Token.PTR; firstCharacter(); break;
			
			case '(': token = Token.LPARENT; firstCharacter(); break;
			case ')': token = Token.RPARENT; firstCharacter(); break;
			case '[': token = Token.LBRACKET; firstCharacter(); break;
			case ']': token = Token.RBRACKET; firstCharacter(); break;
			case '{': token = Token.LBRACE; firstCharacter(); break;
			case '}': token = Token.RBRACE; firstCharacter(); break;
			case ':': token = Token.COLON; firstCharacter(); break;
			case ';': token = Token.SEMIC; firstCharacter(); break;
			case '.': token = Token.DOT; firstCharacter(); break;
			case ',': token = Token.COMMA; firstCharacter(); break;
			
			case '!': //!=
			case '=': //==
			case '<': //<=
			case '>': //>=
				secondCharacter();
				break;
				
			case '\'': readString(); break;
			
			default: readIdentifierOrNumber();
			}
		}
		
		Symbol s = new Symbol(  token, lexeme.toString()
		                      , beginLine, beginColumn
		                      , endLine, endColumn);
		dump(s);
		return s;
	}
	
	private void firstCharacter() {
		lexeme.append((char)character);
		
		beginLine = line;
		beginColumn = column;
		
		endLine = line;
		endColumn = column;
		
		readCharacter();
	}
	private void addCharacter() {
		lexeme.append((char)character);
		
		endLine = line;
		endColumn = column;
		
		readCharacter();
	}
	private void secondCharacter() {
		int prev = character;
		firstCharacter();
		if(character == '=') {
			switch(prev) {
			case '!': token = Token.NEQ; addCharacter(); break;
			case '=': token = Token.EQU; addCharacter(); break;
			case '<': token = Token.LEQ; addCharacter(); break;
			case '>': token = Token.GEQ; addCharacter(); break;
			}
		}
		else {
			switch(prev) {
			case '!': token = Token.NOT; break;
			case '=': token = Token.ASSIGN; break;
			case '<': token = Token.LTH; break;
			case '>': token = Token.GTH; break;
			}
		}
	}
	
	private void readComment() {
		if(character != '#') return;
		while(character != -1 && character != '\n')
			readCharacter();
	}
	/** Prebere celoten niz. */
	private void readString() {
		if(character != '\'') return;
		
		firstCharacter();
		while(token == -1) {
			if(character == -1) {
				Report.warning(  new Position(line, column, line, column)
				               , "Missing \' at the end of string constant.");
				token = Token.STR_CONST;
			}
			else if(character == '\'') {
				addCharacter();
				if(character == '\'')
					addCharacter();
					//readCharacter();
				else
					token = Token.STR_CONST;
			}
			else if(32 <= character && character <= 126) {
				addCharacter();
			}
			else {
				Report.warning(  new Position(line, column, line, column)
				               , "Illegal character in string constant: '"
				               + (char)character + "'(Code: " + character + ")");
				readCharacter();
			}
		}
	}
	/** Prebere celotno ime ali stevilo. */
	private void readIdentifierOrNumber() {
		if(   'A' <= character && character <= 'Z'
		   || 'a' <= character && character <= 'z'
		   || character == '_') {
			readIdentifier();
		}
		else if('0' <= character && character <= '9') {
			readNumber();
		}
		else {
			Report.warning(   new Position(line, column, line, column)
			               , "Unexpected character: " + (char)character
			               + "(code: " + character + ") ignored.");
			readCharacter();
		}
	}
	/** Prebere ime in ugotovi, ce je kljucna beseda. */
	private void readIdentifier() {
		if(! (   'A' <= character && character <= 'Z'
		      || 'a' <= character && character <= 'z'
		      || character == '_'))
			return;
		
		//ime
		firstCharacter();
		while(   'A' <= character && character <= 'Z'
		      || 'a' <= character && character <= 'z'
		      || '0' <= character && character <= '9'
		      || character == '_')
			addCharacter();
			
		//kljucna beseda
		switch(lexeme.toString()) {
		case "arr": token = Token.KW_ARR; break;
		case "else": token = Token.KW_ELSE; break;
		case "for": token = Token.KW_FOR; break;
		case "fun": token = Token.KW_FUN; break;
		case "if": token = Token.KW_IF; break;
		case "rec": token = Token.KW_REC; break;
		case "then": token = Token.KW_THEN; break;
		case "typ": token = Token.KW_TYP; break;
		case "var": token = Token.KW_VAR; break;
		case "where": token = Token.KW_WHERE; break;
		case "while": token = Token.KW_WHILE; break;
		
		case "logical": token = Token.LOGICAL; break;
		case "integer": token = Token.INTEGER; break;
		case "string": token = Token.STRING; break;
		
		case "true":
		case "false":
			token = Token.LOG_CONST; break;
		
		default: token = Token.IDENTIFIER;
		}
	}
	/** Prebere stevilo. */
	private void readNumber() {
		if(! ('0' <= character && character <= '9')) return;
		
		firstCharacter();
		while('0' <= character && character <= '9')
			addCharacter();
		
		token = Token.INT_CONST;
	}

	/**
	 * Izpise simbol v datoteko z vmesnimi rezultati.
	 * 
	 * @param symb
	 *			Simbol, ki naj bo izpisan.
	 */
	private void dump(Symbol symb) {
		if (! dump) return;
		if (Report.dumpFile() == null) return;
		if (symb.token == Token.EOF)
			Report.dumpFile().println(symb.toString());
		else
			Report.dumpFile().println("[" + symb.position.toString() + "] " + symb.toString());
	}

}
