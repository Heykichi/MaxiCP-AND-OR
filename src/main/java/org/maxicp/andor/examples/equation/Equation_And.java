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
import org.maxicp.andor.search.DFSearchMini_And_CS;
import org.maxicp.search.SearchStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.maxicp.andor.Scheme.*;
import static org.maxicp.modeling.Factory.*;

/**
 * Solves a system of sum constraints using constraint programming and AND/OR DFS:
 *   X1 + ... + Xn = Y
 *   Z1 + ... + Zn = Y
 */
public class Equation_And {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ModelDispatcher model = makeModelDispatcher();
        int index = 4; // number of variables per equation
        int domain = 4; // maximum value of the variables
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

        /**
         * Define the DFSearch model: complete solution (CS) or partial solution (PS)
         * Define the tree building strategy:
         *  - fiducciaMattheyses(ConstraintGraph graph, int sizeToFix, boolean splitFirst)
         *  - naiveTreeBuilding(ConstraintGraph graph, int fixToSplit, int sizeToFix)
         *      sizeToFix: threshold on the number of variables below which we no longer create AND branch
         *      splitFirst: try to create an AND branch first
         *      fixToSplit: number of variables to fix before checking if an AND branch is possible
         *  Define the branching strategy:
         *  - firstFail()
         *  - firstOrder()
         */

        //DFSearchMini_And_PS dfs = cp.dfSearchMini_And_PS(graph, naiveTreeBuilding(graph,5,1), firstFail());
        DFSearchMini_And_CS dfs = cp.dfSearchMini_And_CS(graph, naiveTreeBuilding(graph,5,1), firstFail());

        dfs.onSolution(() -> {
            printSum(X,Y);
            printSum(Z,Y);
        });

        long debut = System.nanoTime();
        // solve(int solutionsLimit, boolean showSolutions)
        SearchStatistics stats = dfs.solve(100,true);
        long fin = java.lang.System.nanoTime();

        java.lang.System.out.format("Execution time : %s ms\n", (fin - debut) / 1_000_000);
        java.lang.System.out.format("Statistics: %s\n", stats);
    }


    /**
     * Prints the sum of an array of {@code IntExpression} and a {@code IntExpression}
     * in the format `var1 + var2 + ... + varN = sum`.
     *
     * @param vars an array of {@code IntExpression}
     * @param sum  an {@code IntExpression} element representing the result of the sum.
     */
    public static void printSum(IntExpression[] vars, IntExpression sum){
        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < vars.length -1; i += 1) {
            expression.append(vars[i]).append(" + ");
        }
        expression.append(vars[vars.length-1]);
        System.out.println(expression.toString() + " = " + sum);
    }

}