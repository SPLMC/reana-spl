package tool.analyzers;

import java.io.PrintStream;

import tool.UnknownFeatureException;
import jadd.ADD;
import jadd.UnrecognizedVariableException;

public class ADDReliabilityResults implements IReliabilityAnalysisResults {

    private ADD results;

    public ADDReliabilityResults(ADD results) {
        this.results = results;
    }

    @Override
    public Double getResult(String[] configuration) throws UnknownFeatureException {
        try {
            return results.eval(configuration);
        } catch (UnrecognizedVariableException e) {
            throw new UnknownFeatureException(e.getVariableName());
        }
    }

    @Override
    public void printAllResults(PrintStream output) {
        results.printEverything(output);
    }

    @Override
    public int getNumberOfResults() {
        return (int) results.getPathsToNonZeroTerminalsCount();
    }

    @Override
    public void printStats(PrintStream output) {
        int numVariables = results.getVariables().size();
        int numNodes = results.getNodeCount();
        int numDeadNodes = results.getDeadNodesCount();
        int numTerminalsNonZero = results.getTerminalsDifferentThanZeroCount();
        double numPathsToNonZeroTerminals = results.getPathsToNonZeroTerminalsCount();
        double numPathsToZeroTerminal = results.getPathsToZeroTerminalCount();
        int numReorderings = results.getReorderingsCount();
        int numGarbageCollections = results.getGarbageCollectionsCount();
        long numBytesADD = results.getAddSizeInBytes();

        output.println("# variables: " + numVariables);
        output.println("# internal nodes: " + numNodes);
        output.println("# dead nodes: " + numDeadNodes);
        output.println("# terminals different than zero: " + numTerminalsNonZero);
        output.println("# paths to non-zero terminals: " + numPathsToNonZeroTerminals);
        output.println("# paths to zero terminal: " + numPathsToZeroTerminal);
        output.println("# reorderings: " + numReorderings);
        output.println("# garbage collections: " + numGarbageCollections);
        output.println("ADD's size in # of bytes: " + numBytesADD);

        output.println("Order of variables: " + results.getVariableOrder());
    }

}
