/**
 *
 */
package ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import modeling.DiagramAPI;
import modeling.IModelerAPI;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import paramwrapper.IModelCollector;
import parsing.SplGeneratorModels.SplGeneratorModelingAPI;
import parsing.exceptions.InvalidNodeClassException;
import parsing.exceptions.InvalidNodeType;
import parsing.exceptions.InvalidNumberOfOperandsException;
import parsing.exceptions.InvalidTagException;
import parsing.exceptions.UnsupportedFragmentTypeException;
import tool.Analyzer;
import tool.CyclicRdgException;
import tool.PruningStrategyFactory;
import tool.RDGNode;
import tool.UnknownFeatureException;
import tool.analyzers.IReliabilityAnalysisResults;
import tool.stats.CollectibleTimers;
import tool.stats.IFormulaCollector;
import tool.stats.IMemoryCollector;
import tool.stats.ITimeCollector;
import ui.stats.StatsCollectorFactory;
import jadd.ADD;
import jadd.JADD;
import tool.analyzers.strategies.FeatureFamilyBasedAnalyzer;

/**
 * Command-line application.
 *
 * @author thiago
 *
 */
public class CommandLineInterface {
    private static final Logger LOGGER = Logger.getLogger(CommandLineInterface.class.getName());
    private static final PrintStream OUTPUT = System.out;

    private static IMemoryCollector memoryCollector;
    private static ITimeCollector timeCollector;
    private static IFormulaCollector formulaCollector;
    private static IModelCollector modelCollector;

    private CommandLineInterface() {
        // NO-OP
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, ADD> previousAnalysis = new HashMap<String, ADD>();
        Options options = Options.parseOptions(args);
        int evolutionNumber = Integer.parseInt(options.getFeatureModelFilePath().replaceAll("[^0-9]", ""));

        if(evolutionNumber == 0){
            LogManager logManager = LogManager.getLogManager();
            try {
                logManager.readConfiguration(new FileInputStream("logging.properties"));
            } catch(FileNotFoundException e) {
                e.printStackTrace();
            }
            initializeStatsCollectors(options);

            memoryCollector.takeSnapshot("before model parsing");
            RDGNode rdgRoot = buildRDG(options);
            memoryCollector.takeSnapshot("after model parsing");

            Analyzer analyzer = makeAnalyzer(options, 0);
            Stream<Collection<String>> targetConfigurations = getTargetConfigurations(options, analyzer);

            memoryCollector.takeSnapshot("before evaluation");
            long analysisStartTime = System.currentTimeMillis();
            Stream<Collection<String>> validConfigs = targetConfigurations.filter(analyzer::isValidConfiguration);

            IReliabilityAnalysisResults familyReliability =  evaluateReliability(analyzer,
                                                                                 rdgRoot,
                                                                                 validConfigs,
                                                                                 options,
                                                                                 previousAnalysis);

            long totalAnalysisTime = System.currentTimeMillis() - analysisStartTime;
            memoryCollector.takeSnapshot("after evaluation");

//            if (!options.hasSuppressReport()) {
//                Map<Boolean, List<Collection<String>>> splitConfigs = getTargetConfigurations(options, analyzer)
//                        .collect(Collectors.partitioningBy(analyzer::isValidConfiguration));
//                printAnalysisResults(splitConfigs, familyReliability);
//            }

            if (options.hasStatsEnabled()) {
                printStats(OUTPUT, familyReliability, rdgRoot);
            }
            long totalRunningTime = System.currentTimeMillis() - startTime;
            OUTPUT.println("Total analysis time: " +  totalAnalysisTime + " ms");
            OUTPUT.println("Total running time: " +  totalRunningTime + " ms");
            
            File directory = new File("ADDS");
            if(!directory.exists())
                directory.mkdir();

            for(String i : previousAnalysis.keySet()){
                analyzer.getJadd().dumpADD(i, previousAnalysis.get(i),"ADDS/" + i + ".add");
            }
            
            analyzer.getJadd().writeVariableStore("variableStore.add");

        }

        else{
            Analyzer analyzer = makeAnalyzer(options, evolutionNumber, true);

            evolveModel(options, analyzer, evolutionNumber, previousAnalysis);

            long totalRunningTime = System.currentTimeMillis() - startTime;
            OUTPUT.println("Total running time: " +  totalRunningTime + " ms");
        }
    }

    /**
     * @param analyzer
     * @param rdgRoot
     * @param options
     * @return
     */
    private static IReliabilityAnalysisResults evaluateReliability(Analyzer analyzer, RDGNode rdgRoot, Stream<Collection<String>> validConfigs, Options options, Map<String, ADD> previousAnalysis) {
        IReliabilityAnalysisResults results = null;
        switch (options.getAnalysisStrategy()) {
        case FEATURE_PRODUCT:
            results = evaluateReliability(analyzer::evaluateFeatureProductBasedReliability,
                                          rdgRoot,
                                          validConfigs);
            break;
        case PRODUCT:
            results = evaluateReliability(analyzer::evaluateProductBasedReliability,
                                          rdgRoot,
                                          validConfigs);
            break;
        case FAMILY:
            results = evaluateReliability(analyzer::evaluateFamilyBasedReliability,
                                          rdgRoot,
                                          validConfigs);
            break;
        case FAMILY_PRODUCT:
            results = evaluateReliability(analyzer::evaluateFamilyProductBasedReliability,
                                          rdgRoot,
                                          validConfigs);
            break;
        case FEATURE_FAMILY:
        default:
            results = evaluateFeatureFamilyBasedReliability(analyzer,
                                                            rdgRoot,
                                                            options,
                                                            previousAnalysis);
        }
        return results;
    }

    private static IReliabilityAnalysisResults evaluateReliabilityWithEvolution(Analyzer analyzer, RDGNode rdgRoot, Stream<Collection<String>> validConfigs, Options options, String idFragment, Map<String, ADD> previousAnalysis){
      IReliabilityAnalysisResults results = null;
      results = evaluateFeatureFamilyBasedReliabilityWithEvolution(analyzer,
                                                                   rdgRoot,
                                                                   options,
                                                                   idFragment, previousAnalysis);
      return results;
    }

    private static IReliabilityAnalysisResults evaluateFeatureFamilyBasedReliability(Analyzer analyzer, RDGNode rdgRoot, Options options, Map<String, ADD> previousAnalysis) {
        IReliabilityAnalysisResults results = null;
        String dotOutput = "family-reliability.dot";
        try {
            analyzer.setPruningStrategy(PruningStrategyFactory.createPruningStrategy(options.getPruningStrategy()));
            results = analyzer.evaluateFeatureFamilyBasedReliability(rdgRoot, "saida.dot", previousAnalysis);
        } catch (CyclicRdgException e) {
            LOGGER.severe("Cyclic dependency detected in RDG.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(2);
        }
        OUTPUT.println("Family-wide reliability decision diagram dumped at " + dotOutput);
        return results;
    }

    private static IReliabilityAnalysisResults evaluateFeatureFamilyBasedReliabilityWithEvolution(Analyzer analyzer, RDGNode rdgRoot, Options options, String idFragment, Map<String, ADD> previousAnalysis) {
      IReliabilityAnalysisResults results = null;
      String dotOutput = "family-reliability.dot";
      try {
        analyzer.setPruningStrategy(PruningStrategyFactory.createPruningStrategy(options.getPruningStrategy()));
        results = analyzer.evaluateFeatureFamilyBasedReliabilityWithEvolution(rdgRoot, "saida.dot", idFragment, previousAnalysis);
      } catch (CyclicRdgException e) {
        LOGGER.severe("Cyclic dependency detected in RDG.");
        LOGGER.log(Level.SEVERE, e.toString(), e);
        System.exit(2);
      }
      return results;
    }

    private static IReliabilityAnalysisResults evaluateReliability(BiFunction<RDGNode, Stream<Collection<String>>, IReliabilityAnalysisResults> analyzer,
                                                                   RDGNode rdgRoot,
                                                                   Stream<Collection<String>> validConfigs) {
        IReliabilityAnalysisResults results = null;
        try {
            results = analyzer.apply(rdgRoot, validConfigs);
        } catch (CyclicRdgException e) {
            LOGGER.severe("Cyclic dependency detected in RDG.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(2);
        } catch (UnknownFeatureException e) {
            LOGGER.severe("Unrecognized feature: " + e.getFeatureName());
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        return results;
    }

    /**
     * @param options
     * @return
     */
    private static Analyzer makeAnalyzer(Options options, int i) {
        File featureModelFile = new File(options.getFeatureModelFilePath());
        String featureModel = readFeatureModel(featureModelFile);

        String paramPath = options.getParamPath();
        Analyzer analyzer = new Analyzer(featureModel,
                                         paramPath,
                                         timeCollector,
                                         formulaCollector,
                                         modelCollector,
                                         i);
        analyzer.setConcurrencyStrategy(options.getConcurrencyStrategy());
        return analyzer;
    }

    private static Analyzer makeAnalyzer(Options options, int i, boolean evol) {
        File featureModelFile = new File(options.getFeatureModelFilePath());
        String featureModel = readFeatureModel(featureModelFile);

        String paramPath = options.getParamPath();
        Analyzer analyzer = new Analyzer(featureModel,
                                         paramPath,
                                         timeCollector,
                                         formulaCollector,
                                         modelCollector,
                                         i,
                                         evol);
        analyzer.setConcurrencyStrategy(options.getConcurrencyStrategy());
        return analyzer;
    }

    /**
     * @param options
     */
    private static void initializeStatsCollectors(Options options) {
        StatsCollectorFactory statsCollectorFactory = new StatsCollectorFactory(options.hasStatsEnabled());
        memoryCollector = statsCollectorFactory.createMemoryCollector();
        timeCollector = statsCollectorFactory.createTimeCollector();
        formulaCollector = statsCollectorFactory.createFormulaCollector();
        modelCollector = statsCollectorFactory.createModelCollector();
    }

    private static Stream<Collection<String>> getTargetConfigurations(Options options, Analyzer analyzer) {
        if (options.hasPrintAllConfigurations()) {
            return analyzer.getValidConfigurations();
        } else {
            Set<Collection<String>> configurations = new HashSet<Collection<String>>();

            List<String> rawConfigurations = new LinkedList<String>();
            if (options.getConfiguration() != null) {
                rawConfigurations.add(options.getConfiguration());
            } else {
                Path configurationsFilePath = Paths.get(options.getConfigurationsFilePath());
                try {
                    rawConfigurations.addAll(Files.readAllLines(configurationsFilePath, Charset.forName("UTF-8")));
                } catch (IOException e) {
                    LOGGER.severe("Error reading the provided configurations file.");
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
            }

            for (String rawConfiguration: rawConfigurations) {
                String[] variables = rawConfiguration.split(",");
                configurations.add(Arrays.asList(variables));
            }

            return configurations.stream();
        }
    }

    private static void printAnalysisResults(Map<Boolean, List<Collection<String>>> splitConfigs, IReliabilityAnalysisResults familyReliability) {
        OUTPUT.println("Configurations:");
        OUTPUT.println("=========================================");
//        String[] teste = {"ACC", "Temperature", "SQLite", "ECG", "TEMP", "Storage", "Oxygenation", "Monitoring"};
        List<Collection<String>> validConfigs = splitConfigs.get(true);
        // Ordered report
        validConfigs.sort((c1, c2) -> c1.toString().compareTo(c2.toString()));
        for (Collection<String> validConfig: validConfigs) {
            try {
                String[] configurationAsArray = validConfig.toArray(new String[validConfig.size()]);
//                OUTPUT.println(familyReliability.getResult(teste).toString());
                printSingleConfiguration(validConfig.toString(),
                                         familyReliability.getResult(configurationAsArray));
            } catch (UnknownFeatureException e) {
                LOGGER.severe("Unrecognized feature: " + e.getFeatureName());
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }
        for (Collection<String> invalidConfig: splitConfigs.get(false)) {
            printSingleConfiguration(invalidConfig.toString(), 0);
        }

        OUTPUT.println("=========================================");
        OUTPUT.println(">>>> Total valid configurations: " + splitConfigs.get(true).size());
    }

    private static void printSingleConfiguration(String configuration, double reliability) {
        String message = configuration + " --> ";
        if (Double.doubleToRawLongBits(reliability) != 0) {
            OUTPUT.println(message + reliability);
        } else {
            OUTPUT.println(message + "INVALID");
        }
    }

    private static void printStats(PrintStream out, IReliabilityAnalysisResults familyReliability, RDGNode rdgRoot) {
        out.println("-----------------------------");
        out.println("Stats:");
        out.println("------");
        timeCollector.printStats(out);
        formulaCollector.printStats(out);
        modelCollector.printStats(out);
        memoryCollector.printStats(out);
        printEvaluationReuse(rdgRoot);
        familyReliability.printStats(out);
    }

    private static void printEvaluationReuse(RDGNode rdgRoot) {
        try {
            Map<RDGNode, Integer> numberOfPaths = rdgRoot.getNumberOfPaths();
            int nodes = 0;
            int totalPaths = 0;
            for (Map.Entry<RDGNode, Integer> entry: numberOfPaths.entrySet()) {
                nodes++;
                totalPaths += entry.getValue();
                OUTPUT.println(entry.getKey() + ": " + entry.getValue() + " paths");
            }
            OUTPUT.println("Evaluation economy because of cache: " + 100*(totalPaths-nodes)/(float)totalPaths + "%");
        } catch (CyclicRdgException e) {
            LOGGER.severe("Cyclic dependency detected in RDG.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(2);
        }
    }

    /**
     * @param featureModelFile
     * @return
     */
    private static String readFeatureModel(File featureModelFile) {
        String featureModel = null;
        Path path = featureModelFile.toPath();
        try {
            featureModel = new String(Files.readAllBytes(path), Charset.forName("UTF-8"));
        } catch (IOException e) {
            LOGGER.severe("Error reading the provided Feature Model.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }
        return featureModel;
    }

    /**
     * @param options
     * @return
     */
    private static RDGNode buildRDG(Options options) {
        File umlModels = new File(options.getUmlModelsFilePath());
        RDGNode rdgRoot = null;
        try {
            rdgRoot = model(umlModels, timeCollector);
        } catch (DOMException | UnsupportedFragmentTypeException
                | InvalidTagException | InvalidNumberOfOperandsException
                | InvalidNodeClassException | InvalidNodeType e) {
            LOGGER.severe("Error reading the provided UML Models.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }
        return rdgRoot;
    }

    /**
     * Abstracts UML to RDG transformation.
     *
     * @param umlModels
     * @return
     * @throws InvalidTagException
     * @throws UnsupportedFragmentTypeException
     * @throws DOMException
     * @throws InvalidNodeType
     * @throws InvalidNodeClassException
     * @throws InvalidNumberOfOperandsException
     */
    private static RDGNode model(File umlModels, ITimeCollector timeCollector) throws UnsupportedFragmentTypeException, InvalidTagException, InvalidNumberOfOperandsException, InvalidNodeClassException, InvalidNodeType {
    	String exporter = identifyExporter(umlModels);
    	IModelerAPI modeler = null;

    	timeCollector.startTimer(CollectibleTimers.PARSING_TIME);

    	switch (exporter) {
		case "MagicDraw":
			modeler = new DiagramAPI(umlModels);

			break;

		case "SplGenerator":
			modeler = new SplGeneratorModelingAPI(umlModels);
			break;

		default:
			break;
		}


    	RDGNode result = modeler.transform();
    	timeCollector.stopTimer(CollectibleTimers.PARSING_TIME);

        return result;
    }

    /**
     * @author andlanna
     * This method's role is to identify which behavioral model exporter was
     * used for generating activity and sequence diagrams.
     * @param umlModels - the XML file representing the SPL's activity and sequence diagrams.
     * @return a string with the name of the exporter
     */
	private static String identifyExporter(File umlModels) {
		String answer = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(umlModels);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		NodeList nodes = doc.getElementsByTagName("xmi:exporter");
		if (nodes.getLength() > 0) {
			Element e = (Element) nodes.item(0);
			if (e.getTextContent().equals("MagicDraw UML")) {
				answer = "MagicDraw";
			}
		} else {
			answer = "SplGenerator";
		}


		return answer;
	}

	private static void evolveModel(Options options, Analyzer analyzer, int numberOfEvolutions, Map<String, ADD> previousAnalysis){
      Map<String, ADD> analysis = getPreviousAnalysis(analyzer.getJadd(), "ADDS");

      LogManager logManager = LogManager.getLogManager();
      try {
          logManager.readConfiguration(new FileInputStream("logging.properties"));
      } catch(FileNotFoundException e) {
          e.printStackTrace();
      } catch(IOException e) {

      }
      initializeStatsCollectors(options);

      memoryCollector.takeSnapshot("before model parsing");
      RDGNode rdgRoot = buildRDG(options);
      memoryCollector.takeSnapshot("after model parsing");
      Stream<Collection<String>> targetConfigurations = getTargetConfigurations(options, analyzer);
      memoryCollector.takeSnapshot("before evaluation");
      long analysisStartTime = System.currentTimeMillis();
      Stream<Collection<String>> validConfigs = targetConfigurations.filter(analyzer::isValidConfiguration);
      IReliabilityAnalysisResults familyReliability = evaluateReliabilityWithEvolution(analyzer,
                                                                                       rdgRoot,
                                                                                       validConfigs,
                                                                                       options,
                                                                                       getFragmentId(numberOfEvolutions),
                                                                                       analysis);

      memoryCollector.takeSnapshot("after evaluation");
      
//      if (!options.hasSuppressReport()) {
//          Map<Boolean, List<Collection<String>>> splitConfigs = getTargetConfigurations(options, analyzer)
//                  .collect(Collectors.partitioningBy(analyzer::isValidConfiguration));
//          printAnalysisResults(splitConfigs, familyReliability);
//      }

      if (options.hasStatsEnabled()) {
          printStats(OUTPUT, familyReliability, rdgRoot);
      }

      File directory = new File("ADDS");
      if(!directory.exists())
          directory.mkdir();

      for(String i : analysis.keySet()){
          analyzer.getJadd().dumpADD(i, analysis.get(i),"ADDS/" + i + ".add");
      }
      
      analyzer.getJadd().writeVariableStore("variableStore.add");
      long totalAnalysisTime = System.currentTimeMillis() - analysisStartTime;
      OUTPUT.println("Total analysis time: " +  totalAnalysisTime + " ms\n\n");
      
  }

  private static String getFragmentId(int numberOfEvolutions){
      if(numberOfEvolutions == 0)
          return "";
      else
          return ("SD_" + String.valueOf(3 * numberOfEvolutions - 1 ));
  }

  public static Map<String, ADD> getPreviousAnalysis(JADD jadd, String directoryName) {
      Map<String, ADD> previousAnalysis = new HashMap<String, ADD>();

      File directory = new File(directoryName);

      if(!directory.exists())
          return previousAnalysis;
      File previousADDs[] = directory.listFiles();
      for(File file : previousADDs) {
        String fileName = file.getName();
        previousAnalysis.put(fileName.substring(0, fileName.length() - 4), jadd.readADDpreviousAnalysis("ADDS/"+fileName));
      }

      return previousAnalysis;
  }
}
