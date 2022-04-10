package compiler.imcode;

import compiler.frames.*;
import compiler.asmcode.*;

/**
 * Vmesna koda.
 * 
 */
public abstract class ImcCode {

	/**
	 * Izpise drevo vmesne kode na datoteko vmesnih rezultatov.
	 * 
	 * @param indent Zamik.
	 */
	public abstract void dump(int indent);
	
	public abstract FrmTemp accept(AsmCodeGenerator codeGen);
}
