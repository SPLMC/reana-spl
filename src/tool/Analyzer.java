/**
 *
 */
package tool;

import jadd.ADD;
import jadd.JADD;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

import paramwrapper.IModelCollector;
import paramwrapper.ParamWrapper;
import paramwrapper.ParametricModelChecker;
import tool.analyzers.IPruningStrategy;
import tool.analyzers.IReliabilityAnalysisResults;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;
import tool.analyzers.strategies.FamilyBasedAnalyzer;
import tool.analyzers.strategies.FamilyProductBasedAnalyzer;
import tool.analyzers.strategies.FeatureFamilyBasedAnalyzer;
import tool.analyzers.strategies.FeatureProductBasedAnalyzer;
import tool.analyzers.strategies.ProductBasedAnalyzer;
import tool.analyzers.strategies.FeatureFamilyProductBasedAnalyzer;
import tool.stats.IFormulaCollector;
import tool.stats.ITimeCollector;
import tool.stats.NoopFormulaCollector;
import tool.stats.NoopTimeCollector;
import expressionsolver.ExpressionSolver;

/**
 * Implements the orchestration of analysis tasks.
 *
 * This is the Façade to the domain model.
 * Its responsibility is establishing **what** needs to be done.
 *
 * @author thiago
 */
public class Analyzer {

    private ADD featureModel;
    private ParametricModelChecker modelChecker;
    private ExpressionSolver expressionSolver;
    private JADD jadd;

    private ConcurrencyStrategy concurrencyStrategy = ConcurrencyStrategy.PARALLEL;

    private ITimeCollector timeCollector;
    private IFormulaCollector formulaCollector;

    FeatureFamilyBasedAnalyzer featureFamilyBasedAnalyzerImpl;
    FeatureProductBasedAnalyzer featureProductBasedAnalyzerImpl;
    ProductBasedAnalyzer productBasedAnalyzerImpl;
    FamilyBasedAnalyzer familyBasedAnalyzerImpl;
    FamilyProductBasedAnalyzer familyProductBasedAnalyzerImpl;
    FeatureFamilyProductBasedAnalyzer featureFamilyProductBasedAnalyzerImpl;

    /**
     * Creates an Analyzer which will follow the logical rules
     * encoded in the provided feature model file.
     *
     * @param featureModel String containing a CNF view of the Feature Model
     *          expressed using Java logical operators.
     * @throws IOException if there is a problem reading the file.
     */
    public Analyzer(String featureModel, String paramPath, ITimeCollector timeCollector, IFormulaCollector formulaCollector, IModelCollector modelCollector) {
        this(new JADD(), featureModel, paramPath, timeCollector, formulaCollector, modelCollector);
    }

    /**
     * Package-private constructor for testability.
     * It allows injection of ADD processor an feature model expression.
     * @param jadd
     * @param featureModel
     */
    Analyzer(JADD jadd, String featureModel, String paramPath) {
        this(jadd, featureModel, paramPath, null, null, null);
    }


    /**
     * This is where true initialization happens.
     * @param jadd
     * @param featureModel
     */
    private Analyzer(JADD jadd, String featureModel, String paramPath, ITimeCollector timeCollector, IFormulaCollector formulaCollector, IModelCollector modelCollector) {
        this.jadd = jadd;
        this.expressionSolver = new ExpressionSolver(jadd);
        this.featureModel = expressionSolver.encodeFormula(featureModel);
        // The feature model contains all used variables, so we expect to
        // be able to generate an optimal ordering right after parsing it.
        jadd.reorderVariables();

        this.timeCollector = (timeCollector != null) ? timeCollector : new NoopTimeCollector();
        this.formulaCollector = (formulaCollector != null) ? formulaCollector : new NoopFormulaCollector();
        this.modelChecker = (modelCollector != null) ? new ParamWrapper(paramPath, modelCollector) : new ParamWrapper(paramPath);

        this.featureFamilyBasedAnalyzerImpl = new FeatureFamilyBasedAnalyzer(this.jadd,
                                                                             this.featureModel,
                                                                             this.modelChecker,
                                                                             this.timeCollector,
                                                                             this.formulaCollector);
        this.featureProductBasedAnalyzerImpl = new FeatureProductBasedAnalyzer(this.jadd,
                                                                               this.modelChecker,
                                                                               this.timeCollector,
                                                                               this.formulaCollector);
        this.productBasedAnalyzerImpl = new ProductBasedAnalyzer(this.jadd,
                                                                 this.modelChecker,
                                                                 this.timeCollector,
                                                                 this.formulaCollector);
        this.familyBasedAnalyzerImpl = new FamilyBasedAnalyzer(this.jadd,
                                                               this.featureModel,
                                                               this.modelChecker,
                                                               this.timeCollector,
                                                               this.formulaCollector);
        this.familyProductBasedAnalyzerImpl = new FamilyProductBasedAnalyzer(this.jadd,
                                                                             this.modelChecker,
                                                                             this.timeCollector,
                                                                             this.formulaCollector);
        this.featureFamilyProductBasedAnalyzerImpl = new FeatureFamilyProductBasedAnalyzer(this.jadd,
                                                                             this.modelChecker,
                                                                             this.timeCollector,
                                                                             this.formulaCollector);
    }

    /**
     * Returns the set of all valid configurations according to the feature model.
     * @return
     */
    public Stream<Collection<String>> getValidConfigurations() {
        return featureModel.getExpandedConfigurations();
    }

    /**
     * Returns true if and only if {@code configuration} is valid according
     * to the feature model.
     * @param configuration
     * @return
     */
    public boolean isValidConfiguration(Collection<String> configuration) {
        return featureModel.isValidConfiguration(configuration);
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
        this.featureFamilyBasedAnalyzerImpl.setPruningStrategy(pruningStrategy);
    }

    public void setConcurrencyStrategy(ConcurrencyStrategy concurrencyStrategy) {
        this.concurrencyStrategy = concurrencyStrategy;
    }

    /**
     * Evaluates the feature-family-based reliability function of an RDG node, based
     * on the reliabilities of the nodes on which it depends.
     *
     * A reliability function is a boolean function from the set of features
     * to Real values, where the reliability of any invalid configuration is 0.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @param dotOutput path at where to dump the resulting ADD as a dot file.
     * @return
     * @throws CyclicRdgException
     */
    public IReliabilityAnalysisResults evaluateFeatureFamilyBasedReliability(RDGNode node, String dotOutput) throws CyclicRdgException {
        return featureFamilyBasedAnalyzerImpl.evaluateReliability(node, this.concurrencyStrategy, dotOutput);
    }
    /**
     * Evaluates the feature-family-based reliability function of an RDG node, based
     * on the reliabilities of the nodes on which it depends, but does not dump the
     * resulting ADD.
     *
     * @see {@link Analyzer.evaluateFeatureFamilyBasedReliability(RDGNode, String)}
     */
    public IReliabilityAnalysisResults evaluateFeatureFamilyBasedReliability(RDGNode node) throws CyclicRdgException {
        return evaluateFeatureFamilyBasedReliability(node, null);
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
    public IReliabilityAnalysisResults evaluateFeatureProductBasedReliability(RDGNode node, Stream<Collection<String>> configurations) throws CyclicRdgException, UnknownFeatureException {
        return featureProductBasedAnalyzerImpl.evaluateReliability(node, configurations, this.concurrencyStrategy);
    }

    /**
     * Evaluates the product-based reliability value of an RDG node, based
     * on the derived model for the given configuration.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @return
     * @throws CyclicRdgException
     * @throws UnknownFeatureException
     */
    public IReliabilityAnalysisResults evaluateProductBasedReliability(RDGNode node, Stream<Collection<String>> configurations) throws CyclicRdgException, UnknownFeatureException {
        return productBasedAnalyzerImpl.evaluateReliability(node, configurations, this.concurrencyStrategy);
    }

    /**
     * Evaluates the family-based reliability value of an RDG node, based
     * on the derived 150% model.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @return
     * @throws CyclicRdgException
     * @throws UnknownFeatureException
     */
    public IReliabilityAnalysisResults evaluateFamilyBasedReliability(RDGNode node, Stream<Collection<String>> configurations) throws CyclicRdgException, UnknownFeatureException {
        return familyBasedAnalyzerImpl.evaluateReliability(node, this.concurrencyStrategy);
    }

    /**
     * Evaluates the family-product-based reliability value of an RDG node, based
     * on the derived 150% model.
     *
     * @param node RDG node whose reliability is to be evaluated.
     * @return
     * @throws CyclicRdgException
     * @throws UnknownFeatureException
     */
    public IReliabilityAnalysisResults evaluateFamilyProductBasedReliability(RDGNode node, Stream<Collection<String>> configurations) throws CyclicRdgException, UnknownFeatureException {
        return familyProductBasedAnalyzerImpl.evaluateReliability(node, configurations, this.concurrencyStrategy);
    }

    public IReliabilityAnalysisResults evaluateFeatureFamilyProductBasedReliability(RDGNode node, Stream<Collection<String>> configurations) throws CyclicRdgException, UnknownFeatureException {
        return featureFamilyProductBasedAnalyzerImpl.evaluateReliability(node, configurations, this.concurrencyStrategy);
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
        featureFamilyBasedAnalyzerImpl.generateDotFile(familyReliability, outputFile);
    }

}
