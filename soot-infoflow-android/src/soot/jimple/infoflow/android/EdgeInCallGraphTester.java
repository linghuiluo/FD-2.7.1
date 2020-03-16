package soot.jimple.infoflow.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import soot.Printer;
import soot.Scene;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class EdgeInCallGraphTester {

	private static Set<SourceSinkInfo> sourcesSinkInfos = new HashSet<>();
	private static StringBuilder resultsRegardingSourcesAndSinks;
	private static Set<String> specifiedMethods = new HashSet<>();
	private static StringBuilder resultsRedardingspecifiedMethods;

	public static void test(CallGraph cg) {
		resultsRegardingSourcesAndSinks = new StringBuilder();
		resultsRedardingspecifiedMethods = new StringBuilder();
		Set<String> allMethods = new HashSet<>();
		StringBuilder output = new StringBuilder("specific edges:\n");
		for (Edge edge : cg) {
			String src = edge.getSrc().toString();
			String tgt = edge.getTgt().toString();
			allMethods.add(src);
			allMethods.add(tgt);
			for (String m : specifiedMethods) {
				if (src.equals(m) || tgt.equals(m)) {
					output.append(src + "\n\t=>\t" + tgt + "\n");
				}
			}
		}
		try {
			PrintWriter pw = new PrintWriter(
					new File("E:\\Git\\Github\\taintbench\\FlowDroid_Latest\\specific_edges_271.txt"));
			pw.println(output.toString());
			Printer.v().printTo(Scene.v().getSootClass("dummyMainClass"), pw);
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for (SourceSinkInfo info : sourcesSinkInfos) {
			String sourceMethod = info.source;
			String sinkMethod = info.sink;
			String id = info.id;
			if (!allMethods.contains(sourceMethod)) {
				resultsRegardingSourcesAndSinks.append(id + ";" + sourceMethod + ";source\n");
			}
			if (!allMethods.contains(sinkMethod)) {
				resultsRegardingSourcesAndSinks.append(id + ";" + sinkMethod + ";sink\n");
			}
		}
		for (String m : specifiedMethods) {
			if (!allMethods.contains(m))
				resultsRedardingspecifiedMethods.append(m + "\n");
		}
	}

	public static void setSourceSinkMethodsInfo(Set<SourceSinkInfo> sourceSinkInfos) {
		sourcesSinkInfos = sourceSinkInfos;
	}

	public static String getResultsRegardingSourcesAndSinks() {
		return resultsRegardingSourcesAndSinks.toString();
	}

	public static String getResultsRegardingSpecifiedMethods() {
		return resultsRedardingspecifiedMethods.toString();
	}

	public static void setSpecificMethods(Set<String> methods) {
		specifiedMethods = methods;
	}
}
