package soot.jimple.infoflow.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;

/**
 * 
 * @author Linghui Luo
 */
public class GenCGEntryPointCreator extends BaseEntryPointCreator {

	@Override
	public Collection<String> getRequiredClasses() {
		ArrayList<String> classes = new ArrayList<>();
		classes.add("averroes.DummyMainClass");
		classes.add("averroes.Library");
		return classes;
	}

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		SootMethod entryDoItAll = Scene.v().getMethod("<averroes.Library: void <clinit>()>");
		return Collections.singletonList(entryDoItAll);
	}

	@Override
	public Collection<SootField> getAdditionalFields() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SootMethod createDummyMainInternal() {
		return null;
	}

	@Override
	public SootMethod createDummyMain() {
		mainMethod = Scene.v().getMethod("<averroes.DummyMainClass: void main(java.lang.String[])>");
		return mainMethod;
	}
}
