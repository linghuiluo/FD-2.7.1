package soot.jimple.infoflow.cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.util.MultiMap;

/**
 * 
 * @author Linghui Luo
 *
 */
public class MainClassJava {

	private static List<String> sources;
	private static List<String> sinks;

	public static void main(String... args) throws IOException {
		Infoflow infoflow = new Infoflow();
		InfoflowConfiguration config = infoflow.getConfig();
		config.setCallgraphAlgorithm(CallgraphAlgorithm.SPARK);
		config.setInspectSources(false);
		config.setInspectSinks(false);
		config.setLogSourcesAndSinks(true);
		infoflow.setTaintWrapper(new EasyTaintWrapper());
		// config.getPathConfiguration().setPathReconstructionMode(PathReconstructionMode.NoPaths);
		String appPath = "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\organized-app.jar";
		String libPath = "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\averroes-lib-class.jar";
		libPath += File.pathSeparator + "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\placeholder-lib.jar";
		String sourceSinkFile = "E:\\Git\\Github\\callgraph\\CGBench_Test\\onlineshop\\onlineshop_source_sink.txt";
		loadSourceAndSinks(sourceSinkFile);
		IEntryPointCreator entryPointCreator = new GenCGEntryPointCreator();
		infoflow.computeInfoflow(appPath, libPath, entryPointCreator, sources, sinks);
		MultiMap<ResultSinkInfo, ResultSourceInfo> res = infoflow.getResults().getResults();
		if (res != null) {
			infoflow.getResults().printResults();
		}
	}

	private static void loadSourceAndSinks(String sourceSinkFile) {
		sources = new ArrayList<>();
		sinks = new ArrayList<>();
		ISourceSinkDefinitionProvider parser;
		try {
			parser = PermissionMethodParser.fromFile(sourceSinkFile);
			for (SourceSinkDefinition source : parser.getSources()) {
				sources.add(source.toString());
			}
			for (SourceSinkDefinition sink : parser.getSinks()) {
				sinks.add(sink.toString());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
