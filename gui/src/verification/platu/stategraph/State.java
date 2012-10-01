package verification.platu.stategraph;

import java.io.*;
import java.util.*;

import lpn.parser.LhpnFile;
import lpn.parser.Transition;

import verification.platu.common.PlatuObj;
import verification.platu.lpn.DualHashMap;
import verification.platu.lpn.LPN;
import verification.platu.lpn.LpnTranList;
import verification.platu.lpn.VarSet;
import verification.timed_state_exploration.zone.TimedState;

/**
 * State
 * @author Administrator
 */
public class State extends PlatuObj {

    public static int[] counts = new int[15];
    
    protected int[] marking;
    protected int[] vector;
    protected boolean[] tranVector; // indicator vector to record whether each transition is enabled or not. 
    private int hashVal = 0;
    private LhpnFile lpn = null;
    private int index;
    private boolean localEnabledOnly;
    protected boolean failure = false;
    
 // The TimingState that extends this state with a zone. Null if untimed.
    //protected TimedState timeExtension;
    
    // A list of all the TimedStates that are time extensions of this state.
    private ArrayList<TimedState> timeExtensions;
    
    @Override
    public String toString() {
//        String ret=Arrays.toString(marking)+""+
//               Arrays.toString(vector);
//        return "["+ret.replace("[", "{").replace("]", "}")+"]";
    	return this.print();
    }

    public State(final LhpnFile lpn, int[] new_marking, int[] new_vector, boolean[] new_isTranEnabled) {
    	this.lpn = lpn;
        this.marking = new_marking;
        this.vector = new_vector;
        this.tranVector = new_isTranEnabled;

        if (marking == null || vector == null || tranVector == null) {
            new NullPointerException().printStackTrace();
        }
        
    	//Arrays.sort(this.marking);
    	this.index = 0;
        localEnabledOnly = false;
        counts[0]++;
    }

    public State(State other) {
        if (other == null) {
            new NullPointerException().printStackTrace();
        }
        
        this.lpn = other.lpn;        	
        this.marking = new int[other.marking.length];
        System.arraycopy(other.marking, 0, this.marking, 0, other.marking.length);

        this.vector = new int[other.vector.length];
        System.arraycopy(other.vector, 0, this.vector, 0, other.vector.length);
        
        this.tranVector = new boolean[other.tranVector.length];
        System.arraycopy(other.tranVector, 0, this.tranVector, 0, other.tranVector.length);

//        this.hashVal = other.hashVal;
        this.hashVal = 0;
        this.index = other.index;
        this.localEnabledOnly = other.localEnabledOnly;
        counts[0]++;
    }

    // TODO: (temp) Two Unused constructors, State() and State(Object otherState)
//    public State() {
//        this.marking = new int[0];
//        this.vector = new int[0];//EMPTY_VECTOR.clone();
//        this.hashVal = 0;
//        this.index = 0;
//        localEnabledOnly = false;
//        counts[0]++;
//    }
    //static PrintStream out = System.out;

//    public State(Object otherState) {
//        State other = (State) otherState;
//        if (other == null) {
//            new NullPointerException().printStackTrace();
//        }
//        
//        this.lpnModel = other.lpnModel;  	        
//        this.marking = new int[other.marking.length];
//        System.arraycopy(other.marking, 0, this.marking, 0, other.marking.length);
//
//       // this.vector = other.getVector().clone();
//        this.vector = new int[other.vector.length];
//        System.arraycopy(other.vector, 0, this.vector, 0, other.vector.length);
//        
//        this.hashVal = other.hashVal;
//        this.index = other.index;
//        this.localEnabledOnly = other.localEnabledOnly;
//        counts[0]++;
//    }
    
    public void setLpn(final LhpnFile thisLpn) {
    	this.lpn = thisLpn;
    }
    
    public LhpnFile getLpn() {
    	return this.lpn;
    }
    
    public void setLabel(String lbl) {
    	
    }
    
    public String getLabel() {
    	return null;
    }
    
    /**
     * This method returns the boolean array representing the status (enabled/disabled) of each transition in an LPN.
     * @return
     */
    public boolean[] getTranVector() {   	
    	return tranVector;
    }
    
    public void setIndex(int newIndex) {
    	this.index = newIndex;
    }
    
    public int getIndex() {
    	return this.index;
    }
    
    public boolean hasNonLocalEnabled() {
    	return this.localEnabledOnly;
    }
    
    public void hasNonLocalEnabled(boolean nonLocalEnabled) {
    	this.localEnabledOnly = nonLocalEnabled;
    }

    public boolean isFailure() {
        return false;// getType() != getType().NORMAL || getType() !=
        // getType().TERMINAL;
    }

    public static long tSum = 0;

    @Override
    public State clone() {
        counts[6]++;
        State s = new State(this);
        return s;
    }

    public String print() {
    	DualHashMap<String, Integer> VarIndexMap = this.lpn.getVarIndexMap();
    	String message = "Marking: [";
        for (int i : marking) {
            message += i + ",";
        }
        message += "]\n" + "Vector: [";
        for (int i = 0; i < vector.length; i++) {
            message += VarIndexMap.getKey(i) + "=>" + vector[i]+", ";
        }
        message += "]\n" + "Transition Vector: [";
        for (int i = 0; i < tranVector.length; i++) {
        	message += tranVector[i] + ",";
        }
        message += "]\n";
        return message;
    }

	@Override
	public int hashCode() {
		if(hashVal == 0){
			final int prime = 31;
			int result = 1;
			result = prime * result + ((lpn == null) ? 0 : lpn.getLabel().hashCode());
			result = prime * result + Arrays.hashCode(marking);
			result = prime * result + Arrays.hashCode(vector);
			result = prime * result + Arrays.hashCode(tranVector);
			hashVal = result;
		}
		
		return hashVal;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		State other = (State) obj;
		if (lpn == null) {
			if (other.lpn != null)
				return false;
		} 
		else if (!lpn.equals(other.lpn))
			return false;
		
		if (!Arrays.equals(marking, other.marking))
			return false;
		
		if (!Arrays.equals(vector, other.vector))
			return false;
		
		if (!Arrays.equals(tranVector, other.tranVector))
			return false;
		
		return true;
	}

	public void print(DualHashMap<String, Integer> VarIndexMap) {
        System.out.print("Marking: [");
        for (int i : marking) {
            System.out.print(i + ",");
        }
        System.out.println("]");
        
        System.out.print("Vector: [");
        for (int i = 0; i < vector.length; i++) {
            System.out.print(VarIndexMap.getKey(i) + "=>" + vector[i]+", ");
        }
        System.out.println("]");
        
        System.out.print("Transition vector: [");
        for (boolean bool : tranVector) {
            System.out.print(bool + ",");
        }
        System.out.println("]");
    }
    
    /**
     * @return the marking
     */
    public int[] getMarking() {
        return marking;
    }

    public void setMarking(int[] newMarking) {
        marking = newMarking;
    }

    /**
     * @return the vector
     */
    public int[] getVector() {
        // new Exception("StateVector getVector(): "+s).printStackTrace();
        return vector;
    }

    public HashMap<String, Integer> getOutVector(VarSet outputs, DualHashMap<String, Integer> VarIndexMap) {
    	HashMap<String, Integer> outVec = new HashMap<String, Integer>();
    	for(int i = 0; i < vector.length; i++) {
    		String var = VarIndexMap.getKey(i);
    		if(outputs.contains(var) == true)
    			outVec.put(var, vector[i]);
    	}
    	
    	return outVec;
    }

    public State getLocalState() {
    	//VarSet lpnOutputs = this.lpnModel.getOutputs();
    	//VarSet lpnInternals = this.lpnModel.getInternals();
    	Set<String> lpnOutputs = this.lpn.getAllOutputs().keySet();
    	Set<String> lpnInternals = this.lpn.getAllInternals().keySet();
    	DualHashMap<String,Integer> varIndexMap = this.lpn.getVarIndexMap();
    	 
    	int[] outVec = new int[this.vector.length];
    	
    	/*
    	 * Create a copy of the vector of mState such that the values of inputs are set to 0
    	 * and the values for outputs/internal variables remain the same.
    	 */
    	for(int i = 0; i < this.vector.length; i++) {
    		String curVar = varIndexMap.getKey(i);
    		if(lpnOutputs.contains(curVar) ==true || lpnInternals.contains(curVar)==true)
    			outVec[i] = this.vector[i];
    		else
    			outVec[i] = 0;
    	}
    	// TODO: (??) Need to create outTranVector as well?
    	return new State(this.lpn, this.marking, outVec, this.tranVector);
    }
    
    /**
     * @return the enabledSet
     */
    public int[] getEnabledSet() {
        return null;// enabledSet;
    }
    
    public LpnTranList getEnabledTransitions() {
       LpnTranList enabledTrans = new LpnTranList();
       for (int i=0; i<tranVector.length; i++) {
    	   if (tranVector[i]) {
    		   enabledTrans.add(this.lpn.getTransition(i));
    	   }
       }
       return enabledTrans;
    }
    
    public String getEnabledSetString() {
        String ret = "";
        // for (int i : enabledSet) {
        // ret += i + ", ";
        // }

        return ret;
    }

    /**
     * Return a new state if the newVector leads to a new state from this state; otherwise return null.
     * @param newVector
     * @param VarIndexMap
     * @return
     */
    public State update(StateGraph SG,HashMap<String, Integer> newVector, DualHashMap<String, Integer> VarIndexMap) {
    	int[] newStateVector = new int[this.vector.length];
    	
    	boolean newState = false;
    	for(int index = 0; index < vector.length; index++) {
    		String var = VarIndexMap.getKey(index);
    		int this_val = this.vector[index];
    		
			Integer newVal = newVector.get(var);
    		if(newVal != null) {
    			if(this_val != newVal) {
    				newState = true;
    				newStateVector[index] = newVal;
    			}
    			else
    				newStateVector[index] = this.vector[index]; 
    		}
    		else
    			newStateVector[index] = this.vector[index];    		
    	}
        boolean[] newEnabledTranVector = SG.updateEnabledTranVector(this.getTranVector(), this.marking, newStateVector, null);
    	if(newState == true)
    		return new State(this.lpn, this.marking, newStateVector, newEnabledTranVector);
    	
    	return null;
    }
    
    /**
     * Return a new state if the newVector leads to a new state from this state; otherwise return null.
     * States considered here include a vector indicating enabled/disabled state of each transition. 
     * @param newVector
     * @param VarIndexMap
     * @return
     */
    public State update(HashMap<String, Integer> newVector, DualHashMap<String, Integer> VarIndexMap, 
    		boolean[] newTranVector) {
    	int[] newStateVector = new int[this.vector.length];   	
    	boolean newState = false;
    	for(int index = 0; index < vector.length; index++) {
    		String var = VarIndexMap.getKey(index);
    		int this_val = this.vector[index];
			Integer newVal = newVector.get(var);
    		if(newVal != null) {
    			if(this_val != newVal) {
    				newState = true;
    				newStateVector[index] = newVal;
    			}
    			else
    				newStateVector[index] = this.vector[index]; 
    		}
    		else
    			newStateVector[index] = this.vector[index];    		
    	}
    	if (!this.tranVector.equals(newTranVector))
    		newState = true;
    	
    	if(newState == true)
    		return new State(this.lpn, this.marking, newStateVector, newTranVector);

    	return null;
    }
    
    static public void printUsageStats() {
        System.out.printf("%-20s %11s\n", "State", counts[0]);
        System.out.printf("\t%-20s %11s\n", "State", counts[10]);
        // System.out.printf("\t%-20s %11s\n", "State", counts[11]);
        // System.out.printf("\t%-20s %11s\n", "merge", counts[1]);
        System.out.printf("\t%-20s %11s\n", "update", counts[2]);
        // System.out.printf("\t%-20s %11s\n", "compose", counts[3]);
        System.out.printf("\t%-20s %11s\n", "equals", counts[4]);
        // System.out.printf("\t%-20s %11s\n", "conjunction", counts[5]);
        System.out.printf("\t%-20s %11s\n", "clone", counts[6]);
        System.out.printf("\t%-20s %11s\n", "hashCode", counts[7]);
        // System.out.printf("\t%-20s %11s\n", "resembles", counts[8]);
        // System.out.printf("\t%-20s %11s\n", "digest", counts[9]);
    }
//TODO: (original) try database serialization
    public File serialize(String filename) throws FileNotFoundException,
            IOException {
        File f = new File(filename);
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
        os.writeObject(this);

        os.close();
        return f;
    }

    public static State deserialize(String filename)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        File f = new File(filename);
        ObjectInputStream os = new ObjectInputStream(new FileInputStream(f));
        State zone = (State) os.readObject();
        os.close();
        return zone;
    }

    public static State deserialize(File f) throws FileNotFoundException,
            IOException, ClassNotFoundException {
        ObjectInputStream os = new ObjectInputStream(new FileInputStream(f));
        State zone = (State) os.readObject();
        os.close();
        return zone;
    }
    
    public boolean failure(){
    	return this.failure;
    }
    
    public void setFailure(){
    	this.failure = true;
    }

	public void print(LhpnFile lpn) {
		System.out.print("Marking: [");
//        for (int i : marking) {
//            System.out.print(i + ",");
//        }
        for (int i=0; i < marking.length; i++) {
            System.out.print(lpn.getPlaceList().clone()[i] + "=" + marking[i] + ", ");
        }
        System.out.println("]");
        
        System.out.print("Vector: [");
        for (int i = 0; i < vector.length; i++) {
            System.out.print(lpn.getVarIndexMap().getKey(i) + "=>" + vector[i]+", ");
        }
        System.out.println("]");
        
        System.out.print("Transition vector: [");
        for (boolean bool : tranVector) {
            System.out.print(bool + ",");
        }
        System.out.println("]");
	}
	
	/**
	 * Getter for the TimingState that extends this state.
	 * @return
	 * 		The TimingState that extends this state if it has been set. Null, otherwise.
	 */
	public ArrayList<TimedState> getTimeExtension(){
		return timeExtensions;
	}
	
	/**
	 * Setter for the TimingState that extends this state.
	 * @param s
	 * 		The TimingState that extends this state.
	 */
	public void setTimeExtension(ArrayList<TimedState> s){
		timeExtensions = s;
	}
	
	public void addTimeExtension(TimedState s){
		if(timeExtensions == null){
			timeExtensions = new ArrayList<TimedState>();
		}
		
		timeExtensions.add(s);
	}
	
	/**
	 * Get the current value of the variable according to the state.
	 * @param variable
	 * 		The variable of interest.
	 * @return
	 * 		The value of the variable as stored in the state.
	 */
	public int getCurrentValue(String variable){
		
		// Get the index of the variable according to the LPN.
		int index = lpn.getVarIndexMap().getValue(variable);
		
		return getCurrentValue(index);
	}
	
	/**
	 * Get the current value of the variable according to the state.
	 * @param variableIndex
	 * 		The index of the variable of interest.
	 * @return
	 * 		The value of the variable as stored in the state.
	 */
	public int getCurrentValue(int variableIndex){
		return vector[variableIndex];
	}
}
