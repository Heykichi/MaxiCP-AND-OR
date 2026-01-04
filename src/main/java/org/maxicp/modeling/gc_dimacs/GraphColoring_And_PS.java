package org.maxicp.modeling.gc_dimacs;

import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.search.DFSearchMini_And_PS;
import org.maxicp.search.DFSearchMini_Or;
import org.maxicp.search.SearchStatistics;
import org.maxicp.search.Searches;
import org.maxicp.util.io.InputReader;

import java.util.Arrays;

import static org.maxicp.andor.Scheme.fiducciaMattheyses;
import static org.maxicp.andor.Scheme.firstFail;
import static org.maxicp.modeling.Factory.makeModelDispatcher;

public class GraphColoring_And_PS {
    public static void main(String[] args) {
        String instanceName = (args.length > 0) ? args[0] : "graph_coloring/instance/anna.col";
        InputReader reader = new InputReader(instanceName);

        ModelDispatcher model = makeModelDispatcher();

        String type = reader.getString();
        while (type.equals("c")) {
            reader.skipLine();
            type = reader.getString();
        }
        assert type.equals("p");
        reader.getString();
        Integer[] problem = reader.getIntLine();
        //System.out.println(Arrays.toString(problem));
        IntExpression[] vars = model.intVarArray(problem[0], problem[2]);

        for (int i = 0; i < problem[1]; i++) {
            reader.getString();
            Integer[] edgeVars = reader.getIntLine();
            model.add(new AllDifferent(vars[edgeVars[0]-1],vars[edgeVars[1]-1]));
        }

        int to_fix = Math.max(problem[0] / 20, 4);
        int fix_to_split = Math.min(problem[0] / 10, 10);

        ConcreteCPModel cp = model.cpInstantiate();
        ConstraintGraph graph = model.createGraph(cp);

        DFSearchMini_And_PS search = cp.dfSearchMini_And_PS(graph, fiducciaMattheyses(graph,to_fix, false), firstFail());

        long debut = System.nanoTime();
        SearchStatistics stats = search.solve(100000,false);
        long fin = System.nanoTime();

        System.out.println(stats);
        System.out.format("Execution time : %s ms\n", (fin - debut) / 1_000_000);
    }
}
