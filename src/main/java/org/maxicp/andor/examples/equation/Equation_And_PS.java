/*
 * MaxiCP is under MIT License
 * Copyright (c)  2024 UCLouvain
 *
 */

package org.maxicp.andor.examples.equation;


import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.search.DFSearchMini_And_PS;
import org.maxicp.search.SearchStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.maxicp.andor.Scheme.*;
import static org.maxicp.modeling.Factory.*;

/**
 * The N-Queens problem.
 * <a href="http://csplib.org/Problems/prob054/">CSPLib</a>.
 */
public class Equation_And_PS {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ModelDispatcher model = makeModelDispatcher();
        int index = 4;
        int domain = 4;
        IntExpression[] X = model.intVarArray( index, domain);
        IntExpression[] Z = model.intVarArray( index, domain);
        IntExpression Y = model.intVar(0,domain-1);
        model.add(eq(sum(X), Y));
        model.add(eq(sum(Z), Y));

        List<IntExpression> list = new ArrayList<>();
        Collections.addAll(list, X);
        Collections.addAll(list, Z);
        list.add(Y);

        ConcreteCPModel cp = model.cpInstantiate();
        ConstraintGraph graph = model.createGraph(cp);

        //DFSearchMini_And_PS dfs = cp.dfSearchMini_And_PS(graph, fiducciaMattheyses(graph,5, false), firstFail());
        DFSearchMini_And_PS dfs = cp.dfSearchMini_And_PS(graph, naiveTreeBuilding(graph,5,1), firstFail());
        dfs.onSolution(() -> {
            printSum(X,Y);
            printSum(Z,Y);
        });

        long debut = System.nanoTime();
        SearchStatistics stats = dfs.solve(100,true);
        long fin = java.lang.System.nanoTime();

        java.lang.System.out.format("Execution time : %s ms\n", (fin - debut) / 1_000_000);
        java.lang.System.out.format("Statistics: %s\n", stats);
    }
    public static void printSum(IntExpression[] vars, IntExpression sum){
        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < vars.length -1; i += 1) {
            expression.append(vars[i]).append(" + ");
        }
        expression.append(vars[vars.length-1]);
        System.out.println(expression.toString() + " = " + sum);
    }

}