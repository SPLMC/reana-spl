package tool.analyzers.strategies;

import jadd.JADD;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import paramwrapper.ParametricModelChecker;
import tool.CyclicRdgException;
import tool.RDGNode;
import tool.UnknownFeatureException;
import tool.analyzers.IReliabilityAnalysisResults;
import tool.analyzers.MapBasedReliabilityResults;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;
import tool.analyzers.buildingblocks.PresenceConditions;
import tool.analyzers.buildingblocks.ProductIterationHelper;
import tool.analyzers.buildingblocks.Component;
import tool.stats.CollectibleTimers;
import tool.stats.IFormulaCollector;
import tool.stats.ITimeCollector;
import expressionsolver.Expression;
import expressionsolver.ExpressionSolver;

public class FeatureFamilyProductBasedAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(FeatureProductBasedAnalyzer.class.getName());

    private ExpressionSolver expressionSolver;
    private FeatureBasedFirstPhase firstPhase;

    private ITimeCollector timeCollector;
    private IFormulaCollector formulaCollector;

    public FeatureFamilyProductBasedAnalyzer(JADD jadd,
                                       ParametricModelChecker modelChecker,
                                       ITimeCollector timeCollector,
                                       IFormulaCollector formulaCollector) {
        this.expressionSolver = new ExpressionSolver(jadd);

        this.timeCollector = timeCollector;

        this.formulaCollector = formulaCollector;

        this.firstPhase = new FeatureBasedFirstPhase(modelChecker,
                                                     formulaCollector);
    }

    public IReliabilityAnalysisResults evaluateReliability(RDGNode node, Stream<Collection<String>> configurations, ConcurrencyStrategy concurrencyStrategy) throws CyclicRdgException, UnknownFeatureException {
        if (concurrencyStrategy == ConcurrencyStrategy.PARALLEL) {
            LOGGER.info("Solving the family-wide expression for each product in parallel.");
        }
        List<RDGNode> dependencies = node.getDependenciesTransitiveClosure();

        timeCollector.startTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        List<String> presenceConditions = dependencies.stream()
                .map(RDGNode::getPresenceCondition)
                .collect(Collectors.toList());

        String expression = variabilityEncodingExpression(firstPhase.getReliabilityExpressions(dependencies, concurrencyStrategy), presenceConditions);
        formulaCollector.collectFormula(node, expression);
        timeCollector.stopTimer(CollectibleTimers.MODEL_CHECKING_TIME);

        timeCollector.startTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);

        Map<String, String> pcEquivalence = PresenceConditions.toEquivalenceClasses(presenceConditions);
        Map<String, String> eqClassToPC = pcEquivalence.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getValue(),
                                          e -> e.getKey(),
                                          (a, b) -> a));

        Map<Collection<String>, Double> results;
        if (concurrencyStrategy == ConcurrencyStrategy.SEQUENTIAL) {
            Expression<Double> parsedExpression = expressionSolver.parseExpression(expression);
            results = ProductIterationHelper.evaluate(configuration -> evaluateSingle(parsedExpression,
                                                                                      configuration,
                                                                                      eqClassToPC),
                                                      configurations,
                                                      concurrencyStrategy);
        } else {
            results = ProductIterationHelper.evaluate(configuration -> evaluateSingle(expression,
                                                                                      configuration,
                                                                                      eqClassToPC),
                                                      configurations,
                                                      concurrencyStrategy);
        }

        timeCollector.stopTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
        LOGGER.info("Formulae evaluation ok...");
        return new MapBasedReliabilityResults(results);
    }

    private Double evaluateSingle(Expression<Double> expression, Collection<String> configuration, Map<String, String> eqClassToPC) {
        Function<Map.Entry<String, String>, Boolean> isPresent = e -> PresenceConditions.isPresent(e.getValue(),
                                                                                                   configuration,
                                                                                                   expressionSolver);
        Map<String, Double> values = eqClassToPC.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey(),
                                      isPresent.andThen(present -> present ? 1.0 : 0.0)));

        return expression.solve(values);

    }

    private Double evaluateSingle(String expression, Collection<String> configuration, Map<String, String> eqClassToPC) {
        Expression<Double> parsedExpression = expressionSolver.parseExpression(expression);
        return evaluateSingle(parsedExpression, configuration, eqClassToPC);
    }

    private String variabilityEncodingExpression(List<Component<String>> expressions, List<String> presenceConditions){
        Collections.reverse(expressions);
        String expression = expressions.get(0).getAsset();
        for(int i = 1; i < expressions.size(); i++){
            String ITE = IfThenElse(expressions.get(i).getPresenceCondition(), expressions.get(i).getAsset, "1");
        	  expression = expression.replaceAll("\b"expressions.get(i).getId()"\b", ITE);
        }

        Map<String, String> pcEquivalence = PresenceConditions.toEquivalenceClasses(presenceConditions);

        for(String i :pcEquivalence.keySet()){
            expression = expression.replaceAll("\b"+i+"\b", pcEquivalence.get(i));
        }
        return expression;
    }

    public String ifThenElse(String presence, String ifPresent, String ifAbsent){
        return "("+presence+"*("+ifPresent+")+(1-"+presence+")*("+ifAbsent+"))";
    }
}
