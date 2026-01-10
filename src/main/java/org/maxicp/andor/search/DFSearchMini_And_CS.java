/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.andor.search;

import org.maxicp.andor.Branch;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.andor.SlicedTable;
import org.maxicp.andor.SubBranch;
import org.maxicp.modeling.ModelProxy;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.symbolic.IntVarRangeImpl;
import org.maxicp.search.*;
import org.maxicp.state.StateManager;
import org.maxicp.util.exception.InconsistencyException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.maxicp.andor.SlicedTable.computeSlicedTable;

/**
 * Depth-First Search Branch and Bound implementation
 */

public class DFSearchMini_And_CS extends RunnableSearchMethod {

    private static final DFSListener EMPTY_LISTENER = new DFSListener(){};
    private DFSListener dfsListener = EMPTY_LISTENER;
    private Supplier<Branch> treeBuilding;
    private Function<Set<IntExpression>, Runnable[]> branching;
    private boolean complete = true;
    private boolean showSolutions = false;
    private boolean computeSolutions = true;
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

    public DFSearchMini_And_CS(StateManager sm, ConstraintGraph graph, Supplier<Branch> treeBuilding, Function<Set<IntExpression>, Runnable[]> branching) {
        super(sm, null);
        this.treeBuilding =  treeBuilding;
        this.branching = branching;
        this.graph = graph;
    }
    public DFSearchMini_And_CS(ModelProxy modelProxy, Supplier<Branch> treeBuilding, Function<Set<IntExpression>, Runnable[]> branching) {
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

    /**
     * Start the AND/OR depth-first search (DFS), updating the search statistics
     * and generating solutions, if applicable.
     *
     * @param statistics the object that tracks metrics during the search process.
     * @param solutionsLimit the maximum number of solutions to find before stoping the search.
     * @param showSolutions a flag indicating whether to output the solutions during the search.
     */
    protected void startSolve(SearchStatistics statistics, int solutionsLimit, boolean showSolutions) {
        currNodeId = 0;
        this.showSolutions = showSolutions;
        sm.withNewState(() -> {
            long debut1 = System.nanoTime();
            Solutions solutions = dfs(statistics, -1, solutionsLimit);
            if (solutions.nSolutions >= 0) statistics.incrSolutions(solutions.nSolutions);
            long fin1 = System.nanoTime();
            List<SlicedTable> slicedTables = solutions.slicedTables;
            long debut2 = System.nanoTime();

            if (!slicedTables.isEmpty()) {
                statistics.setSlicedTables(slicedTables);
                if (computeSolutions) {
                    processSolutions(statistics, slicedTables, solutionsLimit);
                }
            }
            long fin2 = System.nanoTime();
            System.out.format("\nSearch time : %s ms", (fin1 - debut1) / 1_000_000);
            System.out.format("\nComputation time : %s ms\n", (fin2 - debut2) / 1_000_000);
        });
        if (!this.complete) throw new StopSearchException();
    }

    /**
     * Processes solutions generated during the search.
     * Optionally, it displays the solutions if the corresponding flag is enabled.
     *
     * @param statistics the object that tracks metrics during the search process.
     * @param slicedTables a list of precomputed sliced tables providing data for solution computation.
     * @param solutionsLimit the maximum number of solutions to process before terminating.
     */
    private void processSolutions(SearchStatistics statistics, List<SlicedTable> slicedTables, int solutionsLimit) {
        List<Map<Integer, Integer>> listSolutions = computeSlicedTable(slicedTables, solutionsLimit);
        statistics.setSolutions(listSolutions);
        if (showSolutions){
            this.graph.newState();
            Set<IntExpression> vars = this.graph.getStateVariables();
            int n_solutions = 0;
            for (Map<Integer, Integer> sol : listSolutions) {
                if (vars.size() != sol.size())
                    throw new RuntimeException("Missing value in a solution, " + vars.size() + " != " + sol.size());
                sm.withNewState(() -> {
                    for (IntExpression var : vars) {
                        if (sol.containsKey(var.getId())) {
                            int value = sol.get(var.getId());
                            IntVarRangeImpl v = (IntVarRangeImpl) var;
                            v.fix(value);
                            var.fix(value);
                        } else {
                            throw new RuntimeException("no solution for " + var.getId());
                        }
                    }
                    notifySolution();
                });
                n_solutions++;
                if (n_solutions >= solutionsLimit) {
                    break;
                }
            }
        }
    }

    /**
     * Constructs and returns a map representing a pattern of fixed variables and their assigned values.
     *
     * @return a map where keys are the identifiers of fixed variables and values are their
     *         corresponding fixed values, or {@code null} if no variables are fixed.
     */
    public Map<Integer, Integer> getPattern() {
        Map<Integer, Integer> pattern = new HashMap<>();
        this.graph.newState();
        for (IntExpression vars : this.graph.getStateVariables()){
            if (vars.isFixed()) {
                pattern.put(vars.getId(),vars.min());
            }
        }
        if (pattern.isEmpty()) return null;
        return pattern;
    }

    /**
     * Represents the results of a search process, including the number of solutions
     * found and the associated processed data in the form of sliced tables.
     */
    public record Solutions(int nSolutions, List<SlicedTable> slicedTables) {}

    /**
     * Performs an AND/OR depth-first search (DFS) traversal in the search tree, processing AND/OR branches
     * and updating the search statistics based on the given parameters.
     *
     * @param statistics the object that tracks metrics during the search process
     * @param parentId the identifier of the parent node in the search tree
     * @param solutionsLimit the maximum number of solutions to find before halting the search
     * @return an instance of {@link Solutions}, containing the number of solutions found and the associated sliced tables.
     */
    private Solutions dfs(SearchStatistics statistics, int parentId,int solutionsLimit) {
        Objects.requireNonNull(this.branching, "No branching instruction");
        Objects.requireNonNull(this.treeBuilding, "No tree building instruction");

        // Get the next branch to process
        Branch branch = treeBuilding.get();
        if (branch == null) {
            if (graph.solutionFound()){
                List<SlicedTable> sols = new ArrayList<>();
                sols.add(new SlicedTable(getPattern()));
                return new Solutions(1, sols);
            }
        }

        // Following the branch, start its process
        Solutions solutions;
        if (branch.getVariables() != null && !branch.getVariables().isEmpty()) {
            solutions = processOrBranch(branch, statistics, parentId, solutionsLimit);
        } else if (branch.getBranches() != null && !branch.getBranches().isEmpty()){
            solutions = processAndBranch(branch, statistics, parentId, solutionsLimit);
        } else {
            throw new IllegalArgumentException("No branch available");
        }
        return solutions;
    }

    /**
     * Processes an AND branch of the search tree by iterating through its subbranches,
     * applying depth-first search (DFS) strategies, and updating search statistics.
     *
     * @param branch the current branch of the search tree to process, containing its variables and subbranches
     * @param statistics the object that tracks metrics during the search process
     * @param parentId the identifier of the parent node in the search tree
     * @param solutionsLimit the maximum number of solutions to find before halting the search
     * @return an instance of {@link Solutions}, containing the number of solutions found and the associated sliced tables.
     */
    private Solutions processAndBranch(Branch branch, SearchStatistics statistics, int parentId, int solutionsLimit){
        statistics.incrAndNodes();
        final int nodeId = currNodeId++;
        final int[] nSolutions = {1};
        List<List<SlicedTable>> subSolutions = new ArrayList<>();
        AtomicReference<Boolean> breaking = new AtomicReference<>(false);
        // Process each subbranch
        for (SubBranch B : branch.getBranches()) {
            sm.withNewState(() -> {
                this.graph.newState(B.getVariables());
                Solutions newST ;
                // Adjusts the limit of solutions
                int limit = solutionsLimit;
                if (solutionsLimit != Integer.MAX_VALUE) {
                    if (nSolutions[0] >= solutionsLimit) {
                        limit = 1;
                    } else if (nSolutions[0] > 1) {
                        limit = (int) Math.ceil((double) solutionsLimit / nSolutions[0]);
                    }
                }
                // Recursively process the variables of the subbranch
                if (B.getToFix()) {
                    newST = processOrBranch(new Branch(B.getVariables()), statistics, parentId, limit);
                } else {
                    newST = dfs(statistics, nodeId, limit);
                }
                if (newST!= null && !newST.slicedTables.isEmpty()) {
                    subSolutions.add(newST.slicedTables);
                    nSolutions[0] *= newST.nSolutions;
                } else {
                    breaking.set(true);
                }
            });
            if (breaking.get()) {
                return null;
            }
        }
        return new Solutions(nSolutions[0], new ArrayList<SlicedTable>(List.of(new SlicedTable(getPattern(), subSolutions))));
    }

    /**
     * Processes an OR branch of the search tree by evaluating its variables and subbranches,
     * applying the branching strategies, and updating search statistics as solutions are found.
     *
     * @param branch the current branch of the search tree to process, containing its variables and subbranches
     * @param statistics the object that tracks metrics during the search process
     * @param parentId the identifier of the parent node in the search tree
     * @param solutionsLimit the maximum number of solutions to find before halting the search
     * @return an instance of {@link Solutions}, containing the number of solutions found and the associated sliced tables.
     */
    private Solutions processOrBranch(Branch branch, SearchStatistics statistics, int parentId, int solutionsLimit){
        final int nodeId = currNodeId++;
        // Get the branching Runnable to reduce a variable domaine
        Runnable[] branches = new Runnable[0];
        if (branch.getVariables() != null){
            branches = this.branching.apply(branch.getVariables());
            notifyBranch(nodeId, parentId);
        }
        List<SlicedTable> sols = new ArrayList<>();
        // Check for a solution or an AND branch
        if (branches.length == 0) {
            this.graph.newState();
            if (this.graph.solutionFound()){
                sols.add(new SlicedTable(getPattern()));
                return new Solutions(1, sols);
            } else if (branch.getBranches() == null ){
                Solutions newST = dfs(statistics, nodeId, solutionsLimit);
                if (newST!=null && !newST.slicedTables.isEmpty()) {
                    return newST;
                }
            } else {
                Solutions newST = processAndBranch(new Branch(branch.getBranches()), statistics, nodeId, solutionsLimit);
                if (newST != null) {
                    return newST;
                }
            }
            return null;
        } else {
            // Process the variables of the branch
            final int[] nSolutions = {0};
            for (Runnable b : branches) {
                if (sols.size() >= solutionsLimit) {
                    this.complete = false;
                    break;
                }
                sm.withNewState(() -> {
                    try {
                        // Apply the Runnable
                        statistics.incrNodes();
                        b.run();
                        int limite = solutionsLimit;
                        if (solutionsLimit != Integer.MAX_VALUE) {
                            limite = solutionsLimit-nSolutions[0];
                        }
                        // Continue the recursive search
                        Solutions newST = processOrBranch(branch,statistics, nodeId, limite);
                        if (newST != null) {
                            sols.addAll(newST.slicedTables);
                            nSolutions[0] += newST.nSolutions;
                        }
                    } catch (InconsistencyException e) {
                        currNodeId++;
                        statistics.incrFailures();
                        notifyFailure();
                    }
                });
            }
            return new Solutions(nSolutions[0], sols);
        }
    }
    @Override
    public SearchStatistics solve() {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, Integer.MAX_VALUE, false);
    }

    public SearchStatistics solve(int solutionsLimit, boolean showSolutions, boolean computeSolutions) {
        SearchStatistics statistics = new SearchStatistics();
        this.computeSolutions = computeSolutions;
        return solve(statistics, solutionsLimit, showSolutions);
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
