/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.search;

import org.maxicp.andor.Branch;
import org.maxicp.andor.SlicedTable;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.modeling.ModelProxy;
import org.maxicp.state.StateManager;
import org.maxicp.util.exception.InconsistencyException;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * Depth-First Search Branch and Bound implementation
 */
public class DFSearchAndPS extends RunnableSearchMethod {

    private static final DFSListener EMPTY_LISTENER = new DFSListener(){};
    private DFSListener dfsListener = EMPTY_LISTENER;
    private Supplier<Branch> treeBuilding;
    private Function<Set<CPIntVar>, Runnable[]> branchingAND;

    public void setDFSListener(DFSListener listener) {
        this.dfsListener = listener;
    }

    private void notifySolution(int nodeId, int parentId) {
        dfsListener.solution(nodeId, parentId);
    }

    private void notifyFailure(int nodeId, int parentId) {
        dfsListener.fail(nodeId, parentId);
    }

    private void notifyBranch(int nodeId, int parentId) {
        dfsListener.branch(nodeId, parentId);
    }

    private void notifyBranchAction(Runnable action) {
        dfsListener.branchingAction(action);
    }

    private void notifySaveState() {
        dfsListener.saveState(sm);
    }

    private void notifyRestoreState() {
        dfsListener.restoreState(sm);
    }

    private int currNodeId = -1;

    public DFSearchAndPS(StateManager sm, Supplier<Branch> treeBuilding, Function<Set<CPIntVar>, Runnable[]> branchingA, Supplier<Runnable[]> branching) {
        super(sm, branching);
        this.treeBuilding =  treeBuilding;
        this.branchingAND = branchingA;
    }
    public DFSearchAndPS(ModelProxy modelProxy, Supplier<Branch> treeBuilding, Function<Set<CPIntVar>, Runnable[]> branchingA, Supplier<Runnable[]> branching) {
        super(modelProxy.getConcreteModel().getStateManager(), branching);
        this.treeBuilding =  treeBuilding;
        this.branchingAND = branchingA;
    }

    // solution to DFS with explicit stack
    private void expandNode(Stack<Runnable> alternatives, SearchStatistics statistics, Runnable onNodeVisit, int parentId) {
        Function<Branch,Integer>[] alts = (Function<Branch, Integer>[]) branching.get();
        if (alts.length == 0) {
            statistics.incrSolutions();
            notifySolution(currNodeId++, parentId);
            notifySolution();
        } else {
            for (int i = alts.length - 1; i >= 0; i--) {
                int nodeId = currNodeId++;
                Runnable a = (Runnable) alts[i];
                alternatives.push(() -> {
                    notifyRestoreState();
                    sm.restoreState();
                });
                alternatives.push(() -> {
                    statistics.incrNodes();
                    onNodeVisit.run();
                    try {
                        notifyBranchAction(a);
                        a.run();
                        notifyBranch(nodeId, parentId);
                        expandNode(alternatives, statistics, onNodeVisit, nodeId);
                    } catch (InconsistencyException e) {
                        notifyFailure(nodeId, parentId);
                        throw e;
                    }
                });
                alternatives.push(() -> {
                    notifySaveState();
                    sm.saveState();
                });
            }
        }
    }

    private SlicedTable processAndBranch(Branch branch, SearchStatistics statistics, int parentId, int position, int solutionLimit){
        return null;
    }

    private List<SlicedTable> processOrBranch(Branch branch, SearchStatistics statistics, int parentId, int position, int solutionLimit){
        return null;
    }

    @Override
    protected void startSolve(SearchStatistics statistics, Predicate<SearchStatistics> limit, Runnable onNodeVisit) {
        currNodeId = 0;
        Stack<Runnable> alternatives = new Stack<Runnable>();
        expandNode(alternatives, statistics, onNodeVisit, currNodeId);
        while (!alternatives.isEmpty()) {
            if (limit.test(statistics)) throw new StopSearchException();
            try {
                alternatives.pop().run();
            } catch (InconsistencyException e) {
                statistics.incrFailures();
                notifyFailure();
            }
        }
    }


    public SearchStatistics solve(DFSListener dfsListener) {
        setDFSListener(dfsListener);
        SearchStatistics stats = super.solve();
        setDFSListener(EMPTY_LISTENER);
        return stats;
    }

    public SearchStatistics optimize(Objective obj, DFSListener dfsListener) {
        setDFSListener(dfsListener);
        SearchStatistics stats = super.optimize(obj);
        setDFSListener(EMPTY_LISTENER);
        return stats;
    }
}
