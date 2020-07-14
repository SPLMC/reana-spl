package ui;

import java.io.File;
import java.io.IOException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import tool.PruningStrategy;
import tool.analyzers.AnalysisStrategy;
import tool.analyzers.buildingblocks.ConcurrencyStrategy;


/**
 * Command-line options.
 * @author thiago
 *
 */
class Options {
    private String featureModelFilePath;
    private String umlModelsFilePath;
    private String persistedAnalysesPath;
    private String paramPath;
    private String configuration;
    private String configurationsFilePath;
    private boolean printAllConfigurations;
    private boolean suppressReport;
    private boolean statsEnabled;
    private ConcurrencyStrategy concurrencyStrategy;
    private PruningStrategy pruningStrategy;
    private AnalysisStrategy analysisStrategy;

    static Options parseOptions(String[] args) throws IOException {
        OptionParser optionParser = new OptionParser();
        OptionSpec<String> featureModelOption = optionParser
                .accepts("feature-model")
                .withRequiredArg()
                .defaultsTo("BSN/fmBSN0.txt")
                .describedAs("File");
        OptionSpec<String> umlModelsOption = optionParser
                .accepts("uml-models")
                .withRequiredArg()
                .defaultsTo("BSN/bmBSN0.xml")
                .describedAs("File");
        OptionSpec<String> persistedAnalysesOption = optionParser
                .accepts("persisted-analyses",
                         "Path where previous analyses can be found and current analysis results are to be persisted")
                .withRequiredArg()
                .defaultsTo("ADDS")
                .describedAs("Directory");
        OptionSpec<String> paramPathOption = optionParser
                .accepts("param-path",
                         "Path to the parametric model checker (either PARAM or Prism)")
                .withRequiredArg()
                .defaultsTo("/opt/param-2-3-64")
                .describedAs("Directory");

        OptionSpec<String> configurationsFileOption = optionParser
                .accepts("configurations-file")
                .withRequiredArg()
                .defaultsTo("configurations.txt")
                .describedAs("File");
        OptionSpec<String> configurationOption = optionParser
                .accepts("configuration")
                .withRequiredArg();
        OptionSpec<Void> allConfigurationsOption = optionParser
                .accepts("all-configurations",
                         "Print the reliabilities of all valid configurations");
        OptionSpec<Void> suppressReportOption = optionParser
                .accepts("suppress-report",
                         "Suppress reliabilities report for all evaluated configurations");
        OptionSpec<Void> statsEnabledOption = optionParser
                .accepts("stats",
                         "Print profiling stats");

        OptionSpec<ConcurrencyStrategy> concurrencyStrategyOption = optionParser
                .accepts("concurrency-strategy",
                        "Run independent computations in parallel (PARALLEL) or sequentially (SEQUENTIAL)")
                        .withRequiredArg()
                        .ofType(ConcurrencyStrategy.class)
                        .defaultsTo(ConcurrencyStrategy.SEQUENTIAL)
                        .describedAs("PARALLEL | SEQUENTIAL");
        OptionSpec<PruningStrategy> pruningStrategyOption = optionParser
                .accepts("pruning-strategy",
                         "The strategy that should be used for pruning invalid configurations. Can be one of: FM (whole feature model); NONE (no pruning)")
                .withRequiredArg()
                .ofType(PruningStrategy.class)
                .defaultsTo(PruningStrategy.FM)
                .describedAs("FM | NONE");

        OptionSpec<AnalysisStrategy> analysisStrategyOption = optionParser
                .accepts("analysis-strategy",
                         "The strategy that should be used for analysis. Can be one of: "
                                 + "FEATURE_FAMILY (feature-family-based); "
                                 + "FEATURE_PRODUCT (feature-product-based); "
                                 + "FAMILY (family-based); "
                                 + "FAMILY_PRODUCT (family-product-based); "
                                 + "PRODUCT (product-based).")
                .withRequiredArg()
                .ofType(AnalysisStrategy.class)
                .defaultsTo(AnalysisStrategy.FEATURE_FAMILY)
                .describedAs("FEATURE_FAMILY | FEATURE_PRODUCT | FAMILY | FAMILY_PRODUCT | PRODUCT");

        OptionSpec<Void> helpOption = optionParser
                .accepts("help")
                .forHelp();

        OptionSet options = optionParser.parse(args);
        if (options.has(helpOption)) {
            optionParser.printHelpOn(System.out);
            System.exit(1);
        }

        Options result = new Options();
        result.featureModelFilePath = options.valueOf(featureModelOption);
        result.umlModelsFilePath = options.valueOf(umlModelsOption);
        result.persistedAnalysesPath = options.valueOf(persistedAnalysesOption);
        result.paramPath = options.valueOf(paramPathOption);
        result.configuration = options.valueOf(configurationOption);
        result.configurationsFilePath = options.valueOf(configurationsFileOption);
        result.printAllConfigurations = options.has(allConfigurationsOption);
        result.suppressReport = options.has(suppressReportOption);
        result.statsEnabled = options.has(statsEnabledOption);
        result.concurrencyStrategy = options.valueOf(concurrencyStrategyOption);
        result.pruningStrategy = options.valueOf(pruningStrategyOption);
        result.analysisStrategy = options.valueOf(analysisStrategyOption);

        return result;
    }

    public String getFeatureModelFilePath() {
        return featureModelFilePath;
    }

    public String getUmlModelsFilePath() {
        return umlModelsFilePath;
    }

    public String getPersistedAnalysesPath() {
        if (!persistedAnalysesPath.endsWith(File.separator)) {
            persistedAnalysesPath += File.separator;
        }
        return persistedAnalysesPath;
    }

    public String getParamPath() {
        return paramPath;
    }

    public boolean hasStatsEnabled() {
        return statsEnabled;
    }

    public boolean hasPrintAllConfigurations() {
        return printAllConfigurations;
    }

    public boolean hasSuppressReport() {
        return suppressReport;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getConfigurationsFilePath() {
        return configurationsFilePath;
    }

    public ConcurrencyStrategy getConcurrencyStrategy() {
        return concurrencyStrategy;
    }

    public PruningStrategy getPruningStrategy() {
        return pruningStrategy;
    }

    public AnalysisStrategy getAnalysisStrategy() {
        return analysisStrategy;
    }

    public void setFeatureModel(String featureModel){
        this.featureModelFilePath = featureModel;
    }

    public void setUmlModel(String umlModel){
        this.umlModelsFilePath = umlModel;
    }

}
