package tool.analyzers.strategies;

import jadd.ADD;
import jadd.JADD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

//Usadas para criar a grande expressão
import java.util.HashMap;

/**
 * Orchestrator of FeatureFamilyProductBasedAnalyzer analyses.
 */
public class FeatureFamilyProductBasedAnalyzer {

    private ADD featureModel;
    private JADD jadd;
    private ExpressionSolver expressionSolver;
    private IPruningStrategy pruningStrategy;

    private FeatureBasedFirstPhase firstPhase;
    private FamilyBasedHelper helper;

    /**
     * Sigma_v
     */
//    private DerivationFunction<ADD, Expression<ADD>, ADD> solve;
//
//
    private ITimeCollector timeCollector;

    public FeatureFamilyProductBasedAnalyzer(JADD jadd,
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

//        AssetProcessor<Expression<ADD>, ADD> evalAndPrune = (expr, values) -> {
//            return this.pruningStrategy.pruneInvalidConfigurations(null,
//                                                                   expr.solve(values),
//                                                                   featureModel);
//        };
//        solve = DerivationFunction.abstractDerivation(ADD::ite,
//                                                      evalAndPrune,
//                                                      jadd.makeConstant(1.0));
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
    public IReliabilityAnalysisResults evaluateReliability(RDGNode node, Stream<Collection<String>> configurations, ConcurrencyStrategy concurrencyStrategy) throws CyclicRdgException {
        List<RDGNode> dependencies = node.getDependenciesTransitiveClosure();

        timeCollector.startTimer(CollectibleTimers.MODEL_CHECKING_TIME);
        // Alpha_v
        List<Component<String>> expressions = firstPhase.getReliabilityExpressions(dependencies, concurrencyStrategy);
        
        List<String> expressionsId = new ArrayList<String>();
        List<String> expressionsAsset = new ArrayList<String>();
        List<String> expressionsPresenceCondition = new ArrayList<String>();
        List<Collection<Component<String>>> expressionsDependencies = 
        		new ArrayList<Collection<Component<String>>>();
        
        for (int i = 0; i < expressions.size(); i++) {
        	expressionsId.add(expressions.get(i).getId());
        	expressionsAsset.add(expressions.get(i).getAsset().replaceAll("[\\[\\](){}]",""));
        	expressionsPresenceCondition.add(expressions.get(i).getPresenceCondition());
        	expressionsDependencies.add(expressions.get(i).getDependencies());
        	
        }
        
        HashMap<String, String> expressionsStructure = new HashMap<String, String>();
        
        
        for (int i = 0; i < expressions.size(); i++) {
        	expressionsStructure.put(expressions.get(i).getId(), expressions.get(i).getAsset().replaceAll("[\\[\\](){}]","")); 

        }
        System.out.println(expressionsStructure.get("BSN"));
            
        
        String rootNode = "BSN";
       
        String expressionBuilted = getITE(rootNode,expressionsStructure);
        System.out.println("-----------------------\nTENTATIVA DE FORMATO ITE");
        System.out.println(expressionBuilted);
        // Lift
//        List<Component<Expression<ADD>>> liftedExpressions = expressions.stream()
//                .map(helper::lift)
//                .collect(Collectors.toList());
//        System.out.println(liftedExpressions);
        // Sigma_v
//        ADD reliability = solveFromMany(liftedExpressions);
//        ADD result = featureModel.times(reliability);
        timeCollector.stopTimer(CollectibleTimers.EXPRESSION_SOLVING_TIME);
//
//        if (dotOutput != null) {
//            generateDotFile(result, dotOutput);
//        }
//
//        return new ADDReliabilityResults(result);
		return null;
    }
    
    public String getITE(String feature, HashMap<String, String> expressionsStructure) {
    	//System.out.println("entrando aqui - " + feature);
    	String[] featuresSeparadas = new String[30];
    	String builtExpression = "";
        featuresSeparadas = expressionsStructure.get(feature).split("\\/")[0].split("\\*|\\+");
        for (String a : featuresSeparadas) {
        	if(Character.isDigit(a.charAt(0))) {
        		if(!expressionsStructure.get(feature).contains("/")) {
        			builtExpression +=  feature + "*" + a + "*";
        		}else {
        			builtExpression += feature + "*" + a + "/" + expressionsStructure.get(feature).split("\\/")[1];
        		}
        	} else {
        			builtExpression += getITE(a,expressionsStructure) + " + (1-" + a + ")";
        	}
        }
        //System.out.println(feature + " = " + expressionsStructure.get(feature));
        //System.out.println("B0 ===" + builtExpression);
		return builtExpression;
    	
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

//    private ADD solveFromMany(List<Component<Expression<ADD>>> dependencies) {
//        return Component.deriveFromMany(dependencies,
//                                        solve,
//                                        c -> expressionSolver.encodeFormula(c.getPresenceCondition()));
//    }

}

