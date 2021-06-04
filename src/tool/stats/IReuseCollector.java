package tool.stats;

import java.io.PrintStream;

import tool.RDGNode;

public interface IReuseCollector {
    public void logImpactedNode(String node);
    public void logReusedNode(String node);
    public void printStats(PrintStream out);
    public void printEvaluationReuse(PrintStream out, RDGNode rdgRoot);
}
