/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.andor.search;

import org.maxicp.modeling.ModelProxy;
import org.maxicp.search.*;
import org.maxicp.state.StateManager;
import org.maxicp.util.exception.InconsistencyException;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Depth-First Search Branch and Bound implementation
 */
public class DFSearchMini_Or extends RunnableSearchMethod {

    private static final DFSListener EMPTY_LISTENER = new DFSListener(){};
    private DFSListener dfsListener = EMPTY_LISTENER;

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

    public DFSearchMini_Or(StateManager sm, Supplier<Runnable[]> branching) {
        super(sm, branching);
    }
    public DFSearchMini_Or(ModelProxy modelProxy, Supplier<Runnable[]> branching) { super(modelProxy.getConcreteModel().getStateManager(), branching); }

    private void dfs(SearchStatistics statistics, Predicate<SearchStatistics> limit, Runnable onNodeVisit, int parentId) {
        if (limit.test(statistics))
            throw new StopSearchException();
        Runnable[] branches = branching.get();
        if (branches.length == 0) {
            statistics.incrSolutions();
            notifySolution(currNodeId++, parentId);
            notifySolution();
        } else {
            for (Runnable b : branches) {
                int nodeId = currNodeId++;
                notifySaveState();
                onNodeVisit.run();
                sm.withNewState(() -> {
                    try {
                        statistics.incrNodes();
                        b.run();
                        notifyBranch(nodeId, parentId);
                        dfs(statistics, limit, onNodeVisit, nodeId);
                    } catch (InconsistencyException e) {
                        statistics.incrFailures();
                        notifyFailure();
                    }
                });
                notifyRestoreState();
            }
        }
    }

    @Override
    protected void startSolve(SearchStatistics statistics, Predicate<SearchStatistics> limit, Runnable onNodeVisit) {
        currNodeId = 0;
        sm.withNewState(() -> {
            dfs(statistics, limit , onNodeVisit, -1);
        });
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
