/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.modeling.concrete;

import org.maxicp.andor.Branch;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.andor.Scheme;
import org.maxicp.andor.search.DFSearchMini_And_CS;
import org.maxicp.andor.search.DFSearchMini_And_PS;
import org.maxicp.andor.search.DFSearchMini_Or;
import org.maxicp.modeling.Constraint;
import org.maxicp.modeling.Model;
import org.maxicp.modeling.algebra.bool.BoolExpression;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.algebra.scheduling.IntervalExpression;
import org.maxicp.modeling.algebra.sequence.SeqExpression;
import org.maxicp.modeling.symbolic.SymbolicModel;
import org.maxicp.search.*;
import org.maxicp.state.StateManaged;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ConcreteModel extends Model, StateManaged {

    ConcreteIntVar getConcreteVar(IntExpression expr);

    ConcreteBoolVar getConcreteVar(BoolExpression expr);

    ConcreteSeqVar getConcreteVar(SeqExpression expr);

    ConcreteIntervalVar getConcreteVar(IntervalExpression expr);

    default void add(Constraint c) { add(c, true); }

    void add(Constraint c, boolean enforceFixPoint);

    /**
     * Jump to a specific constraint node.
     *
     * The new list should have a node in common with current model,
     * and this common node must be either the node at which the model was concretized, or a node created after that.
     *
     * @param m the model to jump to
     */
    void jumpTo(SymbolicModel m, boolean enforceFixPoint);
    default void jumpTo(SymbolicModel m) { jumpTo(m, true); }

    /**
     * Similar function to jumpTo, but enforces a jump to a child model.
     * @param m
     * @param enforceFixPoint
     */
    void jumpToChild(SymbolicModel m, boolean enforceFixPoint);

    default void jumpToChild(SymbolicModel m) { jumpToChild(m, true); }

    Objective createObjective(org.maxicp.modeling.symbolic.Objective obj);

    default ConcurrentDFSearch concurrentDFSearch(Supplier<SymbolicModel[]> symbolicBranching) {
        return new ConcurrentDFSearch(getModelProxy(), symbolicBranching);
    }

    default DFSearch dfSearch(Supplier<Runnable[]> branching) {
        return new DFSearch(getStateManager(), branching);
    }

    default DFSearchMini_Or dfSearchMini(Supplier<Runnable[]> branching) {
        return new DFSearchMini_Or(getStateManager(), branching);
    }

    /**
     * Creates and returns an instance of DFSearch_And_PS, an AND/OR DFS search with partial solutions,
     * by configuring the solver with a tree building strategy and a branching procedure.
     *
     * @param graph the constraint graph representing the problem's variables and constraints
     * @param treeBuilding a supplier that provides a branching tree structure for the search process
     * @param branching a function that generate an array of {@link Runnable} that will be used to commit to child nodes
     * @return a new instance of DFSearch_And_PS initialized with the provided solver and branching.
     *
     * @see Scheme
     */
    default DFSearchMini_And_PS dfSearchMini_And_PS(ConstraintGraph graph, Supplier<Branch> treeBuilding, Function<Set<IntExpression>, Runnable[]> branching) {
        return new DFSearchMini_And_PS(getStateManager(), graph, treeBuilding, branching);
    }

    /**
     * Creates and returns an instance of DFSearch_And_CS, an AND/OR DFS search with complete solutions,
     * by configuring the solver with a tree building strategy and a branching procedure.
     *
     * @param graph the constraint graph representing the problem's variables and constraints
     * @param treeBuilding a supplier that provides a branching tree structure for the search process
     * @param branching a function that generate an array of {@link Runnable} that will be used to commit to child nodes
     * @return a new instance of DFSearch_And_CS initialized with the provided solver, tree-building strategy, and branching.
     *
     * @see Scheme
     */
    default DFSearchMini_And_CS dfSearchMini_And_CS(ConstraintGraph graph, Supplier<Branch> treeBuilding, Function<Set<IntExpression>, Runnable[]> branching) {
        return new DFSearchMini_And_CS(getStateManager(), graph, treeBuilding, branching);
    }

    default <U extends Comparable<U>> BestFirstSearch<U> bestFirstSearch(Supplier<Runnable[]> branching, Supplier<U> nodeEvaluator) {
        return new BestFirstSearch<U>(getModelProxy(), branching, nodeEvaluator);
    }
}
