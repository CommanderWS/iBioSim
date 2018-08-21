/*******************************************************************************
 *
 * This file is part of iBioSim. Please visit <http://www.async.ece.utah.edu/ibiosim>
 * for the latest version of iBioSim.
 *
 * Copyright (C) 2017 University of Utah
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the Apache License. A copy of the license agreement is provided
 * in the file named "LICENSE.txt" included with this software distribution
 * and also available online at <http://www.async.ece.utah.edu/ibiosim/License>.
 *
 *******************************************************************************/
package edu.utah.ece.async.ibiosim.analysis.simulation.hierarchical.math;

import java.util.List;

import edu.utah.ece.async.ibiosim.analysis.simulation.hierarchical.states.HierarchicalState;

/**
 * A node that represents SBML Initial assignments and SBML Assignment Rules.
 *
 * @author Leandro Watanabe
 * @author Chris Myers
 * @author <a href="http://www.async.ece.utah.edu/ibiosim#Credits"> iBioSim Contributors </a>
 * @version %I%
 */
public class FunctionNode extends HierarchicalNode {

  private final HierarchicalNode variable;
  private final HierarchicalNode functionMath;
  private boolean isInitialAssignment;
  private List<HierarchicalNode> variableIndices;

  public FunctionNode(HierarchicalNode variable, HierarchicalNode math) {
    super(Type.FUNCTION);
    this.functionMath = math;
    this.variable = variable;
  }

  public FunctionNode(FunctionNode math) {
    super(Type.FUNCTION);
    this.variable = math.variable;
    this.functionMath = math.functionMath;
  }

  /**
   * Gets the variable node associated with this function.
   *
   * @return the variable.
   */
  public HierarchicalNode getVariable() {
    return variable;
  }

  /**
   * Sets a flag to indicate whether this function is an initial assignment.
   *
   * @param initAssign
   *          - whether this is an initial assignment.
   */
  public void setIsInitAssignment(boolean initAssign) {
    this.isInitialAssignment = initAssign;
  }

  /**
   * Checks if this function is an initial assignment.
   *
   * @return true if this function is an initial assignment and false otherwise.
   */
  public boolean isInitAssignment() {
    return this.isInitialAssignment;
  }

  /**
   * Evaluates the node and updates the corresponding variable.
   *
   * @param index
   *          - the model index.
   * @return true if the value has changed. False otherwise.
   */
  public boolean updateVariable(int index) {
    boolean changed = false;

    if (variable != null) {
      if (!(this.isInitialAssignment && variable.getState().getChild(index).hasRule()) && !isDeleted(index)) {
        double newValue = Evaluator.evaluateExpressionRecursive(functionMath, index);
        changed = variable.setValue(index, newValue);
      }
    }

    return changed;
  }

  /**
   *
   * @param index
   * @return
   */
  public boolean updateRate(int index) {
    double rate = 0;
    boolean changed = false;
    if (variable != null) {
      if (!isDeleted(index)) {
        HierarchicalState variableState = variable.getState().getChild(index);
        rate = Evaluator.evaluateExpressionRecursive(functionMath, index);
        if (!variableState.hasOnlySubstance()) {
          HierarchicalNode compartment = variable.getCompartment();
          double c = compartment.getValue(index);
          rate = rate * c;
          double compartmentChange = compartment.getState().getChild(index).getRateValue();
          if (compartmentChange != 0) {
            rate = rate + variableState.getValue() * compartmentChange / c;
          }
        }
        changed = variable.setRate(index, rate);
      }
    }
    return changed;
  }

  /**
   *
   * @param index
   * @return
   */
  public double computeFunction(int index) {
    return Evaluator.evaluateExpressionRecursive(functionMath, index);
  }

  /**
   * Gets the math associated with this function.
   *
   * @return the right-hand side of this function.
   */
  public HierarchicalNode getMath() {
    return functionMath;
  }

}
