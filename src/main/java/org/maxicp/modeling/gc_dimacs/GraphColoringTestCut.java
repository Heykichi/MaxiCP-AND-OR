package org.maxicp.modeling.gc_dimacs;

import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.util.io.InputReader;

import java.util.*;

import static org.maxicp.andor.Scheme.fiducciaMattheyses;
import static org.maxicp.andor.FiducciaMattheysesCut.fiducciaMattheysesCut;
import static org.maxicp.modeling.Factory.makeModelDispatcher;

public class GraphColoringTestCut {
    public static void main(String[] args) {
        String instanceName = (args.length > 0) ? args[0] : "graph_coloring/instance/homer.col";
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
        IntExpression[] vars = model.intVarArray(problem[0], problem[2]);

        for (int i = 0; i < problem[1]; i++) {
            reader.getString();
            Integer[] edgeVars = reader.getIntLine();
            if (!Objects.equals(edgeVars[0], edgeVars[1])){
                model.add(new AllDifferent(vars[edgeVars[0]-1],vars[edgeVars[1]-1]));
            }
        }

        ConcreteCPModel cp = model.cpInstantiate();
        ConstraintGraph graph = model.createGraph(cp);

        Set<IntExpression> cut = fiducciaMattheysesCut(graph);
        graph.removeNode(cut);
        List<Set<IntExpression>> subSet = graph.findConnectedComponents();
        int largest_s = 0;

        if (!subSet.isEmpty()) {
            largest_s = subSet.stream()
                    .max(Comparator.comparingInt(Set::size))
                    .orElse(Collections.emptySet()).size();
        }
        System.out.println("var: " + problem[0]);
        System.out.println("cut: " + cut.size());
        System.out.println("subset: " + subSet.size());
        System.out.println("largest: " + largest_s);
    }
}
