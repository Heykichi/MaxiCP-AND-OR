package org.maxicp.andor.examples.world_coloring;

import org.maxicp.ModelDispatcher;
import org.maxicp.andor.ConstraintGraph;
import org.maxicp.cp.modeling.ConcreteCPModel;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.modeling.constraints.AllDifferent;
import org.maxicp.andor.search.DFSearchMini_And_CS;
import org.maxicp.search.SearchStatistics;
import org.maxicp.util.io.InputReader;

import java.util.*;

import static org.maxicp.andor.Scheme.*;
import static org.maxicp.modeling.Factory.makeModelDispatcher;

/**
 * AND/OR implementation of the map coloring problem, which uses constraint programming
 * to assign colors to regions on a map such that no two adjacent regions have the same color.
 *
 * Notes:
 * - The visual representation of the map can be achieved through external charting tools.
 * - This implementation uses specific text-based file input and does not include error handling for invalid file formats or contents.
 */
public class MapColoring_And {
    public static void main(String[] args) {
        // Instances:
        // "graph_coloring/regions/france"
        // "graph_coloring/regions/world"
        String path = (args.length >= 1) ? args[0] : "graph_coloring/regions/world";
        int solutionsLimit = (args.length >= 2) ? Integer.parseInt(args[1]) : 1;
        int searchType = (args.length >= 3) ? Integer.parseInt(args[2]) : 2;

        InputReader reader1 = new InputReader(path+"/names.txt");
        InputReader reader2 = new InputReader(path+"/neighbors.txt");

        // Initializes the instance variables
        Map<String, String> names = new HashMap<>();
        try {
            while (true) {
                String name = reader1.getString();
                String code = reader1.getString();
                names.put(code, name);
            }
        } catch (RuntimeException e) {}
        List<String> index = new ArrayList<>(names.keySet());

        int nColors = 4;
        ModelDispatcher model = makeModelDispatcher();
        IntExpression[] vars = model.intVarArray(names.size(), nColors);

        // Initializes the instance constraints
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

        int sizeToFix = Math.max(names.size() / 20, 4);
        int fixToSplit = Math.min(names.size() / 10, 10);

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

        DFSearchMini_And_CS search;
        switch (searchType) {
            case 2:
                search = cp.dfSearchMini_And_CS(graph, naiveTreeBuilding(graph,fixToSplit,sizeToFix), firstFail());
                break;
            default:
                search = cp.dfSearchMini_And_CS(graph, fiducciaMattheyses(graph,sizeToFix, true), firstFail());

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
        // solve(int solutionsLimit, boolean showSolutions)
        SearchStatistics stats = search.solve(solutionsLimit, true);
        long fin = System.nanoTime();

        System.out.format("\nExecution time : %s ms\n", (fin - debut) / 1_000_000);
        System.out.format("Statistics: %s\n", stats);
    }
}
