package org.maxicp.andor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a SlicedTable, which is a hierarchical structure comprising a pattern and a collection of sub-SlicedTable.
 * The class provides methods to retrieve the pattern and sub-tables, as well as static utility methods for processing
 * and combining hierarchical data structures.
 *
 * Based on ideas from:
 * N. Gharbi, F. Hemery, C. Lecoutre, and O. Roussel (2014).
 * "Sliced Table Constraints: Combining Compression and Tabular Reduction".
 * In: Principles and Practice of Constraint Programming (CP 2014).
 */
public class SlicedTable {
    private Map<Integer, Integer> pattern = null;
    private final List<List<SlicedTable>> subSlicedTables;

    public SlicedTable(Map<Integer, Integer> pattern, List<List<SlicedTable>> subSlicedTables) {
        this.pattern = pattern;
        this.subSlicedTables = subSlicedTables;
    }

    public SlicedTable(Map<Integer, Integer> pattern) {
        this.pattern = pattern;
        this.subSlicedTables = new ArrayList<List<SlicedTable>>();
    }

    public SlicedTable() {
        this.subSlicedTables = new ArrayList<List<SlicedTable>>();
    }

    public Map<Integer, Integer> getPattern() {return pattern;}


    public List<List<SlicedTable>> getSubSlicedTables() {return subSlicedTables;}

    /**
     * Computes a list of solutions by processing a list of sliced tables, each of which may contain a pattern
     * and nested subtables. The computation aggregates solutions from subtables.
     * The resulting list is restricted by the specified limit.
     *
     * @param SlicedTables a list of SlicedTable to process, each containing a pattern and optionally nested subtables
     * @param limit the maximum number of solutions to compute and return
     * @return a list of maps representing the aggregated solutions, limited to the specified maximum number
     */
    public static List<Map<Integer, Integer>> computeSlicedTable(List<SlicedTable> SlicedTables, int limit){
        List<Map<Integer, Integer>> solutions = new ArrayList<>();
        for (SlicedTable st : SlicedTables ){
            List<Map<Integer, Integer>> subs = computeSubTables(st,limit);
            if (!subs.isEmpty()) {
                solutions.addAll(subs);
            }
            if (solutions.size() >= limit) {
                break;
            }
        }
        return solutions;
    }

    /**
     * Computes a list of aggregated solutions from a given SlicedTable. The computation
     * involves processing patterns and recursively handling nested sub-tables.
     * The resulting list is restricted by the specified limit.
     *
     * @param slicedTable the SlicedTable to process, which may contain a pattern and nested subtables
     * @param limit the maximum number of solutions to compute and return
     * @return a list of maps representing aggregated solutions, with the total number limited to the specified value
     */
    private static List<Map<Integer, Integer>> computeSubTables(SlicedTable slicedTable, int limit){
        List<Map<Integer, Integer>> solutions = new ArrayList<>();
        Map<Integer, Integer> p = slicedTable.getPattern();

        if (slicedTable.getSubSlicedTables() == null || slicedTable.getSubSlicedTables().isEmpty()){
            if (p != null) {
                solutions.add(new HashMap<>(p));
            }  else {
                throw new IllegalStateException("Null pattern without sub-tables");
            }
            return solutions;
        }
        solutions.add(slicedTable.getPattern());
        for (List<SlicedTable> subTables : slicedTable.getSubSlicedTables()){
            List<Map<Integer, Integer>> subsolutions = new ArrayList<>();
            for (SlicedTable s : subTables) {
                subsolutions.addAll(computeSubTables(s,limit));
            }
            solutions = combine(solutions, subsolutions,limit);
        }
        return solutions;
    }

    /**
     * Combines two lists of subsolutions into a single list of solutions/subsolutions,
     * where each map is a combinaison of one subsolutions from the first list
     * and one subsolutions from the second list.
     * The resulting list is restricted by the specified limit.
     *
     * @param a the first list of subsolutions
     * @param b the second list of subsolutions
     * @param limit the maximum number of solutions to compute and return
     * @return a list of maps representing the aggregated solutions, limited to the specified maximum number
     */
    private static List<Map<Integer, Integer>> combine(List<Map<Integer, Integer>> a, List<Map<Integer, Integer>> b, int limit) {
        List<Map<Integer, Integer>> resultat = new ArrayList<>();
        if (a.isEmpty()) {
            a.add(new HashMap<>());
        }
        if (b.isEmpty()) {
            return a;
        }
        outer:
        for (Map<Integer, Integer> l1 : a) {
            for (Map<Integer, Integer> l2 : b) {
                Map<Integer, Integer> nouvelle = new HashMap<>();
                if (l1 != null) nouvelle.putAll(l1);
                nouvelle.putAll(l2);
                resultat.add(nouvelle);
                if (resultat.size() >= limit) {
                    break outer;
                }
            }
        }
        return resultat;
    }
}


