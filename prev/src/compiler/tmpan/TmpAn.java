package compiler.tmpan;

import java.util.*;

import compiler.Report;
import compiler.imcode.*;
import compiler.tmpan.graph.*;

public class TmpAn {
	
	private boolean dump;
	
	public TmpAn(boolean dump) {
		this.dump = dump;
	}
	
	public void analyse(LinkedList<ImcChunk> chunks) {
		for(ImcChunk chunk : chunks) {
			if(chunk instanceof ImcCodeChunk) {
				TmpAnalyser tmpAnalyser = new TmpAnalyser((ImcCodeChunk)chunk);
				InterferenceGraph graph = tmpAnalyser.analyse();
				if(this.dump)
					Report.dump(0, graph.toString());
			}
		}
	}
}
