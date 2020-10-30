package soot.jimple.infoflow.sourcesSinks.definitions;

import java.util.Set;

public class ParameterSourceSinkDefinition extends SourceSinkDefinition {

	protected final String parameterSignature;
	protected final Set<AccessPathTuple> parameter;

	public ParameterSourceSinkDefinition(String signature) {
		this.parameterSignature = signature;
		this.parameter = null;
	}

	@Override
	public boolean isEmpty() {
		return parameterSignature != null;
	}

	@Override
	public ISourceSinkCategory getCategory() {
		return new ISourceSinkCategory() {

			@Override
			public String getID() {
				return "PARAMETER SOURCE";
			}

			@Override
			public String getHumanReadableDescription() {
				return "PARAMETER SOURCR: " + parameterSignature;
			}
		};
	}

	public String getParameterSignature() {
		return parameterSignature;
	}

	@Override
	public SourceSinkDefinition getSourceOnlyDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SourceSinkDefinition getSinkOnlyDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void merge(SourceSinkDefinition other) {
		// TODO Auto-generated method stub

	}

}
