package verification.timed_state_exploration.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import lpn.parser.ExprTree;
import lpn.parser.LhpnFile;
import lpn.parser.Transition;
import lpn.parser.Variable;

import com.sun.corba.se.spi.activation._InitialNameServiceImplBase;

import verification.platu.lpn.DualHashMap;
import verification.platu.stategraph.State;


/**
 * This class is for storing and manipulating timing zones via difference bound matrices.
 * The underlying structure is backed by linked lists which yields the name
 * DBMLL (difference bound matrix linked list). A difference bound matrix has the form
 *     t0  t1  t2  t3
 * t0 m00 m01 m02 m03
 * t1 m10 m11 m12 m13
 * t2 m20 m21 m22 m23
 * t3 m30 m31 m32 m33
 * where  tj - ti<= mij. In particular, m0k is an upper bound for tk and -mk is a lower
 * bound for tk.
 * 
 * The timers are referred to by an index. 
 * 
 * This class also contains a public nested class DiagonalNonZeroException which extends
 * java.lang.RuntimeException. This exception may be thrown if the diagonal entries of a 
 * zone become nonzero.
 * 
 * @author Andrew N. Fisher
 *
 */
public class Zone {
	
	// Abstraction Function : 
	// The difference bound matrix is represented by int[][].
	// In order to keep track of the upper and lower bounds of timers from when they are first
	// enabled, the matrix will be augmented by a row and a column. The first row will contain
	// the upper bounds and the first column will contain the negative of the lower bounds.
	// For one timer t1 that is between 2 and 3, we might have
	//    lb t0 t1
	// ub  x  0  3
	// t0  0  m  m
	// t1  -2 m  m
	// where x is not important (and will be given a zero value), 3 is the upper bound on t1
	// and -2 is the negative of the lower bound. The m values represent the actual difference
	// bound matrix. Also note that the column heading are not part of the stored representation
	// lb stands for lower bound while ub stands for upper bound.
	// This upper and lower bound information is called the Delay for a Transition object.
	// Since a timer is tied directly to a Transition, the timers are index by the corresponding
	// Transition's index in a LPNTranslator.
	// The timers are named by an integer referred to as the index. The _indexToTimer array
	// connects the index in the DBM sub-matrix to the index of the timer. For example,
	// a the timer t1
	
	// Representation invariant :
	// Zones are immutable.
	// Integer.MAX_VALUE is used to logically represent infinity.
	// The lb and ub values for a timer should be set when the timer is enabled.
	// A negative hash code indicates that the hash code has not been set.
	// The index of the timer in _indexToTimer is the index in the DBM and should contain
	// 	the zeroth timer.
	// The array _indexToTimer should always be sorted.
	
	/* The lower and upper bounds of the times as well as the dbm. */
	private int[][] _matrix;
	
	/* Maps the index to the timer. The index is row/column of the DBM sub-matrix.
	 * Logically the zero timer is given index -1.
	 *  */
	private int[] _indexToTimer;		
	
	/* Infinity is represented by the maximum integer value. */
	public static final int INFINITY = Integer.MAX_VALUE;
	
	/* The hash code. */
	private int _hashCode;
	
	/* A lexicon between a transitions index and its name. */
	private static HashMap<Integer, Transition> _indexToTransition;
	
	/**
	 * Construct a zone that has the given timers.
	 * @param timers 
	 * 				The ith index of the array is the index of the timer. For example,
	 * 					if timers = [1, 3, 5], then the zeroth row/column of the DBM is the
	 * 					timer of the transition with index 1, the first row/column of the 
	 * 					DBM is the timer of the transition with index 3, and the 2nd 
	 * 					row/column is the timer of the transition with index 5. Do not
	 * 					include the zero timer.
	 * @param matrix 
	 * 				The DBM augmented with the lower and upper bounds of the delays for the
	 * 					transitions. For example, suppose a zone has timers [1, 3, 5] (as
	 * 					described in the timers parameters). The delay for transition 1 is
	 * 					[1, 3], the delay for transition 3 is [2,5], and the delay for
	 * 					transition 5 is [4,6]. Also suppose the DBM is
	 * 					    t0 t1 t3 t5
	 * 					t0 | 0, 3, 3, 3 |
	 * 					t1 | 0, 0, 0, 0 |
	 * 					t3 | 0, 0, 0, 0 |
	 * 					t5 | 0, 0, 0, 0 |
	 * 					Then the matrix that should be passed is
	 * 					   lb t0 t1 t3 t5
	 * 					ub| 0, 0, 3, 5, 6|
	 * 					t0| 0, 0, 3, 3, 3|
	 * 					t1|-1, 0, 0, 0, 0|
	 * 					t3|-2, 0, 0, 0, 0|
	 * 					t5|-4, 0, 0, 0, 0|
	 * 					The matrix should be non-null and the zero timer should always be the
	 * 					first timer, even when there are no other timers.
	 */
	public Zone(int[] timers, int[][] matrix)
	{
		// A negative number indicates that the hash code has not been set.
		_hashCode = -1;
		
		// Make a copy to reorder the timers.
		_indexToTimer = Arrays.copyOf(timers, timers.length);
		
		// Sorting the array.
		Arrays.sort(_indexToTimer);
		
		//if(_indexToTimer[0] != 0)
		if(_indexToTimer[0] != -1)
		{
			// Add the zeroth timer.
			int[] newIndexToTimer = new int[_indexToTimer.length+1];
			for(int i=0; i<_indexToTimer.length; i++)
			{
				newIndexToTimer[i+1] = _indexToTimer[i];
			}
			
			_indexToTimer = newIndexToTimer;
			_indexToTimer[0] = -1;
		}
		
//		if(_indexToTimer[0] < 0)
//		{
//			throw new IllegalArgumentException("Timers must be non negative.");
//		}
//		// Add a zero timer.
//		else if(_indexToTimer[0] > 0)
//		{
//			int[] newTimerIndex = new int[_indexToTimer.length+1];
//			for(int i=0; i<_indexToTimer.length; i++)
//			{
//				newTimerIndex[i+1] = _indexToTimer[i];
//			}
//		}
		
		// Map the old index of the timer to the new index of the timer.
		HashMap<Integer, Integer> newIndex = new HashMap<Integer, Integer>();
		
		// For the old index, find the new index.
		for(int i=0; i<timers.length; i++)
		{
			// Since the zeroth timer is not included in the timers passed
			// to the index in the DBM is 1 more than the index of the timer
			// in the timers array.
			newIndex.put(i+1, Arrays.binarySearch(_indexToTimer, timers[i]));
		}
		
		// Add the zero timer index.
		newIndex.put(0, 0);
		
		// Initialize the matrix.
		_matrix = new int[matrixSize()][matrixSize()];

		// Copy the DBM
		for(int i=0; i<dbmSize(); i++)
		{
			for(int j=0; j<dbmSize(); j++)
			{
				// Copy the passed in matrix to _matrix.
				setDBMIndex(newIndex.get(i), newIndex.get(j), 
						matrix[dbmIndexToMatrixIndex(i)][dbmIndexToMatrixIndex(j)]);
			}
		}
		
		// Copy in the upper and lower bounds. The zero time does not have an upper or lower bound
		// so the index starts at i=1, the first non-zero timer.
		for(int i=1; i< dbmSize(); i++)
		{
			setUpperBoundbydbmIndex(newIndex.get(i), matrix[0][dbmIndexToMatrixIndex(i)]);
			
			// Note : The method setLowerBoundbydbmIndex, takes the value of the lower bound
			// and the matrix stores the negative of the lower bound. So the matrix value
			// must be multiplied by -1.
			setLowerBoundbydbmIndex(newIndex.get(i), -1*matrix[dbmIndexToMatrixIndex(i)][0]);
		}
		
		recononicalize();
	}
	
	/**
	 * Initializes a zone according to the markings of state.
	 * @param currentState
	 * 			The zone is initialized as if all enabled timers 
	 * 			have just been enabled.
	 */
	public Zone(State initialState)
	{
		
		LhpnFile lpn = initialState.getLpn();
		
		Transition[] allTran = lpn.getAllTransitions();
		
		/* Create the lexicon if it has not already been created. */
		if(_indexToTransition == null)
		{
			_indexToTransition = new HashMap<Integer, Transition>();
			
			// Get the transitions.
			allTran = lpn.getAllTransitions();
			
			for(Transition T : allTran)
			{
				_indexToTransition.put(T.getIndex(), T);
			}
		}
		
		_hashCode = -1;
		
		//_indexToTimer = initialState.getTranVector();
		
		boolean[] enabledTran = initialState.getTranVector();
		
//		int count = 0;
//		
//		// Find the size of the enabled transitions.
//		for(int i=0; i<enabledTran.length; i++)
//		{
//			if(enabledTran[i])
//			{
//				count++;
//			}
//		}
//		
//		_indexToTimer = new int[count];
//		
//		count =0;
//		
//		// Initialize the _indexToTimer.
//		for(int i=0; i<enabledTran.length; i++)
//		{
//			if(enabledTran[i])
//			{
//				_indexToTimer[count++] = i; 
//			}
//		}
		
		// Associate the Transition with its index.
		//TreeMap<Integer, Transition> indexToTran = new TreeMap<Integer, Transition>();
		//_indexToTransition = new HashMap<Integer, Transition>();
		
		// Get out the Transitions to compare with the enableTran to determine which are
		// enabled.
		//Transition[] allTran = lpn.getAllTransitions();
		
		HashMap<Integer, Transition> enabledTranMap = new HashMap<Integer, Transition>();
		
		for(Transition T : allTran)
		{
			int index = T.getIndex();
			if(enabledTran[index])
			{
				//indexToTran.put(index, T);
				enabledTranMap.put(index, T);
			}
		}
		
		// Enabled timers plus the zero timer.
		//_indexToTimer = new int[indexToTran.size()+1];
		//_indexToTimer = new int[_indexToTransition.size()+1];
		_indexToTimer = new int[enabledTranMap.size()+1];
		_indexToTimer[0] = -1;
		
		// Load the indices starting at index 1, since the zero timer will
		// be index 0.
		int count =1;
		
		// Load the indices of the Transitions into _indexToTimer.
		//for(int i : indexToTran.keySet())
		//for(int i : _indexToTransition.keySet())
		for(int i : enabledTranMap.keySet())
		{
			_indexToTimer[count] = i;
			
			count++;
		}
		
		Arrays.sort(_indexToTimer);
		
		_matrix = new int[matrixSize()][matrixSize()];
		
		for(int i=1; i<dbmSize(); i++)
		{
			// Get the name for the timer in the i-th column/row of DBM
			//String tranName = indexToTran.get(_indexToTimer[i]).getName();
			String tranName = _indexToTransition.get(_indexToTimer[i]).getName();
			ExprTree delay = lpn.getDelayTree(tranName);
			
			// Get the values of the variables for evaluating the ExprTree.
			HashMap<String, String> varValues = 
				lpn.getAllVarsAndValues(initialState.getVector());
			
			// Set the upper and lower bound.
			int upper, lower;
			if(delay.getOp().equals("uniform"))
			{
				ExprTree lowerDelay = delay.getLeftChild();
				ExprTree upperDelay = delay.getRightChild();
				
				lower = (int) lowerDelay.evaluateExpr(varValues);
				upper = (int) upperDelay.evaluateExpr(varValues);
			}
			else
			{
				lower = (int) delay.evaluateExpr(varValues);
				
				upper = lower;
			}
			
			setLowerBoundbydbmIndex(i, lower);
			setUpperBoundbydbmIndex(i, upper);
		}
		
		// Advance the time and tighten the bounds.
		advance();
		recononicalize();
		
	}
	
	/**
	 * Zero argument constructor for use in methods that create Zones where the members
	 * variables will be set by the method.
	 */
	private Zone()
	{
		_matrix = new int[0][0];
		_indexToTimer = new int[0];
		_hashCode = -1;
	}
	
	/**
	 * Logically the DBM is the sub-matrix of _matrix obtained by removing the zeroth
	 * row and column. This method retrieves the (i,j) element of the DBM.
	 * @param i 
	 * 			The i-th row of the DBM.
	 * @param j 
	 * 			The j-th column of the DBM.
	 * @return 
	 * 			The (i,j) element of the DBM.
	 */
	public int getDBMIndex(int i, int j)
	{
		return _matrix[i+1][j+1];
	}
	
	/**
	 * Logically the DBM is the sub-matrix of _matrix obtained by removing the zeroth
	 * row and column. This method sets the (i,j) element of the DBM.
	 * @param i 
	 * 			The ith row of the DBM.
	 * @param j 
	 * 			The jth column of the DBM.
	 * @param value 
	 * 			The value of the matrix.
	 */
	private void setDBMIndex(int i, int j, int value)
	{
		_matrix[i+1][j+1] = value;
	}
	
	/**
	 * Get the value of the upper bound for the delay.
	 * @param timer
	 * 			The timer's index.
	 * @return
	 * 			The upper bound on the transitions delay.
	 */
	public int getUpperBoundbyTransitionIndex(int timer)
	{
		return getUpperBoundbydbmIndex(Arrays.binarySearch(_indexToTimer, timer));
	}
	
	/**
	 * Get the value of the upper bound for the delay.
	 * @param index
	 * 			The timer's row/column of the DBM matrix.
	 * @return
	 * 			The upper bound on the transitions delay.
	 */
	public int getUpperBoundbydbmIndex(int index)
	{
		return _matrix[0][dbmIndexToMatrixIndex(index)];
	}
	
	/**
	 * Set the value of the upper bound for the delay.
	 * @param timer
	 * 			The timer's index.
	 * @param value
	 * 			The value of the upper bound.
	 */
	private void setUpperBoundbyTransitionIndex(int timer, int value)
	{
		setUpperBoundbydbmIndex(Arrays.binarySearch(_indexToTimer, timer), value);
	}
	
	/**
	 * Set the value of the upper bound for the delay.
	 * @param index
	 * 			The timer's row/column of the DBM matrix.
	 * @param value
	 * 			The value of the upper bound.
	 */
	private void setUpperBoundbydbmIndex(int index, int value)
	{
		_matrix[0][dbmIndexToMatrixIndex(index)] = value;
	}
	
	/**
	 * Get the value of the lower bound for the delay.
	 * @param timer
	 * 			The timer's index.
	 * @return
	 * 			The value of the lower bound.
	 */
	public int getLowerBoundbyTransitionIndex(int timer)
	{
		return getLowerBoundbydbmIndex(Arrays.binarySearch(_indexToTimer, timer));
	}
	
	/**
	 * Get the value of the lower bound for the delay.
	 * @param index
	 * 			The timer's row/column of the DBM matrix.
	 * @return
	 * 			The value of the lower bound.
	 */
	public int getLowerBoundbydbmIndex(int index)
	{
		return _matrix[dbmIndexToMatrixIndex(index)][0];
	}
	
	/**
	 * Set the value of the lower bound for the delay.
	 * @param timer
	 * 			The timer's index.
	 * @param value
	 * 			The value of the lower bound.
	 */
	private void setLowerBoundbyTransitionIndex(int timer, int value)
	{
		setLowerBoundbydbmIndex(Arrays.binarySearch(_indexToTimer,timer), value);
	}
	
	/**
	 * Set the value of the lower bound for the delay.
	 * @param index
	 * 			The timer's row/column of the DBM matrix.
	 * @param value
	 * 			The value of the lower bound.
	 */
	private void setLowerBoundbydbmIndex(int index, int value)
	{
		_matrix[dbmIndexToMatrixIndex(index)][0] = -1*value;
	}
	
	/**
	 * Converts the index of the DBM to the index of _matrix.
	 * @param i
	 * 			The row/column index of the DBM.
	 * @return
	 * 			The row/column index of _matrix.
	 */
	private int dbmIndexToMatrixIndex(int i)
	{
		return i+1;
	}
	
	/**
	 * Retrieves an entry of the DBM using the DBM's addressing.
	 * @param i
	 * 			The row of the DBM.
	 * @param j
	 * 			The column of the DBM.
	 * @return
	 * 			The value of the (i, j) element of the DBM.
	 */
	public int getdbm(int i, int j)
	{
		return _matrix[dbmIndexToMatrixIndex(i)][dbmIndexToMatrixIndex(j)];
	}
	
	/**
	 * Sets an entry of the DBM using the DBM's adressing.
	 * @param i
	 * 			The row of the DBM.
	 * @param j
	 * 			The column of the DBM.
	 * @param value
	 * 			The new value for the entry.
	 */
	private void setdbm(int i, int j, int value)
	{
		_matrix[dbmIndexToMatrixIndex(i)][dbmIndexToMatrixIndex(j)] = value;
	}
	
	/**
	 * Converts the index of the timer from the _indexToTimer array to the index of _matrix.
	 * @param i
	 * 			The index of the timer from the _indexToTimer array.
	 * @return
	 * 			The index in the _matrix.
	 */
	private int timerIndexToMatrixIndex(int i)
	{
		//return i+2;
		//return i+1;
		return dbmIndexToMatrixIndex(Arrays.binarySearch(_indexToTimer, i));
	}
	
	/**
	 * Returns the index of the the transition in the DBM given the index of the 
	 * transition.
	 * @param i
	 * 		The transition's index.
	 * @return
	 * 		The row/column of the DBM associated with the i-th transition.
	 */
	private int timerIndexToDBMIndex(int i)
	{
		return Arrays.binarySearch(_indexToTimer, i);
	}
	
	/**
	 * Converts the index of _matrix to the index of the DBM.
	 * @param i
	 * 			The row/column index of _matrix.
	 * @return
	 * 			The row/column index of the DBM.
	 */
	private int matrixIndexTodbmIndex(int i)
	{
		return i-1;
	}
	
	/**
	 * The matrix labeled with 'ti' where i is the transition index associated with the timer.
	 */
	public String toString()
	{
		String result = "Timer and delay.\n";
		
		int count = 0;
		
		// Print the timers.
		for(int i=1; i<_indexToTimer.length; i++, count++)
		{
			if(_indexToTransition == null)
			{
				// If an index to transition map has not been set up,
				// use the transition index for the timer.
				result += " t" + _indexToTimer[i] + " : ";
			}
			else
			{
				result += " " +  _indexToTransition.get(_indexToTimer[i]) + ":";
			}
			result += "[ " + -1*getLowerBoundbydbmIndex(i) + ", " + getUpperBoundbydbmIndex(i) + " ]";
			
			if(count > 9)
			{
				result += "\n";
				count = 0;
			}
		}
		
		result += "\nDBM\n";
		
		//result += "|";
		
		// Print the DBM.
		// TODO: Fix this to print just the DBM.
		for(int i=0; i<_indexToTimer.length; i++)
		{
			//result += " " + _matrix[i][0];
			result += "| " + getdbm(i, 0);
			
			//for(int j=1; j<_indexToTimer.length; j++)
			for(int j=1; j<_indexToTimer.length; j++)
			{
				//result += ", " + _matrix[i][j];
				result += ", " + getdbm(i, j);
			}
			
			result += " |\n";
		}
		
		//result += "|";
		
		return result;
	}
	
	/**
	 * Tests for equality. Overrides inherited equals method.
	 * @return True if o is equal to this object, false otherwise.
	 */
	public boolean equals(Object o)
	{
		// Check if the reference is null.
		if(o == null)
		{
			return false;
		}
		
		// Check that the type is correct.
		if(!(o instanceof Zone))
		{
			return false;
		}
		
		// Check for equality using the Zone equality.
		return equals((Zone) o);
	}
	
	
	/**
	 * Tests for equality.
	 * @param
	 * 		The Zone to compare.
	 * @return 
	 * 		True if the zones are non-null and equal, false otherwise.
	 */
	public boolean equals(Zone otherZone)
	{
		// Check if the reference is null first.
		if(otherZone == null)
		{
			return false;
		}
		
		// Check for reference equality.
		if(this == otherZone)
		{
			return true;
		}
		
		// If the hash codes are different, then the objects are not equal. 
		if(this.hashCode() != otherZone.hashCode())
		{
			return false;
		}
		
		// Check if the timers are the same.
		if(!Arrays.equals(this._indexToTimer, otherZone._indexToTimer))
		{
			return false;
		}
		
		// Check if the matrix is the same.
		for(int i=0; i<_matrix.length; i++)
		{
			for(int j=0; j<_matrix[0].length; j++)
			{
				if(!(this._matrix[i][j] == otherZone._matrix[i][j]))
				{
					return false;
				}
			}
		}
		
		
		return true;
	}
	
	/**
	 * Overrides the hashCode.
	 */
	public int hashCode()
	{
		// Check if the hash code has been set.
		if(_hashCode <0)
		{
			_hashCode = createHashCode();
		}

		return _hashCode;
	}
	
	/**
	 * Creates a hash code for a Zone object.
	 * @return
	 * 		The hash code.
	 */
	private int createHashCode()
	{
		int newHashCode = Arrays.hashCode(_indexToTimer);
		
		for(int i=0; i<_matrix.length; i++)
		{
			newHashCode ^= Arrays.hashCode(_matrix[i]);
		}
		
		return Math.abs(newHashCode);
	}
	
	/**
	 * Checks if the array has square dimensions.
	 * @param array
	 * 			The array to test.
	 * @return
	 * 			True if the arrays is square, non-zero, and non-null, false otherwise.
	 */
	private boolean checkSquare(int[][] array)
	{
		boolean result = true; 
		
		if(array == null )
		{
			return false;
		}
		
		if(array.length == 0)
		{
			return true;
		}
		
		int dimension = array.length;
			
		for(int i=0; i<array.length; i++)
		{
			if(array[i] == null || array[i].length != dimension)
			{
				return false;
			}
		}
		
		return false;
	}
	
	/**
	 * The size of the DBM sub matrix. This is calculated using the size of _indexToTimer.
	 * @return
	 * 			The size of the DBM.
	 */
	private int dbmSize()
	{
		return _indexToTimer.length;
	}
	
	/**
	 * The size of the matrix.
	 * @return
	 * 			The size of the matrix. This is calculated using the size of _indexToTimer.
	 */
	private int matrixSize()
	{
		return _indexToTimer.length + 1;
	}
	
	/**
	 * Performs the Floyd's least pairs algorithm to reduce the DBM.
	 */
	private void recononicalize()
	{
		// TODO : Check if finished.
		
		for(int k=0; k<dbmSize(); k++)
		{
			for (int i=0; i<dbmSize(); i++)
			{
				for(int j=0; j<dbmSize(); j++)
				{
					if(getdbm(i, k) != INFINITY && getdbm(k, j) != INFINITY
							&& getdbm(i, j) > getdbm(i, k) + getdbm(k, j))
					{
						setdbm(i, j, getdbm(i, k) + getdbm(k, j));
					}
					
					if( (i==j) && getdbm(i, j) != 0)
					{
						throw new DiagonalNonZeroException("Entry (" + i + ", " + j + ")" +
								" became " + getdbm(i, j) + ".");
					}
				}
			}
		}
	}
	
	/**
	 * Determines if a timer has reached its lower bound.
	 * @param timer
	 * 			The timer's index.
	 * @return
	 * 			True if the timer has reached its lower bound, false otherwise.
	 */
	public boolean exceedsLowerBoundbyTransitionIndex(int timer)
	{
		// TODO : Check if finished.
		return exceedsLowerBoundbydbmIndex(Arrays.binarySearch(_indexToTimer, timer));
	}
	
	/**
	 * Determines if a timer has reached its lower bound.
	 * @param index
	 * 			The timer's row/column of the DBM.
	 * @return
	 * 			True if the timer has reached its lower bound, false otherwise.
	 */
	public boolean exceedsLowerBoundbydbmIndex(int index)
	{
		// TODO: Check if finished.
		
		// Note : Make sure that the lower bound is stored as a negative number
		// and that the inequality is correct.
		return _matrix[0][dbmIndexToMatrixIndex(index)] <=
			_matrix[1][dbmIndexToMatrixIndex(index)];
	}
	
	/**
	 * Updates the Zone according to a transition firing.
	 * @param timer
	 * 			The index of the transition that fired.
	 * @return
	 * 			The updated Zone.
	 */
	public Zone fireTransitionbyTransitionIndex(int timer, int[] enabledTimers, 
			State state)
	{
		// TODO: Check if finish.
		return fireTransitionbydbmIndex(Arrays.binarySearch(_indexToTimer, timer), 
				enabledTimers, state);
	}
	
	/**
	 * Updates the Zone according to the transition firing.
	 * @param index
	 * 			The index of the timer.
	 * @return
	 * 			The updated Zone.
	 */
	public Zone fireTransitionbydbmIndex(int index, int[] enabledTimers,
			State state)
	{
		// TODO: Finish
		Zone newZone = new Zone();
		
		
		// Copy over the enabledTimers adding a zero timer.
		//newZone._indexToTimer = enabledTimers;
		
		newZone._indexToTimer = new int[enabledTimers.length+1];
		
		// Put the _indexToTimer value for the zeroth timer to -1.
		// See the Abstraction Function section at the top of the
		// class for a discussion on why.
		newZone._indexToTimer[0] = -1;
		
		
		for(int i=0; i<enabledTimers.length; i++)
		{
			newZone._indexToTimer[i+1]=enabledTimers[i];
		}
		
		Arrays.sort(newZone._indexToTimer);
		
		
		HashSet<Integer> newTimers = new HashSet<Integer>();
		HashSet<Integer> oldTimers = new HashSet<Integer>();
		
		for(int i=0; i<newZone._indexToTimer.length; i++)
		{
			// Determine if each value is a new timer or old.
			if(Arrays.binarySearch(this._indexToTimer, newZone._indexToTimer[i])
					>= 0 )
			{
				// The timer was already present in the zone.
				oldTimers.add(newZone._indexToTimer[i]);
			}
			else
			{
				// The timer is a new timer.
				newTimers.add(newZone._indexToTimer[i]);
			}
		}
		
		// Create the new matrix.
		newZone._matrix = new int[newZone.matrixSize()][newZone.matrixSize()];
		
		// TODO: For simplicity, make a copy of the current zone and perform the
		// restriction and re-canonicalization. Later add a copy re-canonicalization
		// that does the steps together.
		
		Zone tempZone = this.clone();
		
		tempZone.restrict(index);
		tempZone.recononicalize();
		
		// Copy the tempZone to the new zone.
		for(int i=0; i<tempZone.dbmSize(); i++)
		{
			if(!oldTimers.contains(tempZone._indexToTimer[i]))
			{
//				break;
				continue;
			}
			// Get the new index of for the timer.
			int newIndexi = i==0 ? 0 : 
				Arrays.binarySearch(newZone._indexToTimer, tempZone._indexToTimer[i]);
			for(int j=0; j<tempZone.dbmSize(); j++)
			{
				if(!oldTimers.contains(tempZone._indexToTimer[j]))
				{
//					break;
					continue;
				}
				int newIndexj = j==0 ? 0 : 
					Arrays.binarySearch(newZone._indexToTimer, tempZone._indexToTimer[i]);
				
				newZone._matrix[newZone.dbmIndexToMatrixIndex(newIndexi)][newZone.dbmIndexToMatrixIndex(newIndexj)]
				                                                            = tempZone.getDBMIndex(i, j);
			}
		}
		
		// Copy the upper and lower bounds.
		//for(int i=0; i<tempZone.dbmSize(); i++)
		for(int i=1; i<tempZone.dbmSize(); i++)
		{
			if(!oldTimers.contains(tempZone._indexToTimer[i]))
			{
//				break;
				continue;
			}
			newZone.setLowerBoundbyTransitionIndex(tempZone._indexToTimer[i], 
					tempZone.getLowerBoundbydbmIndex(i));
			newZone.setUpperBoundbyTransitionIndex(tempZone._indexToTimer[i],
					tempZone.getUpperBoundbydbmIndex(i));
		}
		
		// Copy in the new relations for the new timers.
		for(int timerNew : newTimers)
		{
			for(int timerOld : oldTimers)
			{
//				setdbm(newZone.timerIndexToMatrixIndex(timerNew),
//						newZone.timerIndexToMatrixIndex(timerOld),
//						tempZone.getdbm(0, timerOld));
//				
//				setdbm(newZone.timerIndexToMatrixIndex(timerOld),
//						newZone.timerIndexToMatrixIndex(timerNew),
//						tempZone.getdbm(timerOld, 0));
				
				newZone.setdbm(newZone.timerIndexToDBMIndex(timerNew),
						newZone.timerIndexToDBMIndex(timerOld),
						 tempZone.getdbm(0, tempZone.timerIndexToDBMIndex(timerOld)));
				
				newZone.setdbm(newZone.timerIndexToDBMIndex(timerOld),
						newZone.timerIndexToDBMIndex(timerNew),
						tempZone.getdbm(tempZone.timerIndexToDBMIndex(timerOld), 0));
			}
		}
		
		// Set the upper and lower bounds for the new timers.
		
		LhpnFile lpn = state.getLpn();
		
		// Associate the Transition with its index.
//		TreeMap<Integer, Transition> indexToTran = new TreeMap<Integer, Transition>();
//		
//		// Get out the Transitions to compare with the enableTran to determine which are
//		// enabled.
//		Transition[] allTran = lpn.getAllTransitions();
//		
//		for(Transition T : allTran)
//		{
//			int t = T.getIndex();
//			if(newTimers.contains(t))
//			{
//				indexToTran.put(t, T);
//			}
//		}
		
		for(int i : newTimers){

			// Get all the upper and lower bounds for the new timers.
			// Get the name for the timer in the i-th column/row of DBM
			//String tranName = indexToTran.get(i).getName();
			String tranName = _indexToTransition.get(i).getName();
			ExprTree delay = lpn.getDelayTree(tranName);

			// Get the values of the variables for evaluating the ExprTree.
			HashMap<String, String> varValues = 
				lpn.getAllVarsAndValues(state.getVector());

			// Set the upper and lower bound.
			int upper, lower;
			if(delay.getOp().equals("uniform"))
			{
				ExprTree lowerDelay = delay.getLeftChild();
				ExprTree upperDelay = delay.getRightChild();

				lower = (int) lowerDelay.evaluateExpr(varValues);
				upper = (int) upperDelay.evaluateExpr(varValues);
			}
			else
			{
				lower = (int) delay.evaluateExpr(varValues);

				upper = lower;
			}

			newZone.setLowerBoundbyTransitionIndex(i, lower);
			newZone.setUpperBoundbyTransitionIndex(i, upper);

		}
		
		newZone.advance();
		newZone.recononicalize();
		
		// If the splitting method fails, print an error message.
		if(!testSplit(newZone))
		{
			System.err.print("The reduction does not work. In state" + state
					+ "firing transition: " + index);
		}
		
		return newZone;
	}
	
	/**
	 * Merges this Zone with another Zone.
	 * @param otherZone
	 * 			The zone to merge with this Zone.
	 * @return
	 * 			The merged Zone.
	 */
	public Zone mergeZones(Zone otherZone)
	{
		// TODO: Finish.	
		
		Zone mergedZone = new Zone();
		
		//mergedZone._indexToTimer = mergeTimers(this._indexToTimer, otherZone._indexToTimer);
		
		/* Maps the index of this Zone's timers to the mergedZone. */
		//HashMap<Integer, Integer> thisNewIndex;
		
		//thisNewIndex = makeIndexMap(this._indexToTimer, mergedZone._indexToTimer);
		
		/* Maps the index of otherZone Zone's timers to the mergeZone. */
		//HashMap<Integer, Integer> otherNewIndex;
		
		//otherNewIndex = makeIndexMap(otherZone._indexToTimer, mergedZone._indexToTimer);
		
		//mergedZone._matrix = new int[mergedZone.matrixSize()][mergedZone.matrixSize()];
		
		ZoneTriple[] zoneAndIndex = mergeTimers(otherZone);
		
		mergedZone._indexToTimer = new int[zoneAndIndex.length];
		
		for(int i=0; i<zoneAndIndex.length; i++)
		{
			mergedZone._indexToTimer[i] = zoneAndIndex[i]._timer;
		}
		
		// Create the matrix for the merged zone.
		mergedZone._matrix = new int[mergedZone.matrixSize()][mergedZone.matrixSize()];
		
		for(int i=0; i<mergedZone.dbmSize(); i++)
		{
			for(int j=0; j<mergedZone.dbmSize(); j++)
			{
				// If the timer occurs in both zones to be merged,
				// then check if the values agree.
				if(zoneAndIndex[i]._zone2 != null && zoneAndIndex[j]._zone2 != null)
				{
					int value1 = this.getdbm(zoneAndIndex[i]._index1,
							zoneAndIndex[j]._index1);
					
					int value2 = otherZone.getdbm(zoneAndIndex[i]._index2,
							zoneAndIndex[j]._index2);
					
					if(value1 != value2)
					{
						throw new IncompatibleZoneException("The common timers do not agree.");
					}
					else
					{
						mergedZone.setdbm(i, j, value1);
					}
				}
				
				// The timer does not occur in both.
				else
				{
					int iIndex = 0;
					int jIndex = 0;
					Zone z;
					
					// Get the zone with both the timers.
					if(zoneAndIndex[i]._zone2 != null && 
							zoneAndIndex[i]._zone1 == zoneAndIndex[j]._zone1)
					{
						// zoneAndIndex[i] is the timer in a single zone,
						// and is the same as the first zone in zoneAndIndex.
						
						z = zoneAndIndex[i]._zone1;
						iIndex = zoneAndIndex[i]._index1;
						jIndex = zoneAndIndex[j]._index1;
					}
					else if(zoneAndIndex[i]._zone2 != null && 
							zoneAndIndex[i]._zone1 == zoneAndIndex[j]._zone2)
					{
						// zoneAndIndex[i] is the timer in a single zone,
						// and is the same as the second zone in zoneAndIndex.
						
						z = zoneAndIndex[j]._zone1;
						iIndex = zoneAndIndex[i]._index1;
						jIndex = zoneAndIndex[j]._index2;
					}
					else if(zoneAndIndex[j]._zone2 != null && 
							zoneAndIndex[j]._zone1 == zoneAndIndex[i]._zone1)
					{
						// zoneAndIndex[j] is the timer in a single zone,
						// and is the same as the first zone in zoneAndIndex.
						
						z = zoneAndIndex[j]._zone1;
						iIndex = zoneAndIndex[i]._index1;
						jIndex = zoneAndIndex[j]._index1;
					}
					else
					{
						// zoneAndIndex[j] is the timer in a single zone,
						// and is the same as the second zone in zoneAndIndex.
						
						z = zoneAndIndex[j]._zone1;
						iIndex = zoneAndIndex[i]._index2;
						jIndex = zoneAndIndex[j]._index1;
					}
					
					mergedZone.setdbm(iIndex, j, z.getdbm(iIndex, jIndex));
				}
			}
		}
		
		return mergedZone;
	}
	
	/**
	 * Merges the timers arrays to give a single sorted timer array.
	 * @param timer1
	 * 			The first array to merge.
	 * @param timer2
	 * 			The second array to merge.
	 * @return
	 * 			The merged array.
	 */
//	private int[] mergeTimers(int[] timer1, int[] timer2)
//	{
//		/* These integers give the current index of the _indexToTimer. */
//		int thisIndex = 1;
//		int otherIndex = 1;
//		int newIndex = 1;
//		
//		// Define an array for merging the timers. 
//		int[] tempTimer = new int[timer1.length + timer2.length + 1];
//		
//		while(thisIndex<timer1.length && otherIndex<timer2.length)
//		{
//			if(timer1[thisIndex] == timer2[otherIndex])
//			{
//				tempTimer[newIndex] = timer1[thisIndex];
//				
//				thisIndex++;
//				otherIndex++;
//			}
//			else if (timer1[thisIndex]<timer2[otherIndex])
//			{
//				tempTimer[newIndex] = timer1[thisIndex++];
//			}
//			else
//			{
//				tempTimer[newIndex] = timer2[otherIndex++];
//			}
//			
//			newIndex++;
//		}
//		
//		if(thisIndex<timer1.length)
//		{
//			while(thisIndex<timer1.length)
//			{
//				tempTimer[newIndex] = timer1[thisIndex];
//				newIndex++;
//				thisIndex++;
//			}
//		}
//		else if(otherIndex<timer2.length)
//		{
//			while(otherIndex<timer2.length)
//			{
//				tempTimer[newIndex] = timer2[otherIndex];
//				newIndex++;
//				otherIndex++;
//			}
//		}
//		
//		int[] newTimer = new int[newIndex];
//		
//		for(int i=1; i<newIndex; i++)
//		{
//			newTimer[i] = tempTimer[i];
//		}
//		
//		return newTimer;
//	}
	
	private ZoneTriple[] mergeTimers(Zone otherZone)
	{
		/* These integers give the current index of the _indexToTimer. */
		int thisIndex = 1;
		int otherIndex = 1;
		int newIndex = 1;
		
		int thisTimerLength = this._indexToTimer.length;
		int timer2length = otherZone._indexToTimer.length;
		
		// Define an array for merging the timers.  
		ZoneTriple[] tempTimer = new ZoneTriple[thisTimerLength + timer2length + 1];
		
		// The zero is in common to both zones.
		tempTimer[0] = new ZoneTriple(0, this, 0, otherZone, 0);
		
		while(thisIndex<thisTimerLength && otherIndex<timer2length)
		{
			if(this._indexToTimer[thisIndex] == otherZone._indexToTimer[otherIndex])
			{
				tempTimer[newIndex] = new ZoneTriple(this._indexToTimer[thisIndex],
						this, thisIndex, otherZone, otherIndex);
				
				thisIndex++;
				otherIndex++;
			}
			else if (this._indexToTimer[thisIndex]<otherZone._indexToTimer[otherIndex])
			{
				tempTimer[newIndex] = new ZoneTriple(this._indexToTimer[thisIndex],
						this ,thisIndex);
				thisIndex++;
			}
			else
			{
				tempTimer[newIndex] = new ZoneTriple(otherZone._indexToTimer[otherIndex],
						otherZone, otherIndex);
				
				otherIndex++;
			}
			
			newIndex++;
		}
		
		if(thisIndex<thisTimerLength)
		{
			while(thisIndex<thisTimerLength)
			{
				tempTimer[newIndex] = new ZoneTriple(this._indexToTimer[thisIndex],
						this, thisIndex);
				newIndex++;
				thisIndex++;
			}
		}
		else if(otherIndex<timer2length)
		{
			while(otherIndex<timer2length)
			{
				tempTimer[newIndex] = new ZoneTriple(otherZone._indexToTimer[otherIndex],
						otherZone, otherIndex);
				newIndex++;
				otherIndex++;
			}
		}
		
		ZoneTriple[] newTimer = new ZoneTriple[newIndex];
		
		for(int i=0; i<newIndex; i++)
		{
			newTimer[i] = tempTimer[i];
		}
		return newTimer;
	}
	
	/**
	 * Merges two Zones.
	 * @param firstZone
	 * 				The first Zone to merge.
	 * @param secondZone
	 * 				The second Zone to merge.
	 * @return
	 * 				The merged Zone.
	 */
	public static Zone mergeZones(Zone firstZone, Zone secondZone)
	{
		return firstZone.mergeZones(secondZone);
	}
	
	/**
	 * Maps the indices of a value in one integer array to their indices in another
	 * integer array.
	 * Precondition: The elements in the baseTimers array should be in the newTimers
	 * array.
	 * @param baseTimers
	 * 				The old order for the integer array.
	 * @param newTimers
	 * 				The new order for the integer array.
	 * @return
	 * 				A HashMap that maps the index of a value in the baseTimers array
	 * 				to its index in the newTimers array.
	 */
	private HashMap<Integer, Integer> makeIndexMap(int[] baseTimers, int[] newTimers)
	{	
		// Map the new index of the timer to the old timer.
		HashMap<Integer, Integer> newIndex = new HashMap<Integer, Integer>();
		
		// For the old index, find the new index.
		for(int i=0; i<baseTimers.length; i++)
		{
			newIndex.put(i, Arrays.binarySearch(newTimers, baseTimers[i]));
		}
				
		return newIndex;
	}
	
	/**
	 * Advances time.
	 */
	private void advance()
	{
		for(int i=0; i<dbmSize(); i++)
		{
			_matrix[dbmIndexToMatrixIndex(0)][dbmIndexToMatrixIndex(i)] = 
				getUpperBoundbydbmIndex(i);
		}
	}
	
	/**
	 * Overrides the clone method from Object.
	 */
	public Zone clone()
	{
		// TODO: Check if finished.
		
		Zone clonedZone = new Zone();
		
		clonedZone._matrix = new int[this.matrixSize()][this.matrixSize()];
		
		for(int i=0; i<this.matrixSize(); i++)
		{
			for(int j=0; j<this.matrixSize(); j++)
			{
				clonedZone._matrix[i][j] = this._matrix[i][j];
			}
		}
		
		clonedZone._indexToTimer = Arrays.copyOf(_indexToTimer, _indexToTimer.length);
		
		clonedZone._hashCode = this._hashCode;
		
		return clonedZone;
	}
	
	/**
	 * Restricts the lower bound of a timer.
	 * 
	 * @param timer
	 * 			The timer to tighten the lower bound.
	 */
	private void restrict(int timer)
	{
		//int dbmIndex = Arrays.binarySearch(_indexToTimer, timer);
		
		_matrix[dbmIndexToMatrixIndex(timer)][dbmIndexToMatrixIndex(0)]
		                                            = getLowerBoundbydbmIndex(timer);
	}
	
	public List<Transition> getEnabledTransitions()
	{
		ArrayList<Transition> enabledTransitions = new ArrayList<Transition>();
		
		// Check if the timer exceeds its lower bound staring with the first nonzero
		// timer.
		for(int i=1; i<_indexToTimer.length; i++)
		{
			if(getdbm(0, i) >= -1 * getLowerBoundbydbmIndex(i))
			{
				enabledTransitions.add(_indexToTransition.get(_indexToTimer[i]));
			}
		}
		
		return enabledTransitions;
	}
	
	
	/**
	 * The DiagonalNonZeroException extends the java.lang.RuntimerExpcetion. 
	 * The intention is for this exception to be thrown is a Zone has a non zero
	 * entry appear on the diagonal.
	 * 
	 * @author Andrew N. Fisher
	 *
	 */
	public class DiagonalNonZeroException extends java.lang.RuntimeException
	{

		/**
		 * Generated serialVersionUID.
		 */
		private static final long serialVersionUID = -3857736741611605411L;

		/**
		 * Creates a DiagonalNonZeroException.
		 * @param Message
		 * 			The message to be displayed when the exception is thrown.
		 */
		public DiagonalNonZeroException(String Message)
		{
			super(Message);
		}
	}
	
	/**
	 * This exception is thrown when trying to merge two zones whose corresponding timers
	 * do not agree.
	 * @author Andrew N. Fisher
	 *
	 */
	public class IncompatibleZoneException extends java.lang.RuntimeException
	{

		/**
		 * Generated serialVersionUID
		 */
		private static final long serialVersionUID = -2453680267411313227L;
		
		
		public IncompatibleZoneException(String Message)
		{
			super(Message);
		}
	}
	
	/**
	 * TODO
	 * @author Andrew N. Fisher
	 *
	 */
	private class ZoneTriple
	{
		
		// Representation Invariant:
		// If both _zone1 and _zone2 are non null, then this Zone should be 
		// in _zone1.
		
		private int _timer;
		private Zone _zone1;
		private int _index1;
		private Zone _zone2;
		private int _index2;
		
		public ZoneTriple (int timer, Zone zone, int index)
		{
			_timer = timer;
			_zone1 = zone;
			_index1 = index;
		}
		
		public ZoneTriple(int timer, Zone zone1, int index1, Zone zone2, int index2)
		{
			_timer = timer;
			_zone1 = zone1;
			_index1 = index1;
			_zone2 = zone2;
			_index2 = index2;
		}
		
		public Zone get_zone1() {
			return _zone1;
		}

		public void set_zone1(Zone _zone1) {
			this._zone1 = _zone1;
		}

		public int get_index1() {
			return _index1;
		}

		public void set_index1(int _index1) {
			this._index1 = _index1;
		}

		public Zone get_zone2() {
			return _zone2;
		}

		public void set_zone2(Zone _zone2) {
			this._zone2 = _zone2;
		}

		public int get_index2() {
			return _index2;
		}

		public void set_index2(int _index2) {
			this._index2 = _index2;
		}

		public int get_timer() {
			return _timer;
		}
		public void set_timer(int _timer) {
			this._timer = _timer;
		}
		
		public String toString()
		{	
			String result= "";
			
			result = "Timer : " + _timer + "\n";
			
			if(_zone2 == null)
			{
				result += "In single zone : \n";
				result += "********************************\n";
				result += _zone1 + "\n";
				result += "++++++++++++++++++++++++++++++++\n";
				result += "Index : " + _index1 + "\n";
				result += "********************************\n";
			}
			else
			{
				result += "In both zones : \n";
				result += "***First Zone*******************\n";
				result += _zone1 + "\n";
				result += "++++++++++++++++++++++++++++++++\n";
				result += "Index : " + _index1 + "\n";
				result += "********************************\n";
				result += "***Second Zone*******************\n";
				result += _zone2 + "\n";
				result += "++++++++++++++++++++++++++++++++\n";
				result += "Index : " + _index2 + "\n";
				result += "********************************\n";
			}
			
			return result;
		}
	}
	
	/**
	 * Tests ways of splitting a zone.
	 * @param z
	 * 		The Zone to split.
	 * @return
	 * 		True if the splitting method preserved the Zone; false otherwise.
	 */
	private boolean testSplit(Zone z)
	{
		// Get a new copy of the matrix to manipulate.
		int[][] m = z._matrix;
		
		int[][] newMatrix = new int[m.length][m.length];
		
		// Copy the matrix.
		for(int i=0; i<m.length; i++)
		{
			for(int j=0; j<m.length; j++)
			{
				newMatrix[i][j] = m[i][j];
			}
		}
		
		
		boolean evenRow = true;
		
		for(int i=2; i<m.length; i++)
		{
			for(int j=2; j<m.length; j++, evenRow = !evenRow)
			{
				if(i != j && ((j+1 != i && evenRow) || (i+1 != j && !evenRow)))
				{
					newMatrix[i][j] = INFINITY;
				}
			}
		}
		
		int[] timers = Arrays.copyOfRange(z._indexToTimer, 1, z._indexToTimer.length);
		
		Zone newZone = new Zone(timers, newMatrix);
		
		return z.equals(newZone);
	}
	
}