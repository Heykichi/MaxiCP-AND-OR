/*
 * MaxiCP is under MIT License
 * Copyright (c)  2024 UCLouvain
 *
 */

package org.maxicp.andor.examples.equation;


import org.maxicp.ModelDispatcher;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.andor.search.DFSearchMini_Or;
import org.maxicp.search.SearchStatistics;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.maxicp.andor.examples.equation.Equation_And.printSum;
import static org.maxicp.modeling.Factory.*;
import static org.maxicp.search.Searches.*;

/**
 * Solves a system of sum constraints using constraint programming and OR DFS:
 *   X1 + ... + Xn = Y
 *   Z1 + ... + Zn = Y
 */
public class Equation_Or {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ModelDispatcher model = makeModelDispatcher();
        int index = 4; // number of variables per equation
        int domain = 4; // maximum value of the variables
        IntExpression[] X = model.intVarArray( index, domain);
        IntExpression[] Z = model.intVarArray( index, domain);
        IntExpression Y = model.intVar(0,domain-1);
        model.add(eq(sum(X), Y));
        model.add(eq(sum(Z), Y));

        IntExpression[] all = Stream.concat(
                Stream.concat(Arrays.stream(X), Arrays.stream(Z)),
                Stream.of(Y)
        ).toArray(IntExpression[]::new);

        ConcreteCPModel cp = model.cpInstantiate();
        DFSearchMini_Or dfs = cp.dfSearchMini(firstFail(all));

        dfs.onSolution(() -> {
            printSum(X,Y);
            printSum(Z,Y);
            System.out.println();
        });

        long debut = java.lang.System.nanoTime();
        SearchStatistics stats = dfs.solve(statistics -> statistics.numberOfSolutions() == 100);
        long fin = java.lang.System.nanoTime();

        java.lang.System.out.format("\nExecution time : %s ms\n", (fin - debut) / 1_000_000);
        java.lang.System.out.format("Statistics: %s\n", stats);

    }
}