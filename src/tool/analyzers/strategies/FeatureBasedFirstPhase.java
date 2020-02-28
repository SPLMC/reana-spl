package tool.analyzers.strategies;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import paramwrapper.ParametricModelChecker;
import tool.Analyzer;
import tool.RDGNode;
import tool.analyzers.buildingblocks.Component;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;
import tool.stats.IFormulaCollector;
import fdtmc.FDTMC;

/**
 * First phase of a feature-family- or a feature-product-based strategy.
 */
public class FeatureBasedFirstPhase {
    private static final Logger LOGGER = Logger.getLogger(FeatureBasedFirstPhase.class.getName());

    ParametricModelChecker modelChecker;
    private IFormulaCollector formulaCollector;

    public FeatureBasedFirstPhase(ParametricModelChecker modelChecker,
                                  IFormulaCollector formulaCollector) {
        this.modelChecker = modelChecker;
        this.formulaCollector = formulaCollector;
    }

    /**
     * Computes the reliability expression for the model of the given RDG nodes,
     * returning them in a map which is conveniently sorted in the same order as
     * the input list.
     *
     * This function implements the feature-based part of the analysis.
     * @param concurrencyStrategy
     *
     * @see {@link Analyzer.getReliabilityExpression}
     * @param node
     * @return
     */
    public List<Component<String>> getReliabilityExpressions(List<RDGNode> nodes, ConcurrencyStrategy concurrencyStrategy) {
        if (concurrencyStrategy == ConcurrencyStrategy.PARALLEL) {
            LOGGER.info("Performing model checking in parallel for each FDTMC.");
        }
        // Expressions can be calculated concurrently...
        Stream<RDGNode> expressionStream = (concurrencyStrategy == ConcurrencyStrategy.PARALLEL) ? nodes.parallelStream()
                                                                                                 : nodes.stream();
        
        Map<String, String> expressionsByNode = expressionStream
            .collect(Collectors.toMap(RDGNode::getId,
                                      this::getReliabilityExpression));
        //System.out.println(expressionsByNode);
        // ... but then we need to recover ordering information
        // so that we can format the response accordingly.
        return nodes.stream()
                .map(RDGNode::toComponent)
                .map(c -> c.fmap((FDTMC f) -> expressionsByNode.get(c.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Computes the reliability expression for the model of a given RDG node.
     *
     * @param node
     * @return an algebraic expression on the variables present in the node's model.
     */
    private String getReliabilityExpression(RDGNode node) {
        FDTMC model = node.getFDTMC();
        String reliabilityExpression = modelChecker.getReliability(model);

        formulaCollector.collectFormula(node, reliabilityExpression);
        LOGGER.fine("Reliability expression for "+ node.getId() + " -> " + reliabilityExpression);
        return reliabilityExpression;
    }

}
