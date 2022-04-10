package compiler.proepi;

import java.util.*;

import compiler.imcode.*;

public class ProEpi {
	public void generate(LinkedList<ImcChunk> chunks) {
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				ProEpiGenerator proEpiGen = new ProEpiGenerator((ImcCodeChunk)chunk);
				proEpiGen.generate();
			}
		}
	}
}
