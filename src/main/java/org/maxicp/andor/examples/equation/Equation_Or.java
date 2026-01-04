/*
 * MaxiCP is under MIT License
 * Copyright (c)  2024 UCLouvain
 *
 */

package org.maxicp.andor.examples.equation;


import org.maxicp.ModelDispatcher;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.IntVar;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.search.DFSearchMini_Or;
import org.maxicp.search.SearchStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.maxicp.modeling.Factory.*;
import static org.maxicp.search.Searches.*;

/**
 * The N-Queens problem.
 * <a href="http://csplib.org/Problems/prob054/">CSPLib</a>.
 */
public class Equation_Or {
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
        IntVar[] W = list.toArray(new IntVar[0]);

        Supplier<Runnable[]> branching = () -> {
            IntExpression qs = selectMin(W,
                    qi -> qi.size() > 1,
                    qi -> qi.size());
            if (qs == null)
                return EMPTY;
            else {
                int v = qs.min();
                return branch(() -> model.add(eq(qs, v)), () -> model.add(neq(qs, v)));
            }
        };

        ConcreteCPModel cp = model.cpInstantiate();

        DFSearchMini_Or dfs = cp.dfSearchMini(branching);
        dfs.onSolution(() -> {
            java.lang.System.out.println(Arrays.toString(X)+ " = " + Arrays.toString(Z) + "=> "+ Y);
        });

        long debut = java.lang.System.nanoTime();
        //java.lang.System.out.println("======2");
        // statistics -> statistics.numberOfSolutions() == 5
        SearchStatistics stats = dfs.solve();
        //java.lang.System.out.println("======3");
        long fin = java.lang.System.nanoTime();
        java.lang.System.out.format("\nExecution time : %s ms\n", (fin - debut) / 1_000_000);
        java.lang.System.out.format("Statistics: %s\n", stats);

    }
}