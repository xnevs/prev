package compiler.abstr.tree;

import compiler.*;

/**
 * Definicija.
 * 
 */
public abstract class AbsDef extends AbsTree {

	/**
	 * Ustvari novo definicijo.
	 *
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 */
	public AbsDef(Position pos) {
		super(pos);
	}

}
