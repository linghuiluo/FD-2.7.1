package soot.jimple.infoflow.android;

public class SourceSinkInfo {
	public String id;
	public String source;
	public String sink;

	public SourceSinkInfo(String id, String source, String sink) {
		this.id = id;
		this.source = source;
		this.sink = sink;
	}

	@Override
	public String toString() {
		return "ID:" + id + "\nsource:" + source + "\nsink:" + sink;
	}
}
