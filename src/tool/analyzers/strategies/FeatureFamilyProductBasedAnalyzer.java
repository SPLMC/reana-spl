package tool.analyzers.strategies;

import jadd.JADD;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import paramwrapper.ParametricModelChecker;
import tool.CyclicRdgException;
import tool.RDGNode;
import tool.analyzers.IReliabilityAnalysisResults;
import tool.analyzers.MapBasedReliabilityResults;
import tool.analyzers.buildingblocks.Component;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;
import tool.analyzers.buildingblocks.PresenceConditions;
import tool.analyzers.buildingblocks.ProductIterationHelper;
import tool.stats.CollectibleTimers;
import tool.stats.IFormulaCollector;
import tool.stats.ITimeCollector;
import expressionsolver.Expression;
import expressionsolver.ExpressionSolver;

/**
 * Orchestrator of feature-family-product-based analyses.
 */
public class FeatureFamilyProductBasedAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(FamilyProductBasedAnalyzer.class.getName());

    private ExpressionSolver expressionSolver;

    private FeatureBasedFirstPhase firstPhase;

    private ITimeCollector timeCollector;

    public FeatureFamilyProductBasedAnalyzer(JADD jadd,
                               ParametricModelChecker modelChecker,
                               ITimeCollector timeCollector,
                               IFormulaCollector formulaCollector) {
        this.expressionSolver = new ExpressionSolver(jadd);

        this.timeCollector = timeCollector;
        this.firstPhase = new FeatureBasedFirstPhase(modelChecker,
                formulaCollector);

    }

    /**
     * Evaluates the feature-family-product-based reliability function of an RDG node.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @return
     * @throws CyclicRdgException
     */
    public IReliabilityAnalysisResults evaluateReliability(RDGNode node, Stream<Collection<String>> configurations, ConcurrencyStrategy concurrencyStrategy) throws CyclicRdgException {
    	List<RDGNode> dependencies = node.getDependenciesTransitiveClosure();

        timeCollector.startTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        // Alpha_v
        List<Component<String>> expressionsRDG = firstPhase.getReliabilityExpressions(dependencies, concurrencyStrategy);
        timeCollector.stopTimer(CollectibleTimers.MODEL_CHECKING_TIME);

        
        timeCollector.startTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
        
        List<String> presenceConditions = dependencies.stream()
        		.map(RDGNode::getPresenceCondition)
        		.collect(Collectors.toList());
        Map<String, String> pcEquivalence = PresenceConditions.toEquivalenceClasses(presenceConditions);
        Map<String, String> eqClassToPC = pcEquivalence.entrySet().stream()
        		.collect(Collectors.toMap(e -> e.getValue(),
        				e -> e.getKey(),
        				(a, b) -> a));

        // Lambda_v
        
        String expression = expressionGen(expressionsRDG);
        String finalExpression = changeParameters(expression, pcEquivalence, expressionsRDG);
        
        
        // Sigma_v
        
        Map<Collection<String>, Double> results;
        if (concurrencyStrategy == ConcurrencyStrategy.SEQUENTIAL) {
            Expression<Double> parsedExpression = expressionSolver.parseExpression(finalExpression);
            results = ProductIterationHelper.evaluate(configuration -> evaluateSingle(parsedExpression,
                                                                                      configuration,
                                                                                      eqClassToPC),
                                                      configurations,
                                                      concurrencyStrategy);
        } else {
            results = ProductIterationHelper.evaluate(configuration -> evaluateSingle(finalExpression,
                                                                                      configuration,
                                                                                      eqClassToPC),
                                                      configurations,
                                                      concurrencyStrategy);
        }

        timeCollector.stopTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
        LOGGER.info("Formulae evaluation ok...................");
        return new MapBasedReliabilityResults(results);
    }
    
    private String expressionGen(List<Component<String>> expressionsRDG) {
    	String expression = "";
    	LinkedHashMap<String,String> nodeExp = new LinkedHashMap<String,String>();
    	
    	for (Component<String> node : expressionsRDG) {
    			expression = node.getAsset();
    			expression = putSpaces(expression);
    			for (String key : nodeExp.keySet()) {
    					expression = expression.replace(key, " ( ( "+key+" * "+nodeExp.get(key)+" ) + ( 1 - "+key+" ) ) ");
    			}
    			nodeExp.put(node.getId()+" ",expression);
    	}
    	
    	
    	return nodeExp.get("BSN ");
    }
    
    
    // To avoid some mistakes while replacing strings
    private String putSpaces(String exp) {
    	exp = exp + " ";
    	exp = exp.replaceAll("\\*", " \\* ");
    	exp = exp.replaceAll("\\+", " \\+ ");
    	exp = exp.replaceAll("\\-", " \\- ");
    	exp = exp.replaceAll("\\/", " \\/ ");
    	exp = exp.replaceAll("\\(", "\\( ");
    	exp = exp.replaceAll("\\)", " \\)");

    	return exp;
    }
    
    private String changeParameters(String expression, Map<String, String> eqClassToPC, List<Component<String>> expressionsRDG) {
    	String newParam;
    	String oldParam;
    	for (Component<String> node : expressionsRDG) {
    		if (!(node.getPresenceCondition().contentEquals("true"))) {
    			newParam = eqClassToPC.get(node.getPresenceCondition());
    			oldParam = node.getId();
    			expression = expression.replaceAll(oldParam, newParam);
    		}
    		else {
    			oldParam = node.getId();
    			expression = expression.replaceAll(oldParam, "1");
    		}
    	}
    	return expression;
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

}
