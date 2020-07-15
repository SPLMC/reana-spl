package tool.analyzers;

import java.io.PrintStream;

import tool.UnknownFeatureException;


public interface IReliabilityAnalysisResults {

    /**
     * Gets the specific result for the given configuration or zero if the
     * configuration has no associated result or is invalid.
     *
     * @param configuration
     * @return
     * @throws UnknownFeatureException in case the configuration relies on an
     *      unknown feature.
     */
    public Double getResult(String[] configuration) throws UnknownFeatureException;
    public void printAllResults(PrintStream output);
    public int getNumberOfResults();

    public void printStats(PrintStream output);

}
