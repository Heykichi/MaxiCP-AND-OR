/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.search;

import org.maxicp.andor.SlicedTable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Statistics collected during the
 * execution of
 * {@link DFSearch#solve()} and
 * {@link DFSearch#optimize(Objective)}
 */
public class SearchStatistics {

    private int nFailures = 0;
    private int nNodes = 0;
    private int nSolutions = 0;
    private boolean completed = false;
    private int nAndNodes = 0;
    private List<Map<Integer, Integer>> solutions = null;
    private List<SlicedTable> slicedTables = null;

    public String toString() {
        return "\n\t#choice: " + nNodes
                + "\n\t#fail: " + nFailures
                + "\n\t#sols : " + nSolutions
                + "\n\t#And nodes : " + nAndNodes
                + "\n\tcompleted : " + completed + "\n";
    }

    public void incrFailures() {
        nFailures++;
    }

    public void incrNodes() {
        nNodes++;
    }

    public void incrSolutions() {
        nSolutions++;
    }

    public void incrSolutions(int n) {
        nSolutions += n;
    }

    public void incrAndNodes() {
        nAndNodes ++;
    }

    public void setCompleted() {
        completed = true;
    }

    public int numberOfFailures() {
        return nFailures;
    }

    public int numberOfNodes() {
        return nNodes;
    }

    public int numberOfSolutions() {
        return nSolutions;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setSolutions(List<Map<Integer, Integer>> solutions) {
        this.solutions = solutions;
    }

    public void setSlicedTables(List<SlicedTable> slicedTables) {
        this.slicedTables = slicedTables;
    }

    public List<Map<Integer, Integer>> getSolutions() {
        return solutions;}

    public List<SlicedTable> getSlicedTables() {return
            slicedTables;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchStatistics that = (SearchStatistics) o;
        return nFailures == that.nFailures && nNodes == that.nNodes && nSolutions == that.nSolutions && completed == that.completed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nFailures, nNodes, nSolutions, completed);
    }
}
