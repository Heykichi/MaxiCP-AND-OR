package org.maxicp.modeling.gc_dimacs;

import org.maxicp.ModelDispatcher;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.search.*;
import org.maxicp.util.io.InputReader;

import static org.maxicp.modeling.Factory.*;

public class GraphColoring_Or {
    public static void main(String[] args) {
        String instanceName = (args.length > 0) ? args[0] : "graph_coloring/instance/DSJR500.1.col";
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

        ConcreteCPModel cp = model.cpInstantiate();

        DFSearchMini_Or search = cp.dfSearchMini(Searches.firstFail(vars));
        long debut = System.nanoTime();
        SearchStatistics stats = search.solve(statistics -> statistics.numberOfSolutions() == 100000);
        long fin = System.nanoTime();

        System.out.println(stats);
        System.out.format("Execution time : %s ms\n", (fin - debut) / 1_000_000);
    }
}
