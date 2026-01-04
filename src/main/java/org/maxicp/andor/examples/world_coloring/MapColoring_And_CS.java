package org.maxicp.andor.examples.world_coloring;

import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.search.DFSearchMini_And_CS;
import org.maxicp.search.SearchStatistics;
import org.maxicp.util.io.InputReader;

import java.util.*;

import static org.maxicp.andor.Scheme.*;
import static org.maxicp.modeling.Factory.makeModelDispatcher;


public class MapColoring_And_CS {
    public static void main(String[] args) {
        String path = (args.length >= 1) ? args[0] : "graph_coloring/regions/world";
        int limite = (args.length >= 2) ? Integer.parseInt(args[1]) : 1000;
        int searchType = (args.length >= 3) ? Integer.parseInt(args[2]) : 1;

        InputReader reader1 = new InputReader(path+"/names.txt");
        InputReader reader2 = new InputReader(path+"/neighbors.txt");

        Map<String, String> names = new HashMap<>();
        try {
            while (true) {
                String name = reader1.getString();
                String code = reader1.getString();
                names.put(code, name);
            }
        } catch (RuntimeException e) {}
        List<String> index = new ArrayList<>(names.keySet());

        ModelDispatcher model = makeModelDispatcher();
        IntExpression[] vars = model.intVarArray(names.size(), 4);

        try {
            while (true) {
                String input = reader2.getString();
                String[] neighbors = input.split(",");
                String main = neighbors[0];
                for (String neighbor : neighbors) {
                    if (!Objects.equals(neighbor, main)){
                        model.add(new AllDifferent(vars[index.indexOf(main)],vars[index.indexOf(neighbor)]));
                    }
                }
            }
        } catch (RuntimeException e) {}

        int to_fix = Math.max(names.size() / 20, 4);
        int fix_to_split = Math.min(names.size() / 10, 10);

        ConcreteCPModel cp = model.cpInstantiate();
        ConstraintGraph graph = model.createGraph(cp);

        DFSearchMini_And_CS search;
        switch (searchType) {
            case 2:
                search = cp.dfSearchMini_And_CS(graph, naiveTreeBuilding(graph,fix_to_split,to_fix), firstFail());
                break;
            default:
                search = cp.dfSearchMini_And_CS(graph, fiducciaMattheyses(graph,to_fix, true), firstFail());

        }

        search.onSolution(() -> {
            for (int k = 0; k < vars.length; k++) {
                int n = vars[k].min()+1;
                if (vars[k].isFixed()){
                    System.out.println(names.get(index.get(k))+ " " + n);
                }
            }
        });
        // Visual representation
        // https://paintmaps.com/map-charts/293/World-map-chart
        // https://paintmaps.com/map-charts/76/France-Detailed-map-chart
        long debut = System.nanoTime();
        SearchStatistics stats = search.solve(limite, false);
        long fin = System.nanoTime();

        System.out.format("\nExecution time : %s ms\n", (fin - debut) / 1_000_000);
        System.out.format("Statistics: %s\n", stats);
    }
}
