/*
 * MaxiCP is under MIT License
 * Copyright (c)  2024 UCLouvain
 *
 */

package org.maxicp.cp.examples.raw;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.DFSearch;
import org.maxicp.search.SearchStatistics;
import static org.maxicp.cp.CPFactory.*;

import java.util.function.Supplier;

import static org.maxicp.search.Searches.EMPTY;
import static org.maxicp.search.Searches.selectMin;


/**
 * The N-Queens problem.
 * <a href="http://csplib.org/Problems/prob054/">CSPLib</a>.
 */
public class NQueens {
    public static void main(String[] args) {
        int n = 14;

        CPSolver cp = CPFactory.makeSolver();
        CPIntVar[] q = CPFactory.makeIntVarArray(cp, n, n);
        CPIntVar[] qL = CPFactory.makeIntVarArray(n, i -> minus(q[i],i));
        CPIntVar[] qR = CPFactory.makeIntVarArray(n, i -> plus(q[i],i));

        cp.post(allDifferent(q));
        cp.post(allDifferent(qL));
        cp.post(allDifferent(qR));

        Supplier<Runnable[]> branching = () -> {
            CPIntVar qs = selectMin(q,
                    qi -> qi.size() > 1,
                    qi -> qi.size());
            if (qs == null)
                return EMPTY;
            else {
                int v = qs.min();
                Runnable left = () -> cp.post(CPFactory.eq(qs, v));
                Runnable right = () -> cp.post(CPFactory.neq(qs, v));
                return new Runnable[]{left, right};
            }
        };

        DFSearch search = CPFactory.makeDfs(cp, branching);
        //DFSearchMini search = CPFactory.makeDfsMini(cp, branching);

        // a more compact first fail search using selectors is given next
/*
        DFSearch search = Factory.makeDfs(cp, () -> {
            IntVar qs = selectMin(q,
                    qi -> qi.size() > 1,
                    qi -> qi.size());
            if (qs == null) return EMPTY;
            else {
                int v = qs.min();
                return branch(() -> cp.post(Factory.equal(qs, v)),
                        () -> cp.post(Factory.notEqual(qs, v)));
            }
        });*/


//        search.onSolution(() ->
//                System.out.println("solution:" + Arrays.toString(q))
//        );
        long debut = System.nanoTime();
        System.out.println("======");
        SearchStatistics stats = search.solve();
        System.out.println("======");
        long fin = System.nanoTime();
        System.out.print("MaxiCP 14-Queens, raw, firstFail");
        System.out.format("\nExecution time : %s ms\n", (fin - debut) / 1_000_000);

        //System.out.format("#Solutions: %s\n", stats.numberOfSolutions());
        System.out.format("Statistics: %s\n", stats);

    }
}
