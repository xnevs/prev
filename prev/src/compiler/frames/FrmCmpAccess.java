package compiler.frames;

import compiler.abstr.tree.*;

/**
 * Dostop do komponente strukture.
 *
 */
public class FrmCmpAccess extends FrmAccess {

	/** Opis komponente.  */
	public AbsComp cmp;

	/** Odmik od zacentega naslova strukture.  */
	public int offset;

	/**
	 * Ustvari nov dostop do komponente zapisa.
	 * 
	 * @param cmp Komponenta zapisa.
	 * @param offset Odmik od zacetnega naslova strukture.
	 */
	public FrmCmpAccess(AbsComp cmp, int offset) {
		this.cmp = cmp;
		this.offset = offset;
	}

	@Override
	public String toString() {
		return "CMP(" + cmp.name + ": offset=" + offset + ")";
	}
	
}
