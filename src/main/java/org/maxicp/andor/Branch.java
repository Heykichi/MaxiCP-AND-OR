package org.maxicp.andor;

import org.maxicp.modeling.algebra.integer.IntExpression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * The Branch class is a structure that represents a part of the search tree and manages a set of variables and their associated subbranches.
 * It provides constructors to initializing the branch with different combinations of variables and subbranches.
 * This class is primarily used to organize and manipulate branches of search trees within a constraint-solving context.
 */
public class Branch {

    private Set<IntExpression> variables = null;
    private List<SubBranch> subBranches = null;

    public Branch(Set<IntExpression> variables, List<SubBranch> subBranches) {
        this.variables = variables;
        this.subBranches = subBranches;
    }

    public Branch(IntExpression[] varList, List<SubBranch> subBranches) {
        this.variables = new HashSet<>(Arrays.asList(varList));
        this.subBranches = subBranches;
    }

    public Branch(Set<IntExpression> variables) {
        this.variables = variables;
    }

    public Branch(IntExpression[] varList) {
        this.variables = new HashSet<>(Arrays.asList(varList));
    }

    public Branch(List<SubBranch> subBranches) {
        this.subBranches = subBranches;
    }

    public void setBranches(List<SubBranch> subBranches) {this.subBranches = subBranches;}

    public void setVariables(Set<IntExpression> variables) {this.variables = variables;}

    public Set<IntExpression> getVariables() {return this.variables;}

    public List<SubBranch> getBranches() {return this.subBranches;}
}