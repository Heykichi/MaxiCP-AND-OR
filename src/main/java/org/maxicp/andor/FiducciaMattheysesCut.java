package org.maxicp.andor;

import org.maxicp.modeling.algebra.integer.IntExpression;

import java.util.*;

public class FiducciaMattheysesCut {
    // Fiduccia–Mattheyses algorithm
    public static Set<IntExpression> fiducciaMattheysesCut(ConstraintGraph graph) {
        Set<IntExpression> nodes = graph.getUnfixedVariables();
        if (nodes.size() <= 1) return Collections.emptySet();

        IntExpression[] varsArray = nodes.stream()
                .sorted(Comparator.comparingInt(IntExpression::getId))
                .toArray(IntExpression[]::new);

        // 1. Initial balanced partition
        Set<IntExpression> partA = new HashSet<>();
        Set<IntExpression> partB = new HashSet<>();
        for (int i = 0; i < varsArray.length; i++) {
            if (i < varsArray.length / 2) partA.add(varsArray[i]);
            else partB.add(varsArray[i]);
        }

        Map<IntExpression, Integer> gainA = new HashMap<>();
        Map<IntExpression, Integer> gainB = new HashMap<>();

        boolean improvement = true;
        while (improvement) {
            improvement = false;
            // Calculate gain for each node
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

            // Move node with highest gain that maintains balance
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

    private static IntExpression argMaxByValue(Map<IntExpression, Integer> scores) {
        return scores.entrySet().stream()
                .max(Comparator.<Map.Entry<IntExpression,Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparingInt(e -> -e.getKey().getId())) // ou +getId() selon tie-break
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
