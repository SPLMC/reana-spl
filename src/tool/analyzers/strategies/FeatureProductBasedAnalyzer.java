package tool.analyzers.strategies;

import jadd.JADD;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import paramwrapper.ParametricModelChecker;
import tool.CyclicRdgException;
import tool.RDGNode;
import tool.UnknownFeatureException;
import tool.analyzers.IReliabilityAnalysisResults;
import tool.analyzers.MapBasedReliabilityResults;
import tool.analyzers.buildingblocks.Component;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;
import tool.analyzers.buildingblocks.DerivationFunction;
import tool.analyzers.buildingblocks.IfOperator;
import tool.analyzers.buildingblocks.PresenceConditions;
import tool.analyzers.buildingblocks.ProductIterationHelper;
import tool.stats.CollectibleTimers;
import tool.stats.IFormulaCollector;
import tool.stats.ITimeCollector;
import expressionsolver.ExpressionSolver;

/**
 * Orchestrator of feature-product-based analysis.
 */
public class FeatureProductBasedAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(FeatureProductBasedAnalyzer.class.getName());

    private ExpressionSolver expressionSolver;
    private FeatureBasedFirstPhase firstPhase;

    /**
     * Sigma
     */
    private DerivationFunction<Boolean, String, Double> solve;

    private ITimeCollector timeCollector;

    public FeatureProductBasedAnalyzer(JADD jadd,
                                       ParametricModelChecker modelChecker,
                                       ITimeCollector timeCollector,
                                       IFormulaCollector formulaCollector) {
        this.expressionSolver = new ExpressionSolver(jadd);

        this.timeCollector = timeCollector;

        this.firstPhase = new FeatureBasedFirstPhase(modelChecker,
                                                     formulaCollector);


        solve = DerivationFunction.abstractDerivation(new IfOperator<Double>(),
                                                      expressionSolver::solveExpression,
                                                      1.0);
    }

    /**
     * Evaluates the feature-product-based reliability value of an RDG node, based
     * on the reliabilities of the nodes on which it depends.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @return
     * @throws CyclicRdgException
     * @throws UnknownFeatureException
     */
    public IReliabilityAnalysisResults evaluateReliability(RDGNode node, Stream<Collection<String>> configurations, ConcurrencyStrategy concurrencyStrategy) throws CyclicRdgException, UnknownFeatureException {
        List<RDGNode> dependencies = node.getDependenciesTransitiveClosure();

        timeCollector.startTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        // Alpha_v
        List<Component<String>> expressions = firstPhase.getReliabilityExpressions(dependencies, concurrencyStrategy);
        timeCollector.stopTimer(CollectibleTimers.MODEL_CHECKING_TIME);

        timeCollector.startTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);

        if (concurrencyStrategy == ConcurrencyStrategy.PARALLEL) {
            LOGGER.info("Evaluating all expressions for each product in parallel.");
        }
        Map<Collection<String>, Double> results = ProductIterationHelper.evaluate(configuration -> evaluateSingle(node,
                                                                                                                  configuration,
                                                                                                                  expressions),
                                                                                  configurations,
                                                                                  concurrencyStrategy);

        timeCollector.stopTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
        System.out.println("Feature-Product ******");

        return new MapBasedReliabilityResults(results);
    }

    private Double evaluateSingle(RDGNode node, Collection<String> configuration, List<Component<String>> expressions) {
        return Component.deriveFromMany(expressions,
                                        solve,
                                        c -> PresenceConditions.isPresent(c.getPresenceCondition(),
                                                                          configuration,
                                                                          expressionSolver));
    }

}
