package compiler.mmsfilewriter;

import java.util.*;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import compiler.Report;
import compiler.imcode.*;
import compiler.asmcode.*;

public class MMSFileWriter {
	
	private PrintStream mmsFile;
	
	public MMSFileWriter(String mmsFileName) {
		try {
			this.mmsFile = new PrintStream(mmsFileName);
		} catch (FileNotFoundException __) {
			Report.error("Cannot produce mms file '" + mmsFileName + "'.");
		}
	}
	
	public void write(LinkedList<ImcChunk> chunks) {

		mmsFile.println("	LOC	Data_Segment");
		mmsFile.println("	GREG	@");          //alocira $254 kot globalnega, za dostop do DataSegment-a
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcDataChunk) {
				ImcDataChunk dataChunk = (ImcDataChunk)chunk;
				
				if(dataChunk.value == null) {
					mmsFile.println(dataChunk.label.name()+"	OCTA	0");
					
					int size = dataChunk.size;
					size -= 8;
					if(size > 0)
						mmsFile.println("	LOC	@+"+size);
				}
				else {
					mmsFile.println(dataChunk.label.name()+"	BYTE	"+dataChunk.value);
				}
			}
		}
		
		mmsFile.println("	LOC	#100");
		
		mmsFile.println("Main	SET	$0,250");
		mmsFile.println("	PUT	rG,$0");
		
		mmsFile.println("	SETH	$250,#6000");
		
		/* TOLE ZAENKRAT NI RES
		 * koda za kopico
		 * $252 - kazalec na seznam prostih blokov
		 * $253 - kazalec na seznam zasedenih blokov
		 * 
		 * bloki so oblike : | OCTA size | OCTA next | ... data ... |
		 *   kjer je size velikost data v byte-ih
		 *           next pa kazalec na naslednji prost blok
		 *                  (v seznamu si sledijo po narascajocih naslovih)
		 * ce next == 0 => zadnji blok
		 *                 takrat je size == -1
		 *                 
		 * $253 je na zacetku 0
		 */
		mmsFile.println("	SETH	$252,#4000");
		mmsFile.println("	SET	$253,0");
		
		mmsFile.println("	SUB	$250,$250,16");
		mmsFile.println("	PUSHJ	$0,_main");
		mmsFile.println("	TRAP	0,Halt,0");
		
		
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				ImcCodeChunk codeChunk = (ImcCodeChunk) chunk;
				AsmInstr prev = null;
				for(AsmInstr instr : codeChunk.asmcode) {
					if(instr instanceof AsmLABEL) {
						if(prev instanceof AsmLABEL)
							mmsFile.println("	SWYM");
						mmsFile.print(instr.format(((ImcCodeChunk) chunk).registerAllocation));
					}
					else {
						mmsFile.println(instr.format(codeChunk.registerAllocation));
					}
					
					prev = instr;
				}
			}
		}
		
		mmsFile.println("% standardna knjiznica");
		mmsFile.println("_put_nl	SUB	$255,$250,2");
		mmsFile.println("	SET	$0,0");
		mmsFile.println("	STB	$0,$255,1");
		mmsFile.println("	SET	$0,10");
		mmsFile.println("	STB	$0,$255,0");
		mmsFile.println("	LDO	$0,$250,8");
		mmsFile.println("SL00	BNP	$0,SL01");
		mmsFile.println("	SUB	$255,$250,2");
		mmsFile.println("	TRAP	0,Fputs,StdOut");
		mmsFile.println("	SUB	$0,$0,1");
		mmsFile.println("	JMP	SL00");
		mmsFile.println("SL01	SET	$0,0");
		mmsFile.println("	STO	$0,$250,0");
		mmsFile.println("	POP	0,0");
		
		mmsFile.println("_put_int	SET	$255,$250");
		mmsFile.println("	SET	$0,0");
		mmsFile.println("	SUB	$255,$255,1");
		mmsFile.println("	STB	$0,$255");
		mmsFile.println("	LDO	$1,$250,8");
		mmsFile.println("	BNZ	$1,SL10");
		mmsFile.println("	SET	$2,\"0\"");
		mmsFile.println("	SUB	$255,$255,1");
		mmsFile.println("	STB	$2,$255,0");
		mmsFile.println("	JMP	SL14");
		mmsFile.println("SL10	SET	$3,$1");
		mmsFile.println("SL11	DIV	$3,$3,10");
		mmsFile.println("	GET	$2,rR");
		mmsFile.println("	BZ	$2,SL12");
		mmsFile.println("	BNN	$3,SL12");
		mmsFile.println("	ADD	$3,$3,1");
		mmsFile.println("SL12	BP	$1,SL13");
		mmsFile.println("	BZ	$2,SL13");
		mmsFile.println("	NEG	$2,$2");
		mmsFile.println("	ADD	$2,$2,10");
		mmsFile.println("SL13	ADD	$2,$2,\"0\"");
		mmsFile.println("	SUB	$255,$255,1");
		mmsFile.println("	STB	$2,$255,0");
		mmsFile.println("	BNZ	$3,SL11");
		mmsFile.println("	BP	$1,SL14");
		mmsFile.println("	SET	$2,\"-\"");
		mmsFile.println("	SUB	$255,$255,1");
		mmsFile.println("	STB	$2,$255,0");
		mmsFile.println("SL14	TRAP	0,Fputs,StdOut");
		mmsFile.println("	SET	$0,0");
		mmsFile.println("	STO	$0,$250,0");
		mmsFile.println("	POP	0,0");
		
		mmsFile.println("_get_int	SUB	$0,$250,21");
		mmsFile.println("	SUB	$255,$0,16");
		mmsFile.println("	STO	$0,$255,0");
		mmsFile.println("	SET	$1,21");
		mmsFile.println("	STO	$1,$255,8");
		mmsFile.println("	TRAP	0,Fgets,StdIn");
		mmsFile.println("	SET	$2,0");
		mmsFile.println("	LDB	$1,$0,0");
		mmsFile.println("	SET	$4,1");
		mmsFile.println("	CMP	$3,$1,\"-\"");
		mmsFile.println("	BNZ	$3,SL20");
		mmsFile.println("	NEG	$4,$4");
		mmsFile.println("	ADD	$0,$0,1");
		mmsFile.println("	LDB	$1,$0,0");
		mmsFile.println("SL20	BZ	$1,SL22");
		mmsFile.println("	CMP	$3,$1,10");
		mmsFile.println("	BZ	$3,SL22");
		mmsFile.println("	SUB	$1,$1,\"0\"");
		mmsFile.println("	MUL	$1,$1,$4");
		mmsFile.println("	BZ	$2,SL21");
		mmsFile.println("	MUL	$2,$2,10");
		mmsFile.println("SL21	ADD	$2,$2,$1");
		mmsFile.println("	ADD	$0,$0,1");
		mmsFile.println("	LDB	$1,$0,0");
		mmsFile.println("	JMP	SL20");
		mmsFile.println("SL22	STO	$2,$250,0");
		mmsFile.println("	POP	0,0");
		
		mmsFile.println("_put_str	LDO	$255,$250,8");
		mmsFile.println("	TRAP	0,Fputs,StdOut");
		mmsFile.println("	SET	$0,0");
		mmsFile.println("	STO	$0,$250,0");
		mmsFile.println("	POP	0,0");
		
		mmsFile.println("_malloc	STO	$252,$250,0");
		mmsFile.println("	LDO	$0,$250,8");
		mmsFile.println("	ADD	$252,$252,$0");
		mmsFile.println("	SET	$0,0");
		mmsFile.println("	STB	$0,$252,0");
		mmsFile.println("	ADD	$252,$252,1");
		mmsFile.println("	POP	0,0");

		mmsFile.println("_free	SET	$0,0");
		mmsFile.println("	STO	$0,$250,0");
		mmsFile.println("	POP	0,0");
		
		mmsFile.println("_get_str	GET	$1,rJ");
		mmsFile.println("	SET	$0,255");
		mmsFile.println("	STO	$0,$250,8"); //pokvarim svoj argument, ki ga itak ignoriram
		mmsFile.println("	PUSHJ	$2,_malloc");
		mmsFile.println("	PUT	rJ,$1");
		mmsFile.println("	LDO	$0,$250,0");
		mmsFile.println("	SUB	$255,$250,16");
		mmsFile.println("	STO	$0,$255,0");
		mmsFile.println("	SET	$0,255");
		mmsFile.println("	STO	$0,$255,8");
		mmsFile.println("	TRAP	0,Fgets,StdIn");
		mmsFile.println("	POP	0,0");
		
		mmsFile.println("_get_char_at	LDO	$0,$250,16");
		mmsFile.println("	LDO	$1,$250,8");
		mmsFile.println("	LDB	$0,$0,$1");
		mmsFile.println("	STO	$0,$250,0");
		mmsFile.println("	POP	0,0");
		
	
		mmsFile.println("_put_char_at	LDO	$0,$250,24");
		mmsFile.println("	LDO	$1,$250,16");
		mmsFile.println("	LDO	$2,$250,8");
		mmsFile.println("	STB	$1,$0,$2");
		mmsFile.println("	POP	0,0");
	}
}
