package org.maxicp.andor;


import org.maxicp.modeling.algebra.integer.IntExpression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SubBranch {
    private Set<IntExpression> variables;
    private boolean toFix = false;

    public SubBranch(Set<IntExpression> variables, boolean toFix) {
        this.variables = variables;
        this.toFix = toFix;
    }

    public SubBranch(IntExpression[] varList, boolean toFix) {
        this.variables = new HashSet<>(Arrays.asList(varList));
        this.toFix = toFix;
    }

    public SubBranch(Set<IntExpression> variables) {
        this.variables = variables;
    }

    public SubBranch(IntExpression[] varList) {
        this.variables = new HashSet<>(Arrays.asList(varList));
    }

    public Set<IntExpression> getVariables() {return this.variables;}

    public boolean getToFix() {return this.toFix;}
}