package compiler.imcode;

import compiler.*;
import compiler.asmcode.AsmCodeGenerator;
import compiler.frames.*;

/**
 * Zacasna spremenljivka.
 * 
 */
public class ImcTEMP extends ImcExpr {

	/** Zacasna spremenljivka.  */
	public FrmTemp temp;

	/**
	 * Ustvari novo zacasno spremenljivko.
	 * 
	 * @param temp Zacasna spremenljivka.
	 */
	public ImcTEMP(FrmTemp temp) {
		this.temp = temp;
	}

	@Override
	public void dump(int indent) {
		Report.dump(indent, "TEMP name=" + temp.name());
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
