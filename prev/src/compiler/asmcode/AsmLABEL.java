package compiler.asmcode;

import compiler.frames.*;

/**
 * Opis label.
 * 
 */
public class AsmLABEL extends AsmInstr {

	public AsmLABEL(String assem, FrmLabel label) {
		super(assem, null, null, null);
		labels.add(label);
	}

}
