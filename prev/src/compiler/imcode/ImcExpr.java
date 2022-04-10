package compiler.imcode;

/**
 * Izrazi vmesne kode.
 * 
 */
public abstract class ImcExpr extends ImcCode {

	/**
	 * Vrne linearizirano vmesno kodo izraza.
	 * 
	 * @return Linearizirana vmesna koda izraza.
	 */
	public abstract ImcESEQ linear();

}
