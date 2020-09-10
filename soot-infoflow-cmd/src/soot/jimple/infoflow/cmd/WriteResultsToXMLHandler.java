package soot.jimple.infoflow.cmd;

import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamException;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.results.xml.InfoflowResultsSerializer;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public class WriteResultsToXMLHandler implements ResultsAvailableHandler {
	private String resultsFile;
	private InfoflowConfiguration config;

	public WriteResultsToXMLHandler(String resultsFile, InfoflowConfiguration config) {
		this.resultsFile = resultsFile;
		this.config = config;
	}

	@Override
	public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
		if (resultsFile != null && !resultsFile.isEmpty()) {
			InfoflowResultsSerializer serializer = new InfoflowResultsSerializer(cfg, config);
			try {
				serializer.serialize(results, resultsFile);
			} catch (FileNotFoundException ex) {
				System.err.println("Could not write data flow results to file: " + ex.getMessage());
				ex.printStackTrace();
			} catch (XMLStreamException ex) {
				System.err.println("Could not write data flow results to file: " + ex.getMessage());
				ex.printStackTrace();
			}
		}

	}

}
