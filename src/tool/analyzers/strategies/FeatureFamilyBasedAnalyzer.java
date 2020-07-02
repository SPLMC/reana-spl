package tool.analyzers.strategies;

import jadd.ADD;
import jadd.JADD;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

import paramwrapper.ParametricModelChecker;
import tool.CyclicRdgException;
import tool.RDGNode;
import tool.analyzers.ADDReliabilityResults;
import tool.analyzers.IPruningStrategy;
import tool.analyzers.IReliabilityAnalysisResults;
import tool.analyzers.NoPruningStrategy;
import tool.analyzers.buildingblocks.AssetProcessor;
import tool.analyzers.buildingblocks.Component;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;
import tool.analyzers.buildingblocks.DerivationFunction;
import tool.analyzers.buildingblocks.FamilyBasedHelper;
import tool.stats.CollectibleTimers;
import tool.stats.IFormulaCollector;
import tool.stats.ITimeCollector;
import expressionsolver.Expression;
import expressionsolver.ExpressionSolver;

/**
 * Orchestrator of feature-family-based analyses.
 */
public class FeatureFamilyBasedAnalyzer {

    private ADD featureModel;
    private JADD jadd;
    private ExpressionSolver expressionSolver;
    private IPruningStrategy pruningStrategy;

    private FeatureBasedFirstPhase firstPhase;
    private FamilyBasedHelper helper;

    /**
     * Sigma_v
     */
    private DerivationFunction<ADD, Expression<ADD>, ADD> solve;


    private ITimeCollector timeCollector;

    public FeatureFamilyBasedAnalyzer(JADD jadd,
                                      ADD featureModel,
                                      ParametricModelChecker modelChecker,
                                      ITimeCollector timeCollector,
                                      IFormulaCollector formulaCollector) {
        this.expressionSolver = new ExpressionSolver(jadd);
        this.jadd = jadd;
        this.featureModel = featureModel;

        this.timeCollector = timeCollector;
        this.pruningStrategy = new NoPruningStrategy();

        this.firstPhase = new FeatureBasedFirstPhase(modelChecker,
                                                     formulaCollector);
        this.helper = new FamilyBasedHelper(expressionSolver);

        AssetProcessor<Expression<ADD>, ADD> evalAndPrune = (expr, values) -> {
            return this.pruningStrategy.pruneInvalidConfigurations(null,
                                                                   expr.solve(values),
                                                                   featureModel);
        };
        solve = DerivationFunction.abstractDerivation(ADD::ite,
                                                      evalAndPrune,
                                                      jadd.makeConstant(1.0));
    }

    /**
     * Evaluates the feature-family-based reliability function of an RDG node, based
     * on the reliabilities of the nodes on which it depends.
     *
     * A reliability function is a boolean function from the set of features
     * to Real values, where the reliability of any invalid configuration is 0.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @param concurrencyStrategy
     * @param dotOutput path at where to dump the resulting ADD as a dot file.
     * @return
     * @throws CyclicRdgException
     */
    public IReliabilityAnalysisResults evaluateReliability(RDGNode node, ConcurrencyStrategy concurrencyStrategy, String dotOutput, Map<String, ADD> previousAnalysis) throws CyclicRdgException {
        List<RDGNode> dependencies = node.getDependenciesTransitiveClosure();

        timeCollector.startTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        // Alpha_v
        long alphaTime = System.currentTimeMillis();
        List<Component<String>> expressions = firstPhase.getReliabilityExpressions(dependencies, concurrencyStrategy);
        timeCollector.stopTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        alphaTime = System.currentTimeMillis() - alphaTime;
        timeCollector.startTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
        System.out.println ("++++++ Alpha Time: " + alphaTime + " ++++++");

        // Lift
        List<Component<Expression<ADD>>> liftedExpressions = expressions.stream()
                .map(helper::lift)
                .collect(Collectors.toList());
        // Sigma_v
        long sigmaTime = System.currentTimeMillis();
        ADD reliability = newSolveFromMany(liftedExpressions, previousAnalysis);
        ADD result = featureModel.times(reliability);
        sigmaTime = System.currentTimeMillis() - sigmaTime;
        System.out.println ("++++++ Sigma Time: " + sigmaTime + " ++++++");

        timeCollector.stopTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);

        return new ADDReliabilityResults(result);
    }

    public IReliabilityAnalysisResults evaluateReliabilityWithEvolution(RDGNode node, ConcurrencyStrategy concurrencyStrategy, String dotOutput, String idFragment, Map<String, ADD> previousAnalysis) throws CyclicRdgException {
    	 System.out.println ("***** Entrou no evaluateRealiabilityWithEvolution *****");

    	List<RDGNode> dependencies = getModifiedNodes(node, idFragment, previousAnalysis);
    	long alphaTime = System.currentTimeMillis();
        timeCollector.startTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        // Alpha_v
        List<Component<String>> expressions = firstPhase.getReliabilityExpressions(dependencies, concurrencyStrategy);
        timeCollector.stopTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        alphaTime = System.currentTimeMillis() - alphaTime;
        System.out.println ("++++++ Alpha Time: " + alphaTime + " ++++++");
        timeCollector.startTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
        // Lift
        long liftTime = System.currentTimeMillis();
        List<Component<Expression<ADD>>> liftedExpressions = expressions.stream()
                .map(helper::lift)
                .collect(Collectors.toList());
        liftTime = System.currentTimeMillis() - liftTime;
        System.out.println ("++++++ Lift Time: " + liftTime + " ++++++");
        // Sigma_v
        long sigmaTime = System.currentTimeMillis();

        ADD reliability = newSolveFromMany(liftedExpressions, previousAnalysis);
        ADD result = featureModel.times(reliability);
        sigmaTime = System.currentTimeMillis() - sigmaTime;

        System.out.println ("++++++ Sigma Time: " + sigmaTime + " ++++++");

        timeCollector.stopTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);

        return new ADDReliabilityResults(result);
    }

    private static List<RDGNode> getModifiedNodes(RDGNode root,String idFragment, Map<String, ADD> previousAnalysis){
        if(previousAnalysis.isEmpty())
            return root.getDependenciesTransitiveClosure();
        else{
            List<RDGNode> impacted = new LinkedList();
            getImpactedNodes(root, idFragment, impacted);
            impacted.add(root);
            return impacted;
        }
    }

    private static String getImpactedNodes(RDGNode node, String id, List<RDGNode> impacted){
    	if (node == null)
    		return id;
    	String newId;
        Iterator<RDGNode> itr = node.getDependencies().iterator();
        while(itr.hasNext()){
        	RDGNode aux = itr.next();
        	newId = getImpactedNodes(aux, id, impacted);
            if (aux.getId().equals(newId)) {
            	impacted.add(aux);
            	return node.getId();
            }
        }
        return id;
    }

    /**
     * Sets the pruning strategy to be used for preventing calculation
     * of reliability values for invalid configurations.
     *
     * If none is set, the default behavior is to multiply the reliability
     * mappings by the feature model's 0,1-ADD (so that valid configurations
     * yield the same reliability, but invalid ones yield 0).
     *
     * @param pruningStrategy the pruningStrategy to set
     */
    public void setPruningStrategy(IPruningStrategy pruningStrategy) {
        this.pruningStrategy = pruningStrategy;
    }

    /**
     * Dumps the computed family reliability function to the output file
     * in the specified path.
     *
     * @param familyReliability Reliability function computed by a call to the
     *          {@link #evaluateFeatureFamilyBasedReliability(RDGNode)} method.
     * @param outputFile Path to the .dot file to be generated.
     */
    public void generateDotFile(ADD familyReliability, String outputFile) {
        jadd.dumpDot("Family Reliability", familyReliability, outputFile);
    }

    private ADD solveFromMany(List<Component<Expression<ADD>>> dependencies) {
        return Component.deriveFromMany(dependencies,
                                        solve,
                                        c -> expressionSolver.encodeFormula(c.getPresenceCondition()));
    }

    private ADD newSolveFromMany(List<Component<Expression<ADD>>> dependencies, Map<String, ADD> previousAnalysis) {
        return Component.newDeriveFromMany(dependencies,
                                        solve,
                                        c -> expressionSolver.encodeFormula(c.getPresenceCondition()), previousAnalysis);
    }
}
