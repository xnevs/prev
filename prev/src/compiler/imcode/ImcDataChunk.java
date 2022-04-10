package compiler.imcode;

import compiler.*;
import compiler.frames.*;

/**
 * Fragment podatkov.
 * 
 */
public class ImcDataChunk extends ImcChunk {

	/** Naslov spremenljivke v pomnilniku.  */
	public FrmLabel label;

	/** Velikost spremenljivke v pomnilniku.  */
	public int size;

	public String value;
	
	/**
	 * Ustvari nov fragment podatkov.
	 * 
	 * @param label Labela podatka.
	 * @param size Velikost podatka.
	 */
	public ImcDataChunk(FrmLabel label, int size) {
		this.label = label;
		this.size = size;
		this.value = null;
	}

	@Override
	public void dump() {
		Report.dump(0, "DATA CHUNK: label=" + label.name() + " size=" + size);
	}

}
