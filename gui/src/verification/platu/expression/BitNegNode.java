package verification.platu.expression;

import java.util.HashMap;
import java.util.HashSet;

public class BitNegNode implements ExpressionNode {
	ExpressionNode RightOperand = null;
	
	public BitNegNode(ExpressionNode rightOperand){
		this.RightOperand = rightOperand;
	}
	
	public int evaluate(int[] stateVector){
		return ~RightOperand.evaluate(stateVector);
	}
	
	public void getVariables(HashSet<VarNode> variables){
		RightOperand.getVariables(variables);
	}
	
	@Override
	public String toString(){
		return "~" + RightOperand.toString();
	}
	
	public ExpressionNode copy(HashMap<String, VarNode> variables){
		return new BitNegNode(this.RightOperand.copy(variables));
	}
}