package compiler.lincode;

import java.util.*;

import compiler.*;
import compiler.frames.*;
import compiler.imcode.*;

public class Interpreter {

	int debug = 0;
	
	/** Pomnilnik.  */
	public Map<Long,Long> memory = new HashMap<Long,Long>();

	/** Preslikava label globalnih spremenljivk in konstant v naslove.  */
	public Map<String,Long> labels = new HashMap<String,Long>();

	/** Preslikava label funkcij v kodo.  */
	public Map<String,ImcCodeChunk> chunks = new HashMap<String,ImcCodeChunk>();

	/** Lokalne spremenljivke.  */
	public Map<String,Long> temps = new HashMap<String,Long>();

	/** Kazalec na sklad.  */
	public Long SP;

	/** Kazalec na klicni zapis.  */
	public Long FP;

	/** Kazalec na kopico.  */
	public Long HP;

	public Interpreter(List<ImcChunk> progChunks) {
		this.SP = 65536L;
		this.FP = 65536L;
		this.HP = 1024L;
		
		for(ImcChunk chunk : progChunks) {
			if (chunk instanceof ImcCodeChunk) {
				ImcCodeChunk codeChunk = (ImcCodeChunk)chunk;
				this.chunks.put(codeChunk.frame.label.name(), codeChunk);
			}
			if (chunk instanceof ImcDataChunk) {
				ImcDataChunk dataChunk = (ImcDataChunk)chunk;
				this.labels.put(dataChunk.label.name(), HP);
				HP = HP + dataChunk.size;
			}
		}
	}
	
	public void run() {
		execFun("_main");
	}

	public static String prefix = "";

	private Scanner scanner = new Scanner(System.in);
	
	public Long execFun(String label) {
		if (label.equals("_get_int")) {
			return new Long(scanner.nextInt());
		}
		if (label.equals("_put_int")) {
			System.out.print(load(SP + 8));
			return 0L;
		}
		if (label.equals("_put_nl")) {
			Long n = load(SP + 8);
			for(int i=0; i<n; i++) System.out.println();
			return 0L;
		}

		if (debug == 1) System.err.println(prefix + "=> " + label + " SP:" + SP + " FP:" + FP);

		ImcCodeChunk chunk = chunks.get(label);
		FrmFrame frame = chunk.frame;
		Map<String,Long> outerTemps = temps;
		temps = new HashMap<String,Long>();

		// Prolog:
		store(SP - frame.sizeLocs - 8, FP);
		FP = SP;
		SP = SP - frame.size();
		store(frame.FP, FP);
		
		if (debug == 1) System.err.println(prefix + "in " + label + " SP:" + SP + " FP:" + FP);

		// jedro:
		LinkedList<ImcStmt> stmts = ((ImcSEQ)(chunk.lincode)).stmts;
		int PC = 0;
		while (PC < stmts.size()) {
			FrmLabel newLabel = execStmt(stmts.get(PC));
			if (newLabel != null) {
				// Razveljavimo cevovod :)
				PC = stmts.indexOf(new ImcLABEL(newLabel));
			}
			else
				PC++;
		}

		// Epilog:
		SP = SP + frame.size();
		FP = load(SP - frame.sizeLocs - 8);
		Long retValue = 0L;
		if (frame.RV != null) retValue = load(frame.RV);
		
		if (debug == 1) System.err.println(prefix + "<= " + label + " SP:" + SP + " FP:" + FP);

		temps = outerTemps;
		return retValue;
	}

	public FrmLabel execStmt(ImcStmt stmt) {
		if (debug == 1) System.err.println(prefix + stmt);

		if (stmt instanceof ImcCJUMP) {
			ImcCJUMP cjump = (ImcCJUMP)stmt;
			Long cond = execExpr(cjump.cond);
			return cond != 0 ? cjump.trueLabel : cjump.falseLabel;
		}
		if (stmt instanceof ImcEXP) {
			ImcEXP expr = (ImcEXP)stmt;
			execExpr(expr.expr);
			return null;
		}
		if (stmt instanceof ImcJUMP) {
			return ((ImcJUMP)stmt).label;
		}
		if (stmt instanceof ImcLABEL) {
			return null;
		}
		if (stmt instanceof ImcMOVE) {
			ImcMOVE move = (ImcMOVE)stmt;
			if (move.dst instanceof ImcTEMP) {
				ImcTEMP dst = (ImcTEMP)move.dst;
				Long src = execExpr(move.src);
				store(dst.temp, src);
				return null;
			}
			if (move.dst instanceof ImcMEM) {
				Long dst = execExpr(((ImcMEM)move.dst).expr);
				Long src = execExpr(move.src);
				store(dst, src);
				return null;
			}
			Report.error("Illegal MOVE statement.");
		}
		Report.error("Unknown statement: " + stmt);
		return null;
	}

	public Long execExpr(ImcExpr expr) {
		// if (debug == 1) System.err.println(prefix + expr);

		if (expr instanceof ImcBINOP) {
			ImcBINOP binop = (ImcBINOP)expr;
			long lval = execExpr(binop.limc);  //unboxed
			long rval = execExpr(binop.rimc);  //unboxed
			
			switch (binop.op) {
			case ImcBINOP.ADD: return lval + rval;
			case ImcBINOP.SUB: return lval - rval;
			case ImcBINOP.MUL: return lval * rval;
			case ImcBINOP.DIV: return lval / rval;
			case ImcBINOP.EQU: return (lval == rval ? 1L : 0L);
			case ImcBINOP.NEQ: return (lval != rval ? 1L : 0L);
			case ImcBINOP.LTH: return (lval < rval ? 1L : 0L);
			case ImcBINOP.GTH: return (lval > rval ? 1L : 0L);
			case ImcBINOP.LEQ: return (lval <= rval ? 1L : 0L);
			case ImcBINOP.GEQ: return (lval >= rval ? 1L : 0L);
			case ImcBINOP.AND: return lval * rval;
			case ImcBINOP.OR : return (lval + rval) % 2;
			}
		}
		if (expr instanceof ImcCALL) {
			ImcCALL call = (ImcCALL)expr;
			int offset = 0;
			for (int a = 0; a < call.args.size(); a++) {
				Long v = execExpr(call.args.get(a));
				
				store(SP + 8 * offset, v);
				offset = offset + 1;
			}
			prefix = prefix + "  ";
			Long result = execFun(call.label.name());
			prefix = prefix.substring(2);
			return result;
		}
		if (expr instanceof ImcCONST) {
			return new Long(((ImcCONST)expr).value);
		}
		if (expr instanceof ImcMEM) {
			return load(execExpr(((ImcMEM)expr).expr));
		}
		if (expr instanceof ImcNAME) {
			return labels.get(((ImcNAME)expr).label.name());
		}
		if (expr instanceof ImcTEMP) {
			return load(((ImcTEMP)expr).temp);
		}
		Report.error("Unknown expression: " + expr);
		return null;
	}

	private Long load(Long addr) {
		Long data = memory.get(addr / 8);
		if (debug == 1) System.err.println(prefix + "[" + addr + "]" + "->" + (data == null ? 0 : data));
		if (data == null) return 0L; else return data;
	}

	private Long load(FrmTemp temp) {
		Long value = temps.get(temp.name());
		if (debug == 1) System.err.println(prefix + temp.name() + "->" + (value == null ? 0 : value));
		if (value == null) return 0L;
		else               return value;
	}

	private void store(Long addr, Long data) {
		if (debug == 1) System.err.println(prefix + "[" + addr + "]" + "<-" + data);
		memory.put(addr / 8, data);
	}

	private void store(FrmTemp temp, Long value) {
		if (debug == 1) System.err.println(prefix + temp.name() + "<-" + value);
		temps.put(temp.name(), value);
	}

}
