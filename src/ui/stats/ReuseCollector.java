package ui.stats;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import tool.CyclicRdgException;
import tool.RDGNode;
import tool.stats.IReuseCollector;

public class ReuseCollector implements IReuseCollector {
    private List<String> impactedNodes;
    private List<String> reusedNodes;
    
    public ReuseCollector() {
        impactedNodes = new LinkedList<String>();
        reusedNodes = new LinkedList<String>();
    }
    
    @Override
    public void logImpactedNode(String node) {
        impactedNodes.add(node);
    }

    @Override
    public void logReusedNode(String node) {
        reusedNodes.add(node);
    }

    @Override
    public void printStats(PrintStream out) {
        out.println("Impacted nodes:");
        for (String node: impactedNodes) {
            out.println("\t" + node);
        }

        out.println("Reused nodes:");
        for (String node: reusedNodes) {
            out.println("\t" + node);
        }
    }

    @Override
    public void printEvaluationReuse(PrintStream out, RDGNode rdgRoot) {
	try {
	    Map<RDGNode, Integer> numberOfPaths = rdgRoot.getNumberOfPaths();
	    int nodes = 0;
	    int totalPaths = 0;
	    for (Map.Entry<RDGNode, Integer> entry: numberOfPaths.entrySet()) {
		nodes++;
		totalPaths += entry.getValue();
		out.println(entry.getKey() + ": " + entry.getValue() + " paths");
	    }
	    out.println("Evaluation economy because of cache: " + 100*(totalPaths-nodes)/(float)totalPaths + "%");
	} catch (CyclicRdgException e) {
	    out.println("Cyclic dependency detected in RDG.");
	    // out.println(Level.SEVERE, e.toString(), e);
	    System.exit(2);
	}
    }
}