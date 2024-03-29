/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.sourcesSinks.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import heros.solver.IDESolver;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ParameterSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.internal.JIdentityStmt;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.tagkit.VisibilityParameterAnnotationTag;

/**
 * A {@link ISourceSinkManager} working on lists of source and sink methods
 * 
 * @author Steven Arzt
 */
public class DefaultSourceSinkManager implements ISourceSinkManager {

	private Collection<String> sourceDefs;
	private Collection<String> sinkDefs;
	private Collection<String> parameterSourceDefs;
	private HashMap<Stmt, AccessPath> parameterSourceStmtAccessPath;
	private HashMap<Stmt, String> parameterSourcesStmtDef;

	private Collection<SootMethod> sources;
	private Collection<SootMethod> sinks;

	private Collection<String> returnTaintMethodDefs;
	private Collection<String> parameterTaintMethodDefs;

	private Collection<SootMethod> returnTaintMethods;
	private Collection<SootMethod> parameterTaintMethods;

	private HashMap<String, MethodSourceSinkDefinition> methodSourceSinkDefMap;

	protected final LoadingCache<SootClass, Collection<SootClass>> interfacesOf = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootClass, Collection<SootClass>>() {

				@Override
				public Collection<SootClass> load(SootClass sc) throws Exception {
					Set<SootClass> set = new HashSet<SootClass>(sc.getInterfaceCount());
					for (SootClass i : sc.getInterfaces()) {
						set.add(i);
						set.addAll(interfacesOf.getUnchecked(i));
					}
					SootClass superClass = sc.getSuperclassUnsafe();
					if (superClass != null)
						set.addAll(interfacesOf.getUnchecked(superClass));
					return set;
				}

			});

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sources The list of methods to be treated as sources
	 * @param sinks   The list of methods to be treated as sins
	 */
	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks) {
		this(sources, sinks, null, null);
	}

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sources               The list of methods to be treated as sources
	 * @param sinks                 The list of methods to be treated as sinks
	 * @param parameterTaintMethods The list of methods whose parameters shall be
	 *                              regarded as sources
	 * @param returnTaintMethods    The list of methods whose return values shall be
	 *                              regarded as sinks
	 */
	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks,
			Collection<String> parameterTaintMethods, Collection<String> returnTaintMethods) {
		this.sourceDefs = sources;
		this.sinkDefs = sinks;
		this.parameterTaintMethodDefs = (parameterTaintMethods != null) ? parameterTaintMethods : new HashSet<String>();
		this.returnTaintMethodDefs = (returnTaintMethods != null) ? returnTaintMethods : new HashSet<String>();
	}

	public DefaultSourceSinkManager(Collection<String> sources, Collection<String> sinks,
			Collection<String> parameterSources) {
		this.sourceDefs = sources;
		this.sinkDefs = sinks;
		this.parameterSourceDefs = parameterSources;
		this.parameterTaintMethodDefs = null;
		this.returnTaintMethodDefs = null;
	}

	/**
	 * Creates a new instance of the {@link DefaultSourceSinkManager} class
	 * 
	 * @param sourceSinkProvider The provider that defines source and sink methods
	 */
	public DefaultSourceSinkManager(ISourceSinkDefinitionProvider sourceSinkProvider) {
		this.sourceDefs = new HashSet<>();
		this.sinkDefs = new HashSet<>();
		this.parameterSourceDefs = new HashSet<>();
		this.methodSourceSinkDefMap = new HashMap<>();
		// Load the sources
		for (SourceSinkDefinition ssd : sourceSinkProvider.getSources()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				sourceDefs.add(mssd.getMethod().getSignature());
				methodSourceSinkDefMap.put(mssd.getMethod().getSignature(), mssd);
			}
			if (ssd instanceof ParameterSourceSinkDefinition) {
				ParameterSourceSinkDefinition pssd = (ParameterSourceSinkDefinition) ssd;
				parameterSourceDefs.add(pssd.getParameterSignature());
			}

		}

		// Load the sinks
		for (SourceSinkDefinition ssd : sourceSinkProvider.getSinks()) {
			if (ssd instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition mssd = (MethodSourceSinkDefinition) ssd;
				methodSourceSinkDefMap.put(mssd.getMethod().getSignature(), mssd);
				sinkDefs.add(mssd.getMethod().getSignature());
			}
		}

	}

	/**
	 * Sets the list of methods to be treated as sources
	 * 
	 * @param sources The list of methods to be treated as sources
	 */
	public void setSources(List<String> sources) {
		this.sourceDefs = sources;
	}

	/**
	 * Sets the list of methods to be treated as sinks
	 * 
	 * @param sinks The list of methods to be treated as sinks
	 */
	public void setSinks(List<String> sinks) {
		this.sinkDefs = sinks;
	}

	public void checkIfParameterIsSource(SootMethod m, InfoflowManager manager) {
		if (this.parameterSourceStmtAccessPath == null)
			this.parameterSourceStmtAccessPath = new HashMap();
		if (this.parameterSourcesStmtDef == null)
			this.parameterSourcesStmtDef = new HashMap();
		if (this.parameterSourceDefs == null)
			this.parameterSourceDefs = new HashSet<>();
		VisibilityParameterAnnotationTag tag = (VisibilityParameterAnnotationTag) m
				.getTag("VisibilityParameterAnnotationTag");
		HashMap<Value, AccessPath> paraSources = new HashMap<>();
		HashMap<Value, String> paraSourceDefs = new HashMap<>();
		if (tag != null) {
			for (int i = 0; i < tag.getVisibilityAnnotations().size(); i++) {
				VisibilityAnnotationTag t = tag.getVisibilityAnnotations().get(i);
				if (t != null)
					for (AnnotationTag at : t.getAnnotations()) {
						String type = at.getType();
						String typeInSoot = type.substring(1, type.length() - 1).replace("/", ".");
						if (this.parameterSourceDefs.contains(typeInSoot)) {
							Value paraRef = m.getActiveBody().getParameterRefs().get(i);
							AccessPath targetAP = manager.getAccessPathFactory()
									.createAccessPath(m.getActiveBody().getParameterLocal(i), true);
							paraSources.put(paraRef, targetAP);
							paraSourceDefs.put(paraRef, typeInSoot);
						}
					}
			}
		}
		if (!paraSources.isEmpty()) {
			for (Unit u : m.getActiveBody().getUnits()) {
				if (paraSources.isEmpty())
					break;
				if (u instanceof JIdentityStmt) {
					Value right = ((JIdentityStmt) u).getRightOp();
					boolean remove = false;
					if (paraSources.containsKey(right)) {
						Stmt s = (Stmt) u;
						this.parameterSourceStmtAccessPath.put(s, paraSources.get(right));
						this.parameterSourcesStmtDef.put(s, paraSourceDefs.get(right));
						remove = true;

					}
					if (remove)
						paraSources.remove(right);

				}
			}
		}
	}

	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
		SootMethod callee = sCallSite.containsInvokeExpr() ? sCallSite.getInvokeExpr().getMethod() : null;
		AccessPath targetAP = null;
		if (isSourceMethod(manager, sCallSite)) {
			if (callee.getReturnType() != null && sCallSite instanceof DefinitionStmt) {
				// Taint the return value
				Value leftOp = ((DefinitionStmt) sCallSite).getLeftOp();
				targetAP = manager.getAccessPathFactory().createAccessPath(leftOp, true);
			} else if (sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
				// Taint the base object
				Value base = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
				targetAP = manager.getAccessPathFactory().createAccessPath(base, true);
			}
		}
		// Check whether we need to taint parameters
		else if (sCallSite instanceof IdentityStmt) {
			IdentityStmt istmt = (IdentityStmt) sCallSite;
			if (istmt.getRightOp() instanceof ParameterRef) {
				if (this.parameterSourceStmtAccessPath.containsKey(istmt)) {
					targetAP = this.parameterSourceStmtAccessPath.get(istmt);
					String parameterSourceDef = this.parameterSourcesStmtDef.get(istmt);
					return new SourceInfo(new ParameterSourceSinkDefinition(parameterSourceDef), targetAP);
				} else {
					ParameterRef pref = (ParameterRef) istmt.getRightOp();
					SootMethod currentMethod = manager.getICFG().getMethodOf(istmt);
					if (parameterTaintMethods != null && parameterTaintMethods.contains(currentMethod))
						targetAP = manager.getAccessPathFactory().createAccessPath(
								currentMethod.getActiveBody().getParameterLocal(pref.getIndex()), true);
				}
			}
		} else if (sCallSite instanceof ReturnStmt) {

		}

		if (targetAP == null)
			return null;

		// Create the source information data structure
		return new SourceInfo(callee == null ? null : new MethodSourceSinkDefinition(new SootMethodAndClass(callee)),
				targetAP);
	}

	/**
	 * Checks whether the given call sites invokes a source method
	 * 
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param sCallSite The call site to check
	 * @return True if the given call site invoked a source method, otherwise false
	 */
	private boolean isSourceMethod(InfoflowManager manager, Stmt sCallSite) {
		// We only support method calls
		if (!sCallSite.containsInvokeExpr())
			return false;

		// Check for a direct match
		SootMethod callee = sCallSite.getInvokeExpr().getMethod();
		if (this.sources.contains(callee) || this.sourceDefs.contains(callee.getSignature()))
			return true;

		// Check whether we have any of the interfaces on the list
		String subSig = callee.getSubSignature();
		for (SootClass i : interfacesOf.getUnchecked(sCallSite.getInvokeExpr().getMethod().getDeclaringClass())) {
			SootMethod sm = i.getMethodUnsafe(subSig);
			if (sm != null && (this.sources.contains(sm) || this.sourceDefs.contains(sm.getSignature())))
				return true;
		}

		// Ask the CFG in case we don't know any better
		for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
			if (this.sources.contains(sm) || this.sourceDefs.contains(sm.getSignature()))
				return true;
		}

		// nothing found
		return false;
	}

	@Override
	public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
		// Check whether values returned by the current method are to be
		// considered as sinks
		if (this.returnTaintMethods != null && sCallSite instanceof ReturnStmt) {
			SootMethod sm = manager.getICFG().getMethodOf(sCallSite);
			if (this.returnTaintMethods != null && this.returnTaintMethods.contains(sm))
				return new SinkInfo(new MethodSourceSinkDefinition(new SootMethodAndClass(sm)));
		}

		// Check whether the callee is a sink
		if (this.sinkDefs != null && !sinkDefs.isEmpty() && sCallSite.containsInvokeExpr()) {
			InvokeExpr iexpr = sCallSite.getInvokeExpr();

			// Is this method on the list?
			SootMethodAndClass smac = isSinkMethod(manager, sCallSite);
			if (smac != null) {
				// Check that the incoming taint is visible in the callee at all
				if (SystemClassHandler.isTaintVisible(ap, iexpr.getMethod())) {
					// If we don't have an access path, we can only
					// over-approximate
					if (ap == null) {
						return new SinkInfo(getMethodSourceSinkDefinition(smac));
					}

					// The given access path must at least be referenced
					// somewhere in the sink
					if (!ap.isStaticFieldRef()) {
						for (int i = 0; i < iexpr.getArgCount(); i++)
							if (iexpr.getArg(i) == ap.getPlainValue()) {
								if (ap.getTaintSubFields() || ap.isLocal())
									return new SinkInfo(getMethodSourceSinkDefinition(smac));
							}
						if (iexpr instanceof InstanceInvokeExpr)
							if (((InstanceInvokeExpr) iexpr).getBase() == ap.getPlainValue())
								return new SinkInfo(getMethodSourceSinkDefinition(smac));
					}
				}
			}
		}

		return null;
	}

	private MethodSourceSinkDefinition getMethodSourceSinkDefinition(SootMethodAndClass smac) {
		MethodSourceSinkDefinition def = null;
		if (methodSourceSinkDefMap != null && methodSourceSinkDefMap.containsKey(smac.getSignature())) {
			def = methodSourceSinkDefMap.get(smac.getSignature());
		} else {
			def = new MethodSourceSinkDefinition(smac);
		}
		return def;
	}

	@Override
	public void addSource(SootMethod m) {
		sources.add(m);
		sourceDefs.add(m.getSignature());
	}

	/**
	 * Checks whether the given call sites invokes a sink method
	 * 
	 * @param manager   The manager object providing access to the configuration and
	 *                  the interprocedural control flow graph
	 * @param sCallSite The call site to check
	 * @return The method that was discovered as a sink, or null if no sink could be
	 *         found
	 */
	private SootMethodAndClass isSinkMethod(InfoflowManager manager, Stmt sCallSite) {
		// Is the method directly in the sink set?
		SootMethod callee = sCallSite.getInvokeExpr().getMethod();
		if (this.sinks.contains(callee) || this.sinkDefs.contains(callee.getSignature()))
			return new SootMethodAndClass(callee);

		// Check whether we have any of the interfaces on the list
		String subSig = callee.getSubSignature();
		for (SootClass i : interfacesOf.getUnchecked(sCallSite.getInvokeExpr().getMethod().getDeclaringClass())) {
			SootMethod sm = i.getMethodUnsafe(subSig);
			if (sm != null && (this.sinks.contains(sm) || this.sinkDefs.contains(sm.getSignature())))
				return new SootMethodAndClass(sm);
		}

		// Ask the CFG in case we don't know any better
		for (SootMethod sm : manager.getICFG().getCalleesOfCallAt(sCallSite)) {
			if (this.sinks.contains(sm) || this.sinkDefs.contains(sm.getSignature()))
				return new SootMethodAndClass(sm);
		}

		// nothing found
		return null;
	}

	/**
	 * Sets the list of methods whose parameters shall be regarded as taint sources
	 * 
	 * @param parameterTaintMethods The list of methods whose parameters shall be
	 *                              regarded as taint sources
	 */
	public void setParameterTaintMethods(List<String> parameterTaintMethods) {
		this.parameterTaintMethodDefs = parameterTaintMethods;
	}

	/**
	 * Sets the list of methods whose return values shall be regarded as taint sinks
	 * 
	 * @param returnTaintMethods The list of methods whose return values shall be
	 *                           regarded as taint sinks
	 */
	public void setReturnTaintMethods(List<String> returnTaintMethods) {
		this.returnTaintMethodDefs = returnTaintMethods;
	}

	@Override
	public void initialize() {
		if (sourceDefs != null) {
			sources = new HashSet<>();
			for (String signature : sourceDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					sources.add(sm);
			}
			// sourceDefs = null;
		}

		if (sinkDefs != null) {
			sinks = new HashSet<>();
			for (String signature : sinkDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					sinks.add(sm);
			}
			// sinkDefs = null;
		}

		if (returnTaintMethodDefs != null) {
			returnTaintMethods = new HashSet<>();
			for (String signature : returnTaintMethodDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					returnTaintMethods.add(sm);
			}
			returnTaintMethodDefs = null;
		}

		if (parameterTaintMethodDefs != null) {
			parameterTaintMethods = new HashSet<>();
			for (String signature : parameterTaintMethodDefs) {
				SootMethod sm = Scene.v().grabMethod(signature);
				if (sm != null)
					parameterTaintMethods.add(sm);
			}
			parameterTaintMethodDefs = null;
		}
	}

}
