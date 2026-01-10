package org.maxicp.andor;

import org.maxicp.modeling.algebra.integer.IntExpression;

import java.util.*;

/**
 * Implements the simplified Fiduccia–Mattheyses (FM) algorithm for graph partitioning,
 * which minimizes the cut size of a bipartition while maintaining balance between partitions.
 * The algorithm works by iteratively moving nodes between two partitions based on a calculated gain.
 *
 * Based on:
 * C. M. Fiduccia and R. M. Mattheyses (1982).
 * "A Linear-Time Heuristic for Improving Network Partitions".
 * Proceedings of the 19th Design Automation Conference (DAC).
 */
public class FiducciaMattheysesCut {

    /**
     * Applies the Fiduccia–Mattheyses algorithm to partition a given constraint graph and determine
     * a set of cut nodes. The algorithm iteratively balances the partitions while attempting to
     * minimize the number of edges crossing between them. The resulting cut set consists of nodes
     * that have at least one neighbor in the opposite partition.
     *
     * @param graph the constraint graph representing variables as nodes and constraints as edges
     * @return a set of variables (nodes) that constitute the cut based on the computed partitioning;
     *         The cup must not be empty, even if we have a bad partition, and it cannot find a 'real' cut.
     */
    public static Set<IntExpression> fiducciaMattheysesCut(ConstraintGraph graph) {
        Set<IntExpression> nodes = graph.getUnfixedVariables();
        if (nodes.size() <= 1) return Collections.emptySet();

        IntExpression[] varsArray = nodes.stream()
                .sorted(Comparator.comparingInt(IntExpression::getId))
                .toArray(IntExpression[]::new);

        // Initial balanced partition: first half in A, second half in B
        Set<IntExpression> partA = new HashSet<>();
        Set<IntExpression> partB = new HashSet<>();
        for (int i = 0; i < varsArray.length; i++) {
            if (i < varsArray.length / 2) partA.add(varsArray[i]);
            else partB.add(varsArray[i]);
        }

        // Gains for potential moves from A -> B and B -> A
        Map<IntExpression, Integer> gainA = new HashMap<>();
        Map<IntExpression, Integer> gainB = new HashMap<>();

        // Greedy improvement loop
        boolean improvement = true;
        while (improvement) {
            improvement = false;

            // Compute gains for current partitioning
            gainA.clear();
            gainB.clear();

            addGain(graph, partA, partB, gainA);
            addGain(graph, partB, partA, gainB);

            // Find max gain node in A and B
            IntExpression bestA = argMaxByValue(gainA);
            IntExpression bestB = argMaxByValue(gainB);

            int bestGainA = (bestA == null) ? Integer.MIN_VALUE : gainA.get(bestA);
            int bestGainB = (bestB == null) ? Integer.MIN_VALUE : gainB.get(bestB);

            if (bestGainA <= 0 && bestGainB <= 0) {
                break;
            }

            // Move node with the highest gain that maintains balance
            if (bestA != null && (partA.size() > partB.size() || partA.size() == partB.size())) {
                int gain = gainA.get(bestA);
                if (gain > 0) {
                    partA.remove(bestA);
                    partB.add(bestA);
                    improvement = true;
                }
            }
            if (!improvement && bestB != null && (partB.size() > partA.size() || partA.size() == partB.size())) {
                int gain = gainB.get(bestB);
                if (gain > 0) {
                    partB.remove(bestB);
                    partA.add(bestB);
                    improvement = true;
                }
            }
        }

        // Select the nodes that have at least one neighbor in the opposite partition
        Set<IntExpression> cutNodes = new HashSet<>();
        for (IntExpression node : nodes) {
            for (IntExpression neighbor : graph.getUnfixedNeighbors(node)) {
                if ((partA.contains(node) && partB.contains(neighbor)) ||
                        (partB.contains(node) && partA.contains(neighbor))) {
                    cutNodes.add(node);
                    break;
                }
            }
        }
        return cutNodes;
    }

    /**
     * Computes and assigns a "gain" value for each variable in the specified set.
     * The gain is calculated as the difference between the number of external connections (to the opposite set)
     * and internal connections (within the same set) for each variable.
     *
     * @param graph The constraint graph representing the problem's variables and constraints.
     * @param from the set of variables (nodes) in the current partition being evaluated
     * @param to the set of variables (nodes) in the opposite partition
     * @param gain a map in which the computed gain values for each variable in the "from" set are stored; this map will be cleared and updated
     */
    private static void addGain(ConstraintGraph graph, Set<IntExpression> from, Set<IntExpression> to, Map<IntExpression, Integer> gain) {
        gain.clear();
        for (IntExpression node : from) {
            int ext = 0, inter = 0;
            for (IntExpression neighbor : graph.getUnfixedNeighbors(node)) {
                if (to.contains(neighbor)) ext++;
                else if (from.contains(neighbor)) inter++;
            }
            gain.put(node, ext - inter);
        }
    }

    /**
     * Selects and returns the key from the given map that corresponds to the maximum value.
     *
     * @param scores a map where keys are {@code IntExpression} instances and values are integers
     *               representing the scores associated with each variable
     * @return the {@code IntExpression} key with the highest associated value, or {@code null} if the map is empty
     */
    private static IntExpression argMaxByValue(Map<IntExpression, Integer> scores) {
        return scores.entrySet().stream()
                .max(Comparator.<Map.Entry<IntExpression,Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparingInt(e -> -e.getKey().getId())) // ou +getId() selon tie-break
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
