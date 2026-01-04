/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.search;

import org.maxicp.andor.Branch;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.andor.SubBranch;
import org.maxicp.modeling.ModelProxy;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.state.StateManager;
import org.maxicp.util.exception.InconsistencyException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Depth-First Search Branch and Bound implementation
 */

public class DFSearchMini_And_PS extends RunnableSearchMethod {

    private static final DFSListener EMPTY_LISTENER = new DFSListener(){};
    private DFSListener dfsListener = EMPTY_LISTENER;
    private Supplier<Branch> treeBuilding;
    private Function<Set<IntExpression>, Runnable[]> branching;
    private boolean complete = true;
    private boolean showSolutions = false;
    private ConstraintGraph graph;

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

    public DFSearchMini_And_PS(StateManager sm, ConstraintGraph graph, Supplier<Branch> treeBuilding, Function<Set<IntExpression>, Runnable[]> branching) {
        super(sm, null);
        this.treeBuilding =  treeBuilding;
        this.branching = branching;
        this.graph = graph;
    }
    public DFSearchMini_And_PS(ModelProxy modelProxy, Supplier<Branch> treeBuilding, Function<Set<IntExpression>, Runnable[]> branching) {
        super(modelProxy.getConcreteModel().getStateManager(), null);
        this.treeBuilding =  treeBuilding;
        this.branching = branching;
    }
    public void setShowSolutions(boolean showSolutions) {
        this.showSolutions = showSolutions;
    }

    @Override
    protected void startSolve(SearchStatistics statistics, Predicate<SearchStatistics> limit, Runnable onNodeVisit) {
        throw new RuntimeException("DFSearch type AND/OR wrong input");
    }

    protected void startSolve(SearchStatistics statistics, int solutionsLimit, boolean showSolutions) {
        currNodeId = 0;
        this.showSolutions = showSolutions;
        sm.withNewState(() -> {
            int nSolutions = dfs(statistics, -1, 0, solutionsLimit);
            if (nSolutions >= 0 ) statistics.incrSolutions(nSolutions);
        });
        if (!this.complete) throw new StopSearchException();
    }

    private int dfs(SearchStatistics statistics, int parentId, int andLevel,int solutionsLimit) {
        Objects.requireNonNull(this.branching, "No branching instruction");
        Objects.requireNonNull(this.treeBuilding, "No tree building instruction");
        Branch branch = treeBuilding.get();
        if (branch == null) {
            return 1;
        }

        int n_Solutions = 0;
        if (branch.getVariables() != null && !branch.getVariables().isEmpty()) {
            n_Solutions = processOrBranch(branch, statistics, parentId, andLevel, solutionsLimit);
        } else if (branch.getBranches() != null && !branch.getBranches().isEmpty()){
            n_Solutions = processAndBranch(branch, statistics, parentId, andLevel, solutionsLimit);
        } else {
            throw new IllegalArgumentException("No branch available");
        }
        return n_Solutions;
    }

    private int processAndBranch(Branch branch, SearchStatistics statistics, int parentId, int andLevel, int solutionsLimit){
        statistics.incrAndNodes();
        final int nodeId = currNodeId++;
        if (this.showSolutions) System.out.println("AND branch of depth "+andLevel+" =================================================");
        if (this.showSolutions) notifySolution(parentId,nodeId);
        final int[] nSolutions = {1};
        int a = 0;
        AtomicReference<Boolean> breaking = new AtomicReference<>(false);
        for (SubBranch B : branch.getBranches()) {
            if (this.showSolutions) System.out.println("Depth "+ andLevel +", sub-branch n° " + (a+1) + " \t----------------------");
            a++;
            sm.withNewState(() -> {
                this.graph.newState(B.getVariables());
                int solution = 1;
                int limit = solutionsLimit;
                if (solutionsLimit != Integer.MAX_VALUE) {
                    if (nSolutions[0] >= solutionsLimit) {
                        limit = 1;

                    } else if (nSolutions[0] > 1) {
                        limit = (int) Math.ceil((double) solutionsLimit / nSolutions[0]);
                    }
                }
                if (B.getToFix()) {
                    solution = processOrBranch(new Branch(B.getVariables()), statistics, nodeId, andLevel+1, limit);
                } else {
                    solution = dfs(statistics, nodeId, andLevel+1, limit);
                }
                if (solution == 0) breaking.set(true);
                nSolutions[0] *= solution;
            });
            if (breaking.get()) {
                return 0;
            }
        }
        return nSolutions[0];
    }

    private int processOrBranch(Branch branch, SearchStatistics statistics, int parentId, int andLevel, int solutionsLimit){
        final int nodeId = currNodeId++;
        Runnable[] branches = new Runnable[0];
        if (branch.getVariables() != null){
            branches = this.branching.apply(branch.getVariables());
            notifyBranch(nodeId, parentId);
        }
        if (branches.length == 0) {
            this.graph.newState();//TODO check with need new state
            if (this.showSolutions) System.out.println();
            if (this.graph.solutionFound()){
                if (this.showSolutions) notifySolution();
                return 1;
            } else if (branch.getBranches() == null ){
                return dfs(statistics, nodeId, andLevel, solutionsLimit);
            } else {
                return processAndBranch(new Branch(branch.getBranches()), statistics, nodeId, andLevel, solutionsLimit);
            }
        } else {
            final int[] nSolutions = {0};
            for (Runnable b : branches) {
                if (nSolutions[0] >= solutionsLimit) {
                    this.complete = false;
                    break;
                }
                sm.withNewState(() -> {
                    try {
                        statistics.incrNodes();
                        b.run();
                        int limite = solutionsLimit;
                        if (solutionsLimit != Integer.MAX_VALUE) {
                            limite = solutionsLimit-nSolutions[0];
                        }
                        nSolutions[0] += processOrBranch(branch,statistics, nodeId, andLevel, limite);

                    } catch (InconsistencyException e) {
                        currNodeId++;
                        statistics.incrFailures();
                        notifyFailure();
                    }
                });
            }
            return nSolutions[0];
        }
    }
    @Override
    public SearchStatistics solve() {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, Integer.MAX_VALUE, false);
    }

    public SearchStatistics solve(int solutionsLimit, boolean showSolutions) {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, solutionsLimit, showSolutions);
    }

    public SearchStatistics solve(int solutionsLimit) {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, solutionsLimit, false);
    }


    public SearchStatistics solve(boolean showSolutions) {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, Integer.MAX_VALUE, showSolutions);
    }

    @Override
    public SearchStatistics solve(Predicate<SearchStatistics> limit) {
        throw new RuntimeException("DFSearch type AND/OR doesn't take predicate as limite, use an integer instead");
    }


    protected SearchStatistics solve(SearchStatistics statistics, int solutionsLimit, boolean showSolutions) {
        sm.withNewState(() -> {
            if (!statistics.isCompleted()) {
                try {
                    startSolve(statistics, solutionsLimit, showSolutions);
                    statistics.setCompleted();
                } catch (StopSearchException ignored) {
                }
            }
        });
        return statistics;
    }

    protected SearchStatistics solve(SearchStatistics statistics, Predicate<SearchStatistics> limit, Runnable onNodeVisit) {
        throw new RuntimeException("DFSearch type AND/OR wrong input");
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
