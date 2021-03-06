package compiler.seman.type;

/**
 * Opis kazalcnega tipa.
 * 
 */
public class SemPtrType extends SemType {

	/** Tip elementa. */
	public final SemType type;

	/**
	 * Ustvari nov opis kazalcnega tipa.
	 * 
	 * @param type
	 */
	public SemPtrType(SemType type) {
		this.type = type;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (type.actualType() instanceof SemPtrType) {
			SemPtrType pointerType = (SemPtrType) (type.actualType());
			return (pointerType.type.sameStructureAs(this.type));
		} else
			return false;
	}
	
	@Override
	public int size() {
		return 8;
	}

	@Override
	public String toString() {
		return "PTR(" + type.toString() + ")";
	}

}
