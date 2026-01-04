package org.maxicp.andor;

import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.state.StateManager;
import org.maxicp.state.datastructures.StateStack;

import java.util.*;
import java.util.stream.Collectors;


public class ConstraintGraph {

    private Map<IntExpression, Set<IntExpression>> adjacencyList = new HashMap<>();
    private StateStack<Set<IntExpression>> stateVars;
    private Set<IntExpression> removedNodes = new HashSet<>();

    public ConstraintGraph(StateManager sm) {
        this.stateVars = new StateStack<Set<IntExpression>>(sm);
        this.stateVars.push(adjacencyList.keySet());
    }

    public boolean solutionFound() {
        for (IntExpression var : this.stateVars.getLastElement()) {
            if (!var.isFixed()) {
                return false;
            }
        }
        return true;
    }

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

    public ConstraintGraph newState(){
        Set<IntExpression> newStateValue = new HashSet<>(this.stateVars.getLastElement());
        this.stateVars.push(newStateValue);
        this.removedNodes.clear();
        return this;
    }

    public void newState(Set<IntExpression> Variables){
        newState();
        this.stateVars.push(Variables);
    }

    public Set<IntExpression> getStateVariables(){return this.stateVars.getLastElement();}

    public void addNode(IntExpression node) {
        this.adjacencyList.putIfAbsent(node, new HashSet<IntExpression>());
    }

    public void addNode(IntExpression[] nodes) {
        for (IntExpression n : nodes) {
            this.adjacencyList.putIfAbsent(n, new HashSet<IntExpression>());
        }
    }

    public void addEdge(IntExpression node1, IntExpression node2) {
        if (node1.equals(node2)) {
            throw new IllegalArgumentException("Self-edge are not allowed");
        }
        adjacencyList.get(node1).add(node2);
        adjacencyList.get(node2).add(node1);
    }

    public void addEdge(IntExpression[] nodes) {
        addNode(nodes);
        for (int i = 0; i < nodes.length; i++) {
            for (int j = i + 1; j < nodes.length; j++) {
                addEdge(nodes[i], nodes[j]);
            }
        }
    }

    public void computeStateVariables(){
        this.stateVars.push(new HashSet<IntExpression>(getUnfixedVariables()));
    }

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

    private void dfs(IntExpression node, Set<IntExpression> visited, Set<IntExpression> component) {
        visited.add(node);
        component.add(node);

        for (IntExpression neighbor : this.getUnfixedNeighbors(node)) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, visited, component);
            }
        }
    }

    public void printSubgraph(){
        List<Set<IntExpression>> subgraphs = findConnectedComponents();
        System.out.println(
                subgraphs.stream()
                        .map(set -> set.stream()
                                .map(obj -> String.valueOf(obj.getModelProxy()))
                                .collect(Collectors.joining(", ", "[", "]")))
                        .collect(Collectors.joining(", ", "[", "]"))
        );
    }

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
