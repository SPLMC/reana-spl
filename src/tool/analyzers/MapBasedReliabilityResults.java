package tool.analyzers;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import tool.UnknownFeatureException;

public class MapBasedReliabilityResults implements IReliabilityAnalysisResults {

    private Map<Set<String>, Double> results;
    private Set<String> features;

    public MapBasedReliabilityResults() {
        this.results = new HashMap<Set<String>, Double>();
        this.features = new HashSet<String>();
    }

    public MapBasedReliabilityResults(Map<Collection<String>, Double> backup) {
        this.results = backup.entrySet().stream()
                .collect(Collectors.toMap(e -> new HashSet<String>(e.getKey()),
                                          e -> e.getValue()));
        this.features = backup.keySet().stream()
                .map(Collection::stream)
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
    }

    @Override
    public Double getResult(String[] configuration) throws UnknownFeatureException {
        Set<String> configurationAsSet = new HashSet<String>(Arrays.asList(configuration));
        if (results.containsKey(configurationAsSet)) {
            return results.get(configurationAsSet);
        } else if (hasUnknownFeature(configurationAsSet)) {
            throw new UnknownFeatureException(configuration.toString());
        }
        return 0.0;
    }

    @Override
    public void printAllResults(PrintStream output) {
        for (Map.Entry<Set<String>, Double> result: results.entrySet()) {
            output.println(result.getKey() + " --> " + result.getValue());
        }
    }

    @Override
    public int getNumberOfResults() {
        return results.size();
    }

    public synchronized void putResult(List<String> configuration, Double value) {
        Set<String> configurationAsSet = new HashSet<String>(configuration);
        results.put(configurationAsSet, value);
        features.addAll(configurationAsSet);

    }

    /**
     * Prints the size of the reliability mapping, but not taking
     * into account the inner structures used by java.util.HashMap
     * and java.util.HashSet.
     */
    @Override
    public void printStats(PrintStream output) {
        long size = 0;
        for (Set<String> result: results.keySet()) {
            for (String feature: result) {
                size += feature.length();
            }
            size += 8;  // reliability's size (double)
        }
        output.println("Result's size in bytes: " + size);
    }

    private boolean hasUnknownFeature(Set<String> configuration) {
        return features.containsAll(configuration);
    }

}
