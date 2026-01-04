/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp;

import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.cp.modeling.ModelProxyInstantiatorWithCP;
import org.maxicp.modeling.*;
import org.maxicp.modeling.algebra.Expression;
import org.maxicp.modeling.algebra.bool.BoolExpression;
import org.maxicp.modeling.algebra.bool.Eq;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.algebra.integer.Sum;
import org.maxicp.modeling.concrete.ConcreteModel;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.modeling.constraints.ExpressionIsTrue;
import org.maxicp.modeling.constraints.NegTable;
import org.maxicp.modeling.constraints.Table;
import org.maxicp.modeling.constraints.helpers.CacheScope;
import org.maxicp.modeling.symbolic.*;
import org.maxicp.search.BestFirstSearch;
import org.maxicp.search.ConcurrentDFSearch;
import org.maxicp.search.DFSearch;
import org.maxicp.util.Ints;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.maxicp.Constants.HORIZON;

/**
 * A class that allows to create symbolic models
 */
public class ModelDispatcher implements ModelProxyInstantiator, AutoCloseable, ModelProxyInstantiatorWithCP, ModelProxy {
    private Model initialModel;
    private ThreadLocal<Model> currentModel;
    private ConstraintGraph graph;
    private int id = -1;
    private List<IntExpression> preload ;

    public ModelDispatcher() {
        initialModel = SymbolicModel.emptyModel(this);
        currentModel = new ThreadLocal<>();
        currentModel.set(initialModel);
        graph = null;
        preload = new ArrayList<>();
    }

    /**
     * @return the current model
     */
    public Model getModel() {
        return currentModel.get();
    }

    /**
     * Set the current model to m. m should have this base model as origin.
     * @param m
     */
    public <T extends Model> T setModel(T m) {
        if (m != null && !m.getModelProxy().equals(this))
            throw new RuntimeException("Model being assigned to this ModelProxy does not originate from here");
        currentModel.set(m);
        return m;
    }

    /**
     * Shortcut for baseModel.getModel().getConstraints();
     * @return an iterable with all the constraints in the current model
     */
    public Iterable<Constraint> getConstraints() {
        return getModel().getConstraints();
    }

    /**
     * Create an array of n IntVars with domain between 0 and domSize-1, inclusive.
     * @param n size of the array, number of IntVars
     * @param domSize size of the domains. Domains are [0, domsize-1]
     */
    public IntVar[] intVarArray(int n, int domSize) {
        IntVar[] out = new IntVar[n];
        for(int i = 0; i < n; i++)
            out[i] = new IntVarRangeImpl(this, 0, domSize-1);
        preload.addAll(List.of(out));
        return out;
    }
    public IntVar[] intVarArray(String id, int n, int domSize) {
        IntVar[] out = new IntVar[n];
        for(int i = 0; i < n; i++)
            out[i] = new IntVarRangeImpl(this,id+i, 0, domSize-1);
        preload.addAll(List.of(out));
        return out;
    }

    public IntExpression[] intVarArray(int n, Function<Integer, IntExpression> body) {
        IntExpression[] t = new IntExpression[n];
        for (int i = 0; i < n; i++)
            t[i] = body.apply(i);
        preload.addAll(List.of(t));
        return t;
    }

    public IntVar intVar(int min, int max) {
        IntVar intVar = new IntVarRangeImpl(this, min, max);
        preload.add(intVar);
        return intVar;
    }

    public IntVar intVar(String id, int min, int max) {
        IntVar intVar = new IntVarRangeImpl(this, id, min, max);
        preload.add(intVar);
        return intVar;
    }

    public IntVar intVar(int[] values) {
        IntVar intVar = new IntVarSetImpl(this, Set.copyOf(Ints.asList(values)));
        preload.add(intVar);
        return intVar;
    }

    public IntVar intVar(Set<Integer> values) {
        IntVar intVar = new IntVarSetImpl(this, values);
        preload.add(intVar);
        return intVar;
    }

    public IntVar intVar(String id, int[] values) {
        IntVar intVar = new IntVarSetImpl(this, id, Set.copyOf(Ints.asList(values)));
        preload.add(intVar);
        return intVar;
    }

    public IntVar constant(int value) {
        return intVar(value, value);
    }

    public IntervalVar intervalVar(int startMin, int startMax, int endMin, int endMax, int lengthMin, int lengthMax, boolean isPresent) {
        return new IntervalVarImpl(this, startMin, startMax, endMin, endMax, lengthMin, lengthMax, isPresent);
    }

    public IntervalVar intervalVar(boolean isPresent) {
        return intervalVar(0, HORIZON,0, HORIZON, 0, HORIZON, isPresent);
    }

    public IntervalVar intervalVar(int duration, boolean isPresent) {
        return intervalVar(0, HORIZON,0, HORIZON, duration, duration, isPresent);
    }

    public IntervalVar intervalVar(int startMin, int endMax, int duration, boolean isPresent) {
        return intervalVar(startMin, endMax - duration, startMin + duration, endMax, duration, duration, isPresent);
    }

    public IntervalVar[] intervalVarArray(int n, boolean present) {
        IntervalVar[] out = new IntervalVar[n];
        for (int i = 0 ; i < n ; i++) {
            out[i] = intervalVar(present);
        }
        return out;
    }

    public IntervalVar[] intervalVarArray(int n, Function<Integer, IntervalVar> body) {
        IntervalVar[] out = new IntervalVar[n];
        for (int i = 0 ; i < n ; i++)
            out[i] = body.apply(i);

        return out;
    }

    public BoolVar[] boolVarArray(int n) {
        BoolVar[] out = new BoolVar[n];
        for(int i = 0; i < n; i++)
            out[i] = new BoolVarImpl(this);
        return out;
    }

    public BoolVar boolVar() {
        return new BoolVarImpl(this);
    }

    public SeqVar seqVar(int nNode, int begin, int end) {
        return new SeqVarImpl(this, nNode, begin, end);
    }

    @Override
    public void close() throws Exception {
        currentModel.remove();
        currentModel = null;
        initialModel = null;
    }

    public Objective minimize(Expression v) {
        return switch (getModel()) {
            case SymbolicModel sm -> sm.minimize(v);
            case ConcreteModel ignored -> throw new IllegalStateException("Cannot modify the optimisation method of an instantiated model");
            default -> throw new IllegalStateException("Unexpected value: " + getModel());
        };
    }

    public Objective maximize(Expression v) {
        return switch (getModel()) {
            case SymbolicModel sm -> sm.maximize(v);
            case ConcreteModel ignored -> throw new IllegalStateException("Cannot modify the optimisation method of an instantiated model");
            default -> throw new IllegalStateException("Unexpected value: " + getModel());
        };
    }

    public DFSearch dfSearch(Supplier<Runnable[]> branching) {
        return new DFSearch(this, branching);
    }

    public ConcurrentDFSearch concurrentDFSearch(Supplier<SymbolicModel[]> symbolicBranching) {
        return new ConcurrentDFSearch(this, symbolicBranching);
    }

    public <U extends Comparable<U>> BestFirstSearch<U> bestFirstSearch(Supplier<Runnable[]> branching, Supplier<U> nodeEvaluator) {
        return new BestFirstSearch<U>(this, branching, nodeEvaluator);
    }

    public ConstraintGraph createGraph(ConcreteCPModel cp) {
        if (graph == null) graph = new ConstraintGraph(cp.getStateManager());
        graph.addNode(preload.toArray(new IntExpression[0]));

        List<IntExpression> constrainedNodes = new ArrayList<>();
        for (Constraint c : getConstraints()) {
            Expression[] tab = switch (c) {
                case Table t -> t.x();
                case NegTable t -> t.x();
                case ExpressionIsTrue et -> et.scope().toArray(new Expression[0]);
                case AllDifferent all -> all.scope().toArray(new Expression[0]);
                default -> throw new IllegalStateException("Unknown constraint type: " + c.getClass());
            };
            constrainedNodes.clear();
            for (Expression e : tab) expandExpression(constrainedNodes, e);
            if (constrainedNodes.size() > 1) graph.addEdge(constrainedNodes.toArray(new IntExpression[0]));
        }
        return graph;
    }

    private void expandExpression(List<IntExpression> constrainedNodes, Expression exp) {
        Expression[] exps = exp.subexpressions().toArray(new Expression[0]);
        if (exps.length == 0) constrainedNodes.add((IntExpression) exp);
        else for (Expression e : exps) expandExpression(constrainedNodes, e);
    }
    @Override
    public int getId() {
        id ++;
        return this.id;
    }
}
