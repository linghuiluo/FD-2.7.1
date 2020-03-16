package soot.jimple.infoflow.cmd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.foellix.aql.datastructure.Answer;
import de.foellix.aql.datastructure.Attribute;
import de.foellix.aql.datastructure.Flow;
import de.foellix.aql.datastructure.Flows;
import de.foellix.aql.datastructure.Reference;
import de.foellix.aql.datastructure.handler.AnswerHandler;
import soot.jimple.infoflow.android.EdgeInCallGraphTester;
import soot.jimple.infoflow.android.SourceSinkInfo;

public class Runner271 {
	private static String platformDir = "E:\\Git\\androidPlatforms";
	private static String taintBench = "E:\\Git\\Github\\taintbench\\taint-benchmark\\apps\\android";

	public static void main(String... args) throws Exception {
		String benchmark = "hummingbad_android_samp";
		HashSet<String> methods = new HashSet<>();
		runSingleBenchmark(benchmark, methods);
	}

	public static void called() throws Exception {
		{
			String benchmark = "godwon_samp";
			HashSet<String> methods = new HashSet<>();
			methods.add("<android.sms.core.GoogleService: void onCreate()>");
			runSingleBenchmark(benchmark, methods);
		}
		{
			String benchmark = "fakeplay";
			HashSet<String> methods = new HashSet<>();
			methods.add("<com.googleprojects.mm.JHService: void smsReceived(android.content.Intent)>");
			methods.add(
					"<com.googleprojects.mm.MMMailContentUtil: java.lang.String makeMMMessageBody(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,boolean,java.lang.String)>");
			methods.add(
					"<com.googleprojects.mm.MMMailSender: void sendMail(java.lang.String,java.lang.String,java.lang.String,java.lang.String)>");
			methods.add("<javax.mail.internet.MimeMessage: void setDataHandler(javax.activation.DataHandler)>");
			methods.add("<javax.mail.Transport: void send(javax.mail.Message)>");
			runSingleBenchmark(benchmark, methods);
		}
		{
			String benchmark = "beita_com_beita_contact";
			HashSet<String> methods = new HashSet<>();
			methods.add("<com.beita.contact.MyContacts: void onCreate(android.os.Bundle)>");

			methods.add("<android.os.AsyncTask: android.os.AsyncTask execute(java.lang.Object[])>");
			methods.add("<android.os.AsyncTask: android.os.AsyncTask void onPostExecute(java.util.List)>");
			methods.add("<android.os.AsyncTask: java.lang.Void doInBackground(java.lang.Void[])>");

			methods.add(
					"<com.beita.contact.MyContacts$RetrieceDataTask: android.os.AsyncTask execute(java.lang.Object[])>");
			methods.add("<com.beita.contact.MyContacts$RetrieceDataTask: void onPostExecute(java.util.List)>");
			methods.add(
					"<com.beita.contact.MyContacts$UploadAsyncTask: android.os.AsyncTask execute(java.lang.Object[])>");
			methods.add(
					"<com.beita.contact.MyContacts$UploadAsyncTask: java.lang.Void doInBackground(java.lang.Void[])>");
			methods.add("<com.beita.contact.UploadUtil: void uploadFile()>");
			runSingleBenchmark(benchmark, methods);
		}
	}

	public static void runAllBenchmarks() throws Exception {
		String expectedDir = "E:\\Git\\Github\\taintbench\\26_02_20\\TB\\FlowDroid_new\\export_fd_new_tb_3\\expected";
		PrintWriter printer = new PrintWriter(new File("missed.csv"));
		for (File benchmark : new File(taintBench).listFiles()) {
			if (benchmark.isDirectory()) {
				String pathToBenchmark = benchmark.toString();
				String benchmarkName = benchmark.getName();
				String apk = pathToBenchmark + File.separator + benchmarkName + ".apk";
				String findings = pathToBenchmark + File.separator + benchmarkName + "_findings.json";
				String results = pathToBenchmark + File.separator + benchmarkName + "_results_fd_271.xml";
				String sourcesAndSinks = pathToBenchmark + File.separator + benchmarkName + "_SourcesAndSinks.txt";
				File sourcesAndSinksFile = new File(sourcesAndSinks);
				if (!sourcesAndSinksFile.exists()) {
					createSourcesAndSinksFile(new File(findings), sourcesAndSinksFile);
				}
				readPositiveCases(expectedDir, benchmarkName);
				runFlowDroidforApk(apk, sourcesAndSinks, results);
				String res = EdgeInCallGraphTester.getResultsRegardingSourcesAndSinks();
				if (!res.isEmpty()) {
					printer.println(benchmarkName);
					printer.println(res);
				}
			}

		}
		printer.flush();
		printer.close();
	}

	public static void runSingleBenchmark(String benchmarkName, Set<String> methods) throws Exception {
		String pathToBenchmark = taintBench + File.separator + benchmarkName;
		String apk = pathToBenchmark + File.separator + benchmarkName + ".apk";
		String findings = pathToBenchmark + File.separator + benchmarkName + "_findings.json";
		String results = pathToBenchmark + File.separator + benchmarkName + "_results_fd_15.xml";
		String sourcesAndSinks = pathToBenchmark + File.separator + benchmarkName + "_SourcesAndSinks.txt";
		File sourcesAndSinksFile = new File(sourcesAndSinks);
		if (!sourcesAndSinksFile.exists()) {
			createSourcesAndSinksFile(new File(findings), sourcesAndSinksFile);
		}
		EdgeInCallGraphTester.setSpecificMethods(methods);
		runFlowDroidforApk(apk, sourcesAndSinks, results);
		String res = EdgeInCallGraphTester.getResultsRegardingSpecifiedMethods();
		System.out.println("\n\nAbsence in callgraph:\n" + res);
	}

	public static void createSourcesAndSinksFile(File file, File output) {
		JsonParser parser = new JsonParser();
		try {
			JsonObject obj = parser.parse(new FileReader(file)).getAsJsonObject();
			JsonArray findings = obj.getAsJsonArray("findings");
			Set<String> sourcesInJimple = new HashSet<>();
			Set<String> sinksInJimple = new HashSet<>();
			for (int i = 0; i < findings.size(); i++) {
				JsonObject finding = findings.get(i).getAsJsonObject();
				if (finding.has("source")) {
					JsonObject source = finding.get("source").getAsJsonObject();
					JsonObject jimple = source.get("IRs").getAsJsonArray().get(0).getAsJsonObject();
					String sourceInJimple = jimple.get("IRstatement").getAsString();
					String sourceAPI = "<" + StringUtils.substringBetween(sourceInJimple, "<", ">") + "> -> _SOURCE_";
					sourcesInJimple.add(sourceAPI);
				}
				if (finding.has("sink")) {
					JsonObject sink = finding.get("sink").getAsJsonObject();
					JsonObject jimple = sink.get("IRs").getAsJsonArray().get(0).getAsJsonObject();
					String sinkInJimple = jimple.get("IRstatement").getAsString();
					String sinkAPI = "<" + StringUtils.substringBetween(sinkInJimple, "<", ">") + "> -> _SINK_";
					sinksInJimple.add(sinkAPI);
				}
			}
			StringBuilder str = new StringBuilder();
			for (String s : sourcesInJimple)
				str.append(s + "\n");
			for (String s : sinksInJimple)
				str.append(s + "\n");
			PrintWriter writer = new PrintWriter(output);
			writer.println(str.toString());
			writer.flush();
			writer.close();
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static void readPositiveCases(String expectedDir, String apkName) {
		if (apkName.contains("overlaylocker2_android_samp"))
			apkName = apkName.replace("overlaylocker2_android_samp", "18verlaylocker2androidsampapk");
		if (apkName.contains("smsstealer_kysn_assassincreed_android_samp"))
			apkName = apkName.replace("smsstealer_kysn_assassincreed_android_samp", "31assassincreedandroidsampapk");
		if (apkName.contains("stels_flashplayer_android_update"))
			apkName = apkName.replace("stels_flashplayer_android_update", "34flashplayerandroidupdateapk");
		String suffix = "_positive_case_" + apkName + ".xml";
		Set<SourceSinkInfo> sourceSinkInfos = new HashSet<>();
		StringBuilder str = new StringBuilder();

		for (File f : new File(expectedDir).listFiles()) {
			if (f.getName().endsWith(suffix)) {
				Answer answer = AnswerHandler.parseXML(f);
				Flows flows = answer.getFlows();
				for (Flow flow : flows.getFlow()) {
					String taintBenchID = "";
					for (Attribute a : flow.getAttributes().getAttribute()) {

						if (a.getName().equals("TaintBenchID")) {
							taintBenchID = a.getValue();
						}
					}
					List<Reference> refs = flow.getReference();
					Reference source = null;
					Reference sink = null;
					for (Reference ref : refs) {
						String type = ref.getType();
						if (type.equals("from")) {
							source = ref;
						} else if (type.equals("to")) {
							sink = ref;
						}
					}
					String sourceMethod = source.getMethod();
					String sinkMethod = sink.getMethod();
					sourceSinkInfos.add(new SourceSinkInfo(taintBenchID, sourceMethod, sinkMethod));
				}
			}
		}
		EdgeInCallGraphTester.setSourceSinkMethodsInfo(sourceSinkInfos);
	}

	public static void runFlowDroidforApk(String apk, String sourceAndSinks, String results) throws Exception {
		String[] options = { "-a", apk, "-p", platformDir, "-s", sourceAndSinks, "-o", results };
		MainClass.main(options);
	}
}
