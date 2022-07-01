package soot.jimple.infoflow.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.PathReconstructionMode;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.source.parsers.xml.XMLSourceSinkParser;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.ParameterSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.DefaultSourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

/**
 * 
 * @author Linghui Luo
 *
 */
public class MainClassForJar {

	private Logger logger = LoggerFactory.getLogger(MainClassForJar.class);
	private Options options = new Options();

	public MainClassForJar() {
		options.addOption("a", "appJarPath", true, "Jar file to analyze");
		options.addOption("l", "libPath", true, "libary jar files");
		options.addOption("s", "sourcessinksfile", true, "Definition file for sources and sinks");
		options.addOption("o", "outputfile", true, "Output XML file for the discovered data flows");
		options.addOption("cp", "paths", false,
				"Compute the taint propagation paths and not just source-to-sink connections. This is a shorthand notation for -pr fast.");
		options.addOption("ol", "outputlinenumbers", false,
				"Enable the output of bytecode line numbers associated with sources and sinks in XML results");
	}

	public void run(String... args) throws Exception {
		final HelpFormatter formatter = new HelpFormatter();
		if (args.length == 0) {
			formatter.printHelp("soot-infoflow-cmd [OPTIONS]", options);
			return;
		}
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		// Do we need to display the user manual?
		if (cmd.hasOption("?") || cmd.hasOption("help")) {
			formatter.printHelp("soot-infoflow-cmd [OPTIONS]", options);
			return;
		}
		String appPath = cmd.getOptionValue("a");
		if (appPath == null) {
            formatter.printHelp("soot-infoflow-cmd [OPTIONS]", options);
            return;
        }
		String libPath = cmd.getOptionValue("l");
		String sourceSinkFile = cmd.getOptionValue("s");
		if (sourceSinkFile == null)
			return;
		String resultsFile = cmd.getOptionValue("o");
		if (resultsFile == null)
			resultsFile = appPath.replace(".jar", ".xml");
		Infoflow infoflow = new Infoflow();
		InfoflowConfiguration config = infoflow.getConfig();
		config.setCallgraphAlgorithm(CallgraphAlgorithm.SPARK);
		config.setInspectSources(false);
		config.setInspectSinks(false);
		config.setLogSourcesAndSinks(true);
		infoflow.setTaintWrapper(initializeDefaultTaintWrapper());
		if (cmd.hasOption("cp"))
			config.getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.Fast);
		if (cmd.hasOption("ol"))
			config.setEnableLineNumbers(true);
        ISourceSinkDefinitionProvider ssProvider = null;
        if(sourceSinkFile.endsWith(".xml")) {
            ssProvider = XMLSourceSinkParser.fromFile(sourceSinkFile);
        } else {
            ssProvider = PermissionMethodParser.fromFile(sourceSinkFile);
        }
		IEntryPointCreator entryPointCreator = new GenCGEntryPointCreator();
		infoflow.addResultsAvailableHandler(new WriteResultsToXMLHandler(resultsFile, config));
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, new DefaultSourceSinkManager(ssProvider));
	}

	/**
	 * Initializes the default taint wrapper.
	 */
	private ITaintPropagationWrapper initializeDefaultTaintWrapper() throws Exception {
		ITaintPropagationWrapper result = new SummaryTaintWrapper(new LazySummaryProvider("summariesManual"));
		return result;
	}
}
