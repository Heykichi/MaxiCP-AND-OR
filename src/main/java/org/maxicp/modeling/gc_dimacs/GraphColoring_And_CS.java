package org.maxicp.modeling.gc_dimacs;

import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.andor.search.DFSearchMini_And_CS;
import org.maxicp.search.SearchStatistics;
import org.maxicp.util.io.InputReader;

import static org.maxicp.andor.Scheme.fiducciaMattheyses;
import static org.maxicp.andor.Scheme.firstFail;
import static org.maxicp.modeling.Factory.makeModelDispatcher;

public class GraphColoring_And_CS {
    public static void main(String[] args) {
        String instanceName = (args.length > 0) ? args[0] : "graph_coloring/instance/myciel4.col";
        InputReader reader = new InputReader(instanceName);
        ModelDispatcher model = makeModelDispatcher();

        // Initializes the instance variables
        String type = reader.getString();
        while (type.equals("c")) {
            reader.skipLine();
            type = reader.getString();
        }
        assert type.equals("p");
        reader.getString();
        Integer[] problem = reader.getIntLine();
        IntExpression[] vars = model.intVarArray(problem[0], problem[2]);

        // Initializes the instance constraints
        for (int i = 0; i < problem[1]; i++) {
            reader.getString();
            Integer[] edgeVars = reader.getIntLine();
            model.add(new AllDifferent(vars[edgeVars[0]-1],vars[edgeVars[1]-1]));
        }

        int sizeToFix = Math.max(problem[0] / 20, 4);
        int fixToSplit = Math.min(problem[0] / 10, 10);

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


        DFSearchMini_And_CS search = cp.dfSearchMini_And_CS(graph, fiducciaMattheyses(graph,sizeToFix, false), firstFail());

        long debut = System.nanoTime();
        SearchStatistics stats = search.solve(false);
        long fin = System.nanoTime();

        System.out.println(stats);
        System.out.format("Execution time : %s ms\n", (fin - debut) / 1_000_000);
    }
}
