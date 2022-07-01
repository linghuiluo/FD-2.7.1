package soot.jimple.infoflow.cmd;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import soot.EntryPoints;
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
		classes.add("averroes.Library");
		return classes;
	}

	@Override
	public Collection<SootMethod> getAdditionalMethods() {
		SootMethod entryDoItAll = Scene.v().getMethod("<averroes.Library: void <clinit>()>");
        List<SootMethod> mainMethod = EntryPoints.v().mainsOfApplicationClasses();
        List<SootMethod> methods = new ArrayList<>();
        methods.add(entryDoItAll);
        methods.addAll(mainMethod);
		return methods;
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
		mainMethod = Scene.v().getMethod("<averroes.Library: void main(java.lang.String[])>");
		return mainMethod;
	}
}
