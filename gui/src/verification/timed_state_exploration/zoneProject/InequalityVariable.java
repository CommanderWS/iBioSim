package verification.timed_state_exploration.zoneProject;

import java.util.HashMap;
import java.util.Properties;

import verification.platu.stategraph.State;

import lpn.parser.ExprTree;
import lpn.parser.LhpnFile;
import lpn.parser.Variable;


/**
 * An InequalityVariable is a Boolean variable for an inequality expression involving 
 * continuous variables. It extends lpn.parser.Variable. 
 * @author Andrew N. Fisher
 *
 */
public class InequalityVariable extends Variable {

	
	/*
	 * Representation Invariant :
	 * 		To create a canonical name for the variable, the name should be the string
	 * 		representation of the defining ExprTree pre-appended by a '$' to help
	 * 		avoid the possibility of name collision. Thus the variable for "x>6" is
	 * 		given the name '$x>6'. 
	 */
	
	/* Holds the defining expression for a boolean value derived from an inequality. */
	private ExprTree _inequalityExprTree;
	
	/* The continuous variable that this InequalityVariable depends on.*/
	//String _variable;
	Variable _variable;
	
	
	/* The LhpnFile object that this InequalityVariable belongs to. */
	LhpnFile _lpn;
	
	
//	
//	Not needed anymore since the list of InequalityVariables is not dynamically
//	changing.
//	
//	/* 
//	 * Keeps track of the Transitions that currently reference this InequalityVariable.
//	 */
//	//HashSet<Transition> referencingTransitions;
//	int _referenceCount;
	
	/**
	 * 
	 * @param name
	 * @param type
	 * @param initCond
	 */
	public InequalityVariable(String name, String type, Properties initCond) {
		super(name, type, initCond);
		// TODO Auto-generated constructor stub
		throw new UnsupportedOperationException("This constructor needs to be verified"
				+ "for correctness for inherited class InequalityVariable.");
	}

	/**
	 * @param name
	 * @param type
	 * @param initValue
	 * @param port
	 */
	public InequalityVariable(String name, String type, String initValue,
			String port) {
		super(name, type, initValue, port);
		// TODO Auto-generated constructor stub
		throw new UnsupportedOperationException("This constructor needs to be verified"
				+ "for correctness for inherited class InequalityVariable.");
	}

	/**
	 * Creates an InequalityVariable with a given name, initial value and defining
	 * expression tree.
	 * @param name
	 * 		The name (or ID) of the variable.
	 * @param initValue
	 * 		The initial value of the variable. Note : Inequality variables are
	 * 		a type of boolean variable. So this should be "true" or "false".
	 * @param ET
	 * 		An expression tree that defines the boolean value. This tree should
	 * 		represent a relational operator.
	 */
	public InequalityVariable(String name, String initValue, ExprTree ET, LhpnFile lpn) {
		super(name, BOOLEAN, initValue);
		
		// Check if the name starts with a '$'. If not, yell.
		if(!name.startsWith("$")){
			throw new IllegalArgumentException("InequaltiyVariables' name"
					+ "must start with '$'");
		}
		
		// Declare the new boolean variable an internal signal.
		setPort(INTERNAL);
		
		// Set the defining expression.
		_inequalityExprTree = ET;
		
		// Set the containing LPN.
		_lpn = lpn;
		
		// Extract the variable.
		String contVariableName = "";
		
		if(ET.getLeftChild().containsCont()){
			contVariableName = ET.getLeftChild().toString();
		}
		else{
			contVariableName = ET.getRightChild().toString();
		}
		
		_variable = lpn.getVariable(contVariableName);
//		
//		
//		Reference counts are not needed anymore since the set of 
//		Boolean variables is not dynamically changing.
//		
//		// When created, an expression refers to this variable.
//		_referenceCount = 1;
	}
	
	

	/**
	 * @param name
	 * @param type
	 */
	public InequalityVariable(String name, String type) {
		super(name, type);
		// TODO Auto-generated constructor stub
		throw new UnsupportedOperationException("This constructor needs to be verified"
				+ "for correctness for inherited class InequalityVariable.");
	}
	
	/**
	 * Overrides the toString method of Variable. Removes the pre-appended
	 * '$' of the InequalityVariable name.
	 */
	public String toString(){
		return "Inequality Variable : " + getName().substring(1);
	}
	
//	
//	This is no longer needed since variables will not dynamically change.
//	
//	/**
//	 * Increase the count of how many expressions refer to this InequalityVariable.
//	 */
//	public void increaseCount(){
//		_referenceCount++;
//	}
//	
//	/**
//	 * Decreases the count of how many expressions refer to this InequalityVariable.
//	 */
//	public void decreaseCount(){
//		_referenceCount--;
//	}
//
//	/**
//	 * Returns the count of the number of expressions referring to this
//	 * InequalityVariable.
//	 * @return
//	 * 		The count recorded for how many expressions refer to this InequalityVariable.
//	 */
//	public int getCount(){
//		return _referenceCount;
//	}
	
	/**
	 * Returns false. InequalityVaribles are dealt with separately.
	 */
	public boolean isInput() {
		return false;
	}
	
	/**
	 * Returns false. InequalityVaribles are dealt with separately.
	 */
	public boolean isOutput() {
		return false;
	}

	/**
	 * Returns false. InequalityVaribles are dealt with separately.
	 */
	public boolean isInternal() { 
		return false;
	}
	
	/**
	 * Evaluates the inequality according to the current state and zone.
	 * @param localState
	 * 		The current state.
	 * @param z
	 * 		The zone containing the value of the continuous variable.
	 */
	//public void update(Zone z){
	public String evaluateInequality(State localState, Zone z){
		
		// TODO : This method ignores the case where the range of the continuous variable
		// stradles the bound.
		
		//
		String result = "";
		
		/*
		 *  Extract the current values of the (Boolean and Integer) variables to be able
		 *  to obtain the value of the expression side of the inequality. This
		 *  may need to be changed when the bound evaluator is created and ranges are
		 *  allowed for the Integer variables.
		 */
		HashMap<String, String> variableValues = _lpn.getAllVarsWithValuesAsString(localState.getVector());
		
		// Determine which side of the expression tree has the continuous variable.
		if(_inequalityExprTree.getLeftChild().containsVar(_variable.getName())){
			// Extract the value of the expression side of the inequality.
			int expressionValue = (int) _inequalityExprTree.getRightChild().evaluateExpr(variableValues);
			
			// Determine which type of inequality.
			String op = _inequalityExprTree.getOp();
			
			if(op.equals("<") || op.equals("<=")){
//				if(z.getUpperBoundbyContinuousVariable(_variable) <= expressionValue){
//					this.initValue = "true";
//				}
//				else
//				{
//					this.initValue = "false";
//				}

//				this.initValue = z.getUpperBoundbyContinuousVariable(_variable) <= expressionValue
//						? "true" : "false"; 
				
				result = z.getUpperBoundbyContinuousVariable(_variable) <= expressionValue
						? "true" : "false";
			}
			else{
//				this.initValue = z.getLowerBoundbyContinuousVariable(_variable) >= expressionValue
//						? "true" : "false";
				
				result = z.getLowerBoundbyContinuousVariable(_variable) >= expressionValue
						? "true" : "false";
			}
			
		}
		else{
			// Extract the value of the expression side of the inequality.
			int expressionValue = (int) _inequalityExprTree.getLeftChild().evaluateExpr(variableValues);
			
			// Determine which type of inequality.

			String op = _inequalityExprTree.getOp();
			
			if(op.equals("<") || op.equals("<=")){
//				if(expressionValue <= z.getLowerBoundbyContinuousVariable(_variable)){
//					this.initValue = "true";
//				}
//				else
//				{
//					this.initValue = "false";
//				}
				
//				this.initValue = expressionValue <= z.getLowerBoundbyContinuousVariable(_variable)
//						? "true" : "false"; 
				
				result = expressionValue <= z.getLowerBoundbyContinuousVariable(_variable)
						? "true" : "false"; 
			}
			else{
//				this.initValue = expressionValue >= z.getUpperBoundbyContinuousVariable(_variable)
//						? "true" : "false";
				
				result = expressionValue >= z.getUpperBoundbyContinuousVariable(_variable)
						? "true" : "false";
			}
			
		}
		
		return result;
		
	}
	
//	/**
//	 * Finds which child node of the defining ExprTree that contains the
//	 * continuous variable.
//	 * @return
//	 * 		The ExrTree node of the defining ExprTree containing the continuous
//	 * 		variable.
//	 */
//	private ExprTree findContinuous(){
//		
//		
//		if(_inequalityExprTree.getLeftChild().containsVar(_variable.getName())){
//			
//		}
//			
//		return null;
//	}
}