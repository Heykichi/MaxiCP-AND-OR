/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package org.maxicp.andor;


import org.maxicp.modeling.ModelProxy;
import org.maxicp.modeling.algebra.integer.IntExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.maxicp.andor.FiducciaMattheysesCut.fiducciaMattheysesCut;
import static org.maxicp.modeling.Factory.eq;
import static org.maxicp.modeling.Factory.neq;
import static org.maxicp.search.Searches.branch;
import static org.maxicp.search.Searches.selectMin;

public class Scheme {
    /**
     * First-Fail strategy.
     * Provides a Function that selects the variable
     * with the smallest domain size greater than one.
     * Then it creates two branches. The left branch
     * assigning the variable to its minimum value.
     * The right branch removing this minimum value from the domain.
     *
     * @return A function that, given a set of {@code IntExpression} variables,
     *         returns an array of {@code Runnable} objects representing
     *         the two branches. If the input set is null or empty, or if no variable
     *         satisfies the selection criteria, an empty array is returned.
     */
    public static Function<Set<IntExpression>, Runnable[]> firstFail() {
        return (Set<IntExpression>variables) -> {
            if (variables == null || variables.isEmpty()){
                return new Runnable[0];
            }
            IntExpression xs = selectMin(variables.toArray(new IntExpression[0]),
                    xi -> xi.size() > 1,
                    xi -> xi.size());
            if (xs == null)
                return new Runnable[0];
            else {
                int v = xs.min();
                ModelProxy model = xs.getModelProxy();
                return branch(() -> model.add(eq(xs, v)), () -> model.add(neq(xs, v)));
            }
        };
    }

    /**
     * First-Order strategy.
     * Provides a Function that selects the first unfixed variable.
     * Then it creates two branches. The left branch
     * assigning the variable to its minimum value.
     * The right branch removing this minimum value from the domain.
     *
     * @return A function that, given a set of {@code IntExpression} variables,
     *         returns an array of {@code Runnable} objects representing
     *         the two branches. If the input set is null or empty, or if no variable
     *         satisfies the selection criteria, an empty array is returned.
     */
    public static Function<Set<IntExpression>, Runnable[]> firstOrder() {
        return (Set<IntExpression> variables) -> {
            if (variables == null || variables.isEmpty()){
                return new Runnable[0];
            }
            IntExpression[] varList = variables.toArray(new IntExpression[0]);
            int idx = -1;
            for (int k = 0; k < varList.length; k++)
                if (varList[k].size() > 1) {
                    idx = k;
                    break;
                }
            if (idx == -1)
                return new Runnable[0];
            else {
                IntExpression qs = varList[idx];
                int v = qs.min();
                ModelProxy model = qs.getModelProxy();
                return branch(() -> model.add(eq(qs, v)), () -> model.add(neq(qs, v)));
            }
        };
    }

    /**
     * Creates a naive tree-building strategy in the form of a {@code Supplier<Branch>}.
     * The strategy first tries to split the constraint graph into independent subgraphs.
     * If such a split is possible, each subgraph becomes a subbranch.
     * If no split is possible, the strategy selects up to {@code fixToSplit}
     * unfixed variables with the highest number of unfixed neighbors and creates a
     * branch to fixes them.
     *
     * @param graph The {@code ConstraintGraph} representing the problem's variables and constraints.
     * @param fixToSplit  the number of variables to fix if splitting is not possible
     * @param sizeToFix   the size threshold to no longer create subbranches
     * @return a {@code Supplier<Branch>} that provides the branching strategy.
     *         Returns null if no unfixed variables are available or if no subbranches can be created.
     */
    public static Supplier<Branch> naiveTreeBuilding(ConstraintGraph graph, int fixToSplit, int sizeToFix){
        return () -> {
            // Save a new state of the constraint graph
            graph.newState();
            // check for independent components
            List<SubBranch> subBranches = graph.splitGraph(sizeToFix);
            if (subBranches != null){
                return new Branch(subBranches);
            }

            Set<IntExpression> variables = graph.getUnfixedVariables();
            if (variables.isEmpty()) return null;

            // Get the {@code fixToSplit} unfixed variables with the highest number of unfixed neighbors
            Set<IntExpression> varSet = variables.stream()
                    .sorted((a, b) -> Integer.compare(graph.getUnfixedNeighbors(b).size(), graph.getUnfixedNeighbors(a).size()))
                    .limit(fixToSplit)
                    .collect(Collectors.toSet());

            return new Branch(varSet);
        };
    }

    public static Supplier<Branch> fiducciaMattheyses(ConstraintGraph graph, int sizeToFix) {
        return fiducciaMattheyses(graph, sizeToFix, false);
    }


    /**
     * Implements the Fiduccia-Mattheyses heuristic-based tree-building strategy in the form of a {@code Supplier<Branch>}.
     * The method returns a supplier that dynamically provides branches based on the structure of the constraint graph,
     * attempting to fix variables to create potential splits.
     *
     * @param graph the constraint graph representing the problem's variables and constraints
     * @param sizeToFix   the size threshold to no longer create subbranches
     * @param splitFirst  a flag indicating whether to try splitting the initial graph before applying the main heuristic
     * @return a {@code Supplier<Branch>} that provides the branching strategy.
     *         Returns null if no branches can be generated due to a lack of unfixed variables or other structural issues.
     */
    public static Supplier<Branch> fiducciaMattheyses(ConstraintGraph graph, int sizeToFix, boolean splitFirst){
        boolean[] firstCall = {splitFirst};
        return () -> {
            // Optional initial attempt to split the graph before applying the FM heuristic
            if (firstCall[0]) {
                firstCall[0] = false;
                graph.newState();
                List<SubBranch> b = graph.splitGraph(sizeToFix);
                if (b != null) return new Branch(b);
            }
            // Save a new state of the constraint graph and get the unfixed variables
            graph.newState();
            Set<IntExpression> unFixedVars = graph.getUnfixedVariables();
            if (unFixedVars.isEmpty()) {
                return null;
            }

            // If the remaining problem is small enough, fix all variables directly
            if (unFixedVars.size() <= sizeToFix) {
                return new Branch(unFixedVars);
            }

            // Compute a cut set of variables using the Fiduccia–Mattheyses heuristic
            Set<IntExpression> cut = fiducciaMattheysesCut(graph);

            // Remove the cut variables from the graph and check for independent components
            graph.removeNode(cut);
            List<Set<IntExpression>> subSet = graph.findConnectedComponents();

            // If the cut creates multiple independent components, creates subbranches
            if (subSet.size() > 1) {
                List<SubBranch> subBranches = new ArrayList<>();
                for (Set<IntExpression> s1 : subSet) {
                    // Each subbranch is marked as terminal if it is small enough
                    subBranches.add(new SubBranch(s1,s1.size() <= sizeToFix));
                }
                return new Branch(cut, subBranches);
            }
            return new Branch(cut);
        };
    }
}
