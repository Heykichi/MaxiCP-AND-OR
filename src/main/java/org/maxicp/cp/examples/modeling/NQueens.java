/*
 * MaxiCP is under MIT License
 * Copyright (c)  2024 UCLouvain
 *
 */

package org.maxicp.cp.examples.modeling;


import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;

import static org.maxicp.modeling.Factory.*;
import org.maxicp.modeling.IntVar;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.search.*;
import static org.maxicp.search.Searches.*;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;


import static org.maxicp.search.Searches.EMPTY;
import static org.maxicp.search.Searches.branch;

/**
 * The N-Queens problem.
 * <a href="http://csplib.org/Problems/prob054/">CSPLib</a>.
 */
public class NQueens {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int n = 4;

        ModelDispatcher model = makeModelDispatcher();

        IntVar[] q = model.intVarArray(n, n);
        IntExpression[] qL = model.intVarArray(n,i -> q[i].plus(i));
        IntExpression[] qR = model.intVarArray(n,i -> q[i].minus(i));

        model.add(allDifferent(q));
        model.add(allDifferent(qL));
        model.add(allDifferent(qR));

        Supplier<Runnable[]> branching = () -> {
            IntExpression qs = selectMin(q,
                    qi -> qi.size() > 1,
                    qi -> qi.size());
            if (qs == null)
                return EMPTY;
            else {
                int v = qs.min();
                return branch(() -> model.add(eq(qs, v)), () -> model.add(neq(qs, v)));
            }
        };
        System.out.println("======0");
        ConcreteCPModel cp = model.cpInstantiate();
        ConstraintGraph cg = model.createGraph(cp);
        System.out.println("======1");
//        DFSearch dfs = cp.dfSearch(branching);
        DFSearchMini_Or dfs = cp.dfSearchMini(branching);
        dfs.onSolution(() -> {
            System.out.println(Arrays.toString(q));
        });

        long debut = System.nanoTime();
        System.out.println("======2");
        SearchStatistics stats = dfs.solve(statistics -> statistics.numberOfSolutions() == 1);
        //SearchStatistics stats = dfs.solve(statistics -> statistics.numberOfSolutions() == 2);
        System.out.println("======3");
        long fin = System.nanoTime();
        System.out.format("\nExecution time : %s ms\n", (fin - debut) / 1_000_000);
        System.out.format("Statistics: %s\n", stats);

    }
}