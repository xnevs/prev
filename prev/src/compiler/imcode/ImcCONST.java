package compiler.imcode;

import compiler.*;
import compiler.asmcode.AsmCodeGenerator;
import compiler.frames.FrmTemp;

/**
 * Konstanta.
 * 
 */
public class ImcCONST extends ImcExpr {

	/** Vrednost.  */
	public Long value;

	/**
	 * Ustvari novo konstanto.
	 * 
	 * @param value Vrednost konstante.
	 */
	public ImcCONST(Long value) {
		this.value = value;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "CONST value=" + value.toString());
	}

	@Override
	public ImcESEQ linear() {
		return new ImcESEQ(new ImcSEQ(), this);
	}

	
	@Override
	public FrmTemp accept(AsmCodeGenerator codeGen) {
		return codeGen.munch(this);
	}

}
