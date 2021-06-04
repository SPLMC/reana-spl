package tool.stats;

import java.io.PrintStream;

import tool.RDGNode;

public class NoopReuseCollector implements IReuseCollector {

    @Override
    public void logImpactedNode(String node) {
        // No-op
    }

    @Override
    public void logReusedNode(String node) {
        // No-op
    }

    @Override
    public void printStats(PrintStream out) {
        // No-op
    }

    @Override
    public void printEvaluationReuse(PrintStream out, RDGNode rdgRoot) {
        // No-op
    }

}
