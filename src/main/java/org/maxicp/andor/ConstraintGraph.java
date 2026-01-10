package org.maxicp.andor;

import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.StateStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The {@code ConstraintGraph} class represents a constraint graph,
 * typically used for managing relationships between variables in constraint programming.
 * Nodes in the graph represent decision variables, and edges represent constraints.
 */
public class ConstraintGraph {

    private Map<IntExpression, Set<IntExpression>> adjacencyList = new HashMap<>();
    private StateStack<Set<IntExpression>> stateVars;
    private Set<IntExpression> removedNodes = new HashSet<>();

    public ConstraintGraph(StateManager sm) {
        this.stateVars = new StateStack<Set<IntExpression>>(sm);
        this.stateVars.push(adjacencyList.keySet());
    }

    /**
     * Checks whether a solution has been found in the current state of the constraint graph.
     * The method iterates over the latest state variables and checks whether they are all fixed.
     *
     * @return {@code true} if all variables in the last recorded state are fixed;
     *         {@code false} otherwise.
     */
    public boolean solutionFound() {
        for (IntExpression var : this.stateVars.getLastElement()) {
            if (!var.isFixed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves the set of neighbors of the given variable that are not fixed, are part of the
     * currente state, and have not been removed from the graph.
     *
     * @param key the variable whose unfixed neighbors are to be retrieved
     * @return a set of unfixed neighbors of the specified variable
     */
    public Set<IntExpression> getUnfixedNeighbors(IntExpression key) {
        return adjacencyList.get(key).stream()
                .filter(var -> !var.isFixed() && this.stateVars.getLastElement().contains(var) && !this.removedNodes.contains(var))
                .collect(Collectors.toSet());
    }
    public Set<IntExpression> getVariables() {
        return new HashSet<>(this.stateVars.getLastElement());
    }

    public Set<IntExpression> getUnfixedVariables() {
        return this.stateVars.getLastElement().stream()
                .filter(var -> !var.isFixed() && !this.removedNodes.contains(var))
                .collect(Collectors.toSet());
    }

    /**
     * Captures a new state in the constraint graph by creating a copy of the current state's
     * variables and pushing it onto the state stack.
     * <p>
     * This method is used to record the current state of the graph so that it can
     * later be restored or reverted, supporting backtracking and state management within the
     * solver.
     */
    public void newState(){
        Set<IntExpression> newStateValue = new HashSet<>(this.stateVars.getLastElement());
        this.stateVars.push(newStateValue);
        this.removedNodes.clear();
    }

    public void newState(Set<IntExpression> Variables){
        newState();
        this.stateVars.push(Variables);
    }

    public Set<IntExpression> getStateVariables(){return this.stateVars.getLastElement();}

    /**
     * Adds a node to the constraint graph if it doesn't already exist.
     * It initializes an empty set of neighbors in the adjacency list.
     *
     * @param node the variable to be added as a node in the graph
     */
    public void addNode(IntExpression node) {
        this.adjacencyList.putIfAbsent(node, new HashSet<IntExpression>());
    }

    /**
     * Adds nodes to the constraint graph if they don't already exist.
     * For each node, it initializes an empty set of neighbors in the adjacency list.
     *
     * @param nodes an array of variables to be added as nodes in the graph
     */
    public void addNode(IntExpression[] nodes) {
        for (IntExpression n : nodes) {
            this.adjacencyList.putIfAbsent(n, new HashSet<IntExpression>());
        }
    }

    /**
     * Adds nodes and an edge between them.
     *
     * @param node1 node to be connected
     * @param node2 node to be connected
     */
    public void addEdge(IntExpression node1, IntExpression node2) {
        if (node1.equals(node2)) {
            throw new IllegalArgumentException("Self-edge are not allowed");
        }
        adjacencyList.get(node1).add(node2);
        adjacencyList.get(node2).add(node1);
    }

    /**
     * Adds nodes and adds edges between all pairs of nodes in the given array.
     *
     * @param nodes the array of nodes to be connected
     */
    public void addEdge(IntExpression[] nodes) {
        addNode(nodes);
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                addEdge(nodes[i], nodes[j]);
            }
        }
    }

    /**
     * Identifies and returns the connected components of the constraint graph.
     * Each connected component is represented as a set of variables that are
     * interconnected through the graph's edges.
     *
     * The method uses a depth-first search (DFS) algorithm to traverse the graph.
     *
     * @return a list of sets, where each set contains {@code IntVar} objects
     *         representing a connected component in the constraint graph
     */
    public List<Set<IntExpression>> findConnectedComponents() {
        List<Set<IntExpression>> subgraphs = new ArrayList<>();
        Set<IntExpression> visited = new HashSet<>();

        for (IntExpression node : this.stateVars.getLastElement()) {
            if (!visited.contains(node) && !node.isFixed() && !this.removedNodes.contains(node)) {
                Set<IntExpression> subgraph = new HashSet<>();
                dfs(node, visited, subgraph);
                subgraphs.add(subgraph);
            }
        }
        return subgraphs;
    }

    /**
     * Performs a Depth-First Search (DFS) on the constraint graph starting from the specified node.
     * The DFS identifies all connected components of the graph by traversing its nodes recursively.
     *
     * @param node      the starting node for the DFS
     * @param visited   the set of nodes that have been visited
     * @param component the current component being built
     */
    private void dfs(IntExpression node, Set<IntExpression> visited, Set<IntExpression> component) {
        visited.add(node);
        component.add(node);

        for (IntExpression neighbor : this.getUnfixedNeighbors(node)) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, visited, component);
            }
        }
    }

    /**
     * Splits the constraint graph into smaller subbranches based on the connected components
     * of the graph. Each subbranch represents a subset of variables, and a flag indicates
     * whether its size is less than or equal to the specified threshold.
     *
     * @param sizeToFix the threshold on the number of variables below which we no longer search a split
     * @return a list of SubBranch objects representing the connected components
     *         of the graph, or null if the graph cannot be split into multiple components
     */
    public List<SubBranch> splitGraph(int sizeToFix){
        List<Set<IntExpression>> subgraphs = this.findConnectedComponents();
        List<SubBranch> subBranches = new ArrayList<>();
        if (subgraphs.size() > 1 ){
            for (Set<IntExpression> s : subgraphs){
                subBranches.add(new SubBranch(s,s.size() <= sizeToFix ));
            }
            return subBranches;
        }
        return null;
    }


    public void removeNode(IntExpression nodeToRemove) {
        this.removedNodes.add(nodeToRemove);
    }

    public void removeNode(IntExpression[] nodeToRemove) {
        this.removedNodes.addAll(Arrays.asList(nodeToRemove));
    }

    public void removeNode(Set<IntExpression> nodeToRemove) {
        this.removedNodes.addAll(nodeToRemove);
    }

    public void restoreNode(IntExpression nodeToRestore) {
        this.removedNodes.remove(nodeToRestore);
    }

    public void restoreNode(IntExpression[] nodeToRestore) {
        Arrays.asList(nodeToRestore).forEach(this.removedNodes::remove);
    }

    public void restoreNode(Set<IntExpression> nodeToRestore) {
        this.removedNodes.removeAll(nodeToRestore);
    }

    @Override
    public String toString() {
        if (adjacencyList.isEmpty()) return "Graph is empty";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<IntExpression, Set<IntExpression>> entry : adjacencyList.entrySet()) {
            sb.append(entry.getKey().hashCode()).append(" : ").append(entry.getKey()).append(" -> ");
            if (entry.getValue().isEmpty()) {
                sb.append(" / ");
            } else {
                sb.append(entry.getValue().stream()
                        .map(IntExpression::hashCode)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
