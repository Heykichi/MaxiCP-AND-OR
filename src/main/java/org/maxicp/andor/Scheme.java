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

    public static Supplier<Branch> naiveTreeBuilding(ConstraintGraph graph, int nVars, int sizeToFix){
        return () -> {
            graph.newState();
            List<SubBranch> subBranches = graph.splitGraph(sizeToFix);
            if (subBranches != null){
                return new Branch(subBranches);
            }

            Set<IntExpression> variables = graph.getUnfixedVariables();
            if (variables.isEmpty()) return null;

            Set<IntExpression> varSet = variables.stream()
                    .sorted((a, b) -> Integer.compare(graph.getUnfixedNeighbors(b).size(), graph.getUnfixedNeighbors(a).size()))
                    .limit(nVars)
                    .collect(Collectors.toSet());

            return new Branch(varSet);
        };
    }

    public static Supplier<Branch> naiveTreeBuilding2(ConstraintGraph graph, int nVars, int sizeToFix){
        boolean[] firstCall = {true};
        return () -> {
            graph.newState();
            Set<IntExpression> variables = graph.getUnfixedVariables();
            if (variables.isEmpty()) return null;

            Set<IntExpression> varSet = variables.stream()
                    .sorted((a, b) -> Integer.compare(graph.getUnfixedNeighbors(b).size(), graph.getUnfixedNeighbors(a).size()))
                    .limit(nVars)
                    .collect(Collectors.toSet());

            if (!varSet.isEmpty()) {
                graph.removeNode(varSet);
            }

            List<SubBranch> subBranches = graph.splitGraph(sizeToFix);

            return new Branch(varSet,subBranches);

        };
    }

    public static Supplier<Branch> fiducciaMattheyses(ConstraintGraph graph, int sizeToFix) {
        return fiducciaMattheyses(graph, sizeToFix, false);
    }

    public static Supplier<Branch> fiducciaMattheyses(ConstraintGraph graph, int sizeToFix, boolean splitFirst){
        boolean[] firstCall = {splitFirst};
        return () -> {
            if (firstCall[0]) {
                firstCall[0] = false;
                graph.newState();
                List<SubBranch> b = graph.splitGraph(sizeToFix);
                if (b != null) return new Branch(b);
            }
            graph.newState();
            Set<IntExpression> unFixedVars = graph.getUnfixedVariables();
            if (unFixedVars.isEmpty()) {
                return null;
            }
            if (unFixedVars.size() <= sizeToFix) {
                return new Branch(unFixedVars);
            }

            Set<IntExpression> cut = fiducciaMattheysesCut(graph);

            graph.removeNode(cut);
            List<Set<IntExpression>> subSet = graph.findConnectedComponents();

            if (subSet.size() > 1) {
                List<SubBranch> subBranches = new ArrayList<>();
                for (Set<IntExpression> s1 : subSet) {
                    subBranches.add(new SubBranch(s1,s1.size() <= sizeToFix));
                }
                return new Branch(cut, subBranches);
            }
            return new Branch(cut);
        };
    }
}
